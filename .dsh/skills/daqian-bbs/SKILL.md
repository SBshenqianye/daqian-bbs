---
name: daqian-bbs
description: 大千智荟 BBS 论坛全栈项目（Vue 2 用户前台 bbs-ui + Vue 2/Element UI 管理后台 bbs-admin-ui + Spring Boot 后端 bbs-server）的开发、构建、部署与数据库迁移指南。涉及双数据库（MySQL/PostgreSQL）四文件同步迁移规范、WSL bind-mount 部署、离线升级打包、常见坑位。在本仓库做任何代码修改、SQL 变更、构建部署或故障排查前先加载本技能。
whenToUse: 在 daqian-bbs 仓库中进行功能开发、数据库 schema/数据变更、前端构建、后端打包、容器部署、故障排查等任务时。
---

# 大千智荟 BBS 项目指南

> 本技能是 DSH 智能体在本仓库工作的入口文档，与仓库内 `CLAUDE.md`、`DEPLOY.md`、`scripts/SCRIPT.md` 配套使用。修改仓库前先读本文件，涉及数据库变更必须先读"数据库迁移规范"一节。

## 1. 项目概览

大千智荟 BBS 是一个论坛系统，包含三个子项目：

| 模块 | 目录 | 技术栈 | 说明 |
|------|------|--------|------|
| 用户前台 | `bbs-ui/` | Vue 2.6 + Vue CLI 5 + Element UI + mavon-editor（Markdown 编辑器）+ Tailwind CSS | 面向普通用户的论坛界面 |
| 管理后台 | `bbs-admin-ui/` | Vue 2.6 + Vue CLI 5 + Element UI | 面向管理员的运营后台 |
| 后端 | `bbs-server/` | Spring Boot 2.5.5 + Java 8 + MyBatis-Plus 3.5.1 + Druid + JWT + Swagger2 | REST API + 数据库访问 |

**关键端口**：后端开发默认 `9083`（context-path `/bbs-server`），生产 `60000`；Nginx `60001`；PostgreSQL `5432`（生产可 15432）。

## 2. 仓库结构

```
daqian-bbs/
├── bbs-ui/              # 用户前台（Vue 2）
├── bbs-admin-ui/        # 管理后台（Vue 2 + Element UI）
├── bbs-server/          # Java 后端（Spring Boot + MyBatis-Plus）
│   └── src/main/resources/
│       ├── db/init/     # ★ 数据库脚本（见第 4 节）
│       ├── mapper/      # MyBatis XML 映射
│       └── application*.yml
├── docs/                # 数据迁移资料（org-migration：组织机构导入 CSV/SQL）
├── nginx/               # Nginx 配置与 Dockerfile
├── scripts/             # 构建/部署/运维脚本（详见 scripts/SCRIPT.md）
├── CLAUDE.md            # 项目约定（数据库迁移规范 + 结构）
├── DEPLOY.md            # 部署文档（架构、容器、WSL、离线）
└── .env                 # 部署环境变量（复制自 scripts/.env.example，含密钥不入库）
```

后端代码位于 `bbs-server/src/main/java/com/walker/`：`controller` / `service`(+`impl`) / `mapper` / `pojo` / `vo` / `config`(含 `security`) / `utils`。

## 3. 开发与构建

### 3.1 前端

```bash
# 用户前台
cd bbs-ui && npm install
npm run dev                 # 开发服务器
npm run build               # 生产构建 → dist/
npm run build:dev           # localdev 模式构建

# 管理后台（npm scripts 已内置 NODE_OPTIONS，无需手动加）
cd bbs-admin-ui && npm install
npm run serve               # 开发服务器
npm run build               # 生产构建 → dist/
```

> ⚠ Node 17+ 构建 Webpack 4 项目必须设 `NODE_OPTIONS="--openssl-legacy-provider"`，否则报 `ERR_OSSL_EVP_UNSUPPORTED`。`bbs-admin-ui` 的 npm scripts 已内置；手动执行 `vue-cli-service build` 时要自己加。

### 3.2 后端

```bash
cd bbs-server
mvn clean package -DskipTests   # 打包 → target/bbs-server.jar
```

- Spring profiles：`application.yml` 默认 `dev,local`；容器环境用 `application-podman.yml`。
- 后端启动前 `DatabaseInitHelper` 会**始终**执行 `init-pg.sql`（安全模式，可重复执行）。
- 超级管理员：`asiayak`（密码默认 `1234@abcD`，用 `BBS_SUPER_ADMIN_PASSWORD` 覆盖）。

### 3.3 开发机 → WSL 快速部署（bind-mount 模式）

```bash
# Windows 上构建产物（JAR + dist）→ 同步到 WSL → 执行
bash scripts/deploy/wsl.sh              # 检测产物 + 部署（秒级）
bash scripts/deploy/wsl.sh --restart-only  # 仅重启容器（代码已挂载）
# 热更新：podman restart bbs-server bbs-nginx
```

## 4. 数据库迁移规范（★ 最高优先级）

项目同时支持 **MySQL** 和 **PostgreSQL**。**任何 schema/数据变更必须同时更新 4 个文件**，不允许只改其中一个：

```
bbs-server/src/main/resources/db/init/
├── init-mysql.sql      # 新环境完整建表（MySQL，CREATE TABLE IF NOT EXISTS）
├── init-pg.sql         # 新环境完整建表（PostgreSQL，CREATE TABLE IF NOT EXISTS）
├── upgrade-mysql.sql   # 增量变更（MySQL，每次重启执行，必须幂等）
└── upgrade-pg.sql      # 增量变更（PostgreSQL，每次重启执行，必须幂等）
```

