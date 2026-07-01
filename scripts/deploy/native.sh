#!/bin/bash
# ============================================
# BBS 原生部署脚本（适用于 RHEL 7 / CentOS 7）
# 前置条件:
#   1. PostgreSQL 已安装并运行
#   2. JDK 8+ 已安装
#   3. Nginx 已安装
#   4. 已运行 bash scripts/build/build.sh --native
# 用法:
#   bash scripts/deploy/native.sh
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

# --------------- 载入配置 ---------------
if [ -f ".env" ]; then
    echo "[INFO] 载入 .env 配置"
    set -a; source .env; set +a
fi

# 默认值（生产环境默认 PostgreSQL 端口 5432）
BBS_DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
BBS_DB_PORT="${BBS_DB_PORT:-5432}"
BBS_DB_NAME="${BBS_DB_NAME:-bbs}"
BBS_DB_USER="${BBS_DB_USER:-work_flow}"
BBS_DB_PASSWORD="${BBS_DB_PASSWORD:-work_flow123}"
BBS_SERVER_PORT="${BBS_SERVER_PORT:-9083}"
NGINX_PORT="${NGINX_PORT:-19848}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/data/bbs/bbsUpload}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR%/}/"
BBS_PG_DATA="${BBS_PG_DATA:-/data/sql/postgre}"

# --------------- 颜色 ---------------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${CYAN}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
err()   { echo -e "${RED}[ERR]${NC} $1"; }

# --------------- 进度指示库 ---------------
source scripts/lib/progress.sh

# --------------- 前置检查 ---------------
check_prereqs() {
    show_step "$1" "$2" "前置依赖检查"

    local missing=0

    # PostgreSQL
    if command -v psql >/dev/null 2>&1; then
        ok "PostgreSQL 客户端已安装"
    else
        warn "psql 未安装，请安装 postgresql-client"
        missing=1
    fi

    # JDK
    if command -v java >/dev/null 2>&1; then
        ok "JDK 已安装: $(java -version 2>&1 | head -1)"
    else
        err "JDK 未安装"
        missing=1
    fi

    # Nginx
    if command -v nginx >/dev/null 2>&1; then
        ok "Nginx 已安装"
    else
        warn "Nginx 未安装"
    fi

    if [ "$missing" -ne 0 ]; then
        err "请安装缺失的依赖后重试"
        exit 1
    fi
}

# --------------- PostgreSQL 检查/启动 ---------------
ensure_postgres() {
    show_step "$1" "$2" "检查 PostgreSQL"

    if systemctl is-active postgresql >/dev/null 2>&1; then
        ok "PostgreSQL 服务运行中"
    elif systemctl is-active postgresql-13 >/dev/null 2>&1; then
        ok "PostgreSQL 13 服务运行中"
    elif systemctl is-active postgresql-12 >/dev/null 2>&1; then
        ok "PostgreSQL 12 服务运行中"
    elif pg_isready -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" >/dev/null 2>&1; then
        ok "PostgreSQL 正在监听 $BBS_DB_HOST:$BBS_DB_PORT"
    else
        cat <<'EOF'
警告: PostgreSQL 未运行！
请先启动 PostgreSQL：

  # RHEL 7 / CentOS 7 启动命令:
  sudo systemctl start postgresql-13
  sudo systemctl enable postgresql-13

  # 或手动启动:
  pg_ctl -D /data/sql/postgre start

  # 检查状态:
  pg_isready -h 127.0.0.1 -p 5432

如果 PostgreSQL 数据目录在 /data/sql/postgre，初始化命令:
  sudo postgresql-13-setup initdb
  sudo systemctl start postgresql-13
EOF
        exit 1
    fi

    # 检查数据库是否存在
    if PGPASSWORD="$BBS_DB_PASSWORD" psql -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -c "SELECT 1" >/dev/null 2>&1; then
        ok "数据库 $BBS_DB_NAME 已存在"
    else
        info "创建数据库 $BBS_DB_NAME..."
        PGPASSWORD="$BBS_DB_PASSWORD" psql -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" -d postgres -c "CREATE DATABASE \"$BBS_DB_NAME\";"
        ok "数据库 $BBS_DB_NAME 已创建"
    fi
}

