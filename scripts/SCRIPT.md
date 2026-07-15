# scripts/ 目录说明

> 最后更新: 2026-07-15

## 目录结构

```
scripts/
├── SCRIPT.md              ← 本文件
├── .env.example           ← 环境变量配置模板
├── build/
│   └── base.sh            ← 基础镜像构建/导出/加载
├── deploy/
│   ├── wsl.sh             ← WSL 开发部署（bind-mount 模式）
│   └── offline.sh         ← 离线内网部署（首次安装 + 版本化升级）
├── dist/
│   └── package.sh         ← 离线打包分发
├── lib/
│   └── progress.sh        ← 进度指示工具库（步骤计数、spinner、进度条）
```

---

## 工作流

### WSL 日常开发（测试用）

```bash
# 0. 首次或基础镜像依赖变更时，构建基础镜像
bash scripts/build/base.sh --save

# 1. 开发机 Windows 上编译产物
cd bbs-ui && npm run build
cd bbs-admin-ui && NODE_OPTIONS="--openssl-legacy-provider" npm run build
cd bbs-server && mvn clean package -DskipTests

# 2. WSL 部署（只需几秒）
bash scripts/deploy/wsl.sh

# 3. 热更新（改代码后，WSL 上执行）
podman restart bbs-server bbs-nginx
```

### 离线内网分发

```bash
# 1. 打包轻量升级包（产物已存在则直接打包）
bash scripts/dist/package.sh
# 输出: dist/bbs-upgrade-YYYYMMDD_HHMMSS.tar.gz

# 2. 将 tar 传到目标服务器
scp dist/bbs-upgrade-*.tar.gz user@server:/tmp/

# 3. 在服务器上升级（tar 路径随意，--upgrade 参数指定位置）
sudo bash /data/bbs/deploy-offline.sh --upgrade /tmp/bbs-upgrade-*.tar.gz
```

---

## 各脚本说明

### deploy/wsl.sh — WSL 部署

将 Windows 上已编译的 JAR + dist 通过 bind-mount 挂载到容器，秒级完成。

```bash
bash scripts/deploy/wsl.sh                   # 检测产物 + 部署
bash scripts/deploy/wsl.sh --build           # 强制在 WSL 上编译（慢） + 部署
bash scripts/deploy/wsl.sh --restart-only    # 仅重启容器（代码已挂载）
```

### deploy/offline.sh — 离线部署

随完整环境包分发到内网服务器，支持首次安装和版本化升级。

```bash
bash deploy-offline.sh --install                       # 首次安装
bash deploy-offline.sh --upgrade bbs-upgrade-*.tar.gz  # 升级
```

### dist/package.sh — 离线打包

将已有构建产物打包为 tarball，内嵌 `upgrade.sh`（服务器端自动创建版本目录 + 更新软链 + 重启容器）。

```bash
bash scripts/dist/package.sh                  # 轻量升级包（默认）
bash scripts/dist/package.sh --full           # 完整环境包（含基础镜像 tar + 首次部署脚本）
```

### base.sh — 基础镜像管理

构建/导出/加载运行时基础镜像（不含应用代码），含依赖变更检测。

```bash
bash scripts/build/base.sh              # 检测 + 构建（如需）
bash scripts/build/base.sh --save       # 构建 + 导出 tar 到 dist/base-images/
bash scripts/build/base.sh --load       # 从 tar 加载到本地
bash scripts/build/base.sh --check      # 只检测变更
bash scripts/build/base.sh --force-rebuild  # 强制重建
```

---

## 完整生命周期

```
开发机 Windows                  WSL (测试)                  内网服务器 (生产)
──────────────                 ──────────                  ──────────────
npm run build     ──同步──▶    deploy/wsl.sh
mvn package                    ───测试通过──▶
                                              package.sh
                                                 │
                                           dist/bbs-upgrade-*.tar.gz
                                                 │
                                              scp ──────────▶ offline.sh --upgrade
                                                                  │
                                                            versions/<ts>/
                                                            latest -> <ts>
                                                            podman restart
```
