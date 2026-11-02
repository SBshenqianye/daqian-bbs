-- v039 回滚（PG）：仅回写被升级的普通用户 + 删除列注释 + 删除版本记录
-- 说明：PG 历史版本从未给 bbs_user.user_type 设置过注释（v039 才首次设置），
-- 因此回滚应恢复为“无注释”，而不是写回旧文案。
BEGIN;

UPDATE bbs_user u
SET user_type = b.old_user_type
FROM bbs_user_type_v039_backup b
WHERE u.id = b.user_id
  AND u.user_type = '4';

COMMENT ON COLUMN bbs_user.user_type IS NULL;

DELETE FROM bbs_schema_version WHERE version = 'v039-moderator-role';

COMMIT;
