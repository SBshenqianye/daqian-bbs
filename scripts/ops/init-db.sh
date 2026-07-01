#!/bin/bash
# ============================================
# BBS 数据库初始化脚本
# 手动执行数据库建表 + 基础数据导入
# 适用于 PostgreSQL 已安装但表结构为空的情况
# 用法:
#   bash scripts/ops/init-db.sh                   # 从 .env 读取配置
#   bash scripts/ops/init-db.sh -h 127.0.0.1 -p 5432 -U work_flow -d bbs
# ============================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

# --------------- 参数解析 ---------------
usage() {
    echo "用法: $0 [-h host] [-p port] [-U user] [-d database] [-W password]"
    echo "  -h   PostgreSQL 主机（默认: 127.0.0.1）"
    echo "  -p   PostgreSQL 端口（默认: 5432）"
    echo "  -U   PostgreSQL 用户（默认: work_flow）"
    echo "  -d   数据库名称（默认: bbs）"
    echo "  -W   密码（默认从 .env 读取）"
    echo "  -f   SQL 文件路径（默认: init-pg.sql）"
    exit 1
}

# 载入 .env 配置
if [ -f "$ROOT_DIR/.env" ]; then
    set -a; source "$ROOT_DIR/.env"; set +a
fi

# 默认值
DB_HOST="${BBS_DB_HOST:-127.0.0.1}"
DB_PORT="${BBS_DB_PORT:-5432}"
DB_USER="${BBS_DB_USER:-work_flow}"
DB_NAME="${BBS_DB_NAME:-bbs}"
DB_PASSWORD="${BBS_DB_PASSWORD:-}"
SQL_FILE=""

while getopts "h:p:U:d:W:f:" opt; do
    case "$opt" in
        h) DB_HOST="$OPTARG" ;;
        p) DB_PORT="$OPTARG" ;;
        U) DB_USER="$OPTARG" ;;
        d) DB_NAME="$OPTARG" ;;
        W) DB_PASSWORD="$OPTARG" ;;
        f) SQL_FILE="$OPTARG" ;;
        *) usage ;;
    esac
done

# --------------- 执行 ---------------
if [ -z "$SQL_FILE" ]; then
    # 查找 init-pg.sql（优先用项目内的）
    SQL_FILE="$ROOT_DIR/bbs-server/src/main/resources/db/init/init-pg.sql"
    if [ ! -f "$SQL_FILE" ]; then
        SQL_FILE="$ROOT_DIR/scripts/init/init-pg.sql"
    fi
fi

if [ ! -f "$SQL_FILE" ]; then
    echo "[ERR] 未找到 init-pg.sql: $SQL_FILE"
    echo "请通过 -f 参数指定 SQL 文件路径"
    exit 1
fi

echo "=========================================="
echo " BBS 数据库初始化"
echo "=========================================="
echo "  主机:     $DB_HOST"
echo "  端口:     $DB_PORT"
echo "  数据库:   $DB_NAME"
echo "  用户:     $DB_USER"
echo "  SQL 文件: $SQL_FILE"
echo "=========================================="

# 检查 psql
if ! command -v psql >/dev/null 2>&1; then
    echo "[ERR] 未找到 psql 命令，请安装 PostgreSQL 客户端"
    exit 1
fi

# 测试连接
echo "[INFO] 测试数据库连接..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" >/dev/null 2>&1 || {
    echo "[ERR] 无法连接到 PostgreSQL: $DB_HOST:$DB_PORT"
    echo "请确认 PostgreSQL 已启动: pg_isready -h $DB_HOST -p $DB_PORT"
    exit 1
}
echo "[OK] 连接成功"

# 确保数据库存在
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tc \
    "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1 || {
    echo "[INFO] 创建数据库 $DB_NAME..."
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
        -c "CREATE DATABASE \"$DB_NAME\""
    echo "[OK] 数据库 $DB_NAME 已创建"
}

# 执行 SQL
echo "[INFO] 执行 $SQL_FILE ..."
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"

echo ""
echo "[OK] 数据库初始化完成！"
echo ""
echo "  验证: PGPASSWORD='****' psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c '\\dt'"
