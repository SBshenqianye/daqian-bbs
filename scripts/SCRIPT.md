# scripts/ 目录说明

> 最后更新: 2026-07-03

## 目录结构

```
scripts/
├── SCRIPT.md              ← 本文件
├── .env.example           ← 环境变量配置模板
├── lib/
│   └── progress.sh        ← 进度指示工具库（步骤计数、spinner、进度条）
│
├── build/
│   └── build.sh           ← 构建脚本（自动打包）
│
├── deploy/
│   └── container.sh       ← 容器部署
│
├── ops/
│   ├── teardown.sh        ← 清理脚本
│   ├── init-db.sh         ← 数据库初始化
│   └── pg-start.sh        ← PostgreSQL 容器管理（启动/停止/状态）
│
└── dist/
    ├── package.sh         ← 离线打包分发（Linux）
    └── package.ps1        ← 离线打包分发（Windows PowerShell）
```

---

## 快速工作流

### 开发环境（WSL2 + Podman）

```bash
bash scripts/ops/pg-start.sh start            # 确保 PostgreSQL 就绪
bash scripts/build/build.sh                   # 构建全部 → 自动部署 + 打包
bash scripts/ops/teardown.sh                  # 清理
```

`build.sh` 容器模式会自动部署容器（构建完成后即运行），无需再手动执行 deploy。

### 离线分发

```bash
bash scripts/build/build.sh                   # 构建 + 部署 + 容器离线包
# 传输 bbs-offline-*.tar.gz 到目标服务器后:
sudo mkdir -p /data
sudo tar -xzf bbs-offline-*.tar.gz -C /data
cd /data/bbs
sudo bash deploy.sh                           # 一键部署
```

> **注意**: PostgreSQL 需自行启动，部署脚本不会自动管理数据库。
> 部署前请确保 PG 已运行并在 `.env` 中正确配置 `BBS_DB_HOST`。默认端口 `15432`。

---

## 各分类说明

### build/ — 构建

并行构建前后端（前端 npm 和后端 Maven 同时跑），构建完成后**自动打包**为分发包，无需再手动运行 package.sh。

```bash
bash scripts/build/build.sh                   # 构建 + 部署 + 容器离线包（默认）
bash scripts/build/build.sh --frontend        # 仅构建前端
bash scripts/build/build.sh --backend         # 仅构建后端
```

容器模式构建流程:
1. 安装前端依赖 → 2. 并行构建前后端 → 3. 构建 Nginx 镜像
4. **部署容器**（`container.sh`）和**自动打包**（并行执行）

输出:
- 容器模式 → `bbs-offline-YYYYMMDD-HHMMSS.tar.gz`
- （原生模式已不再使用）

### deploy/ — 部署

**container.sh** — 使用 Podman 部署后端和 Nginx 容器。
容器使用 **host 网络**，后端通过 `127.0.0.1:${BBS_DB_PORT}` 直连宿主机数据库。
PostgreSQL 需自行管理。

```bash
bash scripts/deploy/container.sh              # 部署后端 + Nginx
```

### ops/ — 运维

**teardown.sh** — 停止所有应用容器。不会影响外部管理的 PostgreSQL。

```bash
bash scripts/ops/teardown.sh                  # 停止所有容器
```

**init-db.sh** — 手动执行数据库初始化（安全模式 SQL）。
在自动初始化失败时使用。

```bash
bash scripts/ops/init-db.sh
# 指定参数:
bash scripts/ops/init-db.sh -h 127.0.0.1 -p 5432 -U work_flow -d bbs
```

**pg-start.sh** — PostgreSQL 容器管理（启动/停止/状态）。
自动检测容器是否存在、端口映射（`15432:5432`）是否正确，缺失时自动重建。

```bash
bash scripts/ops/pg-start.sh start   # 启动 PG
bash scripts/ops/pg-start.sh stop    # 停止 PG
bash scripts/ops/pg-start.sh status  # 查看状态
```

> **注意**: `pg-start.sh` 会自行管理 PostgreSQL 容器（`bbs-postgres`），
> 而 `deploy/container.sh` 只管理应用容器，不管理 PG。
> 建议部署前先用 `pg-start.sh` 确保 PG 就绪。

### dist/ — 手动分发打包

> 一般情况下无需手动运行——`build.sh` 构建完后会自动打包。
> 以下命令用于单独需要打包的场景（如用已有构建产物打包）。

**package.sh / package.ps1** — 打包构建产物为可分发的 tar.gz。
容器模式离线包仅包含：镜像 tar、Dockerfile、`.env.example`、独立 `deploy.sh` 和 `README.md`（不再打包 scripts 目录）。

```bash
bash scripts/dist/package.sh                  # 容器离线包（默认）
bash scripts/dist/package.sh --native         # 原生部署包（仅 JAR + 前端）
bash scripts/dist/package.sh --minimal        # 仅运行所需文件
bash scripts/dist/package.sh --with-source    # 含源代码
```

Windows (PowerShell):
```powershell
.\scripts\dist\package.ps1                    # 标准打包
.\scripts\dist\package.ps1 -Minimal           # 仅运行所需文件
.\scripts\dist\package.ps1 -WithSource        # 含源代码
```

输出:
- 容器模式 → `bbs-offline-YYYYMMDD-HHMMSS.tar.gz`
- 原生模式 → `bbs-deploy-YYYYMMDD-HHMMSS.tar.gz`（保留兼容）

### 进度指示

所有主要脚本（build.sh、deploy/*.sh、package.sh/ps1）都集成了进度指示功能，由 `lib/progress.sh` 提供：

| 功能 | 函数 | 适用场景 |
|------|------|----------|
| 步骤编号标题 | `show_step <当前> <总数> <描述>` | 每个主要阶段顶部 |
| 计时器 | `run_with_timer <消息> <命令>` | 长时间单命令（npm install、docker build） |
| 并行任务监控 | `track_parallel <标题> <PID> <标签> ...` | 并行构建的前端项目 |
| 轮询指示器 | `polling_spinner <消息> <当前> <总数>` | 后端健康检查等待 |
| 百分比进度条 | `progress_bar <当前> <总数> [标签]` | 已知总数的文件操作 |
| 总框标题 | `show_header <标题>` | 脚本入口处的步骤概览 |

不引入外部依赖——纯 bash 实现，`pv`/`dialog`/`tqdm` 均非必需。

### 配置

**.env.example** — 所有可配置的环境变量及默认值。

```bash
cp scripts/.env.example .env
# 编辑 .env 填入数据库密码等
# PostgreSQL 需自行启动，确保 BBS_DB_HOST 指向正确的数据库地址
```