**修改规则**：业务需求 → 先加 upgrade 增量 step（幂等），再同步 init 建表与初始数据。upgrade 和 init 总是同步更新，MySQL 和 PostgreSQL 总是同步更新。

**幂等要求**：
- MySQL upgrade：DDL 用 `information_schema` 条件判断 + `PREPARE`/`EXECUTE`；DML 用 `WHERE NOT EXISTS`。
- PostgreSQL upgrade：用原生 `IF NOT EXISTS` / `ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS`。
- init 脚本：`CREATE TABLE IF NOT EXISTS` / `INSERT ... ON CONFLICT DO NOTHING`。

**字段类型对照**：

| MySQL | PostgreSQL |
|-------|------------|
| `tinyint(1)` | `smallint` |
| `int(11)` | `integer` |
| `longtext` | `text` |
| `varchar(N)` | `varchar(N)` |
| `datetime` / `varchar(20)` | `varchar(20)`（本项目统一用字符串存时间） |

**其他约定**：表前缀统一 `bbs_`；时间字段用字符串存；`init-*.sql` 只含首次部署初始数据（超级管理员、标签、组织机构、数据字典、敏感词），不含测试文章数据。

主要表：`bbs_admin`、`bbs_user`、`bbs_article`、`bbs_article_file`、`bbs_article_label`、`bbs_article_type`、`bbs_article_user`、`bbs_comment`、`bbs_reply`、`bbs_community`、`bbs_community_user`、`bbs_fans`、`bbs_dict`、`bbs_system_config`、`bbs_sa_org`、`bbs_sensitive_word`、`bbs_slideshow`、`bbs_area`、`bbs_inventory`。

## 5. 环境变量（部署）

`.env` 从 `scripts/.env.example` 复制。关键项：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `BBS_DB_HOST` / `BBS_DB_PORT` | `127.0.0.1` / `15432` | PostgreSQL 连接 |
| `BBS_DB_NAME` / `BBS_DB_USER` / `BBS_DB_PASSWORD` | `bbs` / `work_flow` / — | 数据库 |
| `BBS_SERVER_PORT` | `60000`（生产） | 后端端口；开发默认 9083 |
| `NGINX_PORT` | `60001` | Nginx 监听 |
| `BBS_UPLOAD_DIR` | `/data/bbs/bbsUpload/` | 上传目录，**必须以 / 结尾** |
| `BBS_SUPER_ADMIN_PASSWORD` | `1234@abcD` | 超级管理员密码 |

## 6. 部署与打包

- **架构**：Nginx(60001) 静态托管 `bbs-ui`、`bbs-admin-ui` 并反代 `/bbs-server` → 后端(60000)；后端直连宿主机 PostgreSQL；容器用 **host 网络**（Podman rootless），无需 `-p` 端口映射。
- **WSL 开发部署**：`bash scripts/deploy/wsl.sh`（bind-mount，应用代码不烘焙进镜像）。
- **离线内网**：`bash scripts/dist/package.sh` 打轻量升级包 `dist/bbs-upgrade-*.tar.gz` → 服务器 `deploy-offline.sh --upgrade <tar>`；首次安装用完整包 `--install`。服务器端 `/data/bbs/versions/<ts>/` + `latest` 软链做版本回滚。
- **基础镜像**：`bash scripts/build/base.sh --save`（首次或依赖变更时）。
- 完整流程见 `DEPLOY.md`；脚本清单见 `scripts/SCRIPT.md`。

## 7. 常见坑位（务必遵守）

1. **Node 17+ / Webpack 4**：构建报 `ERR_OSSL_EVP_UNSUPPORTED` → 加 `NODE_OPTIONS="--openssl-legacy-provider"`。
2. **上传图片 404**：`BBS_UPLOAD_DIR` 缺尾斜杠会拼出 `bbsUploadcommon/` 错误目录。诊断 `bash scripts/ops/diagnose-upload.sh`，修复 `bash scripts/ops/fix-upload-path.sh`；后端统一走 `FilePathNormalizer.joinStoragePath()`。
3. **PostgreSQL 容器数据丢失**：`bbs-postgres` 必须用具名卷 `-v bbs-pgdata:/var/lib/postgresql/data`，否则删容器即丢数据。
4. **Docker Hub 拉取超时（中国网络）**：配置 DaoCloud 等镜像加速器（`~/.config/containers/registries.conf`）。
5. **端口冲突**：`18848` 被历史遗留 Tomcat 占用，后端默认值严禁改去抢；生产端口约定 60000/60001。
6. **空白数据库无表**：不要用"DB 已存在就跳过 init"的老逻辑；`DatabaseInitHelper` 始终执行安全模式的 init SQL。

## 8. 相关文档索引

- `CLAUDE.md` — 项目约定（数据库迁移规范、结构），与本节互为补充
- `DEPLOY.md` — 完整部署文档（架构、容器、WSL、离线、故障排查、环境变量全表）
- `scripts/SCRIPT.md` — 脚本目录说明与工作流
- `bbs-server/doc/` — 开发文档、数据字典、运维文档（.docx）
- `docs/org-migration/` — 组织机构迁移资料（CSV 映射、导入 SQL）
- `bbs-server/src/main/resources/db/init/` — 数据库脚本（见第 4 节）
