#!/bin/bash
# ============================================
# BBS WSL 部署脚本
# 将本地构建产物（JAR + dist）通过 bind mount 部署到容器。
# 支持"开发机 build，WSL 只 deploy"的工作流。
#
# 前置条件:
#   1. PostgreSQL 数据库已运行并可访问
#   2. 产物已构建: bbs-server/target/bbs-server.jar + dist 目录
#      （可在开发机 Windows 上 build 后同步过来）
#
# 用法:
#   bash scripts/deploy/wsl.sh                       # 检测产物 + 部署
#   bash scripts/deploy/wsl.sh --build               # 强制重编 + 部署
#   bash scripts/deploy/wsl.sh --restart-only        # 仅重启容器
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-deploy}"  # deploy | build | restart-only

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1" >&2; }
ok()    { echo -e "${GREEN}[OK]${NC} $1" >&2; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1" >&2; }
err()   { echo -e "${RED}[ERR]${NC} $1" >&2; }

source scripts/lib/progress.sh

# --------------- 容器运行时检测 ---------------
RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

# --------------- 载入配置 ---------------
if [ -f ".env" ]; then
    info "载入 .env 配置"
    sed -i 's/\r$//' .env 2>/dev/null || true
    set -a; source .env; set +a
fi

# 默认值
BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
BBS_DB_PORT="${BBS_DB_PORT:-15432}"
BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
BBS_DB_USER="${BBS_DB_USER:-work_flow}"
BBS_DB_PASSWORD="${BBS_DB_PASSWORD:-work_flow123}"
BBS_SERVER_PORT="${BBS_SERVER_PORT:-9083}"
NGINX_PORT="${NGINX_PORT:-19848}"
BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"
BBS_NGINX_CONTAINER="${BBS_NGINX_CONTAINER:-bbs-nginx}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/data/bbs/bbsUpload}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR%/}/"

# 本地构建产物路径
JAR_PATH="bbs-server/target/bbs-server.jar"
BBS_UI_DIST="bbs-ui/dist"
BBS_ADMIN_DIST="bbs-admin-ui/dist"

# --------------- 检测基础镜像 ---------------
ensure_base_images() {
    info "检测基础镜像..."

    if bash scripts/build/base.sh --check 2>/dev/null; then
        ok "基础镜像已就绪"
    else
        warn "基础镜像需要更新，正在构建..."
        bash scripts/build/base.sh
        ok "基础镜像构建完成"
    fi
}

# --------------- 检测预构建产物 ---------------
check_artifacts() {
    local missing=0

    if [ ! -d "$BBS_UI_DIST" ]; then
        warn "bbs-ui/dist 不存在"
        missing=1
    fi
    if [ ! -d "$BBS_ADMIN_DIST" ]; then
        warn "bbs-admin-ui/dist 不存在"
        missing=1
    fi
    if [ ! -f "$JAR_PATH" ]; then
        warn "bbs-server.jar 不存在: $JAR_PATH"
        missing=1
    fi

    if [ "$missing" -ne 0 ]; then
        echo ""
        warn "部分构建产物缺失。两种解决方式："
        warn "  方式 A：在开发机（Windows）上先 build，同步代码后重试"
        warn "  方式 B：添加 --build 参数自动编译"
        echo ""
        if [ "$MODE" != "build" ]; then
            err "请先构建缺失的产物，或使用 --build 参数"
            exit 1
        fi
    fi
}

# --------------- 构建产物（--build 模式） ---------------
build_artifacts() {
    show_step 1 3 "安装前端依赖"
    # 复用 build.sh 的依赖安装逻辑（只安装缺失的）
    _install_deps_smart() {
        local dir=$1 label=$2
        local checksum_file="${dir}/.cache_checksum"
        local pkg_checksum
        pkg_checksum=$(md5sum "${dir}/package.json" 2>/dev/null | cut -d' ' -f1)

        if [ -d "${dir}/node_modules" ] && [ -f "$checksum_file" ] && [ "$pkg_checksum" = "$(cat "$checksum_file" 2>/dev/null)" ]; then
            info "${label}: package.json 未变化，跳过 npm install"
            return
        fi

        run_with_timer "${label}: npm install" bash -c "
            cd '${dir}' || exit 1
            if [ -f package-lock.json ]; then
                npm ci --legacy-peer-deps
            else
                npm install --legacy-peer-deps
            fi
        "
        echo "$pkg_checksum" > "$checksum_file"
    }

    _install_deps_smart "bbs-ui" "bbs-ui"
    _install_deps_smart "bbs-admin-ui" "bbs-admin-ui"

    show_step 2 3 "构建前端"
    # bbs-ui
    if [ ! -d "$BBS_UI_DIST" ]; then
        run_with_timer "bbs-ui: npm run build" bash -c "cd bbs-ui && npm run build"
    else
        info "bbs-ui/dist 已存在，跳过"
    fi
    # bbs-admin-ui
    if [ ! -d "$BBS_ADMIN_DIST" ]; then
        run_with_timer "bbs-admin-ui: npm run build" bash -c "cd bbs-admin-ui && NODE_OPTIONS='--openssl-legacy-provider' npm run build"
    else
        info "bbs-admin-ui/dist 已存在，跳过"
    fi

    show_step 3 3 "构建后端 JAR"
    if [ ! -f "$JAR_PATH" ]; then
        run_with_timer "mvn clean package" bash -c "cd bbs-server && mvn clean package -DskipTests -B"
    else
        info "bbs-server.jar 已存在，跳过 Maven 编译"
    fi
}

# --------------- 后端容器 ---------------
start_backend() {
    local step=$1 total=$2
    show_step "$step" "$total" "后端服务 (bbs-server)"

    # 移除旧容器
    if $RUNNER container exists "$BBS_SERVER_CONTAINER" 2>/dev/null; then
        info "移除旧后端容器..."
        $RUNNER rm -f "$BBS_SERVER_CONTAINER" 2>/dev/null || true
    fi

    # 确保上传目录存在
    if [ ! -d "$BBS_UPLOAD_DIR" ]; then
        mkdir -p "$BBS_UPLOAD_DIR"
    fi

    # 获取 JAR 的绝对路径
    local jar_abs
    jar_abs=$(cd "$(dirname "$JAR_PATH")" && pwd)/$(basename "$JAR_PATH")

    info "启动 bbs-server 容器（bind-mount JAR）..."
    $RUNNER run -d \
        --name "$BBS_SERVER_CONTAINER" \
        --network host \
        -e BBS_DB_HOST="$BBS_DB_HOST" \
        -e BBS_DB_PORT="$BBS_DB_PORT" \
        -e BBS_DB_NAME="$BBS_DB_NAME" \
        -e BBS_DB_USER="$BBS_DB_USER" \
        -e BBS_DB_PASSWORD="$BBS_DB_PASSWORD" \
        -e BBS_SUPER_ADMIN_PASSWORD="${BBS_SUPER_ADMIN_PASSWORD:-1234@abcD}" \
        -e BBS_UPLOAD_DIR="$BBS_UPLOAD_DIR" \
        -v "$jar_abs:/app/app.jar:Z" \
        -v "$BBS_UPLOAD_DIR:$BBS_UPLOAD_DIR" \
        bbs-server-base

    ok "bbs-server 容器已启动"

    # 健康检查轮询
    info "后端启动中，等待就绪..."
    sleep 5
    local health_start
    health_start=$(date +%s)
    local max_attempts=30
    for i in $(seq 1 "$max_attempts"); do
        polling_spinner "等待后端就绪" "$i" "$max_attempts" "$health_start"
        if curl -s http://127.0.0.1:$BBS_SERVER_PORT/bbs-server/ >/dev/null 2>&1; then
            polling_clear
            local now; now=$(date +%s)
            ok "后端就绪！($(( now - health_start ))s/${i}次)"
            return 0
        fi
        sleep 2
    done
    polling_clear
    warn "后端就绪超时，请检查日志: $RUNNER logs $BBS_SERVER_CONTAINER"
}

# --------------- Nginx 容器 ---------------
start_nginx() {
    local step=$1 total=$2
    show_step "$step" "$total" "Nginx 反向代理"

    if $RUNNER container exists "$BBS_NGINX_CONTAINER" 2>/dev/null; then
        info "移除旧 Nginx 容器..."
        $RUNNER rm -f "$BBS_NGINX_CONTAINER" 2>/dev/null || true
    fi

    # 获取 dist 目录的绝对路径
    local ui_dist_abs admin_dist_abs
    ui_dist_abs=$(cd "$BBS_UI_DIST" && pwd)
    admin_dist_abs=$(cd "$BBS_ADMIN_DIST" && pwd)

    info "启动 Nginx 容器（bind-mount dist）..."
    $RUNNER run -d \
        --name "$BBS_NGINX_CONTAINER" \
        --network host \
        -e NGINX_PORT="$NGINX_PORT" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        -v "$ui_dist_abs:/usr/share/nginx/html/bbs-ui:Z" \
        -v "$admin_dist_abs:/usr/share/nginx/html/bbs-admin:Z" \
        bbs-nginx-base

    ok "Nginx 容器已启动"
    echo ""
    echo -e "  ${CYAN}用户前端:${NC}  http://localhost:${NGINX_PORT}/bbs-user/"
    echo -e "  ${CYAN}管理后台:${NC}  http://localhost:${NGINX_PORT}/bbs-admin/"
    echo -e "  ${CYAN}后端 API:${NC}  http://localhost:${NGINX_PORT}/bbs-server/"
}

# --------------- 主流程 ---------------
show_header "BBS WSL 部署"

case "$MODE" in
    --build|build)
        ensure_base_images
        build_artifacts
        start_backend 1 2
        start_nginx 2 2
        ;;
    --restart-only|restart-only)
        info "仅重启容器..."
        $RUNNER restart "$BBS_SERVER_CONTAINER" 2>/dev/null || warn "bbs-server 容器不存在"
        $RUNNER restart "$BBS_NGINX_CONTAINER" 2>/dev/null || warn "bbs-nginx 容器不存在"
        ok "容器已重启"
        ;;
    *)
        ensure_base_images
        check_artifacts
        start_backend 1 2
        start_nginx 2 2
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  部署完成！"
echo -e "${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  用户前端:  http://localhost:${NGINX_PORT}/bbs-user/"
echo -e "${GREEN}║${NC}  管理后台:  http://localhost:${NGINX_PORT}/bbs-admin/"
echo -e "${GREEN}║${NC}  后端 API:  http://localhost:${NGINX_PORT}/bbs-server/"
echo -e "${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  查看日志:"
echo -e "${GREEN}║${NC}    $RUNNER logs -f $BBS_SERVER_CONTAINER"
echo -e "${GREEN}║${NC}    $RUNNER logs -f $BBS_NGINX_CONTAINER"
echo -e "${GREEN}║${NC}"
echo -e "${YELLOW}╔══════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║${NC}  ⚠ 请确保 PostgreSQL 已启动并正常运行"
echo -e "${YELLOW}║${NC}"
echo -e "${YELLOW}║${NC}  WSL 热更新: 改代码后执行 podman restart ${BBS_SERVER_CONTAINER} ${BBS_NGINX_CONTAINER}"
echo -e "${YELLOW}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}║${NC}  停止容器:  bash scripts/ops/teardown.sh"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
