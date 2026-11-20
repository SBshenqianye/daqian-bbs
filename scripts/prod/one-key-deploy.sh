#!/bin/bash
# ============================================
# 一键部署：打包 → 产物验证 → 部署 → 部署后验证
#
# 两种运行场景，自动识别，通常不用填任何参数：
#
#   【生产服务器】脚本位于 /data/bbs/scripts/prod/
#     cd /data/bbs/scripts/prod && bash one-key-deploy.sh
#     → 自动取 /data/bbs/bbs-upgrade-*.tar.gz（最新的那个）部署
#     → 部署 + 验证成功后，自动把 tar 移入 /data/bbs/archive/
#
#   【开发机 / WSL】脚本位于 <repo>/scripts/prod/
#     ./deploy.sh
#     → 打包 → 产物验证 → 部署到 WSL → 探活
#
# 选项（一般不需要）:
#   -t, --target wsl|prod   强制指定目标（也可直接写位置参数 wsl|prod|w|p）
#   --tar <path>            指定升级包，默认自动查找
#   -y, --yes               跳过生产部署前的 3 秒倒计时
#   --skip-package          复用已有升级包，不重新打包（开发机）
#   --skip-verify           跳过部署后验证（不推荐）
#   -n, --dry-run           只预检并打印计划，不打包/不部署
#   -h, --help              帮助
#
# 环境变量: BBS_HOME（生产根目录，默认脚本所在仓库根）、BBS_TARGET、BBS_SERVER_PORT、NGINX_PORT
# 退出码: 0=全部通过  1=有失败项
# ============================================
set -u

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

TARGET=""
TAR_PATH=""
ASSUME_YES=0
SKIP_PACKAGE=0
SKIP_VERIFY=0
DRY_RUN=0
ARCHIVED_TAR=""

BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
NGINX_PORT="${NGINX_PORT:-60001}"

C_G='\033[0;32m'; C_Y='\033[1;33m'; C_R='\033[0;31m'; C_C='\033[0;36m'; C_N='\033[0m'
STEP=0
TOTAL=4
PREV_LATEST=""
START_TS=$(date +%s)

info() { echo -e "${C_C}[INFO]${C_N} $*"; }
ok()   { echo -e "${C_G}[OK]${C_N} $*"; }
warn() { echo -e "${C_Y}[WARN]${C_N} $*"; }
err()  { echo -e "${C_R}[ERR]${C_N} $*"; }
step() { STEP=$((STEP + 1)); echo ""; echo -e "${C_C}━━━ [$STEP/$TOTAL] $* ━━━${C_N}"; }

usage() { sed -n '2,/^set -u/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0; }

fail() {
    echo ""
    err "$*"
    echo ""
    if [ -n "$PREV_LATEST" ]; then
        err "如需回滚（生产）："
        echo "    cd $BBS_HOME && ln -snf $(basename "$PREV_LATEST") versions/latest && podman restart bbs-server bbs-nginx"
    fi
    err "耗时 $(( $(date +%s) - START_TS ))s，流程中止。"
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        --target|-t)   TARGET="${2:-}"; shift 2 ;;
        --target=*)    TARGET="${1#*=}"; shift ;;
        --tar)         TAR_PATH="${2:-}"; shift 2 ;;
        --tar=*)       TAR_PATH="${1#*=}"; shift ;;
        --yes|-y)      ASSUME_YES=1; shift ;;
        --skip-package) SKIP_PACKAGE=1; shift ;;
        --skip-verify) SKIP_VERIFY=1; shift ;;
        --dry-run|-n)  DRY_RUN=1; shift ;;
        -h|--help)     usage ;;
        wsl|prod|w|p)  TARGET="$1"; shift ;;
        *) err "未知参数: $1（--help 查看用法）"; exit 1 ;;
    esac
done

# ---------- 目标识别 ----------
# 生产服务器：/data/bbs 是项目根，没有 bbs-server/pom.xml，但有 versions/ archive/ .env
detect_target() {
    if [ -n "${BBS_TARGET:-}" ]; then echo "$BBS_TARGET"; return; fi
    if [ -f "$ROOT_DIR/bbs-server/pom.xml" ]; then echo wsl; return; fi
    if [ -d "$ROOT_DIR/versions" ] || [ -d "$ROOT_DIR/archive" ] || [ -f "$ROOT_DIR/.env" ]; then
        echo prod; return
    fi
    echo wsl
}

