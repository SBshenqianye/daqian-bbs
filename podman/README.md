# BBS Podman 部署指南

## 前提条件

- RHEL 7 服务器安装 Podman
- 已导入离线容器镜像（详见 `部署说明.md`）
- 或能联网构建（开发机）
- JDK 8 + Maven（如需在开发机重新构建）

## 在开发机（有网络）构建容器镜像

### 1. 构建后端 JAR

```bash
cd bbs-server
mvn clean package -DskipTests
```

### 2. 构建前端

```bash
# bbs-ui
cd bbs-ui
npm install && npm run build

# bbs-admin-ui
cd bbs-admin-ui
npm install && npm run build
```

### 3. 构建容器镜像

```bash
# 后端（在项目根目录执行）
podman build -t daqian-bbs-server:offline -f bbs-server/Dockerfile .

# Nginx（在项目根目录执行，上下文为项目根）
podman build -t daqian-bbs-nginx:offline -f nginx/Dockerfile .
```

### 4. 导出镜像（用于离线部署）

```bash
podman save -o bbs-server.tar daqian-bbs-server:offline
podman save -o bbs-nginx.tar daqian-bbs-nginx:offline
podman save -o postgres-12.tar docker.io/postgres:12
```

## 部署

### 方式一：快速部署（所有配置通过环境变量）

```bash
# 设置必填项
export BBS_DB_PASSWORD='r123456'

# 可选：修改端口
export BBS_DB_PORT=15432
export BBS_NGINX_PORT=18848

# 创建并启动 Pod
./podman/create-pod.sh
```

支持 `--help` 查看所有配置项：

```bash
./podman/create-pod.sh --help
```

### 方式二：全自动部署（使用 deploy.sh）

```bash
chmod +x deploy.sh
./deploy.sh \
    --db-password r123456 \
    --db-port 15432 \
    --nginx-port 18848 \
    --upload-dir /data/bbs/bbsUpload \
    --pg-data-dir /data/sql/postgre \
    --start-pg
```

## 配置 systemd 自启（可选）

```bash
# 创建环境变量配置文件
cat > /etc/bbs-pod.env << EOF
BBS_DB_PASSWORD=r123456
BBS_NGINX_PORT=18848
BBS_SERVER_PORT=8083
BBS_DB_PORT=15432
BBS_UPLOAD_DIR=/data/bbs/bbsUpload
PG_DATA_DIR=/data/sql/postgre
START_POSTGRES=1
EOF

# 安装服务
cp podman/bbs-pod.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable bbs-pod.service
systemctl start bbs-pod.service
```

## 常用命令

```bash
# 查看 Pod 状态
podman pod ps

# 查看容器日志
podman logs -f bbs-app
podman logs -f bbs-nginx
podman logs -f bbs-postgres

# 进入容器
podman exec -it bbs-app /bin/bash
podman exec -it bbs-postgres psql -U work_flow -d bbs

# 停止/删除
podman pod stop bbs-pod
podman pod rm bbs-pod
```

## SELinux 说明

卷挂载使用 `:Z` 标签会自动设置 SELinux 上下文。
如需手动设置：

```bash
semanage fcontext -a -t container_file_t "/data/bbs/bbsUpload(/.*)?"
restorecon -Rv /data/bbs/bbsUpload/

semanage fcontext -a -t container_file_t "/data/sql/postgre(/.*)?"
restorecon -Rv /data/sql/postgre/
```
