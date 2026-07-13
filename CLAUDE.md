# 大千智荟 BBS 项目约定

## 数据库迁移规范

项目支持 **MySQL** 和 **PostgreSQL** 两种数据库。修改数据库 schema 时必须同时更新 4 个文件。

### 文件清单

```
bbs-server/src/main/resources/db/init/
├── init-mysql.sql      # 新环境完整建表（MySQL）
├── init-pg.sql          # 新环境完整建表（PostgreSQL）
├── upgrade-mysql.sql    # 增量变更（MySQL，每次重启执行）
└── upgrade-pg.sql       # 增量变更（PostgreSQL，每次重启执行）
```

### 修改规则

```
业务需求 → DDL/DML 变更
    ├── 更新 upgrade-mysql.sql（加新 step，必须幂等）
    ├── 更新 upgrade-pg.sql（加新 step，必须幂等）
    ├── 同步更新 init-mysql.sql（建表 + 初始数据保持一致）
    └── 同步更新 init-pg.sql（建表 + 初始数据保持一致）
```

即：**upgrade 和 init 总是同步更新，MySQL 和 PostgreSQL 总是同步更新**，不允许只改其中一个。

### 幂等要求

- **MySQL `upgrade-mysql.sql`**: 所有 DDL 用 `information_schema` 条件判断 + `PREPARE`/`EXECUTE`；DML 用 `WHERE NOT EXISTS`。确保每条语句重复执行零报错。
- **PostgreSQL `upgrade-pg.sql`**: 利用原生 `IF NOT EXISTS` / `ON CONFLICT DO NOTHING` / `WHERE NOT EXISTS` 保证幂等。不需要 information_schema + PREPARE 那套，PostgreSQL 原生支持。
- **`init-*.sql`**: 使用 `CREATE TABLE IF NOT EXISTS` / `INSERT ... ON CONFLICT DO NOTHING`，幂等安全。

### 字段类型对照

| MySQL | PostgreSQL |
|-------|------------|
| `tinyint(1)` | `smallint` |
| `int(11)` | `integer` |
| `longtext` | `text` |
| `varchar(N)` | `varchar(N)` |
| `datetime` / `varchar(20)` | `varchar(20)`（本项目统一用字符串存时间） |

### 测试数据

`init-*.sql` 包含首次部署初始数据（超级管理员、标签、组织机构、数据字典、敏感词），**不含**测试文章数据。

## 前后端结构

- `bbs-admin-ui/` — 管理后台（Vue 2 + Element UI）
- `bbs-ui/` — 用户前台（Vue 2）
- `bbs-server/` — Java 后端（Spring Boot + MyBatis-Plus）
