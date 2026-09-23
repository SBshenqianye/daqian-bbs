# scripts/ 目录说明

> 最后更新: 2026-09-23

## 目录结构

```
scripts/
├── SCRIPT.md              ← 本文件
├── .env.example           ← 环境变量配置模板
├── verify-build.sh        ← 构建产物验证（依赖下沉改造后，构建机跑）
├── verify-server.sh       ← 服务器部署结果验证（生产机跑）
├── build/
│   └── base.sh            ← 基础镜像构建/导出/加载
├── deploy/
│   ├── wsl.sh             ← WSL 开发部署（bind-mount 模式）
│   └── offline.sh         ← 离线内网部署（首次安装 + 版本化升级）
├── dist/
│   └── package.sh         ← 离线打包分发
├── lib/
│   └── progress.sh        ← 进度指示工具库（步骤计数、spinner、进度条）
└── ops/
    ├── pg-start.sh        ← 本地 PostgreSQL 容器管理（必须挂具名卷 bbs-pgdata）
    └── pg-backup.sh       ← 数据库备份/恢复（pg_dump + gzip）
```

> ⚠ **pg 数据持久化红线**：`bbs-postgres` 容器必须用 `-v bbs-pgdata:/var/lib/postgresql/data` 具名卷挂载。postgres 镜像不挂卷时会自动生成**匿名卷**，容器一删数据就跟容器一起消失；脚本内任何"删容器重建"逻辑都会导致换新卷、数据"丢失"（实际数据还在旧匿名卷里，可手动找回）。详见 `ops/pg-start.sh` 头注释。

---

## 依赖下沉（thin jar）说明

2026-09 起，后端改为"依赖下沉"部署：**依赖进基础镜像，升级包只带瘦 jar**。

- `bbs-server/pom.xml` 打包为 ZIP 布局（PropertiesLauncher），并把 runtime 依赖拷到 `target/lib/`
- `bbs-server/Dockerfile.base` 将 `target/lib` COPY 进镜像 `/app/lib`
- 升级包内 jar 为**瘦 jar**（剔除 `BOOT-INF/lib`，~2MB），启动参数 `-Dloader.path=/app/lib` 从镜像加载依赖
- 升级包从 ~67MB 降到 ~5MB；**旧 fat jar 升级包仍兼容**（自包含，任何镜像都能跑，可作回滚）

**生产首次切换顺序（不能反）**：先 `podman load` 新 `bbs-server-base.tar` → **删除旧容器**（restart 不会换镜像）→ 再发瘦升级包（upgrade.sh 自动重建容器）。离线 `offline.sh --install` 检测到镜像已存在会跳过加载，故覆盖镜像必须手动 `podman load`。

---

## 发布静默失败修复说明（2026-09-23）

**故障现象**：发布文章"静默失败"——前端无任何提示、Network 无红字、编辑/传图正常，文章发出去却不在列表。

**根因**（两处叠加）：
1. `ContentQualityUtil` 词表误判：`SPAM_PHRASES` 含"好/看看/可以/不错/顶/打卡"等正常高频词，`contains` 命中即 -40 分；纯图片文章（图片语法被剥离后正文为空）再扣"内容过短"50 分 → 正常文章跌破 40 分判为垃圾 → `enable=0` 不可见。
2. 静默链路：后端对垃圾内容返回 `success`（"已发布但不展示"）且文章入库不可见；前端 `BBSArticleWrite.vue` 用 `handleResponse(resp, { silent: true })` 连失败提示都吞掉 → 用户完全无感知。

**修复内容**：
- `ContentQualityUtil`：词表只保留强灌水特征（叠词灌水/沙发占楼水帖/纯数字/英文 test），移除全部正常高频词；纯图片/图文（含 `![` 或 `<img>`）不扣"内容过短"；多个灌水词叠加扣分（去 break）
- `ArticleServiceImpl.publish`：spam 判定提前到入库前，命中直接 `ResultBean.error`（含扣分原因），**不再"入库但不可见"**；低质量内容（40≤score<60）正常展示且同样计积分
- `BBSArticleWrite.vue`：去掉 `silent: true`——成功仍静默跳转，失败弹出后端 message

**回归测试**：`bbs-server/src/test/java/com/walker/utils/ContentQualityUtilTest.java`（14 用例：正常含高频词/纯图片/图文/Markdown/含"测试"不误判；叠词/灌水黑话/纯数字/空内容/重复字符必拦截）。跑法：`cd bbs-server && mvn test -Dtest=ContentQualityUtilTest`。

**注意**：修复后发布接口行为变更——被判垃圾的请求直接失败且不入库（原为"入库但不可见"），前端会提示原因。升级此包无需重建镜像（依赖未变），只传瘦包即可。

---

## 工作流

### WSL 日常开发（测试用）

```bash
# 0. 首次或基础镜像依赖变更时，构建基础镜像
#    注意：bbs-server-base 依赖 target/lib（mvn 产物），必须先执行第 1 步的 mvn
bash scripts/build/base.sh --save

# 1. 开发机 Windows 上编译产物
cd bbs-ui && npm run build
cd bbs-admin-ui && NODE_OPTIONS="--openssl-legacy-provider" npm run build
cd bbs-server && mvn clean package -DskipTests   # 同时拷出 target/lib（基础镜像依赖层）

# 2. WSL 部署（只需几秒）
bash scripts/deploy/wsl.sh

# 3. 热更新（改代码后，WSL 上执行）
podman restart bbs-server bbs-nginx
```

