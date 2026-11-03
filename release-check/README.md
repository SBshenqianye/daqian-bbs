# 发布检查包（离线内网 / PostgreSQL）

> 对应范围：`3a3130e` 之后的全部提交（权限重构 + 版主角色化 + 权限矩阵 Phase A）。
> 生产机当前是 `3a3130e`，**这些功能都还没在生产验证过**，按本清单走一遍再放量。
> 生产数据库：**PostgreSQL**（MySQL 也保留了一套 SQL，用 `-DbType MySql`）。

这个文件夹是自包含的，整包拷到生产机即可，不需要联网、不需要 Node、不需要 Maven。
依赖：**PowerShell 5.1** + **psql 客户端**（PG）或 **mysql 客户端**（MySQL）。

---

## 一、先看这三句话

1. **部署前**跑一次 `Pre`，它只读数据库，并把基线存到 `out/`。
2. **部署后**跑一次 `Post`，它会对比基线、检查 v039–v043（含 primary_group_id 与四个系统角色组）、再调几个 API。
3. 任何 `FAIL` 都先停下来排查；`SKIP` 表示没提供对应账号，不是失败。

---

## 二、包里有什么

```text
release-check/
├─ README.md                       ← 你正在看的文件
├─ check.ps1                       ← 唯一入口：Pre / Post / Api
├─ sql/
│  ├─ pg/                          ← 生产默认（PostgreSQL）
│  │  ├─ precheck.sql
│  │  ├─ postcheck.sql
│  │  ├─ backup-before-v039.sql
│  │  ├─ rollback.sql
│  │  ├─ rollback-v040.sql
│  │  └─ rollback-v041.sql
│  └─ mysql/                       ← 备用（MySQL 环境）
│     ├─ precheck.sql
│     ├─ postcheck.sql
│     ├─ backup-before-v039.sql
│     ├─ rollback.sql
│     ├─ rollback-v040.sql
│     └─ rollback-v041.sql
└─ out/                            ← 运行后自动生成：基线、日志
```

---

## 三、编码说明（GBK 机器必看）

脚本已经按“**UTF-8 脚本 + UTF-8 SQL + 强制 UTF-8 控制台**”处理：

- `check.ps1` 以 **UTF-8 BOM** 保存，Windows PowerShell 5.1 在 GBK 系统上也能正确读取中文；
- 所有 SQL 都用 `Get-Content -Encoding UTF8` 读取，再以 UTF-8 发给客户端；
- 运行时会设置 `[Console]::OutputEncoding = UTF8`、`PGCLIENTENCODING=UTF8`；
- 断言输出里不含中文比对（列注释用 `comment_has_4 = 0/1` 判断），避免编码导致误判。

如果控制台仍然显示乱码：

```powershell
chcp 65001
```

或直接用 Windows Terminal 运行。**不要用记事本把 `check.ps1` 另存为 ANSI/GBK**，会破坏脚本里的中文。

---

## 四、部署前（约 5 分钟）

### 1. 备份数据库

```powershell
pg_dump -h 127.0.0.1 -p 5432 -U postgres -Fc bbs_db > D:\backup\bbs_db_before_v039.dump
```

### 2. 记录基线（只读）

```powershell
cd release-check
.\check.ps1 -Mode Pre -DbUser postgres -DbName bbs_db
```

默认连 `127.0.0.1:5432`，会提示输入密码。`psql` 不在 PATH 时：

```powershell
.\check.ps1 -Mode Pre -PsqlExe "C:\Program Files\PostgreSQL\16\bin\psql.exe" -DbUser postgres -DbName bbs_db
```

看到 `PASS v039 尚未执行`、`PASS v040 尚未执行`、`PASS v041 尚未执行`、`PASS 审计日志表尚未创建`、`PASS 权限组表尚未创建` 即可。

重点看：

- `candidates`：本次会被升级成版主的人数
- `candidate_user:*`：具体是谁
- `type_2` / `type_3`：管理员、超管数量（部署后必须不变）
- `comment_has_4`：部署前应为 0

> `candidates` 与业务预期不符时，**先别部署**，把名单发给业务确认。

