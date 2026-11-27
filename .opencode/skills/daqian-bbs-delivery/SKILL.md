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
bash scripts/prod/package.sh          # → dist/bbs-upgrade-<时间戳>.tar.gz（瘦包 ~5MB）
bash scripts/prod/verify-build.sh     # 构建机自检，要求 FAIL=0
```

- `package.sh` / `verify-build.sh` 是 bash 脚本，**在 WSL 执行**；Windows 只负责产出构建产物。
- `package.sh` 依赖 `python3/python` 生成瘦 jar（去掉 `BOOT-INF/lib`），依赖由基础镜像 `/app/lib` 提供。

- 首次切换 / 基础镜像依赖变更，顺序**不能反**：
  `bash scripts/prod/base.sh --save` → 服务器 `podman load` 新基础镜像 → 删除旧容器 → 再发瘦升级包。
- 服务器升级：`sudo bash /data/bbs/deploy-offline.sh --upgrade /tmp/bbs-upgrade-*.tar.gz`；
  或新版一体化入口 `bash scripts/prod/offline.sh <tar 路径>`。
- 部署后验证（生产机）：`bash verify-server.sh`（默认 `BBS_HOME=/data/bbs`、`BBS_SERVER_PORT=60000`），要求 `FAIL=0`。

### WSL 测试部署（生产对齐）

```bash
bash scripts/dev/wsl.sh              # 依赖 Windows 构建产物，bind-mount 部署
bash scripts/dev/wsl.sh --restart-only
```

- 端口：nginx `60001` / 后端 `60000` / PG `15432`；E2E 的 `baseUrl` 用 `http://localhost:60001/bbs-user/`。
- 部署后先跑 `verify-server.sh`，再跑 E2E（HTA 或 Selenium）。

### 发布一致性（2026-11-20，stale 包事故）

- **版本号必须从产物取证，不能从源码自证**：包内 `version.txt` 的 `schema_version` 由 `package.sh` 从**包内 jar** 的 `BOOT-INF/classes/db/init/upgrade-mysql.sql` 读取；`verify-build.sh` 再与源码最新 marker 比对。反例：2026-11-18 的包源码已到 v073，jar 里只有 v065 —— 若版本号从源码算，这个包会"自证清白"。
- **只 `mvn compile` 不重打 jar 是 stale 包的根源**：打包前必须 `mvn clean package -DskipTests`。
- **生产升级前记录起点**：`SELECT version FROM bbs_schema_version ORDER BY applied_at DESC LIMIT 3;`，与包内 `schema_version` 一起留档。
- **一键部署**：生产机 `cd /data/bbs/scripts/prod && bash one_key_deploy.sh`（零参数，自动取根目录最新 `bbs-upgrade-*.tar.gz`，校验 `.sha256`，验证通过后归档到 `archive/`）；开发机用根目录 `./deploy.sh`（`-n` 只预检）。服务器上至少需要 `scripts/prod/` 下的 `one-key-deploy.sh`、`one_key_deploy.sh`、`offline.sh`、`verify-server.sh`。

## 3. 数据库

- 本地：`mysql -uroot -proot bbs_db`。
- 四份 SQL 必须同步修改：`db/init/init-mysql.sql`、`init-pg.sql`、`upgrade-mysql.sql`、`upgrade-pg.sql`。
- 迁移块格式：`-- @migration: vNNN-名称`，`DatabaseInitializer` 按 marker 执行并记录到 `bbs_schema_version`；**追加时必须换行开头**。
- `init-*.sql` 含 `DROP TABLE`，**切勿对生产库执行**。
- 版本演进：截至 2026-11-20 已到 `v073`（v072/v073 = 敏感词遮蔽开关、词表分类 + 命中记录表 + `sensitive:read`）；历史逐条列表不再维护，以 `bbs_schema_version` 为准。
- **迁移块必须可重放**：新增 `ALTER TABLE ... ADD COLUMN` 一律先查 `information_schema.COLUMNS` 再 `PREPARE`；`RENAME COLUMN` 要判断"旧列在且新列不在"；`PREPARE` 与 `DEALLOCATE PREPARE` 必须成对（v021 曾漏写 `PREPARE`，纯语法错误）。反例：v012/v015/v016 无条件 `ADD COLUMN`，在 init 已含该列时重放报 `Duplicate column`。
- **CLI 重放结果 ≠ 应用升级结果**：应用层有两条兜底 —— 全新库走 `baselineExistingMigrations`（把文件里所有迁移标记为已执行，一条都不跑）；执行时 `isTolerableFailure` 把 `Duplicate column` / `does not exist` 记为 warning 继续。所以 CLI 报错不代表升级会失败，CLI 通过也不代表升级路径被验证过。
- **验证升级路径要用"落后的库"**：用停在小版本号的副本（如 WSL 的 PG 停在 v036），不是全新库。判据：日志 `Migration applied: vNNN` 的条数与 `bbs_schema_version` 增量一致，且无 `FAILED`。

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

