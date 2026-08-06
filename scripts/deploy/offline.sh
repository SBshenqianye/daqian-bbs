#!/bin/bash
# ============================================
# BBS 离线内网部署脚本
#
# 一体化用法（自动解压 + 识别 + 部署 + 清理）:
#   bash scripts/deploy/offline.sh                       # 自动在 dist/ 找最新 tar
#   bash scripts/deploy/offline.sh dist/bbs-full-*.tar.gz
#   bash scripts/deploy/offline.sh /path/to/bbs-upgrade-*.tar.gz
#
# 传统用法（包内自调用仍是 deploy-offline.sh）:
#   bash deploy-offline.sh --install                     # 完整包内执行
#   bash deploy-offline.sh --upgrade upgrade.tar.gz      # 升级
#
# 目录结构（安装后）:
#   /data/bbs/
#   ├── versions/<timestamp>/     # 版本快照
#   │   ├── bbs-server.jar
#   │   ├── bbs-ui/dist/
#   │   └── bbs-admin-ui/dist/
#   ├── versions/latest -> <ts>/
#   ├── current -> versions/latest
#   ├── base-images/
#   ├── .env
#   └── logs/
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BBS_HOME="${BBS_HOME:-/data/bbs}"

# --------------- 颜色 ---------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $*" >&2; }
ok()    { echo -e "${GREEN}[OK]${NC} $*" >&2; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*" >&2; }
err()   { echo -e "${RED}[ERR]${NC} $*" >&2; }

# --------------- 上传目录：准备 + 挂载验证 ---------------
# 背景：老 podman（如 RHEL7 生产机上的 1.4.4）在部分场景下 bind mount 会静默失效，
#       上传写进容器可写层、宿主/nginx 读不到 → 图片 GET 404（2026-08 生产故障根因）。
# 方案：启动前在宿主目录写入探针文件，启动后从容器内回读；读不到即判定挂载失效并退出。
# 注意：本段与 scripts/deploy/wsl.sh、package.sh 生成的 upgrade.sh 保持同步
#       （离线包内 deploy-offline.sh 是独立副本，无共享 lib，必须各自内联）。
STORAGE_PROBE=".deploy-mount-probe"

ensure_upload_dir() {
    local dir="$1"
    [ -n "$dir" ] || { err "ensure_upload_dir: 目录为空"; exit 1; }
    mkdir -p "$dir"
    # RHEL7 + SELinux Enforcing：bind mount 需要 container_file_t 标签，否则容器读不到上传目录
    if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
        info "SELinux Enforcing，为上传目录设置 container_file_t 标签: $dir"
        command -v chcon >/dev/null 2>&1 && chcon -Rt container_file_t "$dir" 2>/dev/null \
            || warn "chcon 不可用或失败；若容器读不到上传目录请手动执行 chcon"
    fi
    echo ok > "$dir/$STORAGE_PROBE"
}

verify_upload_mount() {
    local container="$1" dir="$2" fail_soft="$3"
    local tries=0
    while [ "$tries" -lt 30 ]; do
        if $RUNNER exec "$container" cat "$dir/$STORAGE_PROBE" 2>/dev/null | grep -q '^ok$'; then
            ok "上传目录挂载验证通过: $dir（容器与宿主共享同一目录）"
            return 0
        fi
        tries=$((tries + 1))
        sleep 2
    done
    err "上传目录挂载验证失败：容器内读不到 $dir/$STORAGE_PROBE"
    err "后果：上传会写进容器可写层，图片 GET 全部 404。请检查："
    err "  1) run 命令必须包含 -v $dir:$dir:Z"
    err "  2) SELinux 环境下先执行: chcon -Rt container_file_t $dir 再重建容器"
    err "  3) 验证: $RUNNER exec $container ls -la $dir"
    # soft 模式（--repair 预检用）：返回 1 交给调用方决定重建，不直接退出
    [ "$fail_soft" = "soft" ] && return 1
    exit 1
}

# --------------- HTTP 文件服务自检（端到端，无需登录） ---------------
# 宿主写一个测试图 → 模拟浏览器 GET /bbs-server/files/... 回读（SecurityConfig 中
# /files/** permitAll）。读不到 = 后端容器看不到宿主上传目录（挂载失效）或文件服务
# 异常 → 与生产故障同症状（GET 404）。注意：本段与 scripts/deploy/wsl.sh 保持同步。
write_serve_probe() {
    local dir="$1"
    dir="${dir%/}"
    local day; day=$(date +%Y-%m-%d)
    local ts; ts=$(date +%s)
    local file="$dir/common/upload/$day/bbs-serve-probe-${ts}_.png"
    local url_path="common/upload/$day/bbs-serve-probe-${ts}_.png"
    mkdir -p "$(dirname "$file")"
    # 1x1 透明 PNG（最小合法图片）
    printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==' | base64 -d > "$file" 2>/dev/null \
        || echo -n 'png' > "$file"
    echo "$url_path"
}

verify_http_serve() {
    local nginx_port="$1" url_path="$2"
    local tries=0
    while [ "$tries" -lt 30 ]; do
        local code
        code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$nginx_port/bbs-server/files/$url_path" 2>/dev/null || echo 000)
        if [ "$code" = "200" ]; then
            ok "HTTP 文件自检通过: GET /bbs-server/files/$url_path → 200"
            return 0
        fi
        tries=$((tries + 1))
        sleep 2
    done
    err "HTTP 文件自检失败: GET /bbs-server/files/$url_path → $code（应为 200）"
    err "原因：后端容器读不到宿主上传目录里的文件（bind mount 失效或未挂载）"
    return 1
}

# --------------- 容器运行时检测 ---------------
detect_runner() {
    if command -v podman >/dev/null 2>&1; then
        echo "podman"
    elif command -v docker >/dev/null 2>&1; then
        echo "docker"
    else
        echo ""
    fi
}

usage() {
    cat <<EOF
用法: bash ${0##*/} [tar_path] [选项]

参数:
  tar_path                完整环境包或升级包路径（可选，默认自动查找）

选项:
  --install [tar_path]    强制安装模式（如不传 tar_path 则自动检测）
  --upgrade <tar_path>    强制升级模式（必须指定 tar）
  --repair                修复模式：验证上传目录挂载 + HTTP 文件服务，
                          当前容器异常时自动重建 bbs-server（无需 tar 包）
  -h, --help              帮助信息

示例:
  bash scripts/deploy/offline.sh
  bash scripts/deploy/offline.sh dist/bbs-full-20260715_154304.tar.gz
  bash scripts/deploy/offline.sh --upgrade dist/bbs-upgrade-20260715_154305.tar.gz
  bash deploy-offline.sh --repair   # 生产机图片 404 等场景：验证/重建后端容器

环境变量:
  BBS_HOME                BBS 根目录（默认: /data/bbs）
EOF
    exit 0
}

# --------------- 后端容器启动（安装/修复共用） ---------------
# rm + run + 挂载验证封装：--repair 重建时复用，保证命令与首次安装完全一致
start_bbs_server() {
    info "启动 bbs-server..."
    $RUNNER rm -f "$BBS_SERVER_CONTAINER" 2>/dev/null || true
    # --entrypoint java + -Xmx2g: 主机内存不足时 JDK8 默认按物理内存 1/4 申请堆（如 32G 机器默认 8G）会启动即崩溃
    $RUNNER run -d \
        --name "$BBS_SERVER_CONTAINER" \
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
        -v "$BBS_UPLOAD_DIR:$BBS_UPLOAD_DIR:Z" \
        bbs-server-base -Xmx2g -jar /app/app.jar --spring.profiles.active=podman

    ok "bbs-server 已启动"

    # 挂载验证：容器必须能读到宿主上传目录里的探针文件，
    # 否则上传会写进容器可写层，图片 GET 全部 404（老 podman 静默失效的典型症状）
    verify_upload_mount "$BBS_SERVER_CONTAINER" "$BBS_UPLOAD_DIR"
}

# ============================================
# 首次安装
# ============================================
do_install() {
    local work_dir="$1"  # 提取后的包目录
    RUNNER=$(detect_runner)
    if [ -z "$RUNNER" ]; then
        err "未找到 podman 或 docker，请先安装容器运行时"
        exit 1
    fi
    info "使用容器引擎: $RUNNER"

    local timestamp; timestamp=$(date +%Y%m%d_%H%M%S)
    local version_dir="$BBS_HOME/versions/$timestamp"

    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  BBS 首次安装"
    echo -e "${CYAN}║${NC}  目标目录: $BBS_HOME"
    echo -e "${CYAN}║${NC}  版本标签: $timestamp"
    echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
    echo ""

    # Step 1: 加载基础镜像
    echo ""
    info "━━━ [1/5] 加载基础镜像 ━━━"
    if [ -d "$work_dir/base" ]; then
        for tar_file in "$work_dir/base/"*.tar; do
            [ -f "$tar_file" ] || continue
            local img_name; img_name=$(basename "$tar_file" .tar)
            if $RUNNER image exists "${img_name}:latest" 2>/dev/null; then
                info "${img_name} 镜像已存在，跳过加载"
            else
                info "加载 $tar_file ..."
                $RUNNER load -i "$tar_file"
                ok "${img_name} 加载完成"
            fi
        done
    else
        warn "base/ 目录不存在，跳过基础镜像加载"
    fi

    # Step 2: 创建目录结构
    echo ""
    info "━━━ [2/5] 创建目录结构 ━━━"
    mkdir -p "$version_dir/bbs-ui" "$version_dir/bbs-admin-ui" \
             "$BBS_HOME/base-images" \
             "$BBS_HOME/logs"

    # Step 3: 复制构建产物
    echo ""
    info "━━━ [3/5] 复制构建产物 ━━━"
    local upgrade_dir
    upgrade_dir=$(find "$work_dir" -maxdepth 1 -type d -name 'upgrade-*' | sort | tail -1)

    if [ -n "$upgrade_dir" ] && [ -d "$upgrade_dir" ]; then
        info "从 $upgrade_dir 复制产物..."
        if [ -f "$upgrade_dir/bbs-server.jar" ]; then
            cp "$upgrade_dir/bbs-server.jar" "$version_dir/"
        fi
        if [ -d "$upgrade_dir/bbs-ui/dist" ]; then
            cp -r "$upgrade_dir/bbs-ui/dist" "$version_dir/bbs-ui/"
        fi
        if [ -d "$upgrade_dir/bbs-admin-ui/dist" ]; then
            cp -r "$upgrade_dir/bbs-admin-ui/dist" "$version_dir/bbs-admin-ui/"
        fi
        ok "构建产物已复制"
    else
        warn "包内未找到 upgrade-* 目录，跳过产物复制"
    fi

    # Step 4: 环境变量配置
    echo ""
    info "━━━ [4/5] 环境变量配置 ━━━"
    if [ ! -f "$BBS_HOME/.env" ]; then
        if [ -f "$work_dir/.env.example" ]; then
            cp "$work_dir/.env.example" "$BBS_HOME/.env"
            info ".env 已创建（使用默认配置）"
            warn "请检查 $BBS_HOME/.env 中的数据库密码等配置"
            echo ""
            echo "  按 Enter 使用默认配置继续..."
            read -r
        else
            warn "未找到 .env.example，跳过"
        fi
    else
        info ".env 已存在，使用现有配置"
    fi

    # Step 5: 启动容器
    echo ""
    info "━━━ [5/5] 启动容器 ━━━"

    # 创建软链
    ln -snf "$timestamp" "$BBS_HOME/versions/latest"
    ln -snf "versions/latest" "$BBS_HOME/current"

    # 载入 .env（先 strip CR，避免 Windows 换行符导致 source 失败）
    sed -i 's/\r$//' "$BBS_HOME/.env" 2>/dev/null || true
    set -a; source "$BBS_HOME/.env"; set +a

    # 检查密码是否仍为占位符
    if [ "$BBS_DB_PASSWORD" = "your_password_here" ]; then
        echo ""
        echo -e "${RED}╔══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${RED}║${NC}  ⚠  数据库密码错误！                                          ${RED}║${NC}"
        echo -e "${RED}║${NC}                                                                  ${RED}║${NC}"
        echo -e "${RED}║${NC}  BBS_DB_PASSWORD 仍是占位符 your_password_here，未被修改。        ${RED}║${NC}"
        echo -e "${RED}║${NC}  部署后数据库连接会失败，容器将反复重启。                          ${RED}║${NC}"
        echo -e "${RED}║${NC}                                                                  ${RED}║${NC}"
        echo -e "${RED}║${NC}  请编辑 $BBS_HOME/.env，将数据库密码改为实际值：       ${RED}║${NC}"
        echo -e "${RED}║${NC}                                                                  ${RED}║${NC}"
        echo -e "${RED}║${NC}    sed -i 's/your_password_here/你真正的密码/' $BBS_HOME/.env  ${RED}║${NC}"
        echo -e "${RED}║${NC}                                                                  ${RED}║${NC}"
        echo -e "${RED}║${NC}  改完后重新执行本脚本即可。                                      ${RED}║${NC}"
        echo -e "${RED}╚══════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        err "密码未配置，部署中止"
        exit 1
    fi

    # 默认值
    BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
    NGINX_PORT="${NGINX_PORT:-60001}"
    BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-$BBS_HOME/bbsUpload}"
    BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"
    BBS_NGINX_CONTAINER="${BBS_NGINX_CONTAINER:-bbs-nginx}"

    # 确保上传目录存在（含 SELinux 标签 + 挂载探针）
    ensure_upload_dir "$BBS_UPLOAD_DIR"

    # 后端（rm + run + 挂载验证封装在 start_bbs_server，--repair 复用同一命令）
    start_bbs_server

    # Nginx
    info "启动 bbs-nginx..."
    $RUNNER rm -f "$BBS_NGINX_CONTAINER" 2>/dev/null || true
    $RUNNER run -d \
        --name "$BBS_NGINX_CONTAINER" \
        --network host \
        --restart=always \
        -e NGINX_PORT="$NGINX_PORT" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        -v "$BBS_HOME/current/bbs-ui/dist:/usr/share/nginx/html/bbs-ui:Z" \
        -v "$BBS_HOME/current/bbs-admin-ui/dist:/usr/share/nginx/html/bbs-admin:Z" \
        bbs-nginx-base

    ok "bbs-nginx 已启动"

    # 完成
    echo ""
    echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║${NC}  安装完成！"
    echo -e "${GREEN}║${NC}  版本: $timestamp"
    echo -e "${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  用户前端:  http://localhost:${NGINX_PORT}/bbs-user/"
    echo -e "${GREEN}║${NC}  管理后台:  http://localhost:${NGINX_PORT}/bbs-admin/"
    echo -e "${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  升级命令:"
    echo -e "${GREEN}║${NC}    bash $BBS_HOME/deploy-offline.sh --upgrade <upgrade-tar>"
    echo -e "${GREEN}║${NC}"
    echo -e "${GREEN}║${NC}  回滚命令:"
    echo -e "${GREEN}║${NC}    cd $BBS_HOME && ls versions/"
    echo -e "${GREEN}║${NC}    ln -snf versions/<旧版本> versions/latest"
    echo -e "${GREEN}║${NC}    $RUNNER restart $BBS_SERVER_CONTAINER $BBS_NGINX_CONTAINER"
    echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
}

# ============================================
# 修复模式：验证当前容器 → 异常才重建 → 端到端自检
# 用于图片 404 等挂载异常场景。先无破坏性验证（探针 + HTTP 回读），
# 当前容器健康则直接证明可用，不重建；坏了才重建并再次验证。
# 用法: bash deploy-offline.sh --repair   （不需要 tar 包，读取 $BBS_HOME/.env）
# ============================================
do_repair() {
    RUNNER=$(detect_runner)
    if [ -z "$RUNNER" ]; then
        err "未找到 podman 或 docker，请先安装容器运行时"
        exit 1
    fi
    info "使用容器引擎: $RUNNER"

    if [ ! -f "$BBS_HOME/.env" ]; then
        err "未找到 $BBS_HOME/.env，请确认部署目录（可用 BBS_HOME 覆盖，默认 /data/bbs）"
        exit 1
    fi
    # 载入 .env（先 strip CR，避免 Windows 换行符导致 source 失败）
    sed -i 's/\r$//' "$BBS_HOME/.env" 2>/dev/null || true
    set -a; source "$BBS_HOME/.env"; set +a

    # 默认值（与 do_install 保持一致）
    BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
    NGINX_PORT="${NGINX_PORT:-60001}"
    BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-$BBS_HOME/bbsUpload}"
    BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"

    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  BBS 修复模式（验证 + 必要时重建）"
    echo -e "${CYAN}║${NC}  上传目录: $BBS_UPLOAD_DIR"
    echo -e "${CYAN}║${NC}  后端端口: $BBS_SERVER_PORT   Nginx: $NGINX_PORT"
    echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
    echo ""

    ensure_upload_dir "$BBS_UPLOAD_DIR"

    # 1) 无破坏预检：当前容器挂载 + HTTP 文件服务
    local need_rebuild=0
    if $RUNNER container exists "$BBS_SERVER_CONTAINER" 2>/dev/null; then
        info "第一步：无破坏验证当前容器..."
        if verify_upload_mount "$BBS_SERVER_CONTAINER" "$BBS_UPLOAD_DIR" soft; then
            local pre_url; pre_url=$(write_serve_probe "$BBS_UPLOAD_DIR")
            if verify_http_serve "$NGINX_PORT" "$pre_url"; then
                rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$pre_url")"
                ok "当前容器健康：挂载正常 + HTTP 文件服务正常，无需重建"
                return 0
            fi
            rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$pre_url")"
        fi
        warn "当前容器存在异常，进入重建修复..."
        need_rebuild=1
    else
        warn "后端容器不存在，直接重建..."
        need_rebuild=1
    fi

    # 2) 重建后端容器（同一命令保证挂载参数一致）
    start_bbs_server

    # 3) 等待就绪 + 端到端自检
    info "等待后端就绪..."
    sleep 5
    local health_start; health_start=$(date +%s)
    local max_attempts=30
    local i
    for i in $(seq 1 "$max_attempts"); do
        if curl -s http://127.0.0.1:$BBS_SERVER_PORT/bbs-server/ >/dev/null 2>&1; then
            ok "后端就绪！(${i}次)"
            break
        fi
        if [ "$i" = "$max_attempts" ]; then
            err "后端就绪超时，请检查: $RUNNER logs $BBS_SERVER_CONTAINER"
            exit 1
        fi
        sleep 2
    done

    local url; url=$(write_serve_probe "$BBS_UPLOAD_DIR")
    if verify_http_serve "$NGINX_PORT" "$url"; then
        rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$url")"
        if [ "$need_rebuild" = "1" ]; then
            ok "修复完成：容器已重建，挂载验证与 HTTP 文件服务验证全部通过"
        fi
        return 0
    fi
    rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$url")"
    err "修复失败：重建后文件服务仍异常，请检查: $RUNNER logs $BBS_SERVER_CONTAINER"
    return 1
}

