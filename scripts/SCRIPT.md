# scripts/ 目录说明

> 最后更新: 2026-07-01

## 目录结构

```
scripts/
├── SCRIPT.md                    ← 本文件
├── .env.example                 ← 环境变量配置模板
│
├── build/
│   └── build.sh                 ← 构建脚本
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
bash scripts/build/build.sh                   # 构建全部
bash scripts/deploy/container.sh              # 一键部署
bash scripts/ops/teardown.sh                  # 清理
```

### 生产环境（RHEL 7）

```bash
bash scripts/build/build.sh --native          # 构建前端 + 编译 JAR
bash scripts/dist/package.sh                  # 打包为 tar.gz
# 传输到生产服务器后:
bash scripts/deploy/native.sh                 # 部署
```

---

## 各分类说明

### build/ — 构建

并行构建前后端，支持容器镜像和原生 JAR 两种模式。

```bash
bash scripts/build/build.sh                   # 完整构建 + Docker 镜像
bash scripts/build/build.sh --native          # 构建 + 编译 JAR（无镜像）
bash scripts/build/build.sh --frontend        # 仅前端
bash scripts/build/build.sh --backend         # 仅后端
```

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

### dist/ — 分发

**package.sh / package.ps1** — 打包构建产物为可分发的 tar.gz。

Linux:
```bash
bash scripts/dist/package.sh                  # 标准打包
bash scripts/dist/package.sh --minimal        # 仅运行所需文件
bash scripts/dist/package.sh --with-source    # 含源代码
```

Windows (PowerShell):
```powershell
.\scripts\dist\package.ps1                    # 标准打包
.\scripts\dist\package.ps1 -Minimal           # 仅运行所需文件
.\scripts\dist\package.ps1 -WithSource        # 含源代码
```

输出: `bbs-deploy-YYYYMMDD-HHMMSS.tar.gz`（在项目根目录）

### 配置

**.env.example** — 所有可配置的环境变量及默认值。

```bash
cp scripts/.env.example .env
# 编辑 .env 填入数据库密码等
# 完整变量列表见 DEPLOY.md 附录
```
