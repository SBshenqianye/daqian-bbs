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

**所有迁移必须全幂等**（DDL + DML），不管数据库处于哪个历史版本，跑完后结构和基础数据一致：

- **DDL 幂等**：`CREATE TABLE IF NOT EXISTS`、`ADD COLUMN IF NOT EXISTS`、`CREATE INDEX IF NOT EXISTS`——有就跳过，没有就建。
- **DML 幂等**：`INSERT ... ON CONFLICT DO NOTHING`、`UPDATE ... WHERE NOT EXISTS`、`INSERT IGNORE`——没数据就插，有数据就跳过。
- **推荐写法**：每个迁移块从 DDL 到 DML 全幂等，一个块补齐所有缺失对象（列+表+索引+数据），不需要依赖特定 baseline 状态。
- **禁止 baseline 逻辑**：`DatabaseInitializer` 不得盲目将所有迁移标记为"已执行"。已因 baseline 跳过而缺失的对象，用新迁移（带 `IF NOT EXISTS`）补齐，不改旧迁移。
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

## 权限模型约定（2026-11 权限组唯一来源）

> 当前实现说明：`docs/权限组-实现说明-2026-11-04.md`

- 权限组是唯一权限来源：`bbs_permission_group` + `bbs_permission_group_item` 直接保存权限点；不再使用固定角色基线或 `base_role`。
- 每个用户有一个主权限组 `bbs_user.primary_group_id`：决定账号身份与数据范围；额外组 `bbs_user_permission_group` 只叠加权限，不改变身份和数据范围。
- 有效权限 = 所有启用组的 `grant` 并集 - `revoke`；不叠加任何角色默认权限。
- `bbs_user.user_type` 仅作为历史兼容/展示字段，**不参与鉴权**。
- 四个内置组 `role_super` / `role_admin` / `role_moderator` / `role_user` 是普通权限组，可编辑、停用、删除；编码只用于初始化、默认组回填和版主任命兜底，不是授权捷径。
- 数据范围只有主组生效：`GLOBAL`（全部板块）/ `BOARD_MODERATOR`（按 `bbs_board_moderator.status=1` 过滤）。版主任职是数据范围，不是账号角色。
- 安全不变量：至少保留一个启用且同时具备 `admin:access`、`permission:manage`、`user:grant:admin` 的权限组；不能从当前登录账号主组移除这三个关键权限，也不能停用/删除该组。
- 删除权限组时，主组用户优先迁移到 `role_user`，其次迁移到“全局且不带 `admin:access`”的普通组；找不到则置空，绝不迁移到管理类组。
- 系统通知按权限点广播，不依赖固定 `id=1`；自审批规则统一走 `SelfApprovalPolicy`。
- 当前登录人必须从 `SecurityContextHolder` 获取（`CurrentUserService`），不信任前端传入的 `operatorId/reviewerId/adminId`。
- 后端是最终权限边界；前端 `permissions` / `primaryGroup*` / `boardScopes` 仅用于菜单、路由、按钮展示。
- 管理端接口统一要求已认证 + `admin:access`（`AdminAccessFilter`）+ 方法级 `@PreAuthorize` 权限点；**禁止**再使用 `hasRole` / `hasAnyRole`。
- 权限不足统一返回既有 403 结构；`AdminAccessFilter` 拒绝时前端立即退出登录，方法级拒绝只弹一次提示并记录审计。
- 登录渠道 `02` 密码正确但缺少 `admin:access` 时，明确提示“该账号没有管理端访问权限”，不要误报密码错误。

### 前端权限

- 登录响应包含 `permissions`、`primaryGroupId/Name/Code`、`dataScopeType`、`boardScopes`。
- 管理后台路由使用 `meta.permission`；无权限弹窗提示并留在当前页，失去 `admin:access` 时退出登录。
- `adminStore.hasPermission()` 使用后端下发的权限点；进入受保护页面前会刷新一次 `/common/user/info`。
- 权限管理页编辑当前账号主组时，前端锁定 `admin:access`、`permission:manage`、`user:grant:admin` 三个关键权限。


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

**浮层消息**（loading/empty/no-match）是简单 div，可用独立 v-if 链切换，无风险。详见 `OrgTree.vue`（已迁移至 el-tree，不再使用 v-once + v-for 自定义渲染）。

### v-once 内禁止 v-if/v-else

**禁止**在 `v-once` 元素内部使用 `v-if`/`v-else`。`v-once` 会缓存渲染后的 vnode，当外层 `v-for` 销毁旧节点时，缓存的 vnode 中 `v-if` 分支的 `elm` 引用可能丢失（变为 `undefined`），导致 Vue 事件清理时 `removeEventListener` 报错。应改为始终挂载两个元素 + CSS class 切换：

```vue
<!-- ✅ 正确：v-once 内始终挂载，CSS 切换 -->
<div v-once>
  <button :class="condition ? '' : 'hidden'" @click.stop="handler">...</button>
  <span :class="condition ? 'hidden' : ''"></span>
</div>

<!-- ❌ 错误：v-once 内 v-if/v-else 会导致 elm 丢失 -->
<div v-once>
  <button v-if="condition" @click.stop="handler">...</button>
  <span v-else></span>
</div>
```

### 动态 :key 陷阱

在 v-for 或 v-if 分支上使用动态 `:key="reactiveValue"`，每次值变化都会销毁重建子树。若值在交互中频繁变化（如每次按键），会触发大量 DOM 操作。应使用静态 key 或稳定标识。