# ============================================
# 主流程 — 解压 + 自动识别 + 部署 + 清理
# ============================================
main() {
    local TAR_PATH=""
    local FORCE_MODE=""
    local CLEANUP_DIR=""

    # --- 参数解析 ---
    case "${1:-}" in
        --install|install)
            FORCE_MODE="install"
            TAR_PATH="${2:-}"
            ;;
        --repair|repair)
            FORCE_MODE="repair"
            ;;
        --upgrade|upgrade)
            FORCE_MODE="upgrade"
            TAR_PATH="${2:-}"
            if [ -z "$TAR_PATH" ]; then
                err "--upgrade 需要指定 tar 包路径"
                usage
            fi
            if [ ! -f "$TAR_PATH" ]; then
                err "文件不存在: $TAR_PATH"
                exit 1
            fi
            ;;
        -h|--help|help)
            usage
            ;;
        "")
            # 无参数，稍后自动检测
            TAR_PATH=""
            FORCE_MODE="upgrade"
            ;;
        *)
            # 认作 tar 路径
            if [ -f "$1" ]; then
                TAR_PATH="$1"
                FORCE_MODE="install"
            else
                err "文件不存在或未知参数: $1"
                usage
            fi
            ;;
    esac

    # --- 自动查找 tar（无参数时在 dist/ 里找最新的；--repair 不需要 tar） ---
    if [ -z "$TAR_PATH" ] && [ "$FORCE_MODE" != "repair" ]; then
        # 按模式匹配包名：upgrade 模式找 upgrade 包，install 模式找 full 包
        local search_pattern
        case "$FORCE_MODE" in
            upgrade) search_pattern="bbs-upgrade-*.tar.gz" ;;
            install) search_pattern="bbs-full-*.tar.gz" ;;
            *)       search_pattern="*.tar.gz" ;;
        esac
        local search_dirs=("$SCRIPT_DIR" "$SCRIPT_DIR/../../dist" "$SCRIPT_DIR/../dist" "dist")
        for d in "${search_dirs[@]}"; do
            d="$(cd "$d" 2>/dev/null && pwd)" || continue
            local latest; latest=$(ls -t "$d/$search_pattern" 2>/dev/null | head -1)
            if [ -n "$latest" ]; then
                TAR_PATH="$latest"
                info "自动检测到: $(basename "$latest")"
                break
            fi
        done
    fi

    # --- 解压 tar ---
    if [ -n "$TAR_PATH" ] && [ -f "$TAR_PATH" ]; then
        CLEANUP_DIR="$(mktemp -d)"
        info "解压 $(basename "$TAR_PATH") ..."
        tar -xzf "$TAR_PATH" -C "$CLEANUP_DIR"

        # 找到包内的顶层目录
        local inner_dir
        inner_dir=$(find "$CLEANUP_DIR" -maxdepth 1 -type d ! -path "$CLEANUP_DIR" | head -1)
        if [ -n "$inner_dir" ]; then
            WORK_DIR="$inner_dir"
        else
            WORK_DIR="$CLEANUP_DIR"
        fi

        # 注册清理
        trap 'rm -rf "$CLEANUP_DIR"; ok "临时文件已清理"' EXIT
    else
        # 没有 tar：传统用法，脚本所在目录就是包目录
        WORK_DIR="$SCRIPT_DIR"
    fi

    # --- 自动识别模式 ---
    if [ -z "$FORCE_MODE" ]; then
        if [ -d "$WORK_DIR/base" ] && ls "$WORK_DIR/base/"*.tar 2>/dev/null | grep -q .; then
            FORCE_MODE="install"
            info "识别为: 完整环境包 → 安装模式"
        elif ls "$WORK_DIR"/upgrade-*/upgrade.sh 2>/dev/null | grep -q .; then
            FORCE_MODE="upgrade"
            info "识别为: 升级包 → 升级模式"
        else
            err "无法自动识别包类型（未找到 base/*.tar 或 upgrade-*/upgrade.sh）"
            info "当前目录: $WORK_DIR"
            info "文件列表: $(ls -la "$WORK_DIR" 2>/dev/null | head -20)"
            exit 1
        fi
    fi

    # --- 执行 ---
    case "$FORCE_MODE" in
        install)
            do_install "$WORK_DIR"
            ;;
        repair)
            do_repair
            ;;
        upgrade)
            local ug_dir
            ug_dir=$(find "$WORK_DIR" -maxdepth 1 -type d -name 'upgrade-*' | head -1)
            if [ -z "$ug_dir" ] || [ ! -f "$ug_dir/upgrade.sh" ]; then
                err "升级包内未找到 upgrade.sh"
                exit 1
            fi
            info "执行升级: $(basename "$ug_dir")"
            BBS_HOME="$BBS_HOME" bash "$ug_dir/upgrade.sh"
            ;;
    esac
}

main "$@"
