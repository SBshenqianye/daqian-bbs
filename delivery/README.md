# 交付总览 · 统一部署入口与结构迁移（2026-11-27）

> 本轮范围：现场采集包、离线构建、测试机迁移演练、回滚演练。**未触碰生产。**
> 结论先说：测试机上「首次迁移 / 同版本重跑 / 升级 / 坏包 / 端口占用 / PG 备份恢复 /
> A/B 软链切换 / restart / 回滚 / 失败自动回滚」共 **75 项判据全 PASS、非预期 FAIL=0**
> （另有 6 项按设计"应该变红"的注入检查也确实变红；明细见 `演练记录-2026-11-27.md`）。
> 生产侧事实仍待 `collect.sh` 采集后确认（见 `未验证项清单.md`）。

## 1. 交付物清单

| 文件 | 说明 |
| --- | --- |
| `deploy.sh`（仓库根） | 新统一入口 v1.0.0：自动识别安装/迁移/升级，`help/status/verify/diag/rollback/repair/restart/migrate/sync-scripts` |
| `scripts/prod/package.sh` | 新包契约 v2：`payload/app` + `scripts` + `manifest.json/manifest.env/SHA256SUMS`，不再生成 `upgrade.sh` |
| `scripts/prod/verify-build.sh` | 构建机自检（23 项），覆盖新包契约 |
| `scripts/prod/deploy-offline.sh` | 兼容壳：`--install/--upgrade/--repair` 转新入口 |
| `scripts/prod/one_key_deploy.sh` | 兼容壳：转新入口（历史文档写的下划线名字） |
| `scripts/prod/one-key-deploy.sh` | 兼容壳：转新入口（旧实现归档 `legacy/`） |
| `legacy/deploy.sh.2026-11-27`、`legacy/one-key-deploy.sh.2026-11-27` | 旧入口原样归档，仅供追溯 |
| `delivery/collect/` | 现场只读采集包（`collect.sh` + `使用说明.txt` + `回传/`） |
| `delivery/rehearsal/` | 演练脚本（可重跑）：`setup-legacy.sh`、`run-scenarios.sh`、`s10-auto-rollback.sh` |
| `delivery/manifest-20261126_102549-291ae68/` | 迁移包的 `manifest.json` / `manifest.env` / `SHA256SUMS`（文本，便于评审） |
| `delivery/回滚命令.md` | 三档回滚命令（指针 / 容器 / 数据库） |
| `delivery/未验证项清单.md` | 生产侧未验证项 + 采集后要确认什么 |
| `delivery/演练记录-2026-11-27.md` | 判据与证据 |

## 2. 迁移包与入口

| 项 | 值 |
| --- | --- |
| `deploy.sh`（单独交付） | SHA256 `6A83DACBA5D834CF94B754CA3F63D327E87E4CC3EF740FB35158254456BFEDE1` |
| 迁移包 | `dist/bbs-migrate-20261127-112521.tar.gz`（8.1 MB） |
| 迁移包 SHA256 | `C317ACB9386C1F233B7F8E2ABCE8DD530053E7BAD749DF03CFA3A8325A3EC9EC` |
| **本次上传项数** | **2 项**（迁移包 + `deploy.sh`；旧入口不认识 manifest、不会自动回滚） |
| 生产采集核对 | `delivery/生产核对-2026-11-27.md`（现网 jar 哈希与包内一致） |
| 升级演练包 | `dist/bbs-upgrade-20261127-093351.tar.gz`（8.1 MB，SHA256 `92B2BAA6…3A7A`，仅用于演练） |
| `manifest.version` | `20261126_102549`（= 生产当前版本号，取自包内 `version.txt`） |
| `manifest.commit` | `291ae68` |
| `mode` / `stage` | `migrate` / `release` |
| `db_compat` | `backward-compatible`（本次不改 schema） |
| `payload/app/bbs-server.jar` | `972b28e3b57d479b3adb4d03ccb717787528d8e2e158deb33d8015b31a5eeefe`（与生产当前 jar 必须一致，迁移时逐字节校验） |
| 包内条目 | 284 个（238 个纳入 `SHA256SUMS`） |

**为什么用 `--from` 重打**：生产基线是 `291ae68`，仓库 HEAD 是 `5bb38ca`（只多了文档提交）。
迁移包必须"只改结构、不改代码"，因此直接复用 `dist/bbs-upgrade-20261126-102606.tar.gz`
里的产物重打成新契约，而不是重新编译 HEAD。新功能另发 `update` 包。