### 3.（可选，但推荐）建回滚备份表

```powershell
psql -h 127.0.0.1 -p 5432 -U postgres -d bbs_db -f sql\pg\backup-before-v039.sql
```

有了它，回滚只影响“本次被升级的人”，不会误伤人工设置的版主。

---

## 五、部署

按现有流程替换 jar / 重启。新版本启动时会自动执行 `v039-moderator-role`、`v040-audit-log`、`v041-permission-group`，这是预期行为。

> v040 只新增 `bbs_audit_log` 表，v041 只新增权限组相关表，均不改动现有业务表；不涉及数据回填。

---

## 六、部署后（约 10 分钟）

### 1. 数据库 + 接口一起检查

```powershell
.\check.ps1 -Mode Post -DbUser postgres -DbName bbs_db `
  -BaseUrl "http://127.0.0.1:8080/bbs-server" `
  -SuperUser "超管账号" -SuperPassword "密码"
```

### 2. 有测试账号就一起带上（没有会自动 SKIP）

```powershell
.\check.ps1 -Mode Post -DbUser postgres -DbName bbs_db `
  -BaseUrl "http://127.0.0.1:8080/bbs-server" `
  -SuperUser "super1" -SuperPassword "***" `
  -AdminUser "admin1" -AdminPassword "***" `
  -ModeratorUser "mod1" -ModeratorPassword "***" `
  -NormalUser "normal1" -NormalPassword "***"
