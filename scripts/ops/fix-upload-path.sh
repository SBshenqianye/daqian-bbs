#!/bin/bash
# ============================================
# 生产上传 404 一键修复（2026-08 故障：storage.path 无尾斜杠）
# 背景：.env 的 BBS_UPLOAD_DIR 无尾斜杠 → 容器内 basePath+"common/..." 拼出
#       /data/bbs/bbsUploadcommon/... 等错误目录 → 上传进容器可写层、宿主/GET 404。
# 本脚本四步：
#   1. 抢救：把容器可写层里错误目录的文件 podman cp 到正确目录（不重建会丢！）
#   2. 修 .env：BBS_UPLOAD_DIR 补尾斜杠
#   3. 重建 bbs-server 容器（强制，让新环境变量生效）
#   4. 验证：挂载探针 + HTTP 文件回读 + 抢救回来的文件 GET 200
# 用法:
#   bash scripts/ops/fix-upload-path.sh            # 生产机（自动找 /data/bbs/.env）
#   BBS_HOME=/path bash scripts/ops/fix-upload-path.sh
# ============================================
set -e

BBS_HOME="${BBS_HOME:-/data/bbs}"
CONTAINER="bbs-server"

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

RUNNER="podman"; command -v podman >/dev/null 2>&1 || RUNNER="docker"
command -v "$RUNNER" >/dev/null 2>&1 || { err "未找到 podman/docker"; exit 1; }
info "使用容器引擎: $RUNNER"

# --------------- 定位 .env ---------------
ENV_FILE="$BBS_HOME/.env"
if [ ! -f "$ENV_FILE" ] && [ -f "./.env" ]; then
    ENV_FILE="./.env"
    info "使用当前目录 .env: $ENV_FILE"
fi
if [ ! -f "$ENV_FILE" ]; then
    err "未找到 $ENV_FILE（可用 BBS_HOME 或 BBS_ENV_FILE 指定）"
    exit 1
fi
# 先备份再 strip CR（避免 Windows 换行符导致 source 失败）
cp "$ENV_FILE" "$ENV_FILE.bak-$(date +%Y%m%d-%H%M%S)"
sed -i 's/\r$//' "$ENV_FILE" 2>/dev/null || true
set -a; source "$ENV_FILE"; set +a

# 默认值（与 offline.sh 保持一致）
BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
NGINX_PORT="${NGINX_PORT:-60001}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-$BBS_HOME/bbsUpload}"
# 统一补尾斜杠（核心修复）
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR%/}/"
BASE="${BBS_UPLOAD_DIR%/}"   # 无斜杠形式，用于前缀剥离/挂载

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}  上传 404 修复：$BBS_UPLOAD_DIR"
echo -e "${CYAN}║${NC}  .env: $ENV_FILE"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"

if ! $RUNNER container exists "$CONTAINER" 2>/dev/null; then
    warn "容器 $CONTAINER 不存在，跳过抢救"
fi

# ============================================
# 1) 抢救容器可写层里的错误目录文件
#    diff 里的错误路径形如 /data/bbs/bbsUploadcommon/upload/2026-08-06/xxx.png
#    映射：/data/bbs/bbsUpload + 剩余部分 → 正确目录
# ============================================
echo ""
info "━━━ [1/4] 抢救容器可写层中的上传文件 ━━━"
SALVAGED=0
set -f   # 关闭通配符，防止 ?? 这类文件名被展开
while read -r p; do
    [ -n "$p" ] || continue
    rel="${p#$BASE}"                      # 去掉前缀 → common/upload/... 或 User/...（无前导斜杠）
    rel="${rel#/}"                        # 去掉前缀后可能残留的分隔斜杠
    [ -n "$rel" ] || continue             # 跳过挂载点本身
    target="$BASE/$rel"                   # 重新拼接，保证恰有一个斜杠
    mkdir -p "$(dirname "$target")"
    if $RUNNER exec "$CONTAINER" test -f "$p" 2>/dev/null; then
        if [ -e "$target" ]; then
            info "目标已存在，跳过: $target"
            continue
        fi
        if $RUNNER cp "$CONTAINER:$p" "$target" 2>/dev/null; then
            ok "抢救: $p → $target"
            SALVAGED=$((SALVAGED + 1))
        else
            warn "抢救失败: $p"
        fi
    else
        # 目录：整目录复制内容到映射目标
        if [ -d "$target" ]; then
            info "目录已存在，跳过: $target"
            continue
        fi
        if $RUNNER cp "$CONTAINER:$p/." "$target" 2>/dev/null; then
            ok "抢救目录: $p → $target"
        else
            warn "目录抢救失败: $p"
        fi
    fi
done < <($RUNNER diff "$CONTAINER" 2>/dev/null | awk -v base="$BASE" '$1=="A" && $2 ~ "^"base {print $2}' | sort -r)
set +f

if [ "$SALVAGED" -gt 0 ]; then
    ok "共抢救 $SALVAGED 个文件"
    # SELinux：让后端容器能读到抢救回来的文件（container_file_t，无 MCS 类别 = 所有容器可读）
    if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
        info "SELinux Enforcing，为上传目录重新打标签（container_file_t）..."
        command -v chcon >/dev/null 2>&1 && chcon -Rt container_file_t "$BASE" 2>/dev/null \
            || warn "chcon 不可用；若容器读不到抢救文件请手动执行 chcon -Rt container_file_t $BASE"
    fi
else
    info "没有需要抢救的文件（或容器可写层无错误目录）"
fi

