#!/bin/bash
# ============================================
# BBS 容器部署脚本（适用于 WSL2 + Podman）
# 前置条件: 先运行 bash scripts/build/build.sh
# 用法:
#   bash scripts/deploy/container.sh              # 使用默认配置
#   bash scripts/deploy/container.sh --no-pg      # 跳过 PostgreSQL 启动
#   bash scripts/deploy/container.sh --pg-only    # 仅启动 PostgreSQL
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
BBS_DB_HOST="${BBS_DB_HOST:-bbs-postgres}"
BBS_DB_PORT="${BBS_DB_PORT:-5432}"
BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
BBS_DB_USER="${BBS_DB_USER:-work_flow}"
BBS_DB_PASSWORD="${BBS_DB_PASSWORD:-work_flow123}"
BBS_SERVER_PORT="${BBS_SERVER_PORT:-9083}"
NGINX_PORT="${NGINX_PORT:-19848}"
BBS_NET_NAME="${BBS_NET_NAME:-bbs-net}"
BBS_PG_CONTAINER="${BBS_PG_CONTAINER:-bbs-postgres}"
BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"
BBS_NGINX_CONTAINER="${BBS_NGINX_CONTAINER:-bbs-nginx}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/data/bbs/bbsUpload}"

RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

# --------------- 网络 ---------------
ensure_network() {
    if $RUNNER network exists "$BBS_NET_NAME" 2>/dev/null; then
        ok "网络 $BBS_NET_NAME 已存在"
    else
        info "创建网络 $BBS_NET_NAME..."
        $RUNNER network create "$BBS_NET_NAME"
        ok "网络 $BBS_NET_NAME 已创建"
    fi
}

# --------------- PostgreSQL ---------------
start_postgres() {
    if $RUNNER ps aux 2>/dev/null | grep -q "$BBS_PG_CONTAINER"; then
        # Podman 方式
        if $RUNNER container exists "$BBS_PG_CONTAINER" 2>/dev/null; then
            info "PostgreSQL 容器已存在，检查状态..."
            if ! $RUNNER ps --filter "name=$BBS_PG_CONTAINER" --format "{{.Status}}" | grep -q "Up"; then
                info "启动已存在的 PostgreSQL 容器..."
                $RUNNER start "$BBS_PG_CONTAINER"
            else
                ok "PostgreSQL 容器已在运行"
            fi
            return
        fi
    fi

    info "启动 PostgreSQL 容器..."
    $RUNNER run -d \
        --name "$BBS_PG_CONTAINER" \
        --network "$BBS_NET_NAME" \
        -e POSTGRES_DB="$BBS_DB_NAME" \
        -e POSTGRES_USER="$BBS_DB_USER" \
        -e POSTGRES_PASSWORD="$BBS_DB_PASSWORD" \
        -v bbs-pg-data:/var/lib/postgresql/data \
        docker.io/postgres:13-alpine

    ok "PostgreSQL 容器已启动（等待初始化中...）"
    sleep 3
}

# --------------- 后端 ---------------
start_backend() {
    # 移除旧容器
    if $RUNNER container exists "$BBS_SERVER_CONTAINER" 2>/dev/null; then
        info "移除旧后端容器..."
        $RUNNER rm -f "$BBS_SERVER_CONTAINER" 2>/dev/null || true
    fi

    info "启动 bbs-server 容器..."

    # 确保上传目录存在
    if [ ! -d "$BBS_UPLOAD_DIR" ]; then
        mkdir -p "$BBS_UPLOAD_DIR"
    fi

    $RUNNER run -d \
        --name "$BBS_SERVER_CONTAINER" \
        --network "$BBS_NET_NAME" \
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
    info "等待后端启动（约 30 秒）..."
    sleep 5

    # 轮询等待后端就绪
    for i in $(seq 1 30); do
        if $RUNNER exec "$BBS_SERVER_CONTAINER" wget -q --spider http://localhost:$BBS_SERVER_PORT/bbs-server/ 2>/dev/null; then
            ok "后端就绪！"
            break
        fi
        if [ "$i" -eq 30 ]; then
            warn "后端启动较慢，请稍后检查: $RUNNER logs $BBS_SERVER_CONTAINER"
        fi
        sleep 2
    done
}

# --------------- Nginx ---------------
start_nginx() {
    if $RUNNER container exists "$BBS_NGINX_CONTAINER" 2>/dev/null; then
        info "移除旧 Nginx 容器..."
        $RUNNER rm -f "$BBS_NGINX_CONTAINER" 2>/dev/null || true
    fi

    # 检查镜像是否存在
    if ! $RUNNER image exists bbs-nginx 2>/dev/null; then
        warn "bbs-nginx 镜像不存在，请先运行 bash scripts/build/build.sh"
        exit 1
    fi

    info "启动 Nginx 容器..."
    $RUNNER run -d \
        --name "$BBS_NGINX_CONTAINER" \
        --network "$BBS_NET_NAME" \
        -p ${NGINX_PORT}:${NGINX_PORT} \
        -e NGINX_PORT="$NGINX_PORT" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        bbs-nginx

    ok "Nginx 容器已启动"
    info "访问地址: http://localhost:${NGINX_PORT}/bbs-user/"
    info "管理后台: http://localhost:${NGINX_PORT}/bbs-admin/"
}

# --------------- 主流程 ---------------
MODE="${1:-all}"

ensure_network

case "$MODE" in
    --pg-only|pg-only)
        start_postgres
        exit 0
        ;;
    --no-pg|no-pg)
        ok "跳过 PostgreSQL 启动"
        ;;
    *)
        start_postgres
        ;;
esac

start_backend
start_nginx

echo ""
ok "========================================"
ok "部署完成！"
echo ""
echo "  用户前端:  http://localhost:${NGINX_PORT}/bbs-user/"
echo "  管理后台:  http://localhost:${NGINX_PORT}/bbs-admin/"
echo "  后端 API:  http://localhost:${NGINX_PORT}/bbs-server/"
echo ""
echo "  查看日志:"
echo "    $RUNNER logs -f $BBS_SERVER_CONTAINER"
echo "    $RUNNER logs -f $BBS_NGINX_CONTAINER"
echo ""
echo "  停止容器:  bash scripts/ops/teardown.sh"
echo "========================================"
