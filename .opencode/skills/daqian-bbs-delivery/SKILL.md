---
name: daqian-bbs-delivery
description: 大千智荟论坛（daqian-bbs）仓库的构建、数据库迁移、核心模块与验收约定。适用于本仓库的开发、构建、迁移、部署与排障。
whenToUse: 在 daqian-bbs 仓库内开发、构建、跑测试、改迁移、部署或排查"改了没生效"时加载。
---

# 大千智荟论坛 · 仓库交付手册

## 1. 端口与目录

| 服务 | 端口 | 路径 |
|---|---|---|
| bbs-server（Spring Boot） | 9083 | 上下文 `/bbs-server` |
| bbs-ui（用户端） | 9081 | 代理前缀 `/bbs-user/` |
| bbs-admin-ui（管理端） | 9082 | 基础路径 `/bbs-admin/` |

- 前端 API 拦截器（`shared/api-interceptor.js`）返回**完整 ResultBean** `{code,message,obj}`，取数据用 `res.obj`，不要直接当对象用。
- 共享代码在仓库根 `shared/`：页面文件用 `../../../shared/...`，`components/` 下用 `../../../../shared/...`。

## 2. 构建与运行

```bash
# 后端
cd bbs-server
mvn -q -DskipTests compile
mvn -q test                     # 全量单测，基线 546 tests / 11 skipped
mvn -q spring-boot:run "-Dspring-boot.run.profiles=dev,local"

# 前端（必须顺序执行，禁止并行）
cd bbs-ui && npm run build
cd bbs-admin-ui && npm run build
```

- 构建前 `Get-Process node | Stop-Process -Force`，并设 `NODE_OPTIONS=--max-old-space-size=4096`。
- 后端启动会**自动备份数据库**（生成 `bbs_db_YYYYMMDD_HHmmss`），多次重启会留多个备份库，属正常现象。
- 管理端若由外部预览服务托管（可能返回单文件 `index.js`），改完需**重启预览服务**才能看到新代码。

## 2.1 打包与部署

### 构建（Windows）

```powershell
cd bbs-ui       ; npm run build
cd bbs-admin-ui ; npm run build
cd bbs-server   ; mvn clean package -DskipTests   # 会拷出 target/lib（基础镜像依赖层）
```

> Maven 没有 `build` 阶段，用 `clean package`；两个前端必须串行，`bbs-ui` 构建前设 `NODE_OPTIONS=--max-old-space-size=4096`。

### 离线升级包（WSL）

```bash
bash scripts/dist/package.sh          # → dist/bbs-upgrade-<时间戳>.tar.gz（瘦包 ~5MB）
bash scripts/verify-build.sh          # 构建机自检，要求 FAIL=0
```

- `package.sh` / `verify-build.sh` 是 bash 脚本，**在 WSL 执行**；Windows 只负责产出构建产物。
- `package.sh` 依赖 `python3/python` 生成瘦 jar（去掉 `BOOT-INF/lib`），依赖由基础镜像 `/app/lib` 提供。

- 首次切换 / 基础镜像依赖变更，顺序**不能反**：
  `bash scripts/build/base.sh --save` → 服务器 `podman load` 新基础镜像 → 删除旧容器 → 再发瘦升级包。
- 服务器升级：`sudo bash /data/bbs/deploy-offline.sh --upgrade /tmp/bbs-upgrade-*.tar.gz`；
  或新版一体化入口 `bash scripts/deploy/offline.sh <tar 路径>`。
- 部署后验证（生产机）：`bash verify-server.sh`（默认 `BBS_HOME=/data/bbs`、`BBS_SERVER_PORT=60000`），要求 `FAIL=0`。

### WSL 测试部署（生产对齐）

```bash
bash scripts/deploy/wsl.sh              # 依赖 Windows 构建产物，bind-mount 部署
bash scripts/deploy/wsl.sh --restart-only
```

- 端口：nginx `60001` / 后端 `60000` / PG `15432`；E2E 的 `baseUrl` 用 `http://localhost:60001/bbs-user/`。
- 部署后先跑 `verify-server.sh`，再跑 E2E（HTA 或 Selenium）。

## 3. 数据库

- 本地：`mysql -uroot -proot bbs_db`。
- 四份 SQL 必须同步修改：`db/init/init-mysql.sql`、`init-pg.sql`、`upgrade-mysql.sql`、`upgrade-pg.sql`。
- 迁移块格式：`-- @migration: vNNN-名称`，`DatabaseInitializer` 按 marker 执行并记录到 `bbs_schema_version`；**追加时必须换行开头**。
- `init-*.sql` 含 `DROP TABLE`，**切勿对生产库执行**。
- 已执行到 `v065`：
  `v044` 测试账号/扣分提醒、`v045` 排行榜分类、`v046` P0 配置、`v047` 种子修正、
  `v048` 内容互动、`v049` 审核配置、`v050` 等级/成就、`v051` 精华公示、
  `v052` 运营周期、`v053` 提醒、`v054` 组织导入、`v055` 评论/回帖点赞、
  `v056` 本部改普通分类、`v057` 等级图标自定义、`v058` SVG 素材库、
  `v059` 两套 21 级方案、`v060` 方案标签修正 + 内置图标入库、
  `v050b`/`v061` 序列同步与指数型升级门槛（两套 21 级）、
  `v062`/`v063` 指数门槛二次同步、`v064` 大阶段名称兜底、`v065` 版主奖励取消表索引改到 `ym`。

