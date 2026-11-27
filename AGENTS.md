# 大千智荟 BBS · 工作契约

> 本文件只放**每次会话都必须遵守**的判据与边界。
> 项目知识（结构、端口、迁移规范、坑位）在 `.opencode/skills/daqian-bbs-delivery/SKILL.md` 与 `CLAUDE.md`，此处不重复。
> 新增于 2026-11-17；不需要时直接删除本文件即可完全恢复原状。

## 1. 验收命令（改完必须跑，禁止用"看起来没问题"代替）

| 改动范围 | 命令 | 通过标准 |
| --- | --- | --- |
| 后端 Java | `cd bbs-server && mvn -q test` | 全绿；用例数不低于基线 546 / 11 skipped |
| 后端只想快速冒烟 | `cd bbs-server && mvn -q -DskipTests compile` | BUILD SUCCESSFUL |
| `bbs-ui` | `cd bbs-ui && npm run build` | 0 error，产出 `dist/` |
| `bbs-admin-ui` | `cd bbs-admin-ui && npm run build` | 0 error（脚本已内置 `NODE_OPTIONS`） |
| 数据库脚本 | 四份 SQL 同步 + 重启后端 | 启动日志出现 `Migration applied: vNNN` |
| 权限 / 端到端 | `tests/` 下的 API 冒烟或 Playwright | 见 `tests/TEST-MATRIX.md` |
| 部署产物 | `bash scripts/prod/verify-build.sh` | `FAIL=0` |
| 部署入口 / 结构迁移 | `bash deploy.sh verify`（测试机或生产只读） | `FAIL=0`：三条指针一致、容器挂 `releases/<id>/app/...` 真实路径、文件回读 200 |

- 两个前端**串行**构建；构建前先结束 node 进程，再单独发起构建。
- API / E2E 只能跑在副本库 `bbs_e2e_test`，禁止对 `bbs_db` 写入。
- **判据自检**：若不确定判据是否有效，故意改坏一行，确认它变红，再还原。仍是绿的说明判据是装饰品。
- **根目录只放部署入口**：`deploy.sh` + `deploy-offline.sh`。其它 `.sh` 必须归 `scripts/<归类>/`（随包下发）；`deploy.sh` 每次部署后自动归位——同名同内容删根目录副本，来历不明移 `legacy/root-scripts/`。运维脚本要"不常改"，需要跟着每次发版改的说明它其实是部署脚本。

### 1.1 生命周期（本地）

| 场景 | 命令 | 通过标准 |
| --- | --- | --- |
| 启动 | `powershell -File scripts/dev/up.ps1` | 9081 / 9082 / 9083 均 listening |
| 停止 | `powershell -File scripts/dev/down.ps1` | 三个端口释放 |
| 重启 | `powershell -File scripts/dev/restart.ps1 -Target all|server|ui|admin` | 目标端口重新 listening |
| 提交前 | `powershell -File scripts/dev/verify.ps1` | 方法契约 + 构建 0 error + 后端就绪 + 负向自检通过 |
| WSL2（生产对齐） | Windows 构建后 `bash scripts/dev/wsl.sh`，测 `http://localhost:60001/bbs-user/` | PG 方言 + nginx 路由通过 |
| 打包（离线升级包） | **Windows** 构建：两个前端 `npm run build` + `cd bbs-server && mvn clean package -DskipTests`；**WSL** 执行 `bash scripts/prod/package.sh` + `bash scripts/prod/verify-build.sh`。也可直接 `powershell -File scripts/dev/package.ps1`（停栈→构建→WSL 打包→**finally 自动恢复开发栈**） | 生成 `dist/bbs-upgrade-<ts>.tar.gz`，`FAIL=0`，打包后 9081/9082/9083 仍 listening |
| 部署（WSL 测试） | `bash scripts/dev/wsl.sh` | `60001` 可访问，`verify-server.sh` 通过 |
| 部署（生产离线） | `bash scripts/prod/offline.sh dist/bbs-upgrade-*.tar.gz` | 容器重建 + 挂载探针通过，`verify-server.sh` `FAIL=0` |
| 一键（生产，零参数） | `cd /data/bbs/scripts/prod && bash one_key_deploy.sh` | 部署根目录最新 `bbs-upgrade-*.tar.gz`，验证通过后归档到 `archive/` |
| 一键（生产，新入口） | 日常 `bash deploy.sh`；一次性 `bash deploy.sh once migrate`（`tmp` 亦可） | 自动找包/校验/备份/切换/验证；失败自动回滚并 exit 1；成功后 `bash deploy.sh verify` 为 `FAIL=0` |
| 现场取数（U 盘） | `cd /root/collect && sudo bash collect.sh` | `回传/<主机>-<时间戳>/19-summary.txt` 生成；MISSING 项人工核对（只读，不停容器） |
| 一键（开发机 WSL） | `bash scripts/dev/wsl.sh` + `bash scripts/prod/package.sh` | 60001 可访问；`dist/bbs-*.tar.gz` 生成且 `verify-build.sh` 通过；根 `deploy.sh` 只用于部署根（在源码仓库里会提示正确入口） |

