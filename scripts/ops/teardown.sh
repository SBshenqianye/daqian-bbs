#!/bin/bash
# ============================================
# BBS 清理脚本
# 停止并移除所有容器，可选择删除数据
# 用法:
#   bash scripts/ops/teardown.sh              # 停止所有容器
#   bash scripts/ops/teardown.sh --all        # 停止所有容器 + 删除数据卷
#   bash scripts/ops/teardown.sh --native     # 停止原生部署的后端进程
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# 载入配置
if [ -f ".env" ]; then
    set -a; source .env; set +a
fi

BBS_NET_NAME="${BBS_NET_NAME:-bbs-net}"
BBS_PG_CONTAINER="${BBS_PG_CONTAINER:-bbs-postgres}"
BBS_SERVER_CONTAINER="${BBS_SERVER_CONTAINER:-bbs-server}"
BBS_NGINX_CONTAINER="${BBS_NGINX_CONTAINER:-bbs-nginx}"

RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

# --------------- 停止容器 ---------------
teardown_container() {
    info "===== 停止容器 ====="

    for container in "$BBS_NGINX_CONTAINER" "$BBS_SERVER_CONTAINER" "$BBS_PG_CONTAINER"; do
        if $RUNNER container exists "$container" 2>/dev/null; then
            info "停止容器 $container..."
            $RUNNER stop "$container" 2>/dev/null || true
            $RUNNER rm "$container" 2>/dev/null || true
            ok "已移除容器 $container"
        else
            info "容器 $container 不存在，跳过"
        fi
    done
}

# --------------- 删除数据卷 ---------------
remove_volumes() {
    warn "删除数据卷 bbs-pg-data..."
    if $RUNNER volume exists bbs-pg-data 2>/dev/null; then
        $RUNNER volume rm bbs-pg-data
        ok "数据卷已删除"
    else
        info "数据卷不存在，跳过"
    fi
}

# --------------- 删除网络 ---------------
remove_network() {
    if $RUNNER network exists "$BBS_NET_NAME" 2>/dev/null; then
        info "删除网络 $BBS_NET_NAME..."
        $RUNNER network rm "$BBS_NET_NAME" 2>/dev/null || true
        ok "网络已删除"
    fi
}

# --------------- 停止原生部署 ---------------
teardown_native() {
    info "===== 停止原生部署 ====="

    # 停止后端进程
    local pid_file="/var/run/bbs-server.pid"
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            info "停止 bbs-server (PID $pid)..."
            sudo kill "$pid" 2>/dev/null || true
            sleep 1
        fi
        sudo rm -f "$pid_file"
        ok "bbs-server 已停止"
    else
        # 尝试通过 systemd 停止
        if systemctl is-active bbs-server >/dev/null 2>&1; then
            sudo systemctl stop bbs-server
            ok "bbs-server 服务已停止"
        else
            # 查找 java 进程
            local java_pid=$(pgrep -f "bbs-server.jar" 2>/dev/null || true)
            if [ -n "$java_pid" ]; then
                info "停止 bbs-server 进程 (PID $java_pid)..."
                kill "$java_pid" 2>/dev/null || true
                ok "bbs-server 已停止"
            else
                info "未发现运行中的 bbs-server 进程"
            fi
        fi
    fi

    # 注意: 不自动停止 PostgreSQL 和 Nginx（它们可能是系统服务）
    warn "请手动停止 PostgreSQL 和 Nginx（如需）:"
    info "  sudo systemctl stop postgresql-13"
    info "  sudo systemctl stop nginx"
}

# --------------- 主流程 ---------------
MODE="${1:-container}"

case "$MODE" in
    --native|native)
        teardown_native
        ;;
    --all|all)
        teardown_container
        remove_volumes
        remove_network
        ok "全部清理完成！（数据卷已删除）"
        ;;
    *)
        teardown_container
        warn "保留数据卷 bbs-pg-data（如需删除数据请加 --all 参数）"
        ok "容器已清理完成"
        ;;
esac
