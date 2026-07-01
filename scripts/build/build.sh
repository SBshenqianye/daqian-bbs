#!/bin/bash
# ============================================
# BBS 构建脚本
# 并行构建前后端，支持两种模式：
#   1. 容器镜像模式（默认）：构建 Docker 镜像
#   2. 原生 JAR 模式（--native）：编译 bbs-server.jar
# 用法:
#   bash scripts/build/build.sh              # 构建全部 + 容器离线包（默认）
#   bash scripts/build/build.sh --native     # 构建全部 + 原生部署包
#   bash scripts/build/build.sh --frontend   # 仅构建前端
#   bash scripts/build/build.sh --backend    # 仅构建后端
# 注意: 默认和 --native 模式构建完后自动打包，无需再运行 package.sh
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

# --------------- 进度指示库 ---------------
source scripts/lib/progress.sh

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
    local step=$1 total=$2
    show_step "$step" "$total" "构建前端（并行 bbs-ui + bbs-admin-ui）"

    local log_ui log_admin
    log_ui=$(mktemp /tmp/bbs-ui-XXXX.log)
    log_admin=$(mktemp /tmp/bbs-admin-XXXX.log)

    # bbs-ui（子 shell 中依次执行 npm install → npm build）
    (
        cd bbs-ui || exit 1
        echo "--- npm install ---" >> "$log_ui"
        npm install --legacy-peer-deps >> "$log_ui" 2>&1 || exit 1
        echo "--- npm run build ---" >> "$log_ui"
        npm run build >> "$log_ui" 2>&1 || exit 1
        [ -d "dist" ] || exit 1
    ) &
    local pid_ui=$!

    # bbs-admin-ui
    (
        cd bbs-admin-ui || exit 1
        echo "--- npm install ---" >> "$log_admin"
        npm install --legacy-peer-deps >> "$log_admin" 2>&1 || exit 1
        echo "--- npm run build (openssl-legacy-provider) ---" >> "$log_admin"
        NODE_OPTIONS="--openssl-legacy-provider" npm run build >> "$log_admin" 2>&1 || exit 1
        [ -d "dist" ] || exit 1
    ) &
    local pid_admin=$!

    # 并行监控两个任务
    track_parallel "前端构建进度:" "$pid_ui" "bbs-ui" "$pid_admin" "bbs-admin-ui"
    local rc=$?

    if [ $rc -ne 0 ]; then
        # 显示失败日志
        if ! wait "$pid_ui" 2>/dev/null; then
            err "[bbs-ui] 构建失败"
            sed 's/^/  /' "$log_ui"
        fi
        if ! wait "$pid_admin" 2>/dev/null; then
            err "[bbs-admin-ui] 构建失败"
            sed 's/^/  /' "$log_admin"
        fi
        rm -f "$log_ui" "$log_admin"
        exit 1
    fi

    ok "前端构建全部完成"
    rm -f "$log_ui" "$log_admin"
}

# --------------- 构建后端 ---------------
build_backend() {
    local step=$1 total=$2
    show_step "$step" "$total" "构建后端"

    if [[ "$MODE" == "container" ]]; then
        # Docker 镜像模式
        local runner="podman"
        command -v podman >/dev/null 2>&1 || runner="docker"
        run_with_spinner "使用 $runner 构建 bbs-server 镜像" "$runner" build -t bbs-server bbs-server
        ok "bbs-server 镜像构建完成"
    else
        # 原生 JAR 模式
        run_with_spinner "使用 Maven 编译 bbs-server.jar" bash -c "cd bbs-server && mvn package -DskipTests -B"
        ok "bbs-server.jar 编译完成: bbs-server/target/bbs-server.jar"
    fi
}

# --------------- 构建 Nginx 镜像 ---------------
build_nginx() {
    local step=$1 total=$2
    if [[ "$MODE" != "container" ]]; then
        return
    fi
    show_step "$step" "$total" "构建 Nginx 镜像"

    # 检查前端 dist 是否存在
    if [ ! -d "bbs-ui/dist" ] || [ ! -d "bbs-admin-ui/dist" ]; then
        err "前端 dist 目录不存在，请先构建前端"
        exit 1
    fi

    local runner="podman"
    command -v podman >/dev/null 2>&1 || runner="docker"
    run_with_spinner "使用 $runner 构建 bbs-nginx 镜像" "$runner" build -t bbs-nginx -f nginx/Dockerfile .
    ok "bbs-nginx 镜像构建完成"
}

# --------------- 主流程 ---------------
case "$MODE" in
    --frontend|frontend)
        MODE="frontend"
        show_header "BBS 前端构建"
        check_prereqs
        build_frontend 1 1
        ;;
    --backend|backend)
        MODE="backend"
        show_header "BBS 后端构建"
        check_prereqs
        build_backend 1 1
        ;;
    --native|native)
        MODE="native"
        show_header "BBS 原生构建 + 打包"
        check_prereqs
        # 前后端并行构建（互不依赖）
        build_frontend 1 3 &
        pid_fe=$!
        build_backend 2 3 &
        pid_be=$!
        wait $pid_fe || { err "前端构建失败"; exit 1; }
        wait $pid_be || { err "后端构建失败"; exit 1; }
        # 自动打包
        show_step 3 3 "自动打包"
        run_with_spinner "创建原生部署包" bash scripts/dist/package.sh --native
        ;;
    *)
        MODE="container"
        show_header "BBS 容器镜像构建 + 打包"
        check_prereqs
        # 前后端并行构建，Nginx 镜像需等前端 dist 就绪
        build_frontend 1 4 &
        pid_fe=$!
        build_backend 2 4 &
        pid_be=$!
        wait $pid_fe || { err "前端构建失败"; exit 1; }
        wait $pid_be || { err "后端构建失败"; exit 1; }
        build_nginx 3 4
        # 自动打包
        show_step 4 4 "自动打包"
        run_with_spinner "创建容器离线包" bash scripts/dist/package.sh
        ;;
esac

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  构建 + 打包全部完成！"
if [[ "$MODE" == "container" ]]; then
    echo -e "${GREEN}║${NC}  分发包: $(ls -t bbs-offline-*.tar.gz 2>/dev/null | head -1)"
else
    echo -e "${GREEN}║${NC}  分发包: $(ls -t bbs-deploy-*.tar.gz 2>/dev/null | head -1)"
fi
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
