#!/bin/bash
# ============================================
# BBS 容器部署脚本
# 前置条件:
#   1. PostgreSQL 数据库已运行并可访问
#   2. 先运行 bash scripts/build/build.sh
# 用法:
#   bash scripts/deploy/container.sh              # 部署后端 + Nginx
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# --------------- 载入配置 ---------------
if [ -f ".env" ]; then
    echo "[INFO] 载入 .env 配置"
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

RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

# --------------- 进度指示库 ---------------
source scripts/lib/progress.sh

# --------------- 后端 ---------------
start_backend() {
    show_step "$1" "$2" "后端服务 (bbs-server)"

    # 移除旧容器
    if $RUNNER container exists "$BBS_SERVER_CONTAINER" 2>/dev/null; then
        info "移除旧后端容器..."
        $RUNNER rm -f "$BBS_SERVER_CONTAINER" 2>/dev/null || true
    fi

    info "启动 bbs-server 容器（host 网络）..."

    # 确保上传目录存在
    if [ ! -d "$BBS_UPLOAD_DIR" ]; then
        mkdir -p "$BBS_UPLOAD_DIR"
    fi

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
        -v "$BBS_UPLOAD_DIR:$BBS_UPLOAD_DIR" \
        bbs-server

    ok "bbs-server 容器已启动"

    # 先等几秒让 Spring Boot 开始初始化
    info "后端启动中，等待就绪..."
    sleep 5

    # 轮询等待后端就绪（显示已用秒数）
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

# --------------- Nginx ---------------
start_nginx() {
    show_step "$1" "$2" "Nginx 反向代理"

    if $RUNNER container exists "$BBS_NGINX_CONTAINER" 2>/dev/null; then
        info "移除旧 Nginx 容器..."
        $RUNNER rm -f "$BBS_NGINX_CONTAINER" 2>/dev/null || true
    fi

    # 检查镜像是否存在
    if ! $RUNNER image exists bbs-nginx 2>/dev/null; then
        warn "bbs-nginx 镜像不存在，请先运行 bash scripts/build/build.sh"
        exit 1
    fi

    info "启动 Nginx 容器（host 网络）..."
    $RUNNER run -d \
        --name "$BBS_NGINX_CONTAINER" \
        --network host \
        -e NGINX_PORT="$NGINX_PORT" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        bbs-nginx

    ok "Nginx 容器已启动"
    echo ""
    echo -e "  ${CYAN}用户前端:${NC}  http://localhost:${NGINX_PORT}/bbs-user/"
    echo -e "  ${CYAN}管理后台:${NC}  http://localhost:${NGINX_PORT}/bbs-admin/"
    echo -e "  ${CYAN}后端 API:${NC}  http://localhost:${NGINX_PORT}/bbs-server/"
}

# --------------- 主流程 ---------------
show_header "BBS 容器部署"

start_backend 1 2
start_nginx 2 2

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
echo -e "${YELLOW}║${NC}  使用 host 网络，容器通过 127.0.0.1:${BBS_DB_PORT} 连接数据库"
echo -e "${YELLOW}║${NC}"
echo -e "${YELLOW}║${NC}  默认端口:  ${BBS_DB_PORT}  |  数据库: ${BBS_DB_NAME}"
echo -e "${YELLOW}║${NC}"
echo -e "${YELLOW}║${NC}  PostgreSQL 需自行管理，脚本不会自动启动或关闭"
echo -e "${YELLOW}╚══════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}║${NC}  停止容器:  bash scripts/ops/teardown.sh"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