[ -z "$TARGET" ] && TARGET=$(detect_target)
case "$TARGET" in
    w|wsl)  TARGET="wsl";  TOTAL=4 ;;
    p|prod) TARGET="prod"; TOTAL=3 ;;
    *) err "--target 只能是 wsl 或 prod（当前: $TARGET）"; exit 1 ;;
esac

if [ -z "${BBS_HOME:-}" ]; then
    if [ "$TARGET" = "prod" ]; then BBS_HOME="$ROOT_DIR"; else BBS_HOME="/data/bbs"; fi
fi

# ---------- 预检 ----------
preflight() {
    info "仓库: $ROOT_DIR"
    info "目标: $TARGET（BBS_HOME=$BBS_HOME）"

    command -v curl >/dev/null 2>&1 || fail "缺少命令: curl"
    if ! command -v podman >/dev/null 2>&1 && ! command -v docker >/dev/null 2>&1; then
        fail "未找到 podman/docker，无法部署"
    fi

    local commit branch dirty
    commit=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
    branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)
    dirty=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
    info "提交: $commit ($branch)，未提交改动: $dirty 个文件"

    if [ "$TARGET" = "prod" ]; then
        [ -d "$BBS_HOME" ] || { [ "$DRY_RUN" = "1" ] && warn "生产目录不存在: $BBS_HOME" || fail "生产目录不存在: $BBS_HOME"; }

        # 同目录依赖：缺文件时提前报错，别等部署到一半才失败
        local need
        for need in scripts/prod/offline.sh scripts/prod/verify-server.sh; do
            [ -f "$ROOT_DIR/$need" ] || fail "缺少 $need：请把整个 scripts/prod/ 目录同步到服务器"
        done

        # 升级包：默认取项目根最新的 bbs-upgrade-*.tar.gz（兼容 dist/）
        if [ -z "$TAR_PATH" ]; then
            TAR_PATH=$(ls -t "$ROOT_DIR"/bbs-upgrade-*.tar.gz 2>/dev/null | head -1)
            [ -z "$TAR_PATH" ] && TAR_PATH=$(ls -t "$ROOT_DIR"/dist/bbs-upgrade-*.tar.gz 2>/dev/null | head -1)
        fi
        [ -n "$TAR_PATH" ] && [ -f "$TAR_PATH" ] || {
            local archived
            archived=$(ls -1 "$ROOT_DIR"/archive/bbs-upgrade-*.tar.gz 2>/dev/null | wc -l | tr -d ' ')
            if [ "$archived" != "0" ] && [ "$archived" != "" ]; then
                warn "archive/ 下有 $archived 个已归档包；如需重发，移回根目录或用 --tar 指定"
            fi
            fail "未找到升级包：$ROOT_DIR/bbs-upgrade-*.tar.gz"
        }

        local multi
        multi=$(ls -1 "$ROOT_DIR"/bbs-upgrade-*.tar.gz 2>/dev/null | wc -l | tr -d ' ')
        [ "$multi" -gt 1 ] && warn "根目录有 $multi 个升级包，使用最新的: $(basename "$TAR_PATH")"
        info "升级包: $TAR_PATH"

        # 有 .sha256 就自动核对
        if [ -f "$TAR_PATH.sha256" ] && command -v sha256sum >/dev/null 2>&1; then
            local expected actual
            expected=$(tr -d '[:space:]' < "$TAR_PATH.sha256" | tr '[:lower:]' '[:upper:]')
            actual=$(sha256sum "$TAR_PATH" | awk '{print toupper($1)}')
            if [ "$expected" = "$actual" ]; then
                ok "SHA256 校验通过"
            else
                fail "SHA256 不匹配（期望 $expected，实际 $actual）"
            fi
        fi

        PREV_LATEST=$(readlink -f "$BBS_HOME/versions/latest" 2>/dev/null || true)
        [ -n "$PREV_LATEST" ] && info "当前线上版本: $(basename "$PREV_LATEST")" || warn "当前无 versions/latest（首次部署）"
        return 0
    fi

    # 开发机：需要构建产物且不能过期
    for bin in python3 tar; do
        command -v "$bin" >/dev/null 2>&1 || fail "缺少命令: $bin"
    done

    local missing=0
    [ -f "bbs-server/target/bbs-server.jar" ] || { warn "缺少 bbs-server/target/bbs-server.jar"; missing=1; }
    [ -d "bbs-ui/dist" ]                       || { warn "缺少 bbs-ui/dist"; missing=1; }
    [ -d "bbs-admin-ui/dist" ]                 || { warn "缺少 bbs-admin-ui/dist"; missing=1; }
    [ "$missing" = "0" ] || fail "构建产物缺失：请先在 Windows 执行两个前端的 npm run build 与 cd bbs-server && mvn clean package -DskipTests"

    local newest_src stale=0
    newest_src=$(find bbs-server/src bbs-server/pom.xml -type f -newer bbs-server/target/bbs-server.jar 2>/dev/null | head -1)
    [ -n "$newest_src" ] && { warn "后端 jar 早于源码（如 $newest_src），可能未重新打包"; stale=1; }
    newest_src=$(find bbs-ui/src -type f -newer bbs-ui/dist/index.html 2>/dev/null | head -1)
    [ -n "$newest_src" ] && { warn "bbs-ui/dist 早于源码（如 $newest_src）"; stale=1; }
    newest_src=$(find bbs-admin-ui/src -type f -newer bbs-admin-ui/dist/index.html 2>/dev/null | head -1)
    [ -n "$newest_src" ] && { warn "bbs-admin-ui/dist 早于源码（如 $newest_src）"; stale=1; }

    if [ "$stale" = "1" ] && [ "$ASSUME_YES" != "1" ] && [ -t 0 ]; then
        echo ""
        read -r -p "产物可能过期，继续部署？(y/N) " ans
        [[ "$ans" =~ ^[Yy]$ ]] || fail "已取消"
    fi
}