```

### 3. 预期结果

| 检查 | 预期 |
|---|---|
| `v039 已记录` | PASS |
| `v040 已记录` | PASS |
| `v041 已记录` | PASS |
| `审计日志表存在` | PASS |
| `权限组表存在` | PASS |
| `不存在非法 user_type` | PASS |
| `没有遗漏的升级候选` | PASS |
| `列注释包含 4：版主` | PASS |
| `用户总数 / 管理员 / 超管 未变化` | PASS |
| `版主数量增加符合预期` | PASS |
| `超级管理员登录` + `permission:manage` | PASS |
| `会话刷新返回角色/permissions/boardScopes` | PASS |
| `权限矩阵接口 200`，endpoints > 0 | PASS |
| `ADMIN 访问权限矩阵 403` | PASS（提供管理员账号时） |
| `MODERATOR 访问权限矩阵 403`、业务列表 200、用户管理 403 | PASS（提供版主账号时） |
| `普通用户 channel=02 登录失败` | PASS（提供普通账号时） |

全部 PASS 或只有 SKIP，才算通过。

---

## 七、手工 UI 检查（5 项，约 5 分钟）

| # | 操作 | 预期 |
|---|---|---|
| 1 | 超管登录 → 左侧“系统管理” | 出现“权限矩阵”，点开能看到：角色→权限点、权限点→角色、接口→注解 |
| 2 | 管理员登录 → 直接访问 `/permission-matrix` | 跳 `/403`，**不会被退出登录** |
| 3 | 版主（有板块）登录 | 只有“业务管理”；帖子管理只看到自己板块；看不到用户/配置/版主管理 |
| 4 | 版主（无板块）登录 | 顶部提示“尚未分配板块，请联系管理员”；列表为空；写操作提示同一句 |
| 5 | 普通用户走后台登录 | 登录失败，提示用户名或密码不正确 |
| 6 | 首次登录或被重置密码的账号登录 | 强制跳转“修改初始密码”，改密成功后才能进入后台；直接调管理接口返回 403 |
| 7 | 超管登录 → 系统管理 → 审计日志 | 能看到操作记录；ADMIN/MODERATOR 看不到菜单，直接访问 `/audit` 跳 403 |

> 生产没有版主账号时，3/4 可以先跳过；上线后第一次任命版主时补做。

---

## 八、回滚（只有出问题时才做）

**顺序不能反**：

1. 停服，回退到旧 jar（不含 v039 的版本）；
2. 执行回滚 SQL：

```powershell
psql -h 127.0.0.1 -p 5432 -U postgres -d bbs_db -f sql\pg\rollback.sql
```

3. 再启动旧版本。

原因：当前版本一启动就会重新执行 v039；先回滚 SQL 再启动新 jar，等于白做。

回滚后复核：

```sql
SELECT COUNT(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role';  -- 应为 0
```

> PG 历史库没有 `user_type` 列注释，v039 才首次设置，所以 PG 回滚是 `COMMENT ... IS NULL`，不是写回旧文案。
> 没有备份表时，`rollback.sql` 末尾有应急语句（会把所有 `user_type=4` 降回 1，会误伤人工版主），默认注释状态。
>
> v040 是新增表，回滚可单独执行 `sql/*/rollback-v040.sql`。**会丢失全部审计日志**，执行前先备份。
>
> v041 是新增表，回滚可单独执行 `sql/*/rollback-v041.sql`。**会丢失权限组、用户绑定和预设**，执行前先备份。

---

## 九、MySQL 环境（备用）

```powershell
.\check.ps1 -Mode Pre -DbType MySql -DbUser root -DbName bbs_db
.\check.ps1 -Mode Post -DbType MySql -DbUser root -DbName bbs_db -BaseUrl "http://127.0.0.1:8080/bbs-server" -SuperUser "..."
```

回滚对应 `sql\mysql\rollback.sql`（列注释恢复为旧文案）。

---

## 十、常见问题

**Q：`psql` 不是内部或外部命令？**
用 `-PsqlExe` 指到完整路径，或在装有 psql 的机器上运行。生产不建议依赖 WSL。

**Q：控制台中文乱码 / 断言误判？**
先 `chcp 65001`，或改用 Windows Terminal。脚本本身按 UTF-8 BOM 保存，不要另存为 ANSI。

**Q：`Post` 报 `v039 已记录` FAIL？**
先看后端启动日志有没有迁移报错；再看 `bbs_schema_version` 是否有记录。不要手工补记录。

**Q：`candidates` 很多，正常吗？**
说明历史上有大量“普通用户 + 有效版主任职”。这是把存量版主升级为账号角色，属于预期；但名单要让业务确认。

**Q：`comment_has_4` 是 0？**
v039 没执行成功，或连错了库。确认 `-DbName` 是目标库。

**Q：手工用 curl / Postman 调接口，被拒绝却返回 HTTP 200？**
后端统一把权限拒绝包装成 `HTTP 200 + body.code=403`，判断时以 `body.code` 为准；本脚本已按这个口径处理。

---

## 十一、本次发布包含什么（对照 `3a3130e`）

| 功能 | 影响 |
|---|---|
| 统一角色 / 权限点 / 数据范围 | 管理端接口统一鉴权，移除 id=1 硬编码 |
| 左侧菜单按权限过滤、业务/系统分组 | 菜单结构变化 |
| 版主账号角色 `MODERATOR`（`user_type=4`） | 新版主可登录后台，仅本板块 |
| 无板块版主 | 可登录、菜单可见、列表为空、写操作 403 |
| 版主管理“编辑板块” | 差异更新任职，撤销不降级 |
| Q9 | 有有效版主的板块禁止删除 |
| v039 迁移 | 普通用户 + 有效任职 → 版主；ADMIN/SUPER 不变 |
| 权限矩阵 Phase A | 新增只读页面，仅超管可见 |
| 首次登录强制改密（C3） | 管理员/超管/版主 `is_first_login=1` 时先改密，后端同时拦截管理接口 |
| 关键操作审计日志（C4） | 新增 `bbs_audit_log`（v040）；关键写操作与 403 留痕；仅超管可查 |
| 权限组 Phase B/C | 新增 `bbs_permission_group*`（v041）；grant-only；管理端权限管理页；审计留痕 |
| 前台动作权限 Phase D | 新增 `post:create` 等权限点；关键前台写接口接后端校验 |

---

## 十二、脚本安全说明

- `Pre` / `Post` / `Api` 都不执行写 SQL，只读查询 + HTTP 只读业务接口。
- 写操作只存在于 `sql/*/rollback.sql` 和 `sql/*/backup-before-v039.sql`，都需要手工执行。
- 密码不会写进日志；`PGPASSWORD` / `MYSQL_PWD` 用完即清理。
