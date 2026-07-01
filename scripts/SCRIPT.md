# scripts/ 目录说明

> 最后更新: 2026-07-01

## 目录结构

```
scripts/
├── SCRIPT.md                    ← 本文件
├── .env.example                 ← 环境变量配置模板
│
├── build/
│   └── build.sh                 ← 构建脚本（自动打包）
│
├── deploy/
│   ├── container.sh             ← 容器部署（WSL2 + Podman）
│   └── native.sh                ← 原生部署（RHEL 7）
│
├── ops/
│   ├── teardown.sh              ← 清理脚本
│   └── init-db.sh               ← 数据库初始化
│
└── dist/
    ├── package.sh               ← 离线打包分发（Linux）
    └── package.ps1              ← 离线打包分发（Windows PowerShell）
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

**container.sh** — 在 WSL2 上使用 Podman 部署整套环境。
自动创建网络、启动 PostgreSQL/后端/Nginx 三个容器。

```bash
bash scripts/deploy/container.sh              # 完整部署
bash scripts/deploy/container.sh --no-pg      # 跳过 PG（用已有数据库）
bash scripts/deploy/container.sh --pg-only    # 仅启动 PG 容器
```

**native.sh** — 在 RHEL 7 上原生部署（不依赖容器）。
自动执行数据库初始化、启动后端进程、创建 systemd 服务、配置 Nginx。

```bash
bash scripts/deploy/native.sh
```

### ops/ — 运维

**teardown.sh** — 停止所有容器/进程，可选择删除数据。

```bash
bash scripts/ops/teardown.sh                  # 停止容器（保留数据卷）
bash scripts/ops/teardown.sh --all            # 停止容器 + 删除数据
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

### 配置

**.env.example** — 所有可配置的环境变量及默认值。

```bash
cp scripts/.env.example .env
# 编辑 .env 填入数据库密码等
# 完整变量列表见 DEPLOY.md 附录
```
