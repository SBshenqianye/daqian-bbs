-- v039 回滚（PostgreSQL）
-- 执行顺序（必须遵守）：
--   1) 先把应用回退到不含 v039 的旧版本并停服；
--   2) 执行本文件；
--   3) 再启动旧版本。
-- 否则当前版本启动时会把 v039 重新执行一遍。
--
-- 前提：部署前执行过 backup-before-v039.sql。
-- 没有备份表时：只能把“有有效板块的 4”降回 1（会误伤人工设置的版主），见文件末尾。

BEGIN;

UPDATE bbs_user u
SET user_type = b.old_user_type
FROM bbs_user_type_v039_backup b
WHERE u.id = b.user_id
  AND u.user_type = '4';

COMMIT;

-- PG 历史库没有 user_type 列注释，v039 才首次设置；回滚恢复为“无注释”
COMMENT ON COLUMN bbs_user.user_type IS NULL;

-- 标记迁移未执行
DELETE FROM bbs_schema_version WHERE version = 'v039-moderator-role';

-- 校验：应为 0
SELECT COUNT(1) AS remaining_v039 FROM bbs_schema_version WHERE version = 'v039-moderator-role';

-- =========================================================================
-- 没有备份表时的应急回滚（取消下面两行的注释后执行；会误伤人工版主）
-- UPDATE bbs_user SET user_type = '1' WHERE user_type = '4';
-- =========================================================================
