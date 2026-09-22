# 敏感词库（国内常见）

## 文件

- `sensitive_words.txt` —— 中文敏感词库（每行一个词）

## 来源

- 上游项目：GitHub `caroltc/SensitivePy`（`words.txt`）
- 原始地址：`https://raw.githubusercontent.com/caroltc/SensitivePy/master/words.txt`
- 下载日期：2026-09-21
- 使用前请以该项目仓库的许可证为准（若需商用/再分发，请确认 MIT 或相应许可条款）。

## 统计

| 项目 | 值 |
|------|-----|
| 格式 | UTF-8（无 BOM），每行一个词 |
| 总行数 | 7,516 |
| 去重后 | 7,509 |
| 词长范围 | 2 ~ 66 字符 |
| 覆盖类别 | 涉政、色情、暴恐、毒品、违禁品、违法交易、代考代开票、赌博、诈骗引流等泛类别（以文件内容为准） |

## 使用建议（与 #10 需求配套）

1. **导入方式（已执行）**：已按项目迁移规范生成迁移块 **v031-sensitive-words-import**（7,509 条，分 16 块）并追加至：
   - `bbs-server/src/main/resources/db/init/upgrade-pg.sql`
   - `bbs-server/src/main/resources/db/init/upgrade-mysql.sql`
   PG 用 `INSERT ... ON CONFLICT (keyword) DO NOTHING`，MySQL 用 `INSERT IGNORE`，全幂等；服务下次启动 `DatabaseInitializer` 自动执行。
   - 生成脚本：`docs/敏感词库/generate_migration.py`（可重复运行，先截断旧 v031 块再重新生成）。
2. **策略**：按用户已确认的方案，采用「命中替换 / 警告」而非一刀切拦截；后端 `SensitiveWordUtil.desensitize()` 已支持脱敏替换，可先接入该词库验证效果。
3. **启动后核对**：`SELECT count(*) FROM bbs_sensitive_word;`（应为原种子 + 7,509 − 与既有种子重复的条数）。
4. **误伤评估**：
   - 词库含部分整句/带空格描述类词条（最长 66 字符），匹配易误伤，建议抽样人工审核后按需清理；
   - 与现有 `ContentQualityUtil` 的敏感词扣分逻辑联调，确认命中后行为（降低质量分 vs 直接脱敏）。
5. **合规提醒**：敏感词过滤属于平台内容治理的合规措施；词库只用于本系统内容审核，请勿对外传播词库原文。
