# 发布检查包（离线内网可用）

> 对应范围：`3a3130e` 之后的全部提交（权限重构 + 版主角色化 + 权限矩阵 Phase A）。
> 生产机当前是 `3a3130e`，**这些功能都还没在生产验证过**，按本清单走一遍再放量。

这个文件夹是自包含的，直接整包拷到生产机即可，不需要联网、不需要 Node、不需要 Maven。
唯一依赖：**PowerShell 5.1** 和 **mysql 客户端**（`mysql.exe`）。

---

## 一、先看这三句话

1. **部署前**跑一次 `Pre`，它只读数据库，并把基线存到 `out/`。
2. **部署后**跑一次 `Post`，它会对比基线、检查 v039、再调几个 API。
3. 任何 `FAIL` 都先停下来排查，不要继续放量；`SKIP` 表示你没提供对应账号，不是失败。

---

## 二、包里有什么

```text
release-check/
├─ README.md                       ← 你正在看的文件
├─ check.ps1                       ← 唯一入口，三种模式：Pre / Post / Api
├─ sql/
│  ├─ precheck.sql                 ← 部署前只读检查
│  ├─ postcheck.sql                ← 部署后只读检查
│  ├─ backup-before-v039.sql       ← 可选：部署前建回滚备份表
│  └─ rollback.sql                 ← 回滚专用，需手工确认后执行
└─ out/                            ← 运行后自动生成：基线、日志
```

---

## 三、部署前（约 5 分钟）

### 1. 备份数据库

```powershell
# 整库备份（文件名自己改日期）
mysqldump -uroot -p bbs_db > D:\backup\bbs_db_before_v039.sql
```

### 2. 记录基线（只读，不写库）

```powershell
cd release-check
.\check.ps1 -Mode Pre -MySqlExe "C:\mysql\bin\mysql.exe" -DbUser root -DbName bbs_db
```

会提示输入 MySQL 密码。看到 `PASS v039 尚未执行` 即可。

重点看输出里的：

- `candidates`：本次会被升级成版主的人数
- `candidate_user:*`：具体是谁
- `type_2` / `type_3`：管理员、超管数量（部署后必须不变）
- `comment`：当前列注释（应该还不含“4：版主”）

> 如果 `candidates` 与业务预期不符，**先别部署**，把名单发给业务确认。

### 3.（可选，但推荐）建回滚备份表

```powershell
mysql -uroot -p bbs_db < sql\backup-before-v039.sql
```

有了它，回滚只影响“本次被升级的人”，不会误伤人工设置的版主。

---

## 四、部署

按你们现有流程替换 jar / 重启即可。

**注意**：新版本启动时会自动执行 `v039-moderator-role`，这是预期行为。
启动后确认日志里有迁移成功、没有 `ERROR`。

---

## 五、部署后（约 10 分钟）

### 1. 数据库 + 接口一起检查

```powershell
.\check.ps1 -Mode Post `
  -MySqlExe "C:\mysql\bin\mysql.exe" -DbUser root -DbName bbs_db `
  -BaseUrl "http://127.0.0.1:8080/bbs-server" `
  -SuperUser "你的超管账号" -SuperPassword "密码"
```

### 2. 有测试账号就一起带上（没有会自动 SKIP）

```powershell
.\check.ps1 -Mode Post `
  -MySqlExe "C:\mysql\bin\mysql.exe" -DbUser root -DbName bbs_db `
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
| `不存在非法 user_type` | PASS |
| `没有遗漏的升级候选` | PASS |
| `列注释包含“4：版主”` | PASS |
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

## 六、手工 UI 检查（5 项，约 5 分钟）

| # | 操作 | 预期 |
|---|---|---|
| 1 | 超管登录 → 左侧“系统管理” | 出现“权限矩阵”，点开能看到：角色→权限点、权限点→角色、接口→注解 |
| 2 | 管理员登录 → 直接访问 `/permission-matrix` | 跳 `/403`，**不会被退出登录** |
| 3 | 版主（有板块）登录 | 只有“业务管理”；帖子管理只看到自己板块；看不到用户/配置/版主管理 |
| 4 | 版主（无板块）登录 | 顶部提示“尚未分配板块，请联系管理员”；列表为空；写操作提示同一句 |
| 5 | 普通用户走后台登录 | 登录失败，提示用户名或密码不正确 |

> 生产没有版主账号时，3/4 可以先跳过；但上线后第一次任命版主时补做。

---

## 七、回滚（只有出问题时才做）

**顺序不能反**：

1. 先停服，回退到旧 jar（不含 v039 的版本）；
2. 执行回滚 SQL：

```powershell
mysql -uroot -p bbs_db < sql\rollback.sql
```

3. 再启动旧版本。

原因：当前版本一启动就会重新执行 v039；先回滚 SQL 再启动新 jar，等于白做。

回滚后复核：

```sql
SELECT COUNT(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role';  -- 应为 0
```

没有备份表时，`rollback.sql` 末尾有应急语句（会把所有 `user_type=4` 降回 1，会误伤人工版主），默认是注释状态。

---

## 八、常见问题

**Q：`Post` 报 `v039 已记录` FAIL？**
先看后端启动日志有没有迁移报错；再看 `bbs_schema_version` 是否有记录。不要手工补记录。

**Q：`candidates` 很多，正常吗？**
说明历史上有大量“普通用户 + 有效版主任职”。这是把存量版主升级为账号角色，属于预期；但名单要让业务确认。

**Q：`comment` 没变？**
v039 没执行成功，或连错了库。确认 `-DbName` 是目标库。

**Q：`ADMIN 访问权限矩阵` 不是 403？**
检查是否真的用管理员账号登录；超管能访问是正常的。

**Q：脚本提示找不到 `mysql`？**
把 `-MySqlExe` 指到完整路径，例如 `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`。

**Q：手工用 curl / Postman 调接口，被拒绝却返回 HTTP 200？**
后端统一把权限拒绝包装成 `HTTP 200 + body.code=403`，判断时以 `body.code` 为准；本脚本已按这个口径处理。

**Q：能不能只跑接口？**

```powershell
.\check.ps1 -Mode Api -BaseUrl "http://127.0.0.1:8080/bbs-server" -SuperUser "..." -SuperPassword "..."
```

---

## 九、本次发布包含什么（对照 `3a3130e`）

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

---

## 十、脚本安全说明

- `Pre` / `Post` / `Api` 都**不执行写 SQL**，只读查询 + HTTP GET/POST 业务接口。
- `Post` 会调用 `POST /admin/article/list` 等**只读业务接口**，不会修改数据。
- 写操作只存在于 `sql/rollback.sql` 和 `sql/backup-before-v039.sql`，都需要你手工执行。
- 密码不会写进日志；`MYSQL_PWD` 用完即清理。
