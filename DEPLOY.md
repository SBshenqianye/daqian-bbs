# BBS 部署文档

## 目录

1. [架构概述](#1-架构概述)
2. [前置条件](#2-前置条件)
3. [快速开始](#3-快速开始)
4. [容器部署](#4-容器部署)
5. [打包分发](#5-打包分发)
6. [数据库初始化说明](#6-数据库初始化说明)
7. [故障排查](#7-故障排查)
8. [本次部署总结](#8-本次部署总结)

---

## 1. 架构概述

```
┌───────────────┐      ┌───────────────┐
│  浏览器/Windowns  │      │  Nginx (19848)│
│  bbs-ui/bbs-admin │─────▶│  反向代理 +   │
└───────────────┘      │  静态文件服务  │
                          │               │
                          └───────┬───────┘
                                  │
                          ┌───────▼───────┐
                          │  bbs-server   │
                          │  (9083)       │
                          │  Spring Boot  │
                          └───────┬───────┘
                                  │
                          ┌───────▼───────┐
                          │  PostgreSQL   │
                          │  (5432/15432) │
                          └───────────────┘
```

### 组件版本

| 组件 | 技术栈 | 端口 |
|------|--------|------|
| 用户前端 (bbs-ui) | Vue CLI 4.x, Webpack 4 | 静态文件 → Nginx |
| 管理后台 (bbs-admin-ui) | Vue CLI 5.x, Webpack 4 | 静态文件 → Nginx |
| 后端 (bbs-server) | Spring Boot 2.5.5, Java 8, MyBatis-Plus | 9083 |
| PostgreSQL | 13.x | 5432 (默认) / 15432 |
| Nginx | stable-alpine | 19848 |

---

## 2. 前置条件

### 2.1 开发/构建环境（WSL2）

```bash
# Node.js >= 14（推荐 v18 或 v22）
node --version

# npm
npm --version

# JDK 8+（编译后端需要）
java -version

# Maven
mvn --version

# Podman 或 Docker
podman --version
```

### 2.2 生产环境（RHEL 7）

```bash
# 安装 JDK 8
sudo yum install -y java-1.8.0-openjdk-devel

# 安装 PostgreSQL 13
sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
sudo yum install -y postgresql13-server
sudo /usr/pgsql-13/bin/postgresql-13-setup initdb
sudo systemctl enable postgresql-13
sudo systemctl start postgresql-13

# 安装 Nginx
sudo yum install -y nginx
sudo systemctl enable nginx

# 安装 envsubst（用于 Nginx 模板渲染）
sudo yum install -y gettext
```

### 2.3 配置 PostgreSQL

```bash
# RHEL 7 启动 PostgreSQL
sudo systemctl start postgresql-13

# 确认状态
pg_isready -h 127.0.0.1 -p 5432

# 如果数据目录在 /data/sql/postgre：
export PGDATA=/data/sql/postgre
pg_ctl start -D /data/sql/postgre

# 创建数据库用户
sudo -u postgres psql -c "CREATE USER work_flow WITH PASSWORD 'your_password';"
sudo -u postgres psql -c "CREATE DATABASE bbs OWNER work_flow;"
```

> **注意**: PostgreSQL 默认端口为 **5432**。如果生产环境使用自定义端口（如 15432），请在 `.env` 中设置 `BBS_DB_PORT=15432`。

---

## 3. 快速开始

```bash
# 1. 配置环境变量
cp scripts/.env.example .env
# 编辑 .env 修改数据库密码等

# 2. 一键构建 + 部署（并行构建前端 + Docker 镜像，自动启动容器）
bash scripts/build.sh

# 3. 访问
# 用户前端: http://localhost:19848/bbs-user/
# 管理后台: http://localhost:19848/bbs-admin/

# 4. 清理
bash scripts/ops/teardown.sh
```

---

## 4. 容器部署

### 4.1 构建

```bash
# 完整构建（前端 + 后端镜像 + Nginx 镜像）
bash scripts/build.sh

# 仅构建前端
bash scripts/build.sh --frontend

# 仅构建后端镜像
bash scripts/build.sh --backend
```

构建脚本会自动并行构建两个前端项目（bbs-ui 和 bbs-admin-ui），然后构建后端和 Nginx 的 Docker 镜像。
构建完成后**自动部署容器**并同时打包离线分发包。

### 4.2 部署

部署脚本单独使用（`build.sh` 已自动调用，通常无需手动执行）：

```bash
bash scripts/deploy/container.sh              # 部署后端 + Nginx
```

容器使用 **host 网络**模式：
- 后端通过 `127.0.0.1:${BBS_DB_PORT}` 直连宿主机 PostgreSQL
- Nginx 通过 `127.0.0.1:${BBS_SERVER_PORT}` 代理到后端
- 无需自定义网络、无需端口映射（`-p`）

---

## 5. 打包分发

### 5.1 打包命令

```bash
# 构建 + 自动打包（推荐）
bash scripts/build/build.sh                   # 容器模式 → bbs-offline-*.tar.gz

# 或用已有构建产物手动打包
bash scripts/dist/package.sh                  # 容器离线包（默认）
bash scripts/dist/package.sh --native         # 原生部署包
bash scripts/dist/package.sh --minimal        # 仅运行所需文件
bash scripts/dist/package.sh --with-source    # 含源代码
```

### 5.2 输出文件

打包完成后，在项目根目录生成：

```
bbs-deploy-YYYYMMDD-HHMMSS.tar.gz   # 具体版本包
bbs-deploy-latest.tar.gz            # 最新版软链接
```

### 5.3 解压后结构（容器离线包）

```
bbs-offline-YYYYMMDD-HHMMSS/
├── deploy.sh                # 一键部署入口
├── bbs-server.tar           # 后端镜像
├── bbs-nginx.tar            # Nginx 镜像（含前端静态文件）
├── .env.example             # 配置模板
├── docker/
│   ├── Dockerfile.nginx
│   └── Dockerfile.server
└── README.md
```

### 5.4 传输到目标服务器

```bash
# SCP 传输
scp bbs-deploy-20260701-120000.tar.gz user@target-server:/opt/

# 或在目标服务器上直接下载
# (如果自建了文件服务器)
wget http://build-server/bbs-deploy-latest.tar.gz
```

---

## 6. 数据库初始化说明

### 6.1 自动初始化机制

`DatabaseInitHelper.java` 在 Spring Boot 启动前执行：

1. 连接 PostgreSQL，检查数据库是否存在
2. 不存在 → 自动创建数据库
3. **始终**执行 `init-pg.sql`（安全模式）

### 6.2 SQL 安全模式

`init-pg.sql` 已改为可重复执行的安全模式：

| 操作 | 旧版本 | 新版本（安全模式） |
|------|--------|-------------------|
| 建表 | `DROP TABLE IF EXISTS ... CREATE TABLE` | `CREATE TABLE IF NOT EXISTS` |
| 插入基础数据 | `INSERT INTO ...` | `INSERT INTO ... ON CONFLICT DO NOTHING` |
| 序列重置 | `SELECT setval(...)` | `SELECT setval(GREATEST(...))` |

这意味着：
- 首次执行 → 建表 + 插入基础数据
- 重复执行 → 不会覆盖已有数据，不会报错
- 数据迁移 → 安全追加，不破坏已有记录

### 6.3 手动初始化

```bash
# 如果自动初始化失败，或需要重新执行
bash scripts/init-db.sh
```

---

## 7. 故障排查

### 7.1 前端构建失败

**问题**: `ERR_OSSL_EVP_UNSUPPORTED`
**原因**: Node.js 17+ 移除了 OpenSSL legacy provider，而 Webpack 4 依赖它
**解决**:
```bash
NODE_OPTIONS="--openssl-legacy-provider" npm run build
```
脚本已自动处理此问题。

### 7.2 后端无法连接数据库

**问题**: `relation "bbs_sensitive_word" does not exist`
**原因**: 数据库已存在（如通过 `POSTGRES_DB` 自动创建），但表结构未初始化
**解决**: 
1. 新版本已自动处理（SQL 安全模式始终执行）
2. 如仍未解决，手动运行:
```bash
bash scripts/init-db.sh
```

### 7.3 容器内连接不上 PostgreSQL

**问题**: `Connection refused`
**原因**: 容器使用 host 网络，但 PostgreSQL 未监听 `127.0.0.1` 或端口不匹配
**解决**:
- 确认 PG 已启动: `pg_isready -h 127.0.0.1 -p ${BBS_DB_PORT}`
- 检查 `.env` 中 `BBS_DB_HOST` 和 `BBS_DB_PORT` 配置

### 7.4 Docker Hub 拉取超时（中国网络）

**问题**: `timeout` 或 `TLS handshake timeout`
**原因**: 中国网络对 docker.io 的访问受限
**解决**: 配置镜像加速器

```bash
# ~/.config/containers/registries.conf
unqualified-search-registries = ["docker.io"]
[[registry]]
prefix = "docker.io"
location = "docker.io"
[[registry.mirror]]
location = "docker.m.daocloud.io"
```

---

## 8. 本次部署总结

### 8.1 架构决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 容器化方案 | Podman（rootless） | WSL2 兼容性好，无需 Docker daemon |
| 网络模式 | host 网络 | 简化网络拓扑，容器直连宿主机服务 |
| 前端服务 | Nginx 反向代理 | 统一入口，方便配置 CORS 和路由 |
| 镜像仓库 | Docker Hub + DaoCloud 镜像 | 中国网络环境下 Docker Hub 不可直接访问 |
| 数据库初始化 | Spring Boot 启动前 JDBC 初始化 | 不依赖 Spring 容器，保证表结构先就绪 |

### 8.2 关键参数对照

| 参数 | 值 | 配置位置 |
|------|-----|----------|
| 后端端口 | 9083 | `application.yml` → `server.port` |
| 后端上下文路径 | `/bbs-server` | `application.yml` → `server.servlet.context-path` |
| 数据库端口（生产） | 15432 | `application-podman.yml` → `BBS_DB_PORT` 默认值 |
| 数据库端口（默认） | 5432 | PostgreSQL 默认 |
| Nginx 监听端口 | 19848 | 环境变量 `NGINX_PORT` |
| Nginx → 后端上游 | `127.0.0.1:${BBS_SERVER_PORT}` | `nginx.conf.template` |
| 数据库主机 | `127.0.0.1` | 环境变量 `BBS_DB_HOST` |
| 上传目录 | `/data/bbs/bbsUpload` | 环境变量 `BBS_UPLOAD_DIR` |
| PostgreSQL 数据目录 | `/data/sql/postgre` | RHEL 7 生产环境配置 |

### 8.3 部署流程总结

```
源代码
  │
  ├── bbs-ui (Vue CLI 4)       ── npm install + npm build ──▶ dist/
  ├── bbs-admin-ui (Vue CLI 5) ── npm install + NODE_OPTIONS build ──▶ dist/
  └── bbs-server (Spring Boot) ── podman build ──▶ bbs-server 镜像
                                                      │
                                          ┌───────────┴───────────┐
                                          │                       │
                                    ┌─────▼─────┐           ┌─────▼─────┐
                                    │ 容器部署   │           │ 离线打包   │
                                    │ host 网络  │           │ tar.gz     │
                                    │ bbs-server │           │            │
                                    │ bbs-nginx  │           │            │
                                    └───────────┘           └───────────┘
```

### 8.4 经验教训

1. **Node 22 + Webpack 4**: 必须设置 `NODE_OPTIONS="--openssl-legacy-provider"` 才能构建
2. **容器网络**: 容器使用 host 网络，通过 `127.0.0.1` 直连宿主机服务
3. **数据库初始化**: `DatabaseInitHelper.java` 旧逻辑是"DB 已存在 → 跳过 init"，导致 `POSTGRES_DB` 创建的空白数据库没有表结构。新版本改为始终执行安全的 init SQL
4. **Rootless Podman**: host 网络模式下无需端口映射，简化网络配置
5. **中国网络环境**: Docker Hub 拉取镜像需要配置镜像加速器（如 DaoCloud）
6. **构建上下文**: `nginx/Dockerfile` 需要在项目根目录构建（因 COPY 引用了前端 dist 的相对路径）

---

## 附录：环境变量完整列表

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `BBS_DB_HOST` | `127.0.0.1` | PostgreSQL 主机 |
| `BBS_DB_PORT` | `15432` | PostgreSQL 端口 |
| `BBS_DB_NAME` | `bbs` | 数据库名 |
| `BBS_DB_USER` | `work_flow` | 数据库用户 |
| `BBS_DB_PASSWORD` | — | 数据库密码（必须设置） |
| `BBS_SERVER_PORT` | `9083` | 后端端口 |
| `NGINX_PORT` | `19848` | Nginx 监听端口 |
| `BBS_UPLOAD_DIR` | `/data/bbs/bbsUpload` | 文件上传目录 |
| `BBS_SUPER_ADMIN_PASSWORD` | `1234@abcD` | 超级管理员密码 |
| `BBS_SERVER_CONTAINER` | `bbs-server` | 后端容器名 |
| `BBS_NGINX_CONTAINER` | `bbs-nginx` | Nginx 容器名 |
