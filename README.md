# 大千智荟论坛（BBS）

内网离线的论坛系统：Spring Boot 后端 + 两个 Vue 前端 + PostgreSQL，容器化部署（Podman/Docker），
升级包走"一个包、一个脚本、一条命令"。

## 技术栈

| 组件 | 说明 |
| --- | --- |
| `bbs-server/` | Spring Boot 2.5.5 / Java 8 / MyBatis-Plus，上下文路径 `/bbs-server` |
| `bbs-ui/` | 用户端（Vue CLI 4 / Webpack 4） |
| `bbs-admin-ui/` | 管理端（Vue CLI 5） |
| `nginx/` | 反向代理模板 + 基础镜像 Dockerfile |
| `shared/` | 前后端共享的纯 JS 逻辑 |
| `scripts/` | 分层脚本：`dev/` 本机、`prod/` 打包部署、`prod/ops/` 运维、`test/` 验收 |
| `delivery/` | 交付文档：采集包、演练记录、回滚命令、未验证项清单 |

## 快速开始

```bash
# 1. 本机三个服务（9081 用户端 / 9082 管理端 / 9083 后端）
powershell -File scripts/dev/up.ps1

# 2. 构建（前端串行，后端打瘦包）
cd bbs-ui && npm run build
cd ../bbs-admin-ui && npm run build
cd ../bbs-server && mvn clean package -DskipTests

# 3. 打包 + 产物自检
bash scripts/prod/package.sh
bash scripts/prod/verify-build.sh

# 4. 部署（包与 deploy.sh 同目录；日常升级就这一条）
sudo bash deploy.sh
# 一次性动作（迁移/回滚/修复）
sudo bash deploy.sh once migrate
```

## 文档入口

| 想知道 | 看 |
| --- | --- |
| 人看的文档怎么组织 | [`docs/README.md`](docs/README.md) |
| 需求 / 设计 / 验收 / 手册 | `docs/human/` |
| agent 的硬约束与判据 | [`AGENTS.md`](AGENTS.md) |
| 编码约定（数据库迁移、前端规范） | [`docs/agent/编码约定.md`](docs/agent/编码约定.md) |
| 部署与排障 | [`DEPLOY.md`](DEPLOY.md)、[`delivery/README.md`](delivery/README.md) |
| 变更记录 | [`CHANGELOG.md`](CHANGELOG.md) |

## 部署形态（当前）

```
/data/bbs/
├── deploy.sh                 # 唯一入口（根目录只允许它和 deploy-offline.sh）
├── releases/<版本>-<commit>/ # app/ + scripts/ + manifest.json + SHA256SUMS
├── releases/current          # 当前版本
├── versions/latest -> ../releases/current
├── current -> versions/latest   # 旧路径兼容（容器历史挂载）
├── packages/  backups/  log/  data/
├── bbsUpload/                # 上传文件（业务数据）
└── .env
```

- 容器挂 `releases/<id>/app/...` **真实路径**，不挂兼容链；
- 升级 = 备份 → 落盘 → 校验 → 切指针 → 重建容器 → 验证，失败自动回滚；
- 变更历史：`CHANGELOG.md`；部署细节：`DEPLOY.md`。
