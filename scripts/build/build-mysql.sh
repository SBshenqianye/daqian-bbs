#!/bin/bash
# ============================================
# BBS MySQL 构建脚本（临时测试用）
# 构建 bbs-server:mysql + bbs-nginx 镜像
# 用法:
#   bash scripts/build/build-mysql.sh
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# --------------- 颜色输出 ---------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

# --------------- 前置检查 ---------------
check_prereqs() {
    local missing=0
    command -v node  >/dev/null 2>&1 || { warn "缺少 nodejs"; missing=1; }
    command -v npm   >/dev/null 2>&1 || { warn "缺少 npm"; missing=1; }
    command -v podman >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || {
        warn "缺少 podman/docker"; missing=1; }
    if [ "$missing" -ne 0 ]; then
        err "请安装缺失的依赖后重试"; exit 1
    fi
}

RUNNER="podman"
command -v podman >/dev/null 2>&1 || RUNNER="docker"

# --------------- 构建前端 ---------------
build_frontend() {
    info "===== 并行构建前端 ====="

    # bbs-ui
    (
        info "[bbs-ui] 安装依赖..."
        cd bbs-ui
        npm install --legacy-peer-deps 2>&1 | tail -1
        info "[bbs-ui] 构建..."
        npm run build 2>&1 | tail -1
        if [ -d "dist" ]; then
            ok "[bbs-ui] 构建成功 → $(pwd)/dist"
        else
            err "[bbs-ui] 构建失败: dist 目录未生成"
            exit 1
        fi
    ) &
    PID_UI=$!

    # bbs-admin-ui
    (
        info "[bbs-admin-ui] 安装依赖..."
        cd bbs-admin-ui
        npm install --legacy-peer-deps 2>&1 | tail -1
        info "[bbs-admin-ui] 构建（openssl-legacy-provider）..."
        NODE_OPTIONS="--openssl-legacy-provider" npm run build 2>&1 | tail -1
        if [ -d "dist" ]; then
            ok "[bbs-admin-ui] 构建成功 → $(pwd)/dist"
        else
            err "[bbs-admin-ui] 构建失败: dist 目录未生成"
            exit 1
        fi
    ) &
    PID_ADMIN=$!

    wait $PID_UI || { err "bbs-ui 构建失败"; exit 1; }
    wait $PID_ADMIN || { err "bbs-admin-ui 构建失败"; exit 1; }
    ok "前端构建全部完成"
}

# --------------- 构建后端（MySQL 镜像） ---------------
build_backend_mysql() {
    info "===== 构建 bbs-server:mysql 镜像（Dockerfile.mysql）====="
    $RUNNER build -t bbs-server:mysql -f bbs-server/Dockerfile.mysql bbs-server
    ok "bbs-server:mysql 镜像构建完成"
}

# --------------- 构建 Nginx 镜像 ---------------
build_nginx() {
    info "===== 构建 bbs-nginx 镜像 ====="
    if [ ! -d "bbs-ui/dist" ] || [ ! -d "bbs-admin-ui/dist" ]; then
        err "前端 dist 目录不存在，请先构建前端"
        exit 1
    fi
    $RUNNER build -t bbs-nginx -f nginx/Dockerfile .
    ok "bbs-nginx 镜像构建完成"
}

# --------------- 主流程 ---------------
info "=========================================="
info " BBS MySQL 临时构建脚本"
info "=========================================="

check_prereqs

# 前后端并行构建
build_frontend &  PID_FE=$!
build_backend_mysql &  PID_BE=$!
wait $PID_FE || { err "前端构建失败"; exit 1; }
wait $PID_BE || { err "后端构建失败"; exit 1; }

build_nginx

echo ""
ok "========================================"
ok "构建全部完成！"
echo ""
echo "  后端镜像: bbs-server:mysql"
echo "  Nginx镜像: bbs-nginx:latest"
echo ""
info "下一步部署: bash scripts/deploy/container-mysql.sh"
echo "========================================"
