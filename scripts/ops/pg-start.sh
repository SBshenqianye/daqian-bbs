#!/bin/bash
# ============================================
# PostgreSQL 容器管理脚本
# 启动 / 停止 / 查看状态
# 用法:
#   bash /app/pg/start.sh            # 启动 PG
#   bash /app/pg/start.sh stop       # 停止 PG
#   bash /app/pg/start.sh status     # 查看状态
# ============================================
set -e

PG_CONTAINER="bbs-postgres"
PG_IMAGE="docker.io/library/postgres:13-alpine"
PG_PORT="15432"

GREEN='\033[0;32m'; CYAN='\033[0;36m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

check_port_mapping() {
    # 检查容器是否有宿主机 PG_PORT -> 5432 的端口映射
    if $RUNNER port "$PG_CONTAINER" 5432 2>/dev/null | grep -q "$PG_PORT"; then
        return 0
    fi
    return 1
}

recreate_container() {
    warn "端口映射 5432->$PG_PORT 缺失，重建容器..."
    $RUNNER stop "$PG_CONTAINER" 2>/dev/null || true
    $RUNNER rm "$PG_CONTAINER" 2>/dev/null || true
    $RUNNER run -d \
        --name "$PG_CONTAINER" \
        -e POSTGRES_PASSWORD="r123456" \
        -p ${PG_PORT}:5432 \
        "$PG_IMAGE"
    ok "容器已重建，端口映射 ${PG_PORT}:5432"
}

ACTION="${1:-start}"

case "$ACTION" in
    start)
        if $RUNNER container exists "$PG_CONTAINER" 2>/dev/null; then
            if $RUNNER ps --filter "name=$PG_CONTAINER" --filter "status=running" | grep -q "$PG_CONTAINER"; then
                ok "PostgreSQL 已在运行"
                # 运行中但缺端口映射，也重建（重启生效）
                if ! check_port_mapping; then
                    recreate_container
                fi
            else
                # 容器存在但未运行，先检查端口映射
                if ! check_port_mapping; then
                    recreate_container
                else
                    info "启动 PostgreSQL 容器..."
                    $RUNNER start "$PG_CONTAINER"
                    ok "PostgreSQL 容器已启动"
                fi
            fi
        else
            info "创建 PostgreSQL 容器..."
            $RUNNER run -d \
                --name "$PG_CONTAINER" \
                -e POSTGRES_PASSWORD="r123456" \
                -p ${PG_PORT}:5432 \
                "$PG_IMAGE"
            ok "PostgreSQL 容器已创建并启动"
        fi

        info "等待就绪..."
        for i in $(seq 1 15); do
            if $RUNNER exec "$PG_CONTAINER" pg_isready -U postgres >/dev/null 2>&1; then
                ok "PostgreSQL 就绪"
                break
            fi
            sleep 2
        done
        ;;
    stop)
        info "停止 PostgreSQL 容器..."
        $RUNNER stop "$PG_CONTAINER" 2>/dev/null || ok "PostgreSQL 未在运行"
        ok "PostgreSQL 已停止"
        ;;
    status)
        if $RUNNER ps --filter "name=$PG_CONTAINER" --filter "status=running" | grep -q "$PG_CONTAINER" 2>/dev/null; then
            ok "PostgreSQL 运行中"
            $RUNNER exec "$PG_CONTAINER" psql -U postgres -c "SELECT version();" 2>/dev/null
        elif $RUNNER container exists "$PG_CONTAINER" 2>/dev/null; then
            warn "PostgreSQL 容器存在但未运行"
        else
            warn "PostgreSQL 容器不存在"
        fi
        ;;
    *)
        echo "用法: bash /app/pg/start.sh [start|stop|status]"
        ;;
esac
