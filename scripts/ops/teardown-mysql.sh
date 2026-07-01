#!/bin/bash
# ============================================
# BBS 清理脚本 - MySQL 版（临时测试用）
# 停止并移除 bbs-mysql + bbs-server + bbs-nginx
# 用法:
#   bash scripts/ops/teardown-mysql.sh               # 停止所有容器
#   bash scripts/ops/teardown-mysql.sh --all          # 停止所有容器 + 删除数据卷
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# 载入配置
if [ -f ".env" ]; then
    set -a; source .env; set +a
fi

BBS_NET_NAME="${BBS_NET_NAME:-bbs-net}"
BBS_MYSQL_CONTAINER="${BBS_MYSQL_CONTAINER:-bbs-mysql}"
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

    for container in "$BBS_NGINX_CONTAINER" "$BBS_SERVER_CONTAINER" "$BBS_MYSQL_CONTAINER"; do
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
    warn "删除数据卷 bbs-mysql-data..."
    if $RUNNER volume exists bbs-mysql-data 2>/dev/null; then
        $RUNNER volume rm bbs-mysql-data
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

# --------------- 主流程 ---------------
MODE="${1:-container}"

case "$MODE" in
    --all|all)
        teardown_container
        remove_volumes
        remove_network
        ok "全部清理完成！（数据卷已删除）"
        ;;
    *)
        teardown_container
        warn "保留数据卷 bbs-mysql-data（如需删除数据请加 --all 参数）"
        ok "MySQL 容器已清理完成"
        ;;
esac
