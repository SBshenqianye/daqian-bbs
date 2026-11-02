-- 可选：部署前建立 PG 回滚备份表（只记录“会被 v039 升级”的候选用户）
-- 幂等：可重复执行。
CREATE TABLE IF NOT EXISTS bbs_user_type_v039_backup (
  user_id integer PRIMARY KEY,
  old_user_type varchar(20),
  backed_up_at varchar(20)
);

DELETE FROM bbs_user_type_v039_backup;

INSERT INTO bbs_user_type_v039_backup (user_id, old_user_type, backed_up_at)
SELECT u.id, u.user_type, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_user u
WHERE u.user_type = '1'
  AND u.is_delete = 0
  AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m
              WHERE m.user_id = u.id AND m.status = 1);

SELECT COUNT(1) AS backup_rows FROM bbs_user_type_v039_backup;
