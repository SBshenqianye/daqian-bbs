#!/bin/bash
# ============================================
# BBS 一键部署脚本（离线环境）
# 在 RHEL 7 服务器上执行，简化 create-pod.sh 调用
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# --- 默认值 ---
DB_PASSWORD=""
DB_PORT="15432"
NGINX_PORT="18848"
SERVER_PORT="8083"
UPLOAD_DIR="/data/bbs/bbsUpload"
PG_DATA_DIR="/data/sql/postgre"
START_PG=0
SKIP_SCHEMA=1

# --- 帮助 ---
usage() {
    cat <<EOF
BBS 一键部署脚本

用法: $0 [选项]

选项:
  --db-password PWD     PostgreSQL 密码（必填）
  --db-port PORT        PostgreSQL 端口          [默认: 15432]
  --nginx-port PORT     Nginx 对外端口            [默认: 18848]
  --server-port PORT    后端内部端口              [默认: 8083]
  --upload-dir DIR      上传文件目录              [默认: /data/bbs/bbsUpload]
  --pg-data-dir DIR     PostgreSQL 数据目录       [默认: /data/sql/postgre]
  --start-pg            启动 PostgreSQL 容器      [默认: 否]
  --init-schema         首次部署时初始化数据库     [默认: 否，安全起见需要显式指定]
  --skip-schema         跳过数据库初始化（兼容旧版） [默认: 是]
  -h, --help            显示帮助信息

示例:
  # 首次部署（容器内 PostgreSQL，初始化数据库）
  $0 --db-password r123456 --start-pg --init-schema

  # 重部署（保留已有数据）
  $0 --db-password r123456 --start-pg
EOF
    exit 0
}

# --- 解析参数 ---
while [ $# -gt 0 ]; do
    case "$1" in
        --db-password)    DB_PASSWORD="$2";    shift 2 ;;
        --db-port)        DB_PORT="$2";        shift 2 ;;
        --nginx-port)     NGINX_PORT="$2";     shift 2 ;;
        --server-port)    SERVER_PORT="$2";    shift 2 ;;
        --upload-dir)     UPLOAD_DIR="$2";     shift 2 ;;
        --pg-data-dir)    PG_DATA_DIR="$2";    shift 2 ;;
        --start-pg)       START_PG=1;          shift   ;;
        --init-schema)    SKIP_SCHEMA=0;       shift   ;;
        --skip-schema)    SKIP_SCHEMA=1;       shift   ;;
        -h|--help)        usage                        ;;
        *) echo "错误: 未知参数 $1"; usage ;;
    esac
done

# --- 校验 ---
if [ -z "$DB_PASSWORD" ]; then
    echo "错误: --db-password 为必填项"
    echo ""
    usage
fi

# --- 导出环境变量（供 create-pod.sh 使用） ---
export BBS_DB_PASSWORD="$DB_PASSWORD"
export BBS_DB_PORT="$DB_PORT"
export BBS_NGINX_PORT="$NGINX_PORT"
export BBS_SERVER_PORT="$SERVER_PORT"
export BBS_UPLOAD_DIR="$UPLOAD_DIR"
export PG_DATA_DIR="$PG_DATA_DIR"
export START_POSTGRES="$START_PG"
export SKIP_SCHEMA_INIT="$SKIP_SCHEMA"

echo "=========================================="
echo " BBS 一键部署"
echo "=========================================="
echo " Nginx 端口:     $BBS_NGINX_PORT"
echo " 后端端口:       $BBS_SERVER_PORT"
echo " DB 端口:        $BBS_DB_PORT"
echo " 上传目录:       $BBS_UPLOAD_DIR"
echo " PG 数据目录:    $PG_DATA_DIR"
echo " 启动 PG 容器:   $([ "$START_POSTGRES" = "1" ] && echo '是' || echo '否')"
echo "=========================================="
echo ""

# --- 检查镜像是否已加载 ---
echo "-----> 检查容器镜像..."
IMAGES_OK=0
for img in daqian-bbs-server:offline daqian-bbs-nginx:offline; do
    if podman image exists "$img" 2>/dev/null; then
        echo "       ✓ $img"
    else
        echo "       ✗ $img 未加载"
        IMAGES_OK=1
    fi
done
if [ "$START_POSTGRES" = "1" ]; then
    if podman image exists daqian-bbs-postgres:12 2>/dev/null; then
        echo "       ✓ daqian-bbs-postgres:12"
    else
        echo "       ! daqian-bbs-postgres:12 未加载，将尝试使用 postgres:12"
    fi
fi

if [ "$IMAGES_OK" = "1" ]; then
    echo ""
    echo "错误: 部分容器镜像未加载。请先执行:"
    echo "  for f in images/*.tar; do podman load -i \$f; done"
    exit 1
fi

# --- 调用 create-pod.sh ---
echo ""
echo "-----> 调用 create-pod.sh..."
echo ""
cd "$SCRIPT_DIR"
exec ./podman/create-pod.sh
