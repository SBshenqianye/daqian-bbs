# 测试资产

> 完整的覆盖矩阵、双版本结论和运行记录见 [`TEST-MATRIX.md`](./TEST-MATRIX.md)。

## 权限组 / 权限边界测试资产（HEAD `538385d` 起）

| 路径 | 说明 |
|---|---|
| `api/permission-group-api-smoke.js` | 11 个真实 HTTP 冒烟：channel 02 登录、权限组 CRUD、预设详情/删除、删除组迁移、`permission:manage`/`audit:read` 边界、`config:write` 变更、CORS DELETE 预检 |
| `../bbs-server/src/test/e2e/permission-groups.spec.ts` | 管理端 E2E：首页我的权限/权限安全/快捷入口过滤、权限组关键权限锁定、未保存保护、创建/预设/删除、审计详情弹层 |
| `dev/check-dev-port.test.js` | `scripts/check-dev-port.js` 的空闲/占用/非法参数行为（`node --test`） |

运行前置与命令见 `TEST-MATRIX.md` 第 3 节。API/E2E 必须使用从 `bbs_db` 复制的
`bbs_e2e_test`，禁止直接写 `bbs_db`。

## 版主角色化测试资产（历史）

本目录同时保留了 `MODERATOR`（`user_type=4`）角色功能的测试资产，配套报告见
`docs/版主角色化-测试报告-2026-10-30.md`（该文档被 `.gitignore` 忽略，仅本地保留）。

> ⚠️ 历史资产提示：`fixtures/moderator-seed.sql`、`api/moderator-api-smoke.js` 等仍使用
> `bbs_permission_group.base_role` 和旧版主组模型；该列在权限组重构后已移除，
> 不能在当前 HEAD 的库上直接运行。保留仅用于追溯，新的权限测试请使用上表资产。

## 目录

| 路径 | 说明 |
|---|---|
| `api/moderator-api-smoke.js` | 登录、板块数据范围、无板块 403、系统接口 403、生命周期、Q9 的 API 冒烟 |
| `e2e/moderator-role.spec.js` | Playwright 管理端 E2E：菜单、无板块横幅、跨板块 403、编辑板块 |
| `db/moderator-migration-smoke.js` | MySQL：v039 迁移 → 校验 → 回滚 → 再迁移（自建测试数据，无需外部 seed） |
| `db/moderator-migration-smoke-pg.sh` | PostgreSQL：同一套 6 组检查点，基于 podman 容器副本库 |
| `db/pg/` | PG 烟测用的种子、版本记录与回滚 SQL |
| `fixtures/moderator-seed.sql` | API/E2E 固定账号、板块、文章等测试数据（仅限 `bbs_e2e_test`） |

## 前置条件

- JDK 8、Maven、Node 18+、MySQL 8
- 测试库**必须是 `bbs_db` 的副本**，禁止直接使用 `bbs_db`：
  - `bbs_e2e_test`：API + E2E
  - `bbs_moderator_test`：迁移烟测
- Windows PowerShell 下用 `npm.cmd` / `npx.cmd`

```powershell
# 只在副本库上执行，二选一
mysqldump -uroot -proot bbs_db > tmp/bbs_db_backup.sql
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS bbs_e2e_test DEFAULT CHARACTER SET utf8mb4"
mysql -uroot -proot bbs_e2e_test < tmp/bbs_db_backup.sql
```

`fixtures/moderator-seed.sql` 内置库名安全阀：当前连接不是 `bbs_e2e_test` 时会直接报错中断。

## 运行

### 1. 后端单测（不需要数据库）

```powershell
cd bbs-server
mvn test
```

### 2. API 冒烟

```powershell
# 1) 准备数据
mysql -uroot -proot bbs_e2e_test < tests/fixtures/moderator-seed.sql

# 2) 以 e2e profile 启动后端（端口 9084，context-path /bbs-server）
cd bbs-server
mvn spring-boot:run "-Dspring-boot.run.profiles=e2e"

# 3) 另一个终端运行
node tests/api/moderator-api-smoke.js
# 可用 BBS_E2E_BASE 覆盖后端地址
```

### 3. 管理端 E2E

```powershell
# 后端按上一步启动后
cd bbs-admin-ui
$env:VUE_APP_BBS_BASE_API = "http://127.0.0.1:9084"
npm.cmd run serve   # 默认 9082/9086，按实际端口设置 E2E_ADMIN_BASE

# 仓库根目录
npx.cmd playwright test tests/e2e/moderator-role.spec.js --reporter=line
```

### 4. v039 迁移烟测

MySQL：

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS bbs_moderator_test DEFAULT CHARACTER SET utf8mb4"
# 先把 bbs_db 结构与数据导入 bbs_moderator_test
node tests/db/moderator-migration-smoke.js
```

PostgreSQL（在 WSL 里执行，PG 跑在 podman 容器）：

```bash
# 默认：容器 bbs-postgres、用户 work_flow、源库 bbs、副本库 bbs_moderator_test_pg
bash tests/db/moderator-migration-smoke-pg.sh

# 全部可覆盖
PG_CONTAINER=bbs-postgres PG_USER=work_flow \
PG_SOURCE_DB=bbs PG_TEST_DB=bbs_moderator_test_pg \
bash tests/db/moderator-migration-smoke-pg.sh
```

- 脚本会从源库 `pg_dump` 复制出副本库，再跑 v039 → 校验 → 回滚 → 再迁移；源库只读、不会被写入。
- 安全阀：`PG_TEST_DB` 必须包含 `test` 且不能等于 `PG_SOURCE_DB`，否则直接拒绝执行。
- PG 历史库没有 `user_type` 列注释，v039 才首次设置，所以回滚是 `COMMENT ... IS NULL`，不是写回旧文案。

## 已知限制

- PG 烟测依赖 podman 容器；本机没有容器时需先准备一个可连接的 PG 副本库。
- E2E 只跑 Chromium。
- API/E2E 断言依赖固定 ID（9001–9008 账号，9001–9005 板块），共享库需先重置测试数据。
