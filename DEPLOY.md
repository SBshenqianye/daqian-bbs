# BBS 部署文档

## 目录

1. [架构概述](#1-架构概述)
2. [前置条件](#2-前置条件)
3. [快速开始](#3-快速开始)
4. [容器部署（WSL2 / Podman）](#4-容器部署wsl2--podman)
5. [原生部署（RHEL 7 / CentOS 7）](#5-原生部署rhel-7--centos-7)
6. [打包分发](#6-打包分发)
7. [数据库初始化说明](#7-数据库初始化说明)
8. [故障排查](#8-故障排查)
9. [本次部署总结](#9-本次部署总结)

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

### 3.1 容器部署（WSL2 开发环境）

```bash
# 1. 配置环境变量
cp scripts/.env.example .env
# 编辑 .env 修改数据库密码等

# 2. 一键构建全部（并行构建前端 + Docker 镜像）
bash scripts/build.sh

# 3. 一键部署
bash scripts/deploy-container.sh

# 4. 访问
# 用户前端: http://localhost:19848/bbs-user/
# 管理后台: http://localhost:19848/bbs-admin/

# 5. 清理
bash scripts/teardown.sh
```

### 3.2 原生部署（RHEL 7 生产环境）

```bash
# 1. 配置环境变量
cp scripts/.env.example .env
vi .env

# 2. 构建（编译后端 JAR，不构建容器镜像）
bash scripts/build.sh --native

# 3. 部署
bash scripts/deploy-native.sh

# 4. 访问
# 用户前端: http://<服务器IP>:19848/bbs-user/
# 管理后台: http://<服务器IP>:19848/bbs-admin/

# 5. 清理
bash scripts/teardown.sh --native
```

---

## 4. 容器部署（WSL2 / Podman）

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

### 4.2 部署

```bash
# 完整部署（PostgreSQL + 后端 + Nginx）
bash scripts/deploy-container.sh

# 仅启动 PostgreSQL
bash scripts/deploy-container.sh --pg-only

# 跳过 PostgreSQL（使用已有数据库）
bash scripts/deploy-container.sh --no-pg
```

### 4.3 网络说明

容器使用 Podman 自定义 bridge 网络 `bbs-net`，容器间通过**容器名**通信：

| 通信链路 | 方式 |
|----------|------|
| 浏览器 ↔ Nginx | `localhost:19848`（主机端口映射） |
| Nginx → 后端 | `bbs-server:9083`（容器名 + 端口） |
| 后端 → PostgreSQL | `bbs-postgres:5432`（容器名 + 端口） |

---

## 5. 原生部署（RHEL 7 / CentOS 7）

### 5.1 构建

```bash
# 在开发/构建机上执行
bash scripts/build.sh --native
```

这将编译 `bbs-server/target/bbs-server.jar`，并构建前端 dist 目录。

### 5.2 打包分发

```bash
# 打包成 tar.gz 用于传输到生产服务器
bash scripts/package.sh
# 输出: bbs-deploy-YYYYMMDD-HHMMSS.tar.gz
```

### 5.3 部署步骤（在生产服务器上执行）

#### 5.3.1 解压包

```bash
tar -xzf bbs-deploy-20260701-120000.tar.gz
cd bbs-deploy-20260701-120000
cp scripts/.env.example .env
vi .env   # 配置数据库连接信息
```

#### 5.3.2 初始化数据库

```bash
# 方式一：通过部署脚本（自动执行）
bash scripts/deploy-native.sh

# 方式二：手动初始化
bash scripts/init-db.sh -h 127.0.0.1 -p 5432 -U work_flow -d bbs
```

#### 5.3.3 部署后端

```bash
# 部署脚本会自动处理以下步骤:
# 1. 停止旧进程
# 2. 启动新进程 (java -jar bbs-server.jar)
# 3. 创建 systemd 服务（可选）
# 4. 配置 Nginx
bash scripts/deploy-native.sh
```

#### 5.3.4 系统服务管理

```bash
# 使用 systemd 管理（脚本会自动创建服务）
sudo systemctl start bbs-server
sudo systemctl stop bbs-server
sudo systemctl restart bbs-server
sudo journalctl -u bbs-server -f   # 查看实时日志

# Nginx 管理
sudo systemctl reload nginx
sudo nginx -t   # 测试配置
```

---

## 6. 打包分发

### 6.1 打包命令

```bash
# 标准打包（仅构建产物）
bash scripts/package.sh

# 最小打包（仅运行所需文件）
bash scripts/package.sh --minimal

# 含源代码打包
bash scripts/package.sh --with-source
```

### 6.2 输出文件

打包完成后，在项目根目录生成：

```
bbs-deploy-YYYYMMDD-HHMMSS.tar.gz   # 具体版本包
bbs-deploy-latest.tar.gz            # 最新版软链接
```

### 6.3 解压后结构

```
bbs-deploy-YYYYMMDD-HHMMSS/
├── bbs-server.jar          # 后端 JAR
├── bbs-ui/                 # 用户前端静态文件
├── bbs-admin-ui/           # 管理后台静态文件
├── nginx/
│   ├── Dockerfile
│   └── nginx.conf.template
├── db/init/
│   └── init-pg.sql         # 数据库初始化 SQL
├── scripts/
│   ├── deploy-native.sh    # 原生部署脚本
│   ├── deploy-container.sh # 容器部署脚本
│   ├── teardown.sh         # 清理脚本
│   ├── init-db.sh          # 数据库初始化脚本
│   ├── build.sh            # 构建脚本
│   └── .env.example        # 环境变量配置
├── DEPLOY.md               # 部署文档（如打包时存在）
└── README.txt              # 快速入门
```

### 6.4 传输到目标服务器

```bash
# SCP 传输
scp bbs-deploy-20260701-120000.tar.gz user@target-server:/opt/

# 或在目标服务器上直接下载
# (如果自建了文件服务器)
wget http://build-server/bbs-deploy-latest.tar.gz
```

---

## 7. 数据库初始化说明

### 7.1 自动初始化机制

`DatabaseInitHelper.java` 在 Spring Boot 启动前执行：

1. 连接 PostgreSQL，检查数据库是否存在
2. 不存在 → 自动创建数据库
3. **始终**执行 `init-pg.sql`（安全模式）

### 7.2 SQL 安全模式

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

### 7.3 手动初始化

```bash
# 如果自动初始化失败，或需要重新执行
bash scripts/init-db.sh
```

---

## 8. 故障排查

### 8.1 前端构建失败

**问题**: `ERR_OSSL_EVP_UNSUPPORTED`
**原因**: Node.js 17+ 移除了 OpenSSL legacy provider，而 Webpack 4 依赖它
**解决**:
```bash
NODE_OPTIONS="--openssl-legacy-provider" npm run build
```
脚本已自动处理此问题。

### 8.2 后端无法连接数据库

**问题**: `relation "bbs_sensitive_word" does not exist`
**原因**: 数据库已存在（如通过 `POSTGRES_DB` 自动创建），但表结构未初始化
**解决**: 
1. 新版本已自动处理（SQL 安全模式始终执行）
2. 如仍未解决，手动运行:
```bash
bash scripts/init-db.sh
```

### 8.3 容器内连接不上 PostgreSQL

**问题**: `Connection refused` 或 `no route to host`
**原因**: 容器内 `127.0.0.1` ≠ 主机的 `127.0.0.1`
**解决**: 
- 容器部署时，通过容器名通信: `BBS_DB_HOST=bbs-postgres`
- 原生部署时，用实际 IP 或 `127.0.0.1`

### 8.4 Nginx 端口不通

**问题**: `Connection reset by peer`
**原因**: 容器未加入自定义网络（如使用默认 `pasta` 网络）
**解决**:
```bash
podman rm -f bbs-nginx
podman run -d --name bbs-nginx --network bbs-net -p 19848:19848 -e NGINX_PORT=19848 -e BBS_SERVER_PORT=9083 bbs-nginx
```

### 8.5 Docker Hub 拉取超时（中国网络）

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

## 9. 本次部署总结

### 9.1 架构决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 容器化方案 | Podman（rootless） | WSL2 兼容性好，无需 Docker daemon |
| 网络模式 | 自定义 bridge（bbs-net） | 容器间通过名称通信，避免 127.0.0.1 问题 |
| 前端服务 | Nginx 反向代理 | 统一入口，方便配置 CORS 和路由 |
| 镜像仓库 | Docker Hub + DaoCloud 镜像 | 中国网络环境下 Docker Hub 不可直接访问 |
| 数据库初始化 | Spring Boot 启动前 JDBC 初始化 | 不依赖 Spring 容器，保证表结构先就绪 |

### 9.2 关键参数对照

| 参数 | 值 | 配置位置 |
|------|-----|----------|
| 后端端口 | 9083 | `application.yml` → `server.port` |
| 后端上下文路径 | `/bbs-server` | `application.yml` → `server.servlet.context-path` |
| 数据库端口（生产） | 15432 | `application-podman.yml` → `BBS_DB_PORT` 默认值 |
| 数据库端口（默认） | 5432 | PostgreSQL 默认 |
| Nginx 监听端口 | 19848 | 环境变量 `NGINX_PORT` |
| Nginx → 后端上游 | `bbs-server:${BBS_SERVER_PORT}` | `nginx.conf.template` |
| 数据库主机（容器） | `bbs-postgres`（容器名） | 环境变量 `BBS_DB_HOST` |
| 数据库主机（原生） | `127.0.0.1` | 环境变量 `BBS_DB_HOST` |
| 上传目录 | `/data/bbs/bbsUpload` | 环境变量 `BBS_UPLOAD_DIR` |
| PostgreSQL 数据目录 | `/data/sql/postgre` | RHEL 7 生产环境配置 |

### 9.3 部署流程总结

```
源代码
  │
  ├── bbs-ui (Vue CLI 4)       ── npm install + npm build ──▶ dist/
  ├── bbs-admin-ui (Vue CLI 5) ── npm install + NODE_OPTIONS build ──▶ dist/
  └── bbs-server (Spring Boot) ── mvn package ──▶ bbs-server.jar
                                           │
                              ┌────────────┴────────────┐
                              │                         │
                    ┌─────────▼─────────┐     ┌─────────▼─────────┐
                    │ 容器部署           │     │ 原生部署 (RHEL 7)  │
                    │ Podman build      │     │ java -jar          │
                    │ bbs-server 镜像    │     │ Nginx 配置          │
                    │ bbs-nginx 镜像     │     │ systemd 服务        │
                    │ bbs-net 网络       │     │                    │
                    └───────────────────┘     └────────────────────┘
```

### 9.4 经验教训

1. **Node 22 + Webpack 4**: 必须设置 `NODE_OPTIONS="--openssl-legacy-provider"` 才能构建
2. **容器网络**: 容器内 `127.0.0.1` 指向容器自身，不是宿主机。跨容器通信必须使用自定义网络 + 容器名
3. **数据库初始化**: `DatabaseInitHelper.java` 旧逻辑是"DB 已存在 → 跳过 init"，导致 `POSTGRES_DB` 创建的空白数据库没有表结构。新版本改为始终执行安全的 init SQL
4. **Rootless Podman 端口映射**: 需要将容器加入自定义 bridge 网络才能正确映射端口
5. **中国网络环境**: Docker Hub 拉取镜像需要配置镜像加速器（如 DaoCloud）
6. **构建上下文**: `nginx/Dockerfile` 需要在项目根目录构建（因 COPY 引用了前端 dist 的相对路径）

---

## 附录：环境变量完整列表

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `BBS_DB_HOST` | `127.0.0.1` / `bbs-postgres` | PostgreSQL 主机 |
| `BBS_DB_PORT` | `5432` | PostgreSQL 端口 |
| `BBS_DB_NAME` | `bbs` | 数据库名 |
| `BBS_DB_USER` | `work_flow` | 数据库用户 |
| `BBS_DB_PASSWORD` | — | 数据库密码（必须设置） |
| `BBS_SERVER_PORT` | `9083` | 后端端口 |
| `NGINX_PORT` | `19848` | Nginx 监听端口 |
| `BBS_UPLOAD_DIR` | `/data/bbs/bbsUpload` | 文件上传目录 |
| `BBS_SUPER_ADMIN_PASSWORD` | `1234@abcD` | 超级管理员密码 |
| `BBS_PG_DATA` | `/data/sql/postgre` | PostgreSQL 数据目录（RHEL 7） |
| `BBS_NET_NAME` | `bbs-net` | 容器网络名 |
