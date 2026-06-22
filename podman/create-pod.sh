#!/bin/bash
# ============================================
# BBS Podman Pod 创建与启动脚本
# 用法: ./podman/create-pod.sh
# 前提条件:
#   - Podman 已安装
#   - 镜像已构建 (bbs-server:latest, bbs-nginx:latest)
#   - 设置 BBS_DB_PASSWORD 环境变量
# ============================================
set -e

# 配置
BBS_DB_PASSWORD="${BBS_DB_PASSWORD:?错误: 请设置 BBS_DB_PASSWORD 环境变量}"
BBS_UPLOAD_DIR="${BBS_UPLOAD_DIR:-/home/asiayak/bbsUpload}"
PG_DATA_DIR="${PG_DATA_DIR:-/data/sql/postgre}"
POD_NAME="bbs-pod"

echo "===== 创建 BBS Pod ($POD_NAME) ====="

# 清理已存在的 pod
if podman pod exists $POD_NAME 2>/dev/null; then
    echo "--> Pod '$POD_NAME' 已存在，正在删除..."
    podman pod stop $POD_NAME
    podman pod rm $POD_NAME
fi

# 1. 创建 Pod（共享网络命名空间）
echo "--> 创建 Pod..."
podman pod create \
    --name $POD_NAME \
    --publish 18848:18848 \
    --label app=bbs

echo ""

# 2. 启动 PostgreSQL 容器
echo "--> 启动 PostgreSQL..."
# 确保数据目录存在
sudo mkdir -p $PG_DATA_DIR 2>/dev/null || mkdir -p $PG_DATA_DIR

podman run --pod $POD_NAME --name bbs-postgres -d \
    --label app=bbs \
    -e POSTGRESQL_USER=bbs_user \
    -e POSTGRESQL_PASSWORD=$BBS_DB_PASSWORD \
    -e POSTGRESQL_DATABASE=bbs \
    -v $PG_DATA_DIR:/var/lib/pgsql/data:Z \
    registry.access.redhat.com/rhscl/postgresql-12-rhel7

echo "--> 等待 PostgreSQL 初始化（15秒）..."
sleep 15

# 可选：执行数据库初始化脚本
if [ -f scripts/bbs-pg-schema.sql ]; then
    echo "--> 应用数据库 Schema..."
    podman exec -i bbs-postgres psql -U bbs_user -d bbs < scripts/bbs-pg-schema.sql
fi

echo ""

# 3. 启动 BBS 应用容器
echo "--> 启动 BBS 应用..."
# 确保上传目录存在
sudo mkdir -p $BBS_UPLOAD_DIR 2>/dev/null || mkdir -p $BBS_UPLOAD_DIR

podman run --pod $POD_NAME --name bbs-app -d \
    --label app=bbs \
    -e BBS_DB_PASSWORD=$BBS_DB_PASSWORD \
    -e SPRING_PROFILES_ACTIVE=podman \
    -v $BBS_UPLOAD_DIR:/home/asiayak/bbsUpload:Z \
    bbs-server:latest

echo ""

# 4. 启动 Nginx 容器
echo "--> 启动 Nginx..."
podman run --pod $POD_NAME --name bbs-nginx -d \
    --label app=bbs \
    bbs-nginx:latest

echo ""
echo "===== Pod 已创建 ====="
echo ""
echo "检查状态: podman pod ps"
echo "检查容器: podman ps -a --pod"
echo "查看日志:"
echo "  podman logs bbs-app"
echo "  podman logs bbs-nginx"
echo "  podman logs bbs-postgres"
echo ""
echo "访问地址:"
echo "  用户前端: http://<服务器IP>:18848/bbs-user/"
echo "  管理前端: http://<服务器IP>:18848/bbs-admin/"
echo "  API:      http://<服务器IP>:18848/bbs-server/"
echo ""
echo "停止: podman pod stop $POD_NAME"
echo "删除: podman pod rm $POD_NAME"
