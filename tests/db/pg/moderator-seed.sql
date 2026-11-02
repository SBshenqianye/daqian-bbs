-- PG v039 迁移烟测固定数据（仅用于副本库，库名必须包含 test）
SET client_encoding = 'UTF8';

DELETE FROM bbs_schema_version WHERE version = 'v039-moderator-role';
DROP TABLE IF EXISTS bbs_user_type_v039_backup;
CREATE TABLE bbs_user_type_v039_backup (
  user_id integer PRIMARY KEY,
  old_user_type varchar(20),
  backed_up_at varchar(20)
);

DELETE FROM bbs_board_moderator WHERE user_id BETWEEN 9101 AND 9110;
DELETE FROM bbs_user WHERE id BETWEEN 9101 AND 9110;

INSERT INTO bbs_user (id, username, password, nickname, is_alive, is_delete, create_time, user_type, is_first_login)
VALUES
  (9101, 'NORMAL_MIG_PG',  (SELECT password FROM bbs_user WHERE id = 1), '迁移-普通用户有板块', 0, 0, '2026-10-01 10:00:00', '1', 0),
  (9102, 'ADMIN_MIG_PG',   (SELECT password FROM bbs_user WHERE id = 1), '迁移-管理员有板块',   0, 0, '2026-10-01 10:00:00', '2', 0),
  (9103, 'SUPER_MIG_PG',   (SELECT password FROM bbs_user WHERE id = 1), '迁移-超管有板块',     0, 0, '2026-10-01 10:00:00', '3', 0),
  (9106, 'MOD_MIG_PG',     (SELECT password FROM bbs_user WHERE id = 1), '迁移-人工版主',       0, 0, '2026-10-01 10:00:00', '4', 0),
  (9107, 'DELETED_MIG_PG', (SELECT password FROM bbs_user WHERE id = 1), '迁移-已注销用户',     0, 1, '2026-10-01 10:00:00', '1', 0),
  (9108, 'INVALID_MIG_PG', (SELECT password FROM bbs_user WHERE id = 1), '迁移-非法角色',       0, 0, '2026-10-01 10:00:00', '9', 0);

INSERT INTO bbs_board_moderator (user_id, label_id, role_type, status, appoint_time, create_time)
VALUES
  (9101, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9102, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9103, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9106, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9107, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00');

-- 只备份“可能被 v039 改写”的普通用户；9108 的非法值归一不属于回滚范围
INSERT INTO bbs_user_type_v039_backup (user_id, old_user_type, backed_up_at)
SELECT id, user_type, '2026-11-02 00:00:00'
FROM bbs_user
WHERE id BETWEEN 9101 AND 9110 AND user_type = '1';
