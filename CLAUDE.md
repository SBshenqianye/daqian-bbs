# 大千智荟 BBS 项目约定

## 数据库迁移规范

项目支持 **MySQL** 和 **PostgreSQL** 两种数据库。修改数据库 schema 时必须同时更新 4 个文件。

### 文件清单

```
bbs-server/src/main/resources/db/init/
├── init-mysql.sql      # 新环境完整建表（MySQL）
├── init-pg.sql          # 新环境完整建表（PostgreSQL）
├── upgrade-mysql.sql    # 增量迁移（MySQL，带版本追踪，只执行一次）
└── upgrade-pg.sql       # 增量迁移（PostgreSQL，带版本追踪，只执行一次）
```

### 修改规则

```
业务需求 → DDL/DML 变更
    ├── 更新 upgrade-mysql.sql（加新 @migration 块）
    ├── 更新 upgrade-pg.sql（加新 @migration 块）
    ├── 同步更新 init-mysql.sql（建表 + 初始数据保持一致）
    └── 同步更新 init-pg.sql（建表 + 初始数据保持一致）
```

即：**upgrade 和 init 总是同步更新，MySQL 和 PostgreSQL 总是同步更新**，不允许只改其中一个。

### 迁移版本追踪（@migration 标记）

`upgrade-*.sql` 中每个迁移块必须以 `-- @migration: <id> <description>` 开头：

```sql
-- @migration: v012-my-feature 我的功能描述
ALTER TABLE bbs_xxx ADD COLUMN ...;
UPDATE bbs_yyy SET ...;
```

- **版本 ID**：`v` + 三位序号 + `-` + 英文短名（如 `v012-add-xxx-field`）
- **描述**：中文简述迁移内容
- **执行策略**：`DatabaseInitializer` 启动时读取 `bbs_schema_version` 表，**只执行未记录的迁移块**，执行成功后记录版本号。已执行过的迁移不会重复执行。
- **文件开头到第一个 @migration 之间的 SQL**：视为"版本前遗留"，首次启动时自动标记为已执行（不会实际运行 DML）。

### 幂等要求

由于迁移只执行一次，**不再要求 DML 操作天然幂等**。但仍建议保留 DDL 幂等（`IF NOT EXISTS`）作为安全网：

- **MySQL `upgrade-mysql.sql`**: DDL 用 `information_schema` 条件判断 + `PREPARE`/`EXECUTE`；DML 无需特殊处理（迁移框架保证只执行一次）。
- **PostgreSQL `upgrade-pg.sql`**: 利用原生 `IF NOT EXISTS` / `ON CONFLICT DO NOTHING`。
- **`init-*.sql`**: 使用 `CREATE TABLE IF NOT EXISTS` / `INSERT ... ON CONFLICT DO NOTHING`，幂等安全。

### 版本追踪表

```sql
-- bbs_schema_version（由 init-*.sql 建表，DatabaseInitializer 管理）
CREATE TABLE bbs_schema_version (
    version     varchar(50) PRIMARY KEY,  -- 迁移版本标识（如 v011-lingdao-display）
    description varchar(255),             -- 迁移描述
    applied_at  varchar(20) NOT NULL      -- 执行时间
);
```

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

## 前端开发规范

### v-if/v-else 与复杂 DOM 组件

**禁止**在包含 `v-for`、`v-once`、事件委托的复杂组件外部使用 v-if/v-else 来控制其可见性。v-if 切换会**销毁并重建**整个子树 DOM，Vue 内部事件指令清理可能在 `vnode.elm === undefined` 上调用 `removeEventListener`，导致 `TypeError`，触发全局 `errorHandler` → `attemptRepair` → 页面重建 → 组件冻结。

**正确模式**：始终挂载复杂组件 DOM，用 CSS class 控制可见性：

```vue
<!-- ✅ 正确：始终挂载，CSS 切换 -->
<div :class="visible ? '' : 'opacity-0 h-0 overflow-hidden pointer-events-none'">
  <ComplexComponent />
</div>

<!-- ❌ 销误：v-if 会销毁/重建 DOM -->
<div v-if="visible">
  <ComplexComponent />
</div>
```

**浮层消息**（loading/empty/no-match）是简单 div，可用独立 v-if 链切换，无风险。详见 `OrgTree.vue`。

### 动态 :key 陷阱

在 v-for 或 v-if 分支上使用动态 `:key="reactiveValue"`，每次值变化都会销毁重建子树。若值在交互中频繁变化（如每次按键），会触发大量 DOM 操作。应使用静态 key 或稳定标识。