- 验收清单：`docs/human/验收/验收清单-2026-11-13.md`（已入库），保持**一页**、可逐条打勾。
- 提交按模块分组：`feat(server)` / `feat(admin)` / `feat(ui)` / `chore`；提交前 `git status` 必须干净。
- 推送默认只走 `github`（`SBshenqianye/daqian-bbs`）；`upstream`（`kimljx/daqian-bbs`）是他人仓库，**未明确要求时不推**（2026-11-20 用户裁决）。
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
- Win7 现场（数据库客户端 HeidiSQL/libpq、HTA/Selenium 验收、DLL 兼容）统一见 skill `win7-field-ops`。

## 8. 内容治理口径（2026-11-20 起）

- **三档处置**（后台「敏感词管理」逐条切换）：`严禁词·打码`（前台 `***`，计入风险与审核阈值）／`严禁词·仅监控`（不打码，仍计入风险）／`垃圾词·警告放行`（不打码、不计风险、不计分，内容照常可见）。
- **一张词表两个分类**：`bbs_sensitive_word.category = sensitive|spam`；`mask_enabled` 只对 sensitive 生效，spam 一律不参与打码。
- **命中必须在写库时采集**：脱敏是读时替换（`SensitiveWordUtil.desensitize`），从 `***` 反推不出原词；采集落 `bbs_sensitive_hit`（一内容一行，`uk(target_type,target_id)`，编辑只更新）。采集异常必须吞掉，不能影响发布。
- **质量分与合规分离**：`ContentQualityUtil` 只判形式（长度/纯数字/重复/灌水词），已删除"敏感词 -30 分"；垃圾词走"警告放行"不再硬拒绝（2026-09 事故根因是静默不可见，不是拒绝）。
- **进审核靠行为阈值**：同一用户 24h 命中 ≥3 条（字典 `sensitive_review_threshold`）→ 新内容 `enable=0`，通知作者 + 板块版主；无版主时按 `article:review` 上推。
- **打码只对外、对作者不隐藏**：编辑页必须走 `GET /article/getForEdit/{id}`（仅作者本人、不脱敏），否则回显 `***` 再保存会把原文永久覆盖（库里已有此类脏标题）。
- **新增权限点要写三处**：`Permission` 枚举、`admin-ui` 的 `permissionCatalog.js`、迁移里给 `role_super/role_admin/role_moderator` 插 `bbs_permission_group_item`；漏第三处等于谁都没有（`hasPermission` 只读权限组）。

## 9. 2026-11-23 迭代沉淀（编辑器 / 渲染安全 / 编辑链路 / 系统维护）

### 9.1 为什么“单测 + E2E 全绿”仍能把功能发坏

- 真实案例：前端用 `postRequest` 调 `@GetMapping` 的 `/article/getForEdit/{id}`，登录后必然 405。单测直连 Service（不经过 MVC），E2E 没覆盖编辑流程，负向自检只有 4 个固定探针 —— 三层都测不出来。
- **判据**：任何“前端调后端”的改动，先看 controller 注解，再看前端用哪个 request 方法。
- **已固化的防线**：`tests/dev/api-method-contract.test.js` 扫控制器映射（含类级 `@RequestMapping` 前缀）与两套前端的请求调用，路径归一化后比对方法；已接入 `verify.ps1` 第 1 步（方法契约失败直接中止）。变异自检：把 `getRequest` 改回 `postRequest` 必须变红。
- 同一类坑的另一半：`ResultBean` 未解包。除 `Article` 等直接返回实体的老接口外，一律取 `resp.obj`。

### 9.2 `contenteditable` 不要接 Markdown 工具栏

- 现象：点 B 只插入 `**粗体**` 原文；无选区时用 `Range` 回退选区算错，选中“占位符尾部 + 闭合标记”，一打字就吃掉 `**`，越点越乱。
- 结论：要么纯 WYSIWYG（`execCommand`），要么纯 Markdown 源码（`textarea`）。本仓库选后者：`textarea` + `setSelectionRange` 精确包裹，图片/链接直接插 `![]()` / `[]()`，删掉 `htmlToMd`（它会把 `<strong>`/`<ul>` 静默抹成纯文本）。
- 工具栏按钮加 `@mousedown.prevent`，否则点击会抢走编辑区焦点。

### 9.3 `v-html` 渲染用户内容必须有白名单