# ---------- 生产：部署前提醒（零参数也不误触） ----------
prod_countdown() {
    [ "$TARGET" = "prod" ] || return 0
    [ "$ASSUME_YES" = "1" ] && return 0
    [ -t 0 ] || return 0
    echo ""
    echo -e "${C_Y}即将部署到生产（$BBS_HOME），容器会重启；成功后将归档升级包。${C_N}"
    echo -e "${C_Y}3 秒后开始，Ctrl+C 取消...${C_N}"
    sleep 3
}

# ---------- 1. 打包（仅开发机） ----------
do_package() {
    [ "$TARGET" = "wsl" ] || return 0
    step "打包（轻量升级包）"
    if [ "$SKIP_PACKAGE" = "1" ]; then
        warn "已跳过打包（--skip-package）"
    else
        bash scripts/prod/package.sh || fail "打包失败"
    fi

    if [ -z "$TAR_PATH" ]; then
        TAR_PATH=$(ls -t dist/bbs-upgrade-*.tar.gz 2>/dev/null | head -1)
    fi
    [ -n "$TAR_PATH" ] && [ -f "$TAR_PATH" ] || fail "未找到升级包（dist/bbs-upgrade-*.tar.gz）"
    ok "升级包: $TAR_PATH"
}

# ---------- 2. 产物验证（仅开发机） ----------
do_verify_build() {
    [ "$TARGET" = "wsl" ] || return 0
    step "产物验证（verify-build）"
    bash scripts/prod/verify-build.sh || fail "产物验证未通过，已停止部署"
}

# ---------- 3. 部署 ----------
do_deploy() {
    if [ "$TARGET" = "prod" ]; then
        step "部署（生产离线升级）"
        prod_countdown
        bash scripts/prod/offline.sh --upgrade "$TAR_PATH" || fail "生产部署失败"
    else
        step "部署（WSL）"
        bash scripts/dev/wsl.sh || fail "WSL 部署失败"
    fi
}

