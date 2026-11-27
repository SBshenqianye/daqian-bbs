-- v039 回滚（MySQL）
-- 执行顺序（必须遵守）：
--   1) 先把应用回退到不含 v039 的旧版本并停服；
--   2) 执行本文件；
--   3) 再启动旧版本。
-- 否则当前版本启动时会把 v039 重新执行一遍。
--
-- 前提：部署前执行过 backup-before-v039.sql。
-- 如果没有备份表：只能把“有有效板块的 4”降回 1（会误伤人工设置的版主），见文件末尾。

START TRANSACTION;

-- 只回滚备份表里记录的候选用户，人工设置的版主不受影响
UPDATE `bbs_user` u
JOIN `bbs_user_type_v039_backup` b ON b.`user_id` = u.`id`
SET u.`user_type` = b.`old_user_type`
WHERE u.`user_type` = '4';

COMMIT;

-- 恢复列注释
ALTER TABLE `bbs_user`
  MODIFY COLUMN `user_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci
  NULL DEFAULT '1'
  COMMENT '用户类型（1：普通用户，2：管理员，3：超级管理员）';

-- 标记迁移未执行
DELETE FROM `bbs_schema_version` WHERE `version` = 'v039-moderator-role';

-- 校验：应为 0
SELECT COUNT(1) AS remaining_v039 FROM `bbs_schema_version` WHERE `version` = 'v039-moderator-role';

-- =========================================================================
-- 没有备份表时的应急回滚（取消下面两行的注释后执行；会误伤人工版主）
-- UPDATE `bbs_user` SET `user_type` = '1' WHERE `user_type` = '4';
-- =========================================================================