- `mdToHtml` 不自带转义，`<img onerror>`、`javascript:` 链接会原样进 DOM。统一走 `shared/sanitize-html.js`：删 `script/iframe/svg/form`，清 `on*` 与危险协议，保留排版标签与安全 `style`；详情页、发帖预览、后台 markdown-it 预览、mavon（`html:false`）都要接。
- 用 `DOMParser` 走真实 DOM 再序列化，比正则可靠；历史数据可能存过裸 HTML，所以选“过滤”而不是“整体转义”。

### 9.4 迁移编号是完整 id，不是 `vNNN` 前缀

- `bbs_schema_version.version` 存的是 `@migration:` 后整串（如 `v076-editor-preview-default`），两个 `v076-*` 可以共存。**不要因为“前缀撞车”擅自改号**（2026-11-23 曾改出一个孤儿 `v077-editor-preview-default` 行，无对应块、无副作用但会干扰排查）。
- 结论：新增迁移直接取下一个未被占用的完整 id；已执行的块永远不改，只新增。

### 9.5 有效权限只来自权限组（枚举默认值不生效）

- `Permission` 的 `adminDefault` 仅用于旧库 `base_role` 物化；新权限点若不写迁移，**组里没有这个 code，谁都拿不到**（含超管）。
- 一个权限点要落到五处：枚举、`permissionCatalog.js`、`bbs_permission_group_item` 迁移、接口 `@PreAuthorize`、前端 `meta.permission` + 侧边栏 `permission` 字段。

### 9.6 隔离库实测流程（不碰 `bbs_db`）

1. `mysqldump bbs_db` → 导入 `bbs_e2e_test`；
2. 往副本库插测试数据（如把某用户标成 `is_test_account=1`、造待审举报）；
3. `SPRING_PROFILES_ACTIVE=e2e BBS_SERVER_PORT=9084` 起后端（本机内存紧张时加 `MAVEN_OPTS=-Xmx512m`）；
4. 管理端可直接静态托管 `bbs-admin-ui/dist`（约 30 行的 node 静态服务器 + `/admin-api` 反代到 9084），**别在内存紧张时再起一个 webpack dev server**；
5. 验完停掉临时服务，副本库与测试数据保留便于复看。

### 9.7 Vue dev server 的三个验收假象

- 只改 hash 的 `navigate` 不会重载文档，旧 bundle 还在跑 → 页面“看起来没变化”。改完路由/页面后必须整页重载。
- 用 `/bbs-admin/index.html#/...` 打开会破坏 router 的 `base`，深链会落到默认页。
- 自定义下拉的 `onBlur` 延时关闭会在“失焦后又立刻聚焦”时把刚打开的面板关掉；延时回调里要二次确认 `document.activeElement`。

### 9.8 打包与开发栈

- 构建只在 Windows、打包只在 WSL；构建前必须停开发栈（`mvn clean` 与 devtools 抢 `target/`）。
- **`scripts/dev/package.ps1`** 已用 `try/finally` 包住：无论构建/打包成败，结束都执行 `up.ps1`；判据是打包后 9081/9082/9083 仍 listening。
- 打包判据另见 `verify-build.sh`：`FAIL=0`、瘦 jar <5MB、包内 jar 的 schema 与源码一致。

### 9.9 PostgreSQL 备份的两层，别混为一谈

- 迁移前自动备份 = 同库新 schema `bbs_backup_yyyyMMdd_HHmmss`，**只保留最近 3 份**，更早的在下次备份前被 `DROP SCHEMA ... CASCADE`；与业务库同生共死，容器重建一起没。
- 灾备导出 = `scripts/prod/ops/pg-backup.sh`（`podman exec <pg> pg_dump | gzip`），**目前未接入部署**；宿主机没有 `pg_dump`，必须 exec 进容器。
- 页面只读展示、不提供删除/恢复；部署时自动备份的方案见 `docs/human/运维/系统维护-运维能力规划-2026-11-23.md`（待实现）。

### 9.10 原生 SQL 返回 Map 时，PG 会把别名折叠成小写（2026-11-23 生产问题）

