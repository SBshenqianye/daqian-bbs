# 版主角色化测试资产

本目录是 `MODERATOR`（`user_type=4`）角色功能的可复现测试资产，配套报告见
`docs/版主角色化-测试报告-2026-10-30.md`（该文档被 `.gitignore` 忽略，仅本地保留）。

## 目录

| 路径 | 说明 |
|---|---|
| `api/moderator-api-smoke.js` | 登录、板块数据范围、无板块 403、系统接口 403、生命周期、Q9 的 API 冒烟 |
| `e2e/moderator-role.spec.js` | Playwright 管理端 E2E：菜单、无板块横幅、跨板块 403、编辑板块 |
| `db/moderator-migration-smoke.js` | v039 迁移 → 校验 → 回滚 → 再迁移（自建测试数据，无需外部 seed） |
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

```powershell
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS bbs_moderator_test DEFAULT CHARACTER SET utf8mb4"
# 先把 bbs_db 结构与数据导入 bbs_moderator_test
node tests/db/moderator-migration-smoke.js
```

脚本会自行创建 9101–9108 测试用户、执行 v039 块、校验、回滚、再迁移，并在 `tmp/` 下留备份。

## 已知限制

- 仅验证 MySQL，PostgreSQL 的 v039 需另行烟测。
- E2E 只跑 Chromium。
- API/E2E 断言依赖固定 ID（9001–9008 账号，9001–9005 板块），共享库需先重置测试数据。