# --------------- 数据库初始化 ---------------
init_database() {
    show_step "$1" "$2" "初始化数据库表结构"
    local sql_file="$ROOT_DIR/bbs-server/src/main/resources/db/init/init-pg.sql"
    if [ ! -f "$sql_file" ]; then
        warn "未找到 init-pg.sql，跳过数据库初始化"
        return
    fi

    PGPASSWORD="$BBS_DB_PASSWORD" psql -h "$BBS_DB_HOST" -p "$BBS_DB_PORT" -U "$BBS_DB_USER" -d "$BBS_DB_NAME" -f "$sql_file"
    ok "数据库初始化完成"
}

# --------------- 部署后端 ---------------
deploy_backend() {
    show_step "$1" "$2" "部署后端 (bbs-server)"

    local jar_path="$ROOT_DIR/bbs-server/target/bbs-server.jar"
    if [ ! -f "$jar_path" ]; then
        err "未找到 bbs-server.jar，请先运行 bash scripts/build/build.sh --native"
        exit 1
    fi

    # 确保上传目录存在
    if [ ! -d "$BBS_UPLOAD_DIR" ]; then
        sudo mkdir -p "$BBS_UPLOAD_DIR"
        sudo chmod 755 "$BBS_UPLOAD_DIR"
    fi

    # 停止旧进程
    local pid_file="/var/run/bbs-server.pid"
    if [ -f "$pid_file" ]; then
        local old_pid=$(cat "$pid_file")
        if kill -0 "$old_pid" 2>/dev/null; then
            info "停止旧进程 PID $old_pid..."
            kill "$old_pid" 2>/dev/null || true
            sleep 2
        fi
        rm -f "$pid_file"
    fi

    # 启动后端
    info "启动 bbs-server (端口 $BBS_SERVER_PORT)..."
    nohup java -jar "$jar_path" \
        --server.port=$BBS_SERVER_PORT \
        --spring.profiles.active=podman \
        -DBBS_DB_HOST="$BBS_DB_HOST" \
        -DBBS_DB_PORT="$BBS_DB_PORT" \
        -DBBS_DB_NAME="$BBS_DB_NAME" \
        -DBBS_DB_USER="$BBS_DB_USER" \
        -DBBS_DB_PASSWORD="$BBS_DB_PASSWORD" \
        -DBBS_UPLOAD_DIR="$BBS_UPLOAD_DIR" \
        -DBBS_SUPER_ADMIN_PASSWORD="${BBS_SUPER_ADMIN_PASSWORD:-1234@abcD}" \
        > /var/log/bbs-server.log 2>&1 &

    local new_pid=$!
    echo $new_pid | sudo tee "$pid_file" >/dev/null

    ok "bbs-server 已启动 (PID: $new_pid)"
    info "查看日志: tail -f /var/log/bbs-server.log"

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
    warn "后端就绪超时，请检查日志: tail -f /var/log/bbs-server.log"
}

# --------------- 部署前端到 Nginx ---------------
deploy_frontend() {
    show_step "$1" "$2" "部署前端静态文件"

    local nginx_html_dir="/usr/share/nginx/html"

    # 检查前端 dist
    if [ ! -d "$ROOT_DIR/bbs-ui/dist" ] || [ ! -d "$ROOT_DIR/bbs-admin-ui/dist" ]; then
        err "前端 dist 目录不存在，请先运行 bash scripts/build/build.sh"
        exit 1
    fi

    # 复制前端文件到 Nginx 目录
    sudo mkdir -p "$nginx_html_dir/bbs-ui" "$nginx_html_dir/bbs-admin"
    sudo cp -r "$ROOT_DIR/bbs-ui/dist/"* "$nginx_html_dir/bbs-ui/"
    sudo cp -r "$ROOT_DIR/bbs-admin-ui/dist/"* "$nginx_html_dir/bbs-admin/"

    ok "前端文件已部署到 $nginx_html_dir"
}