- 现象：生产（PostgreSQL）点「清理测试账号帖子」提示“当前没有测试账号发布的帖子”，MySQL 开发库正常。
- 根因：`SELECT a.user_id AS userId ... COUNT(1) AS articleCount` 在 PG 里返回的列名是 `userid` / `articlecount`（未加引号的标识符一律折叠小写），用 `List<Map<String,Object>>` 接收时 `row.get("articleCount")` 恒为 null，合计恒 0。
- 判据：**聚合/联表查询一律用 VO 接收**（属性匹配大小写不敏感），不要用 Map；历史 Map 结果用 `MapRowUtil.get(row, "userId")`（忽略大小写与下划线）。
- 同类隐患已一起修：`LevelService` 批量等级、`MonthlyDeductionAlertService` 月度扣分提醒、违规弹窗内容定位（`commentId` / `articleId` / `replyId`）——这些在 PG 上原本都静默失效。
- 复验命令（WSL，只读）：`podman exec -i bbs-postgres psql -U work_flow -d bbs < 查询.sql`，看表头是不是 `userid / articlecount`；表头小写即证明 Map 方式取不到。

### 9.11 首页 Hero 影像带与打包的环境坑（2026-11-26）

- **WSL 冷启动会让 `package.ps1` 假失败**：机器刚重启时首跑抛 `Starting the CLR failed with HRESULT 80004005`，此时**没有产出任何产物**（`dist/` 不会多包）。先 `wsl -e bash -lc "echo ok"` 确认 WSL 可用再重跑，通常一次就通；不要为此改脚本或换方案。
- **追加迁移块时别让两份 SQL 串味**：用 PowerShell `Add-Content` 往 `upgrade-pg.sql` 追加时，若把 MySQL 的反引号（`` `column` ``）一起带过去，PG 会直接语法错。**两份 upgrade/init SQL 必须分别写、分别核对**；本地只跑 PG 时，MySQL 那份只能人工 review，发版后要到生产日志确认 `Migration applied: vNNN-xxx` 才算过。
- **`home` 配置组驱动首页影像带**（`v078-home-hero-images` 起）：`bbs_system_config` 里 `config_group='home'` 的 `hero_image_1..4`（每格图片地址，留空=用打包内置图）+ `hero_caption`（右下角来源标注，留空=隐藏）。后台「配置管理 → 系统配置 → 首页 Hero 影像带」逐格上传走 `/common/upload`、写入走 `config:write`；`/common/systemConfig/listByGroup` 是 permitAll，用户端 `mounted` 读一次即可。**换图不需要重新打包**，但自定义图不套第 4 格的“竖幅推近”裁切（`.hero-photo__slot--custom` 走 cover 居中）。

### 9.12 图片裁剪上传的坑（Hero 裁剪，2026-11-26）

- **`getCroppedCanvas({maxWidth, maxHeight})` 会二次缩放**：cropperjs 先按上限缩源图（`source.width`），再把裁剪框坐标乘以 `source.width / naturalWidth`，所以 3840×2160 的源图选 3:2 也裁，结果只有 1350×900，而不是预期的 1600×1067。正确做法：先 `getCroppedCanvas()` 取原分辨率，再自己建 canvas 等比缩到目标最长边（本次 1600）。
- **重新裁剪只能基于已上传的图**：反复裁会一层层掉清晰度。必须给“恢复默认 → 重新传原图”的逃生口，并在弹窗里写明，否则用户裁两次就以为功能坏。想彻底避免要额外保存原图 URL（当前未做）。
- **跨域图必须先进 blob**：把已上传 URL 直接塞给 cropper，`canvas.toBlob()` 会抛 `SecurityError`（画布被污染）。先 `fetch(url, {credentials:'same-origin'}) → blob → URL.createObjectURL`，用完 `revokeObjectURL`。
- **上传前裁剪顺带解决“原图太大”**：4K 原图裁成 1600×1067、q0.9 后约 400KB（实测 391KB），比原图上传快且不占磁盘；大小上限（`spring.servlet.multipart.max-file-size=100MB`）反而不是瓶颈，慢在传输。
- **Hero 每格的显示比随视口变，单一裁剪比例不可能精确**：260px 定高 + 四等分列，实测 1920≈2.07、1440≈1.55、1280≈1.38、1100≈1.19、768≈0.83（390 宽时条高 360、第 3 格隐藏）。所以默认给「3:2」折中，另配 16:9 / 4:3 / 1:1 / 自由比例，由使用者按主要观看宽度选。
- **依赖与 lock**：`vue-cropperjs` / `cropperjs` 原本就在 `bbs-admin-ui` 依赖里但没用过，可直接复用；一旦 `import cropperjs` 就要把它提为直接依赖，`npm install --package-lock-only --offline` 在 lockfileVersion 3 下只增一行，不会重写整份。
- **这类弹窗的验收姿势**：Playwright `setInputFiles()` 对隐藏 `<input type=file>` 同样有效；以 `.cropper-container` 可见判弹窗就绪；上传后从预览的 `background-image` 里取 URL，再用页面内 `new Image()` 读 `naturalWidth/naturalHeight` 断言比例（4K 源必须得到 1600×1067），比肉眼看截图可靠。


