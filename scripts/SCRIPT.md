# scripts/ 目录说明

> 最后更新: 2026-07-01

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
│   ├── container.sh       ← 容器部署（WSL2 + Podman）
│   └── native.sh          ← 原生部署（RHEL 7）
│
├── ops/
│   ├── teardown.sh        ← 清理脚本
│   └── init-db.sh         ← 数据库初始化
│
└── dist/
    ├── package.sh         ← 离线打包分发（Linux）
    └── package.ps1        ← 离线打包分发（Windows PowerShell）
```

---

## 快速工作流

### 开发环境（WSL2 + Podman）

```bash
bash scripts/build/build.sh                   # 构建全部 + 容器离线包（默认）
bash scripts/deploy/container.sh              # 一键部署
bash scripts/ops/teardown.sh                  # 清理
```

### 生产环境（RHEL 7）

```bash
bash scripts/build/build.sh --native          # 构建全部 + 原生部署包
# 传输 bbs-deploy-*.tar.gz 到生产服务器后:
sudo tar -xzf bbs-deploy-*.tar.gz -C /data
cd /data/bbs-deploy
bash scripts/deploy/native.sh                 # 部署
```

### 离线分发（容器模式）

```bash
bash scripts/build/build.sh                   # 构建 + 容器离线包
# 传输 bbs-offline-*.tar.gz 到内网服务器后:
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
bash scripts/build/build.sh                   # 构建 + 容器离线包（默认）
bash scripts/build/build.sh --native          # 构建 + 原生部署包
bash scripts/build/build.sh --frontend        # 仅构建前端
bash scripts/build/build.sh --backend         # 仅构建后端
```

输出:
- 容器模式 → `bbs-offline-YYYYMMDD-HHMMSS.tar.gz`
- 原生模式 → `bbs-deploy-YYYYMMDD-HHMMSS.tar.gz`

### deploy/ — 部署

**container.sh** — 在 WSL2 上使用 Podman 部署后端和 Nginx 容器。
自动创建网络、启动后端/Nginx 两个容器。
PostgreSQL 需自行管理。

```bash
bash scripts/deploy/container.sh              # 完整部署
```

**native.sh** — 在 RHEL 7 上原生部署（不依赖容器）。
自动启动后端进程、创建 systemd 服务、配置 Nginx。
PostgreSQL 需自行管理。

```bash
bash scripts/deploy/native.sh
```

### ops/ — 运维

**teardown.sh** — 停止所有容器/进程。不会影响外部管理的 PostgreSQL。

```bash
bash scripts/ops/teardown.sh                  # 停止容器
bash scripts/ops/teardown.sh --all            # 停止容器 + 删除网络
bash scripts/ops/teardown.sh --native         # 停止原生部署的后端进程
```

**init-db.sh** — 手动执行数据库初始化（安全模式 SQL）。
在自动初始化失败时使用。

```bash
bash scripts/ops/init-db.sh
# 指定参数:
bash scripts/ops/init-db.sh -h 127.0.0.1 -p 5432 -U work_flow -d bbs
```

### dist/ — 手动分发打包

> 一般情况下无需手动运行——`build.sh` 构建完后会自动打包。
> 以下命令用于单独需要打包的场景（如用已有构建产物打包）。

**package.sh / package.ps1** — 打包构建产物为可分发的 tar.gz。

```bash
bash scripts/dist/package.sh                  # 容器离线包（默认）
bash scripts/dist/package.sh --native         # 原生部署包
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
- 原生模式 → `bbs-deploy-YYYYMMDD-HHMMSS.tar.gz`

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
