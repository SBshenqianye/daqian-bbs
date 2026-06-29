#!/bin/bash
# ============================================
# BBS 离线部署包打包脚本
# 在开发机（有网络）上执行，将所有产物打包为 tar.gz
# 开发机环境：Windows 11 + Git Bash / WSL
# 依赖：Maven, Node.js 18+, Podman
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_DIR="${PROJECT_DIR}/daqian-bbs-offline"
OUTPUT_TAR="${PROJECT_DIR}/daqian-bbs-offline.tar.gz"

BBS_SERVER_IMAGE="${BBS_SERVER_IMAGE:-daqian-bbs-server:offline}"
BBS_NGINX_IMAGE="${BBS_NGINX_IMAGE:-daqian-bbs-nginx:offline}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-docker.io/postgres:12}"

echo "=========================================="
echo " BBS 离线部署包打包工具"
echo "=========================================="
echo "项目路径: ${PROJECT_DIR}"
echo "输出目录: ${OUTPUT_DIR}"
echo ""

# ============================================
# Step 1: 清除旧的输出目录
# ============================================
echo "===== [1/7] 清理旧的输出目录 ====="
rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}/images"
mkdir -p "${OUTPUT_DIR}/podman"
mkdir -p "${OUTPUT_DIR}/scripts"
echo "       done"

# ============================================
# Step 2: 构建后端 JAR
# ============================================
echo "===== [2/7] 构建后端 JAR ====="
cd "${PROJECT_DIR}/bbs-server"
mvn clean package -DskipTests -B
cp target/bbs-server.jar "${OUTPUT_DIR}/"
echo "       done: bbs-server.jar"

# ============================================
# Step 3: 构建前端
# ============================================
echo "===== [3/7] 构建前端 bbs-ui ====="
cd "${PROJECT_DIR}/bbs-ui"
npm install --silent
npm run build
echo "       done: bbs-ui/dist"

echo "===== [4/7] 构建前端 bbs-admin-ui ====="
cd "${PROJECT_DIR}/bbs-admin-ui"
npm install --silent
npm run build
echo "       done: bbs-admin-ui/dist"

# ============================================
# Step 4: 构建容器镜像
# ============================================
echo "===== [5/7] 构建后端容器镜像 ====="
cd "${PROJECT_DIR}"
podman build -t "${BBS_SERVER_IMAGE}" -f bbs-server/Dockerfile .
echo "       done: ${BBS_SERVER_IMAGE}"

echo "===== [6/7] 构建 Nginx 容器镜像 ====="
podman build -t "${BBS_NGINX_IMAGE}" -f nginx/Dockerfile .
echo "       done: ${BBS_NGINX_IMAGE}"

# ============================================
# Step 5: 导出容器镜像为 tar
# ============================================
echo "===== [7/7] 导出容器镜像 ====="
podman save -o "${OUTPUT_DIR}/images/bbs-server.tar" "${BBS_SERVER_IMAGE}"
echo "       done: images/bbs-server.tar"
podman save -o "${OUTPUT_DIR}/images/bbs-nginx.tar" "${BBS_NGINX_IMAGE}"
echo "       done: images/bbs-nginx.tar"

# 拉取并导出 PostgreSQL 镜像
if ! podman image exists "${POSTGRES_IMAGE}" 2>/dev/null; then
    echo "       Pulling PostgreSQL image..."
    podman pull "${POSTGRES_IMAGE}"
fi
podman save -o "${OUTPUT_DIR}/images/postgres-12.tar" "${POSTGRES_IMAGE}"
echo "       done: images/postgres-12.tar"

# ============================================
# Step 6: 复制配置和脚本文件
# ============================================
echo "===== 复制配置和脚本 ====="
cp "${PROJECT_DIR}/podman/create-pod.sh"   "${OUTPUT_DIR}/podman/"
cp "${PROJECT_DIR}/podman/bbs-pod.service" "${OUTPUT_DIR}/podman/"
cp "${PROJECT_DIR}/podman/README.md"       "${OUTPUT_DIR}/podman/"
cp "${PROJECT_DIR}/scripts/bbs-pg-schema.sql"   "${OUTPUT_DIR}/scripts/"
cp "${PROJECT_DIR}/scripts/init-pg-database.sh"  "${OUTPUT_DIR}/scripts/"
cp "${PROJECT_DIR}/scripts/migrate-mysql-to-pg.sh" "${OUTPUT_DIR}/scripts/"
cp "${PROJECT_DIR}/podman/部署说明.md" "${OUTPUT_DIR}/"

# 复制 deploy.sh（如果存在）
if [ -f "${PROJECT_DIR}/podman/deploy.sh" ]; then
    cp "${PROJECT_DIR}/podman/deploy.sh" "${OUTPUT_DIR}/"
fi

chmod +x "${OUTPUT_DIR}/podman/create-pod.sh"
chmod +x "${OUTPUT_DIR}/scripts/"*.sh
echo "       done"

# ============================================
# Step 7: 打包
# ============================================
echo "===== 打包为 tar.gz ====="
cd "${PROJECT_DIR}"
tar -czf "${OUTPUT_TAR}" -C "$(dirname "${OUTPUT_DIR}")" "$(basename "${OUTPUT_DIR}")"
echo "       done: ${OUTPUT_TAR}"

# ============================================
# 完成
# ============================================
echo ""
echo "=========================================="
echo " 离线部署包已生成！"
echo "=========================================="
echo "打包文件: ${OUTPUT_TAR}"
echo "大小:     $(du -h "${OUTPUT_TAR}" | cut -f1)"
echo ""
echo "部署步骤："
echo "  1. 将 daqian-bbs-offline.tar.gz 传输到 RHEL 7 服务器"
echo "  2. 解压: tar -xzf daqian-bbs-offline.tar.gz"
echo "  3. 导入镜像: cd daqian-bbs-offline && for f in images/*.tar; do podman load -i \"\$f\"; done"
echo "  4. 部署: export BBS_DB_PASSWORD='r123456' && ./podman/create-pod.sh"
echo "=========================================="
