#!/bin/bash
# ============================================
# BBS 构建脚本
# 并行构建前后端，支持两种模式：
#   1. 容器镜像模式（默认）：构建 Docker 镜像
#   2. 原生 JAR 模式（--native）：编译 bbs-server.jar
# 用法:
#   bash scripts/build/build.sh              # 构建全部 + Docker 镜像
#   bash scripts/build/build.sh --native     # 构建全部 + bbs-server.jar
#   bash scripts/build/build.sh --frontend   # 仅构建前端
#   bash scripts/build/build.sh --backend    # 仅构建后端
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

MODE="${1:-container}"  # container | native | frontend | backend

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
    if [[ "$MODE" == "container" ]]; then
        command -v podman >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || {
            warn "缺少 podman/docker"; missing=1; }
    fi
    if [[ "$MODE" == "native" || "$MODE" == "backend" ]]; then
        command -v java  >/dev/null 2>&1 || { warn "缺少 java (JDK 8+)"; missing=1; }
        command -v mvn   >/dev/null 2>&1 || { warn "缺少 maven"; missing=1; }
    fi
    if [ "$missing" -ne 0 ]; then
        err "请安装缺失的依赖后重试"; exit 1
    fi
}

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

    # 等待两个前端构建完成
    wait $PID_UI || { err "bbs-ui 构建失败"; exit 1; }
    wait $PID_ADMIN || { err "bbs-admin-ui 构建失败"; exit 1; }
    ok "前端构建全部完成"
}

# --------------- 构建后端 ---------------
build_backend() {
    info "===== 构建后端 ====="

    if [[ "$MODE" == "container" ]]; then
        # Docker 镜像模式
        local runner="podman"
        command -v podman >/dev/null 2>&1 || runner="docker"
        info "使用 $runner 构建 bbs-server 镜像..."
        $runner build -t bbs-server bbs-server
        ok "bbs-server 镜像构建完成"
    else
        # 原生 JAR 模式
        info "使用 Maven 编译 bbs-server.jar..."
        cd bbs-server
        mvn package -DskipTests -B
        cd "$ROOT_DIR"
        ok "bbs-server.jar 编译完成: bbs-server/target/bbs-server.jar"
    fi
}

# --------------- 构建 Nginx 镜像 ---------------
build_nginx() {
    if [[ "$MODE" != "container" ]]; then
        info "非容器模式，跳过 Nginx 镜像构建"
        return
    fi
    info "===== 构建 Nginx 镜像 ====="
    local runner="podman"
    command -v podman >/dev/null 2>&1 || runner="docker"

    # 检查前端 dist 是否存在
    if [ ! -d "bbs-ui/dist" ] || [ ! -d "bbs-admin-ui/dist" ]; then
        err "前端 dist 目录不存在，请先构建前端"
        exit 1
    fi

    $runner build -t bbs-nginx -f nginx/Dockerfile .
    ok "bbs-nginx 镜像构建完成"
}

# --------------- 主流程 ---------------
case "$MODE" in
    --frontend|frontend)
        check_prereqs
        build_frontend
        ;;
    --backend|backend)
        check_prereqs
        build_backend
        ;;
    --native|native)
        MODE="native"
        check_prereqs
        build_frontend
        build_backend
        ;;
    *)
        MODE="container"
        check_prereqs
        build_frontend
        build_backend
        build_nginx
        ;;
esac

echo ""
ok "========================================"
ok "构建全部完成！"
if [[ "$MODE" == "container" ]]; then
    ok "下一步: bash scripts/deploy/container.sh"
else
    ok "下一步: bash scripts/deploy/native.sh"
fi
echo "========================================"