- 前端普通 `.vue/.js` 源码改动走 HMR，不用重启；`vue.config.js`、`.env*`、`package.json`、`tailwind/postcss/babel` 配置改动必须重启。
- 重启一律按端口杀进程树（日志在 `tmp/run/`），不杀全部 node。
- WSL2 端口：nginx `60001` / 后端 `60000` / PG `15432`，可与 Windows 开发栈（9081–9083）同时存在。
- 有 DB / 后端改动时，Windows 验收之外还要在 WSL2 再跑一遍同一套冒烟。
- 基础镜像或依赖变更必须按「`base.sh --save` → 服务器 `podman load` 新基础镜像 → 删除旧容器 → 再发瘦升级包」执行，顺序不能反；参数见 `scripts/SCRIPT.md`。
- 构建只在 **Windows**（两个 `npm run build` + `mvn clean package -DskipTests`）；打包只在 **WSL** 跑（`package.sh` 依赖 python3 生成瘦 jar），不要在 PowerShell 里执行这些 bash 脚本。

### 1.2 负向自检（每次提交前）

`scripts/dev/negative-smoke.ps1` 固定 4 个注入，只读、不写库：

| 注入 | 期望 |
| --- | --- |
| 基线公开接口 | 200 且业务码 200 |
| 错误 baseUrl（`127.0.0.1:1`） | 请求必须失败 |
| 错误断言（`__MUST_NOT_EXIST__`） | 断言必须抛错 |
| 未登录访问 `/notification/unreadSummary` | 业务码 401 |

- 判定：`expectedFailures == actualFailures`，且失败用例名一致；
- 注入失败单独计为 `mutation`，**不得混入业务失败数**；
- 基线不通过时先修环境，不做注入；
- 自检本身必须能变红：`-ApiBase http://127.0.0.1:1/bbs-server` 必须 exit 1。
- **随包自检**：生产包内置负向检查，运行即执行——HTA 的 `[负向]` 组（3 项）、Selenium 每次运行自动跑 4 项（`selfcheck.enabled=false` 可关）；两者失败都单独统计，不并入业务失败，但都会导致整体退出码为 1。

### 1.3 交付话术（对人，2026-11-27 起）

每次交付给用户，必须一次说清三件事，缺一算没交付：

1. **要上传几个文件**：默认 1 个升级包；`deploy.sh` 或基础镜像有变化时点名第 2 项并说原因。
2. **一条命令**：日常 `bash deploy.sh`；复杂/一次性 `bash deploy.sh once <动作>`
   （`once` 也可写 `tmp`）。不许要求现场连敲 `cd` / `cp` / `sha256sum` / `verify` / `diag`。
3. **为什么 + 失败怎么办**：每步一句人话；失败给一条 `bash deploy.sh once rollback`。

`package.sh` 打包结束会打印「交付卡」（上传项数 + 一条命令），交付时照它念，不要自己重编。

## 2. 停止规则（命中即停，不要自己决定）

- 同一问题连续 3 次尝试仍失败 → 停下报告，不要换方案硬试。
- 任何 `DROP` / `TRUNCATE`、对 `bbs_db` 或生产库写数据 → 先问；`init-*.sql` 永不对生产库执行。
- 要改动**已发布的迁移块**（`vNNN` 已记录在 `bbs_schema_version`）→ 先问；只能新增迁移。
- 要 `git push`、改 `.gitignore`、改 `.env` → 先问。
- **推送只走本账号远程 `github`（SBshenqianye/daqian-bbs）**；未明确要求时**一律不推 `upstream`（kimljx/daqian-bbs）**，也不推其他他人仓库（2026-11-20 用户裁决）。
- 需求有歧义，或验收标准不明确 → 先问，不要按自己的理解往下做。

## 3. 失败与交接

- 每个可验证单元一次 commit，按模块分组：`feat(server)` / `feat(admin)` / `feat(ui)` / `chore`。
- 中途停下（换会话、换机器、等答复）必须写 `tmp/HANDOFF.md`（该目录已 gitignore，不入库），四段：现状 / 上次验证（命令 + 输出摘要 + 时间）/ 已知坑 / 下一步。
- 汇报时区分"已验证"与"推测"；没跑过的命令不要写成"应该没问题"。

## 4. 沉淀位置

| 经验 | 落哪 |
| --- | --- |
| 每次都要遵守的硬判据 | 本文件 |
| 仓库结构、端口、迁移规范、常见坑 | `.opencode/skills/daqian-bbs-delivery/SKILL.md` |
| 数据库 / 权限 / 前端长期约定 | `CLAUDE.md` |
| 历史决策与事实记忆 | Life-Recordings 私有库 `skill-inbox/daqian-bbs/MEMORIES.md` |
| 一次性、以后不会再遇到 | 不沉淀，直接删 |

每条经验标注来源日期；同类问题半年未再发生就删，避免本文件膨胀。
