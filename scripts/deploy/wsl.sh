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
#   bash scripts/deploy/wsl.sh --repair              # 验证挂载/文件服务，异常自动重建后端容器
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

# --------------- 上传目录：准备 + 挂载验证 ---------------
# 背景：老 podman（如 RHEL7 生产机上的 1.4.4）在部分场景下 bind mount 会静默失效，
#       上传写进容器可写层、宿主/nginx 读不到 → 图片 GET 404（2026-08 生产故障根因）。
# 方案：启动前在宿主目录写入探针文件，启动后从容器内回读；读不到即判定挂载失效并退出。
# 注意：本段与 scripts/deploy/offline.sh、package.sh 生成的 upgrade.sh 保持同步
#       （离线包内 deploy-offline.sh 是独立副本，无共享 lib，必须各自内联）。
STORAGE_PROBE=".deploy-mount-probe"

ensure_upload_dir() {
    local dir="$1"
    [ -n "$dir" ] || { err "ensure_upload_dir: 目录为空"; exit 1; }
    mkdir -p "$dir"
    # SELinux Enforcing（RHEL7 rootful）：bind mount 需要 container_file_t 标签
    if command -v getenforce >/dev/null 2>&1 && [ "$(getenforce 2>/dev/null)" = "Enforcing" ]; then
        info "SELinux Enforcing，为上传目录设置 container_file_t 标签: $dir"
        command -v chcon >/dev/null 2>&1 && chcon -Rt container_file_t "$dir" 2>/dev/null \
            || warn "chcon 不可用或失败；若容器读不到上传目录请手动执行 chcon"
    fi
    echo ok > "$dir/$STORAGE_PROBE"
}

verify_upload_mount() {
    local container="$1" dir="$2" fail_soft="$3"
    local tries=0
    while [ "$tries" -lt 30 ]; do
        if $RUNNER exec "$container" cat "$dir/$STORAGE_PROBE" 2>/dev/null | grep -q '^ok$'; then
            ok "上传目录挂载验证通过: $dir（容器与宿主共享同一目录）"
            return 0
        fi
        tries=$((tries + 1))
        sleep 2
    done
    err "上传目录挂载验证失败：容器内读不到 $dir/$STORAGE_PROBE"
    err "后果：上传会写进容器可写层，图片 GET 全部 404。请检查："
    err "  1) run 命令必须包含 -v $dir:$dir:Z"
    err "  2) SELinux 环境下先执行: chcon -Rt container_file_t $dir 再重建容器"
    err "  3) 验证: $RUNNER exec $container ls -la $dir"
    # soft 模式（--repair 预检用）：返回 1 交给调用方决定重建，不直接退出
    [ "$fail_soft" = "soft" ] && return 1
    exit 1
}

# --------------- HTTP 文件服务自检（端到端，无需登录） ---------------
# 宿主写一个测试图 → 模拟浏览器 GET /bbs-server/files/... 回读（SecurityConfig 中
# /files/** permitAll）。读不到 = 后端容器看不到宿主上传目录（挂载失效）或文件服务
# 异常 → 与生产故障同症状（GET 404）。注意：本段与 scripts/deploy/offline.sh 保持同步。
write_serve_probe() {
    local dir="$1"
    dir="${dir%/}"
    local day; day=$(date +%Y-%m-%d)
    local ts; ts=$(date +%s)
    local file="$dir/common/upload/$day/bbs-serve-probe-${ts}_.png"
    local url_path="common/upload/$day/bbs-serve-probe-${ts}_.png"
    mkdir -p "$(dirname "$file")"
    # 1x1 透明 PNG（最小合法图片）
    printf '%s' 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==' | base64 -d > "$file" 2>/dev/null \
        || echo -n 'png' > "$file"
    echo "$url_path"
}