## 4. 核心模块位置

- **等级体系**：`shared/level-icons.js`（星/月/日/冠，行式 + 徽记）；`bbs_level_profile` / `bbs_level_item`；方案切换 `/admin/level/*`。
- **SVG 素材库**：`bbs_svg_asset` + `/admin/svg-asset/*`；绘制页面 `/svgStudio`（`SvgStudioPage.vue`）。
- **账户中心**：`BBSUserinfo.vue` 只负责「侧栏 + Tab 路由 + 红点」；概览/信息/密码在 `components/account/*`，内容面板在 `views/BBSMy*.vue`（`embedded` 模式）；URL 统一 `/userinfo?tab=<key>`。
- **排行榜**：`bbs_ranking_category(_org)`；`legacy_root`（内江供电公司本部）已是普通可配置分类，`categoryId=0` 仅作兼容映射。
- **内容互动**：`ArticleInteractionService`（赞/藏/分享）、`ThumbService`（评论/回帖赞）、`LabelPointsPolicyService`（不计分标签）。
- **通知**：`NotificationCategory` 注册表；新增通知类型必须在此登记分组。
- **权限**：`Permission` 枚举 + 权限组；提醒配置的权限下拉走 `/admin/reminder/permissions`。

## 5. 前端约定

- 统一使用 Element UI 弹窗（`el-dialog` / `MessageBox`）；手写弹层必须取 `utils/overlay.js` 的 `nextOverlayZ()`（内部复用 `PopupManager`）。
- `assets/css/overlay.css` 负责弹层样式：**不要给 `display` 加 `!important`**，否则 `v-show` 关不掉；全局阴影/圆角/居中都在这里改。
- 头像统一 `BBSAvatar`（自带等级角标与配色环），等级图标统一 `BBSLevelIcon` / `BBSLevelBadge`。
- 列表空态/到底提示：细分隔线 + 可清除筛选的空态卡，不再用大虚线卡片。

## 6. 验收与提交

- 验收清单：`docs/验收清单-*.md`，已在 `.gitignore` 中排除，保持**一页**、可逐条打勾。
- 提交按模块分组：`feat(server)` / `feat(admin)` / `feat(ui)` / `chore`；提交前 `git status` 必须干净。
- 涉及数据库的变更，验收时必须先备份，并在启动日志确认 `Migration applied: vNNN`。
- 打包/部署按 §2.1；生产部署后必须 `verify-server.sh` `FAIL=0`，再跑 E2E（内置负向自检）。

## 7. 本仓库已知坑

- 9082 可能是外部预览进程（返回 26MB 单文件 `index.js`），与 `dist` 无关；新路由 404 先怀疑产物过期。
- Element UI 的 `$confirm` 曾因 `display:flex !important` 无法关闭，改样式时优先保住关闭语义。
- 用户端/管理端构建并行会 OOM，必须串行。
- `init-*.sql` 与 `upgrade-*.sql` 里的新表要显式 `COLLATE=utf8mb4_general_ci`，否则与既有表联查会报 1267。

### 7.1 迁移双栈坑（2026-11-18 实测，生产事故来源）

- **`JOIN (SELECT ...) t` 缺 `ON`**：MySQL 当笛卡尔积照常执行，PostgreSQL 直接语法错误，整条迁移的插入全部丢失 —— 表现是「方案一/二等级项为空」，页面显示 0 级。写迁移时见到 `JOIN (` 一律补 `ON TRUE`。
- **init 与 upgrade 是两条独立路径，必须各验证一次**：
  `init-pg.sql` 里建表已经是新列名（`ym`），但历史迁移 v024/v027 仍按旧列名（`year_month`）建索引 → 升级路径正常、**全新安装报错**。判据：全新库启动日志里 `v024/v027` 的失败计数为 0。
- **已发布的迁移不改，只新增**：改法永远是加一条 `vNNN`（如 `v065` 在新列名上补索引）。MySQL 用 `information_schema.STATISTICS` 判断后 `PREPARE` 动态建，PG 用 `CREATE INDEX IF NOT EXISTS`。
- **序列（SERIAL）要单独核对**：种子数据手写了 `id` 时，`setval` 必须放在种子插入**之后**，否则新插入撞主键。
- 全新 PG 复验 SQL（期望 `默认 24 / 方案一 21 / 方案二 21`，阈值 `0…1900`）：
  ```sql
  SELECT p.profile_name, COUNT(i.id) AS items,
         MIN(i.points_threshold), MAX(i.points_threshold)
  FROM bbs_level_profile p LEFT JOIN bbs_level_item i ON i.profile_id = p.id
  GROUP BY p.id, p.profile_name ORDER BY p.id;
  ```
- 现场查库用 `dist/HeidiSQL-12.22-Win7-中文便携版.zip`（免费、Win7 可用、自带 `libpq-17.dll` 与简体中文语言包）；Navicat Premium Lite 官方要 Win8+，**Win7 装不了**。
