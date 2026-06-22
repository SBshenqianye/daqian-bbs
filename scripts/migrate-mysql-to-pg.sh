#!/bin/bash
# ============================================
# BBS MySQL → PostgreSQL 数据迁移脚本
# 方式1：使用 pgloader（推荐，自动迁移）
# 方式2：mysqldump + sed 手动转换
# ============================================
set -e

MYSQL_HOST="${MYSQL_HOST:-24.62.0.65}"
MYSQL_PORT="${MYSQL_PORT:-63155}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASSWORD}"
MYSQL_DB="${MYSQL_DB:-bbs}"

PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-bbs_user}"
PG_PASS="${BBS_DB_PASSWORD}"
PG_DB="${PG_DB:-bbs}"

# ============ 方式1: pgloader（推荐） ============
migrate_with_pgloader() {
    echo "===== 使用 pgloader 迁移数据 ====="

    if ! command -v pgloader &> /dev/null; then
        echo "pgloader 未安装，请先安装:"
        echo "  yum install -y pgloader"
        echo "或使用方式2（mysqldump + psql）"
        return 1
    fi

    pgloader \
        mysql://${MYSQL_USER}:${MYSQL_PASS}@${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB} \
        postgresql://${PG_USER}:${PG_PASS}@${PG_HOST}:${PG_PORT}/${PG_DB}

    echo "===== pgloader 迁移完成 ====="
}

# ============ 方式2: mysqldump + sed + psql ============
migrate_with_dump() {
    echo "===== 使用 mysqldump + psql 迁移数据 ====="

    DUMP_FILE="/tmp/bbs-data-dump.sql"

    # Step 1: 导出数据（仅数据，不包含 DDL）
    echo "--> 从 MySQL 导出数据..."
    mysqldump -h ${MYSQL_HOST} -P ${MYSQL_PORT} \
        -u ${MYSQL_USER} -p${MYSQL_PASS} \
        --no-create-info \
        --complete-insert \
        --skip-quote-names \
        ${MYSQL_DB} > ${DUMP_FILE}

    # Step 2: 清理 MySQL 特定语法
    echo "--> 清理 MySQL 特定语法..."
    sed -i \
        -e 's/`//g' \
        -e 's/\\'\''/'\''/g' \
        -e '/^SET /d' \
        -e '/^\/\*.*\*\//d' \
        ${DUMP_FILE}

    # Step 3: 导入到 PostgreSQL
    echo "--> 导入到 PostgreSQL..."
    PGPASSWORD=${PG_PASS} psql -h ${PG_HOST} -p ${PG_PORT} \
        -U ${PG_USER} -d ${PG_DB} \
        -f ${DUMP_FILE}

    echo "===== 数据迁移完成 ====="
}

# ============ 主入口 ============
echo ""
echo "============================================"
echo " BBS MySQL → PostgreSQL 数据迁移"
echo "============================================"
echo ""

if [ -z "${MYSQL_PASS}" ] || [ -z "${BBS_DB_PASSWORD}" ]; then
    echo "错误: 请设置环境变量 MYSQL_PASSWORD 和 BBS_DB_PASSWORD"
    echo "  export MYSQL_PASSWORD='your_mysql_password'"
    echo "  export BBS_DB_PASSWORD='your_pg_password'"
    exit 1
fi

# 尝试 pgloader，失败则回退到 mysqldump
migrate_with_pgloader || migrate_with_dump

echo ""
echo "===== 迁移完成！===="
echo "请验证数据完整性："
echo "  PGPASSWORD=\${BBS_DB_PASSWORD} psql -h ${PG_HOST} -U ${PG_USER} -d ${PG_DB} -c 'SELECT count(*) FROM bbs_article;'"
