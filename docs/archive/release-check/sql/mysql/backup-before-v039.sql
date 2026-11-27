-- 可选：部署前建立回滚备份表（只记录“会被 v039 升级”的候选用户）
-- 说明：不执行也可以部署；但执行后回滚可以精确恢复，不会误伤人工设置的版主。
-- 幂等：可重复执行。
CREATE TABLE IF NOT EXISTS `bbs_user_type_v039_backup` (
  `user_id` int NOT NULL PRIMARY KEY,
  `old_user_type` varchar(20) DEFAULT NULL,
  `backed_up_at` varchar(20) DEFAULT NULL
);

DELETE FROM `bbs_user_type_v039_backup`;

INSERT INTO `bbs_user_type_v039_backup` (`user_id`, `old_user_type`, `backed_up_at`)
SELECT u.`id`, u.`user_type`, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_user` u
WHERE u.`user_type` = '1'
  AND u.`is_delete` = 0
  AND u.`id` <> 1
  AND EXISTS (SELECT 1 FROM `bbs_board_moderator` m
              WHERE m.`user_id` = u.`id` AND m.`status` = 1);

SELECT COUNT(1) AS backup_rows FROM `bbs_user_type_v039_backup`;