# --------------- 配置 Nginx ---------------
configure_nginx() {
    show_step "$1" "$2" "配置 Nginx"

    local nginx_conf="/etc/nginx/conf.d/bbs.conf"
    local template="$ROOT_DIR/nginx/nginx.conf.template"

    if [ ! -f "$template" ]; then
        err "Nginx 配置模板不存在: $template"
        exit 1
    fi

    # 渲染模板（替换环境变量）
    sudo env NGINX_PORT="$NGINX_PORT" \
         BBS_SERVER_PORT="$BBS_SERVER_PORT" \
         envsubst '${NGINX_PORT} ${BBS_SERVER_PORT}' < "$template" \
         | sudo tee "$nginx_conf" >/dev/null

    # 替换 upstream 中的 bbs-server 为 127.0.0.1（原生模式后端在本地）
    sudo sed -i 's/server bbs-server:/server 127.0.0.1:/g' "$nginx_conf"

    # 测试配置
    sudo nginx -t 2>/dev/null || {
        err "Nginx 配置测试失败"
        exit 1
    }

    # 重载 Nginx
    sudo systemctl reload nginx 2>/dev/null || sudo nginx -s reload 2>/dev/null || {
        warn "Nginx 重载失败，尝试启动..."
        sudo systemctl start nginx 2>/dev/null || sudo nginx 2>/dev/null || true
    }

    ok "Nginx 配置完成"
}

# --------------- systemd 服务（可选） -----------
setup_systemd() {
    show_step "$1" "$2" "配置 systemd 服务"

    if [ ! -d "/etc/systemd/system" ]; then
        return
    fi

    local service_file="/etc/systemd/system/bbs-server.service"
    if [ -f "$service_file" ]; then
        ok "bbs-server.service 已存在"
        return
    fi

    local jar_path="$ROOT_DIR/bbs-server/target/bbs-server.jar"
    sudo tee "$service_file" >/dev/null <<EOF
[Unit]
Description=BBS Server
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=$ROOT_DIR
ExecStart=/usr/bin/java -jar $jar_path --spring.profiles.active=podman
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
EnvironmentFile=$ROOT_DIR/.env

[Install]
WantedBy=multi-user.target
EOF

    sudo systemctl daemon-reload
    sudo systemctl enable bbs-server
    ok "systemd 服务已创建: bbs-server.service"
    info "启动: sudo systemctl start bbs-server"
}

# --------------- 主流程 ---------------
show_header "BBS 原生部署 (RHEL 7 / CentOS 7)"

check_prereqs 1 7
ensure_postgres 2 7
init_database 3 7
deploy_backend 4 7
deploy_frontend 5 7
configure_nginx 6 7
setup_systemd 7 7

echo ""
echo -e "${GREEN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║${NC}  部署完成！"
echo -e "${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  用户前端:  http://<服务器IP>:${NGINX_PORT}/bbs-user/"
echo -e "${GREEN}║${NC}  管理后台:  http://<服务器IP>:${NGINX_PORT}/bbs-admin/"
echo -e "${GREEN}║${NC}  后端 API:  http://127.0.0.1:${BBS_SERVER_PORT}/bbs-server/"
echo -e "${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  管理命令:"
echo -e "${GREEN}║${NC}    查看后端日志:  journalctl -u bbs-server -f"
echo -e "${GREEN}║${NC}    重启后端:     sudo systemctl restart bbs-server"
echo -e "${GREEN}║${NC}    重启 Nginx:   sudo systemctl reload nginx"
echo -e "${GREEN}║${NC}"
echo -e "${GREEN}║${NC}  停止服务:  bash scripts/ops/teardown.sh"
echo -e "${GREEN}╚══════════════════════════════════════════════╝${NC}"
