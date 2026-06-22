# BBS Podman 部署指南

## 前提条件

- RHEL 7 服务器安装 Podman
- Node.js 14+（用于前端构建）
- JDK 8 + Maven（或使用 Docker 多阶段构建）

## 构建步骤

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
podman build -t bbs-server:latest -f bbs-server/Dockerfile .

# Nginx（在项目根目录执行，上下文为项目根）
podman build -t bbs-nginx:latest -f nginx/Dockerfile .
```

### 4. 部署

```bash
# 设置数据库密码
export BBS_DB_PASSWORD='your_secure_password'

# 创建并启动 Pod
./podman/create-pod.sh
```

### 5. 配置 systemd 自启（可选）

```bash
# 生成 systemd 配置
podman generate systemd --name bbs-pod --files --new

# 或者使用预置的服务文件
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
podman exec -it bbs-postgres psql -U bbs_user -d bbs

# 停止/删除
podman pod stop bbs-pod
podman pod rm bbs-pod
```

## SELinux 说明

卷挂载使用 `:Z` 标签会自动设置 SELinux 上下文。
如需手动设置：

```bash
semanage fcontext -a -t container_file_t "/home/asiayak/bbsUpload(/.*)?"
restorecon -Rv /home/asiayak/bbsUpload/

semanage fcontext -a -t container_file_t "/data/sql/postgre(/.*)?"
restorecon -Rv /data/sql/postgre/
```
