#!/bin/bash
# ============================================
# BBS PostgreSQL 数据库初始化脚本
# 运行方式：在 PostgreSQL 容器启动时自动执行
# 或者手动执行：
#   podman exec -i bbs-postgres bash < scripts/init-pg-database.sh
# ============================================
set -e

# 数据库配置
BBS_DB_NAME="${POSTGRESQL_DATABASE:-bbs}"
BBS_DB_USER="${POSTGRESQL_USER:-bbs_user}"

echo "===== 初始化 BBS 数据库 ====="

# 应用 Schema
echo "--> 创建表结构..."
psql -U "${BBS_DB_USER}" -d "${BBS_DB_NAME}" -f /docker-entrypoint-initdb.d/bbs-pg-schema.sql

echo "===== 数据库初始化完成 ====="