复现命令：

```bash
bash scripts/prod/package.sh --from dist/bbs-upgrade-20261126-102606.tar.gz --mode migrate
bash scripts/prod/package.sh --from dist/bbs-upgrade-20261126-102606.tar.gz --mode update --version 20261127_093500
bash scripts/prod/verify-build.sh
```

## 3. 现场执行顺序（生产）

> 前提：`collect.sh` 已回传并核对（`未验证项清单.md` 全部填实），且确认
> 「迁移包内 jar 哈希 == 生产 `current/bbs-server.jar` 哈希」。

**一条命令**（包在 U 盘或当前目录都行）：

```bash
cd /data/bbs
sudo bash deploy.sh once migrate
```

脚本自己完成：找包 → 校验整包 SHA 与逐文件 SHA → `pg_dump` 备份 → 落 `releases/` →
预校验 → 切指针 → 重建容器 → `verify`；**验证失败自动回滚并 exit 1**，包不归档。

- 首次迁移用 `once migrate`；之后日常升级只用 `bash deploy.sh`（不带参数）。
- 想先看不动手：`bash deploy.sh --dry-run`；看现状：`bash deploy.sh status`。
- 出问题：`bash deploy.sh once rollback`。

**窗口**：15 分钟（首次）。实际占用 = 备份（生产库可能 1–5 分钟）+ 重建容器（40–90s）+ 验证（30–60s）。
**不用 curl 判断切换是否成功**：挂载在容器启动时解析，指针切换本身对运行中容器不可见；
判据是 `readlink -f` 三条链一致 + 容器 `Mounts` 为 `releases/<id>/app/...` + `deploy.sh verify` 全 PASS。

## 4. 行为契约（与旧流程的差异）

| 场景 | 旧行为 | 新行为 |
| --- | --- | --- |
| 入口 | `deploy-offline.sh` / `one-key-deploy.sh` / `one_key_deploy.sh` 三个名字三套实现 | 全部转 `deploy.sh`；旧文件归档 `legacy/` |
| 版本目录 | `versions/<ts>/`，指针两跳 | `releases/<版本>-<commit>/app/`，`releases/current` 原子替换 |
| 容器挂载 | 挂 `/data/bbs/current/...` 兼容链 | 挂 `releases/<id>/app/...` **真实路径** |
| 旧回滚命令 | `ln -snf <ts> versions/latest && podman restart` | **仍然有效**（`current → versions/latest → ../releases/current`） |
| 升级脚本 | 包内 `upgrade.sh` | 废弃；由 `deploy.sh` 统一处理 |
| 切换时机 | 先切指针，再重启，失败不自动回滚 | 先校验/备份/落盘，再切换；**验证失败自动回滚并 exit 1** |
| 同版本重跑 | 新建一个版本目录 | 按修复处理，不新建目录 |
| 备份 | 无（只有库内 `bbs_backup_*` schema） | `pg_dump` → `backups/<ts>/`，`gunzip -t` + 非空 + 大小校验，KEEP=30 |
| 锁 | 无 | `.deploy.lock`，并发执行直接拒绝 |
| 坏包 | 解压后才可能失败 | 解压前拒绝绝对路径/`..`/包内 `deploy.sh`；整包 SHA + 逐文件 SHA 双重校验 |
| 诊断 | 只有上传专项目 `diagnose-upload.sh` | `deploy.sh diag` 通用诊断包（脱敏，<20MB） |

## 5. 目录契约（迁移后）

```
/data/bbs/
├── deploy.sh                     # 新入口（单独交付）
├── deploy.env                    # 主配置（600）；.env 原样保留兼容
├── current.json                  # 当前/上一版/时间
├── releases/
│   ├── 20261126_102549-291ae68/  # app/ + scripts/ + manifest.json + SHA256SUMS
│   └── current -> 20261126_102549-291ae68
├── versions/
│   ├── 20261126_102549/          # 旧目录，原样保留（回滚/取证）
│   └── latest -> ../releases/current
├── current -> versions/latest     # 兼容链（容器历史挂载源）
├── src -> current/app
├── scripts/                      # 全量同步（真实目录）
├── packages/  backups/  log/  data/  upload -> bbsUpload
├── bbsUpload/                    # 原样保留（容器挂载 + 历史图片）
├── base-images/  archive/  legacy/
└── .env
```

迁移**只用 `cp -a` 复制**：不 `mv`、不删 `versions/`、不动 `.env`、不动 `bbsUpload/`。
