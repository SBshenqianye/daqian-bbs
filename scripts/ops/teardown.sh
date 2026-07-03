#!/bin/bash
# ============================================
# BBS 清理脚本
# 停止应用容器，不影响外部管理的 PostgreSQL
# 用法:
#   bash scripts/ops/teardown.sh              # 停止所有容器
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# 载入配置
if [ -f ".env" ]; then
    set -a; source .env; set +a
fi

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

    for container in "$BBS_NGINX_CONTAINER" "$BBS_SERVER_CONTAINER"; do
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

# --------------- 主流程 ---------------
teardown_container
ok "清理完成！"
