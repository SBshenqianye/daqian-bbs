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
| 部署产物 | `bash scripts/verify-build.sh` | `FAIL=0` |

- 两个前端**串行**构建；构建前先结束 node 进程，再单独发起构建。
- API / E2E 只能跑在副本库 `bbs_e2e_test`，禁止对 `bbs_db` 写入。
- **判据自检**：若不确定判据是否有效，故意改坏一行，确认它变红，再还原。仍是绿的说明判据是装饰品。

### 1.1 生命周期（本地）

| 场景 | 命令 | 通过标准 |
| --- | --- | --- |
| 启动 | `powershell -File scripts/dev/up.ps1` | 9081 / 9082 / 9083 均 listening |
| 停止 | `powershell -File scripts/dev/down.ps1` | 三个端口释放 |
| 重启 | `powershell -File scripts/dev/restart.ps1 -Target all|server|ui|admin` | 目标端口重新 listening |
| 提交前 | `powershell -File scripts/dev/verify.ps1` | 构建 0 error + 后端就绪 + 负向自检通过 |
| WSL2（生产对齐） | Windows 构建后 `bash scripts/deploy/wsl.sh`，测 `http://localhost:60001/bbs-user/` | PG 方言 + nginx 路由通过 |

- 前端普通 `.vue/.js` 源码改动走 HMR，不用重启；`vue.config.js`、`.env*`、`package.json`、`tailwind/postcss/babel` 配置改动必须重启。
- 重启一律按端口杀进程树（日志在 `tmp/run/`），不杀全部 node。
- WSL2 端口：nginx `60001` / 后端 `60000` / PG `15432`，可与 Windows 开发栈（9081–9083）同时存在。
- 有 DB / 后端改动时，Windows 验收之外还要在 WSL2 再跑一遍同一套冒烟。

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
- **随包自检**：生产包也内置负向检查——HTA 的 `[负向]` 组（3 项）、Selenium 的 `run.bat selfcheck`（4 项）；两者失败都单独统计，不并入业务失败。

## 2. 停止规则（命中即停，不要自己决定）

- 同一问题连续 3 次尝试仍失败 → 停下报告，不要换方案硬试。
- 任何 `DROP` / `TRUNCATE`、对 `bbs_db` 或生产库写数据 → 先问；`init-*.sql` 永不对生产库执行。
- 要改动**已发布的迁移块**（`vNNN` 已记录在 `bbs_schema_version`）→ 先问；只能新增迁移。
- 要 `git push`、改 `.gitignore`、改 `.env` → 先问。
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