### 离线内网分发

```bash
# 1. 打包轻量升级包（产物已存在则直接打包；需 python3/python 生成瘦 jar）
bash scripts/dist/package.sh
# 输出: dist/bbs-upgrade-YYYYMMDD_HHMMSS.tar.gz（瘦包，~5MB）

# 2. 构建产物验证（可选但建议）
bash scripts/verify-build.sh

# 3. 将 tar 传到目标服务器
scp dist/bbs-upgrade-*.tar.gz user@server:/tmp/

# 4. 在服务器上升级（tar 路径随意，--upgrade 参数指定位置）
sudo bash /data/bbs/deploy-offline.sh --upgrade /tmp/bbs-upgrade-*.tar.gz
```

---

## 各脚本说明

### deploy/wsl.sh — WSL 部署

将 Windows 上已编译的 JAR + dist 通过 bind-mount 挂载到容器，秒级完成。
启动参数为 `-Dloader.path=/app/lib -jar /app/app.jar`（依赖在镜像内，JAR 为瘦 jar）。

```bash
bash scripts/deploy/wsl.sh                   # 检测产物 + 部署
bash scripts/deploy/wsl.sh --build           # 强制在 WSL 上编译（慢） + 部署（先构建再建镜像）
bash scripts/deploy/wsl.sh --restart-only    # 仅重启容器（代码已挂载）
```

### deploy/offline.sh — 离线部署

随完整环境包分发到内网服务器，支持首次安装和版本化升级。

```bash
bash deploy-offline.sh --install                       # 首次安装
bash deploy-offline.sh --upgrade bbs-upgrade-*.tar.gz  # 升级
bash deploy-offline.sh --repair                        # 验证/修复（挂载探针 + HTTP 回读，异常自动重建）
```

> ⚠ 依赖下沉后覆盖基础镜像不能用 `--install`（镜像已存在会跳过加载），需手动 `podman load`。

### dist/package.sh — 离线打包

将已有构建产物打包为 tarball，内嵌 `upgrade.sh`（服务器端自动创建版本目录 + 更新软链 + 重启/重建容器）。
升级包内 jar 为**瘦 jar**（用 python3/python 剔除 `BOOT-INF/lib`，依赖走镜像 `/app/lib`）。

```bash
bash scripts/dist/package.sh                  # 轻量升级包（默认，~5MB）
bash scripts/dist/package.sh --full           # 完整环境包（含基础镜像 tar + 首次部署脚本）
```

### verify-build.sh — 构建产物验证（构建机跑）

依赖下沉改造后，打包完成检查产物是否合格：

```bash
bash scripts/verify-build.sh
```

验证项：fat jar 为 PropertiesLauncher 布局、`target/lib` 包含 fat jar 的全部内置依赖（允许多出空壳 starter，无类无害）、升级包 <15MB、包内为瘦 jar（不含 BOOT-INF/lib）、upgrade.sh 含 `-Dloader.path=/app/lib`。

### verify-server.sh — 服务器部署验证（生产机跑）

瘦升级包部署完成后，在服务器上检查部署结果：

```bash
bash /tmp/verify-server.sh    # 支持 BBS_HOME / BBS_SERVER_PORT 环境变量覆盖
```

验证项：镜像含 `/app/lib`、容器启动参数含 `-Dloader.path=/app/lib`、当前 jar 为瘦 jar、后端健康 200、上传目录挂载探针。全部通过不代表业务功能正常，仍需浏览器回归（发帖/上传）。

### pg-backup.sh — 数据库备份/恢复

自动检测运行环境（容器 or 原生），容器名从 `.env` 的 `BBS_PG_CONTAINER` 读取。

```bash
bash scripts/ops/pg-backup.sh                  # 备份到当前目录（bbs_YYYYMMDD_HHMMSS.sql.gz）
bash scripts/ops/pg-backup.sh --list           # 列出容器内可用备份
bash scripts/ops/pg-backup.sh --env            # 显示检测到的环境信息
bash scripts/ops/pg-backup.sh --restore bbs_20260825_120000.sql.gz  # 恢复
```

> ⚠ 生产环境容器名可能和测试不同（如 `work-flow-db`），务必在 `.env` 中配置 `BBS_PG_CONTAINER`。

### base.sh — 基础镜像管理

构建/导出/加载运行时基础镜像（不含应用代码），含依赖变更检测。
`bbs-server-base` 的依赖文件列表含 `bbs-server/pom.xml`：**后端依赖变更（pom 变化）→ checksum 变化 → 自动重建镜像**。构建前校验 `bbs-server/target/lib` 存在（缺失时报错提示先跑 mvn）。

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
mvn package（拷出                ───测试通过──▶
  target/lib）                                     ┌─ 首次切换（依赖下沉）：
                                base.sh --save     │  1) podman load 新 bbs-server-base.tar
                                package.sh         │  2) podman rm -f bbs-server（重启不换镜像）
                                   │               │  3) 再发瘦升级包
                                   │               └──────────────▶
                           dist/bbs-upgrade-*.tar.gz（瘦包 ~5MB）
                                   │
                                scp ───────────────▶ offline.sh --upgrade
                                                                  │
                                                            versions/<ts>/
                                                            latest -> <ts>
                                                            重建容器 + 挂载验证
```