verify_http_serve() {
    local nginx_port="$1" url_path="$2"
    local tries=0
    while [ "$tries" -lt 30 ]; do
        local code
        code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$nginx_port/bbs-server/files/$url_path" 2>/dev/null || echo 000)
        if [ "$code" = "200" ]; then
            ok "HTTP 文件自检通过: GET /bbs-server/files/$url_path → 200"
            return 0
        fi
        tries=$((tries + 1))
        sleep 2
    done
    err "HTTP 文件自检失败: GET /bbs-server/files/$url_path → $code（应为 200）"
    err "原因：后端容器读不到宿主上传目录里的文件（bind mount 失效或未挂载）"
    return 1
}

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
BBS_SERVER_PORT="${BBS_SERVER_PORT:-60000}"
NGINX_PORT="${NGINX_PORT:-60001}"
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

    # 确保上传目录存在（含 SELinux 标签 + 挂载探针）
    ensure_upload_dir "$BBS_UPLOAD_DIR"

    # 获取 JAR 的绝对路径
    local jar_abs
    jar_abs=$(cd "$(dirname "$JAR_PATH")" && pwd)/$(basename "$JAR_PATH")

    info "启动 bbs-server 容器（bind-mount JAR）..."
    # --entrypoint java + -Xmx2g: 主机内存不足时 JDK8 默认按物理内存 1/4 申请堆会启动即崩溃
    $RUNNER run -d \
        --name "$BBS_SERVER_CONTAINER" \
        --network host \
        --entrypoint java \
        -e BBS_DB_HOST="$BBS_DB_HOST" \
        -e BBS_DB_PORT="$BBS_DB_PORT" \
        -e BBS_DB_NAME="$BBS_DB_NAME" \
        -e BBS_DB_USER="$BBS_DB_USER" \
        -e BBS_DB_PASSWORD="$BBS_DB_PASSWORD" \
        -e BBS_SUPER_ADMIN_PASSWORD="${BBS_SUPER_ADMIN_PASSWORD:-1234@abcD}" \
        -e BBS_UPLOAD_DIR="$BBS_UPLOAD_DIR" \
        -e BBS_SERVER_PORT="$BBS_SERVER_PORT" \
        -v "$jar_abs:/app/app.jar:Z" \
        -v "$BBS_UPLOAD_DIR:$BBS_UPLOAD_DIR:Z" \
        bbs-server-base -Xmx2g -jar /app/app.jar --spring.profiles.active=podman

    ok "bbs-server 容器已启动"

    # 挂载验证：容器必须能读到宿主上传目录里的探针文件，
    # 否则上传会写进容器可写层，图片 GET 全部 404（老 podman 静默失效的典型症状）
    verify_upload_mount "$BBS_SERVER_CONTAINER" "$BBS_UPLOAD_DIR"

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
        ensure_upload_dir "$BBS_UPLOAD_DIR"
        verify_upload_mount "$BBS_SERVER_CONTAINER" "$BBS_UPLOAD_DIR"
        ok "容器已重启"
        ;;
    --repair|repair)
        # 修复模式：先无破坏验证当前容器（探针 + HTTP 回读），异常才重建
        info "修复模式：验证当前容器，必要时重建..."
        ensure_upload_dir "$BBS_UPLOAD_DIR"
        local need_rebuild=0
        if $RUNNER container exists "$BBS_SERVER_CONTAINER" 2>/dev/null; then
            if verify_upload_mount "$BBS_SERVER_CONTAINER" "$BBS_UPLOAD_DIR" soft; then
                local pre_url; pre_url=$(write_serve_probe "$BBS_UPLOAD_DIR")
                if verify_http_serve "$NGINX_PORT" "$pre_url"; then
                    rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$pre_url")"
                    ok "当前容器健康：挂载正常 + HTTP 文件服务正常，无需重建"
                else
                    rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$pre_url")"
                    need_rebuild=1
                fi
            else
                need_rebuild=1
            fi
        else
            need_rebuild=1
        fi
        if [ "$need_rebuild" = "1" ]; then
            warn "当前容器异常，重建后端容器..."
            start_backend 1 1
            local url; url=$(write_serve_probe "$BBS_UPLOAD_DIR")
            if verify_http_serve "$NGINX_PORT" "$url"; then
                rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$url")"
                ok "修复完成：容器已重建，挂载验证与 HTTP 文件服务验证全部通过"
            else
                rm -f "${BBS_UPLOAD_DIR%/}/common/upload/$(date +%Y-%m-%d)/$(basename "$url")"
                err "修复失败：重建后文件服务仍异常，请检查: $RUNNER logs $BBS_SERVER_CONTAINER"
                exit 1
            fi
        fi
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