# ---------- 4. 验证 + 归档 ----------
do_verify_runtime() {
    if [ "$TARGET" = "prod" ]; then
        step "部署后验证并归档"
    else
        step "部署后验证"
    fi

    local base="http://127.0.0.1:${NGINX_PORT}"
    local i code
    for i in $(seq 1 30); do
        code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
            "${base}/bbs-server/common/article/getNewest" 2>/dev/null) || code=000
        [ "$code" = "200" ] && break
        [ "$i" = "1" ] && info "等待服务就绪（最多 60s）..."
        sleep 2
    done

    local code_api code_user code_admin
    code_api=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "${base}/bbs-server/common/article/getNewest" 2>/dev/null) || code_api=000
    code_user=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "${base}/bbs-user/" 2>/dev/null) || code_user=000
    code_admin=$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 "${base}/bbs-admin/" 2>/dev/null) || code_admin=000

    echo "  API      ${base}/bbs-server/  → ${code_api}"
    echo "  用户端   ${base}/bbs-user/    → ${code_user}"
    echo "  管理端   ${base}/bbs-admin/   → ${code_admin}"

    [ "$code_api" = "200" ]   || fail "后端 API 未返回 200（实际 $code_api）"
    [ "$code_user" = "200" ]  || fail "用户端未返回 200（实际 $code_user）"
    [ "$code_admin" = "200" ] || fail "管理端未返回 200（实际 $code_admin）"

    if [ "$TARGET" = "prod" ]; then
        info "运行生产验证脚本 verify-server.sh"
        BBS_HOME="$BBS_HOME" BBS_SERVER_PORT="$BBS_SERVER_PORT" \
            bash scripts/prod/verify-server.sh || fail "verify-server.sh 未通过，升级包保留在根目录，未归档"
        archive_package
    fi
    ok "部署后验证通过"
}

# ---------- 归档（仅验证成功后调用） ----------
archive_package() {
    local dir="$ROOT_DIR/archive"
    mkdir -p "$dir" || fail "无法创建归档目录: $dir"
    [ -f "$TAR_PATH" ] || return 0
    mv "$TAR_PATH" "$dir/" || fail "归档失败: $TAR_PATH"
    [ -f "$TAR_PATH.sha256" ] && mv "$TAR_PATH.sha256" "$dir/" 2>/dev/null
    ARCHIVED_TAR="$dir/$(basename "$TAR_PATH")"
    ok "已归档: $ARCHIVED_TAR"
}

# ---------- 主流程 ----------
echo ""
echo -e "${C_C}╔══════════════════════════════════════════════╗${C_N}"
echo -e "${C_C}║${C_N}  一键部署：打包 → 验证 → 部署 → 再验证      ${C_C}║${C_N}"
echo -e "${C_C}╚══════════════════════════════════════════════╝${C_N}"

echo ""
echo -e "${C_C}━━━ [预检] ━━━${C_N}"
preflight

if [ "$DRY_RUN" = "1" ]; then
    echo ""
    ok "预检通过（--dry-run，未执行打包/部署）"
    if [ "$TARGET" = "prod" ]; then
        echo "  计划: 1) 部署 $TAR_PATH  2) 探活  3) verify-server.sh  4) 归档到 $ROOT_DIR/archive/"
    else
        echo "  计划: 1) package  2) verify-build  3) deploy(wsl)  4) verify-runtime"
    fi
    exit 0
fi

do_package
do_verify_build
do_deploy
do_verify_runtime

echo ""
echo -e "${C_G}╔══════════════════════════════════════════════╗${C_N}"
echo -e "${C_G}║${C_N}  一键部署完成（$(( $(date +%s) - START_TS ))s）              ${C_G}║${C_N}"
echo -e "${C_G}╚══════════════════════════════════════════════╝${C_N}"
if [ -n "$ARCHIVED_TAR" ]; then
    echo "  已归档: $ARCHIVED_TAR"
else
    echo "  升级包: $TAR_PATH"
    [ -f "$TAR_PATH.sha256" ] && echo "  SHA256: $(cat "$TAR_PATH.sha256")"
fi
[ -n "$PREV_LATEST" ] && echo "  上一版本: $(basename "$PREV_LATEST")（回滚: cd $BBS_HOME && ln -snf $(basename "$PREV_LATEST") versions/latest && podman restart bbs-server bbs-nginx）"
echo ""
exit 0