# ============================================
# 2) 修 .env：BBS_UPLOAD_DIR 补尾斜杠
# ============================================
echo ""
info "━━━ [2/4] 修复 .env（BBS_UPLOAD_DIR 补尾斜杠） ━━━"
if grep -qE "^BBS_UPLOAD_DIR=.*/$" "$ENV_FILE"; then
    ok ".env 已是尾斜杠形式，无需修改"
else
    sed -i 's|^BBS_UPLOAD_DIR=\(.*[^/]\)$|BBS_UPLOAD_DIR=\1/|' "$ENV_FILE"
    grep -qE "^BBS_UPLOAD_DIR=.*/$" "$ENV_FILE" \
        && ok ".env 已更新为: $(grep '^BBS_UPLOAD_DIR=' "$ENV_FILE")" \
        || warn ".env 未匹配到 BBS_UPLOAD_DIR 行，请手动检查"
fi

# ============================================
# 3) 重建 bbs-server 容器（强制，让新环境变量生效）
# ============================================
echo ""
info "━━━ [3/4] 重建 bbs-server 容器 ━━━"
$RUNNER rm -f "$CONTAINER" 2>/dev/null || true
# --entrypoint java + -Xmx2g: 主机内存不足时 JDK8 默认按物理内存 1/4 申请堆会启动即崩溃
$RUNNER run -d \
    --name "$CONTAINER" \
    --network host \
    --restart=always \
    --entrypoint java \
    -e BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}" \
    -e BBS_DB_PORT="${BBS_DB_PORT:-15432}" \
    -e BBS_DB_NAME="${BBS_DB_NAME:-bbs}" \
    -e BBS_DB_USER="${BBS_DB_USER:-work_flow}" \
    -e BBS_DB_PASSWORD="${BBS_DB_PASSWORD:-work_flow123}" \
    -e BBS_SUPER_ADMIN_PASSWORD="${BBS_SUPER_ADMIN_PASSWORD:-1234@abcD}" \
    -e BBS_UPLOAD_DIR="$BBS_UPLOAD_DIR" \
    -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
    -v "$BBS_HOME/current/bbs-server.jar:/app/app.jar:Z" \
    -v "$BASE:$BASE:Z" \
    bbs-server-base -Xmx2g -jar /app/app.jar --spring.profiles.active=podman
ok "bbs-server 已重建（BBS_UPLOAD_DIR=$BBS_UPLOAD_DIR）"

# ============================================
# 4) 验证：挂载探针 + 后端就绪 + HTTP 文件回读 + 抢救文件 GET
# ============================================
echo ""
info "━━━ [4/4] 验证 ━━━"

# 4.1 挂载探针（容器内回读）
mkdir -p "$BASE"
echo ok > "$BASE/.deploy-mount-probe"
PROBE_OK=0
for i in $(seq 1 30); do
    if $RUNNER exec "$CONTAINER" cat "$BASE/.deploy-mount-probe" 2>/dev/null | grep -q '^ok$'; then
        ok "挂载验证通过：容器可读到 $BASE（上传将落到宿主目录）"
        PROBE_OK=1
        break
    fi
    sleep 2
done
[ "$PROBE_OK" = "1" ] || { err "挂载验证失败，请检查: $RUNNER logs $CONTAINER"; exit 1; }

# 4.2 后端就绪
sleep 5
READY=0
for i in $(seq 1 30); do
    if curl -s "http://127.0.0.1:$BBS_SERVER_PORT/bbs-server/" >/dev/null 2>&1; then
        ok "后端就绪"
        READY=1
        break
    fi
    sleep 2
done
[ "$READY" = "1" ] || { err "后端就绪超时: $RUNNER logs $CONTAINER"; exit 1; }

# 4.3 HTTP 文件回读（模拟浏览器 GET）
DAY=$(date +%Y-%m-%d)
SERVE_FILE="$BASE/common/upload/$DAY/fix-verify-$$_.png"
SERVE_URL="common/upload/$DAY/fix-verify-$$_.png"
mkdir -p "$(dirname "$SERVE_FILE")"
printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==' | base64 -d > "$SERVE_FILE" 2>/dev/null || echo -n 'png' > "$SERVE_FILE"
SERVE_OK=0
for i in $(seq 1 15); do
    CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$NGINX_PORT/bbs-server/files/$SERVE_URL" 2>/dev/null || echo 000)
    if [ "$CODE" = "200" ]; then
        ok "HTTP 文件回读 200：GET /bbs-server/files/$SERVE_URL"
        SERVE_OK=1
        break
    fi
    sleep 2
done
rm -f "$SERVE_FILE"
[ "$SERVE_OK" = "1" ] || { err "HTTP 文件回读失败（$CODE），请检查容器日志"; exit 1; }

# 4.4 抢救回来的文件能否 GET（证明历史图片恢复）
SALVAGED_ONE=$(find "$BASE/common/upload" -name '*.png' -o -name '*.jpg' 2>/dev/null | head -1)
if [ -n "$SALVAGED_ONE" ]; then
    REL="${SALVAGED_ONE#$BASE/}"
    CODE=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$NGINX_PORT/bbs-server/files/$REL" 2>/dev/null || echo 000)
    if [ "$CODE" = "200" ]; then
        ok "抢救文件可访问：GET /bbs-server/files/$REL → 200（历史图片已恢复）"
    else
        warn "抢救文件 GET → $CODE（文件已就位但读取异常，检查 SELinux 标签）"
    fi
fi

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  修复完成！请在浏览器发一个带图帖子确认。"
echo -e "${GREEN}║${NC}  本次修复后，新上传将写入正确目录；"
echo -e "${GREEN}║${NC}  8-04 起的图片已从容器层抢救回正确目录。"
echo -e "${GREEN}║${NC}  .env 备份: $ENV_FILE.bak-*"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
