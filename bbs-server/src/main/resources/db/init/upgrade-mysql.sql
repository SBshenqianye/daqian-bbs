-- ============================================
-- BBS 数据库升级脚本 - MySQL 版
-- 可重复执行：错误会被 catch 并记录为 warning
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段（幂等检查）
SELECT COUNT(*) INTO @col_icon_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article_label' AND COLUMN_NAME = 'icon';
SET @sql_icon = IF(@col_icon_exists = 0, 'ALTER TABLE bbs_article_label ADD COLUMN `icon` varchar(50) DEFAULT NULL COMMENT ''标签图标''', 'SELECT 1');
PREPARE stmt_icon FROM @sql_icon;
EXECUTE stmt_icon;
DEALLOCATE PREPARE stmt_icon;

SELECT COUNT(*) INTO @col_desc_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article_label' AND COLUMN_NAME = 'description';
SET @sql_desc = IF(@col_desc_exists = 0, 'ALTER TABLE bbs_article_label ADD COLUMN `description` varchar(200) DEFAULT NULL COMMENT ''标签描述''', 'SELECT 1');
PREPARE stmt_desc FROM @sql_desc;
EXECUTE stmt_desc;
DEALLOCATE PREPARE stmt_desc;

-- 2026-07-06: 支持评论和回复内容中的 Emoji 表情（utf8mb4 支持 4 字节 UTF-8）
-- MODIFY COLUMN 本身是幂等的，但检查字符集避免不必要的表重建
SELECT COUNT(*) INTO @col_not_utf8mb4 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_comment' AND COLUMN_NAME = 'comment_content' AND CHARACTER_SET_NAME != 'utf8mb4';
SET @sql_comment = IF(@col_not_utf8mb4 > 0, 'ALTER TABLE bbs_comment MODIFY COLUMN `comment_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ''评论的内容''', 'SELECT 1');
PREPARE stmt_comment FROM @sql_comment;
EXECUTE stmt_comment;
DEALLOCATE PREPARE stmt_comment;

SELECT COUNT(*) INTO @col_reply_not_utf8mb4 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_reply' AND COLUMN_NAME = 'reply_content' AND CHARACTER_SET_NAME != 'utf8mb4';
SET @sql_reply = IF(@col_reply_not_utf8mb4 > 0, 'ALTER TABLE bbs_reply MODIFY COLUMN `reply_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT ''回复的具体内容''', 'SELECT 1');
PREPARE stmt_reply FROM @sql_reply;
EXECUTE stmt_reply;
DEALLOCATE PREPARE stmt_reply;

-- 2026-07-13: 精华帖功能 — 文章表增加 is_featured 字段（通过 information_schema 判断，幂等安全）
-- 检查列是否存在，不存在才 ADD COLUMN（兼容 MySQL 5.7+）
SELECT COUNT(*) INTO @col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article' AND COLUMN_NAME = 'is_featured';
SET @col_sql = IF(@col_exists = 0, 'ALTER TABLE `bbs_article` ADD COLUMN `is_featured` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否为精华帖(0=否,1=是)''', 'SELECT 1');
PREPARE col_stmt FROM @col_sql;
EXECUTE col_stmt;
DEALLOCATE PREPARE col_stmt;

-- 检查索引是否存在，不存在才 ADD INDEX
SELECT COUNT(*) INTO @idx_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article' AND INDEX_NAME = 'idx_article_featured_time';
SET @idx_sql = IF(@idx_exists = 0, 'ALTER TABLE `bbs_article` ADD INDEX `idx_article_featured_time` (`is_featured`, `create_time`)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

-- 2026-07-13: 精华帖积分配置（存在则跳过，WHERE NOT EXISTS 在 MySQL 中不需要 FROM DUAL）
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `remark`)
SELECT 'featured', '10', '精华帖积分', 2, '被设为精华帖额外获得的积分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'featured');

-- 2026-07-15: 新增系统配置表（bbs_system_config），通过 information_schema 判断，幂等安全
SELECT COUNT(*) INTO @tbl_sc_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_system_config';
SET @sql_sc = IF(@tbl_sc_exists = 0, 'CREATE TABLE `bbs_system_config` (
  `id`           int(11) NOT NULL AUTO_INCREMENT COMMENT ''唯一标识'',
  `config_key`   varchar(100) NOT NULL COMMENT ''配置键'',
  `config_value` longtext COMMENT ''配置值（支持任意长度文本/JSON）'',
  `config_label` varchar(255) DEFAULT NULL COMMENT ''配置名称/说明'',
  `config_group` varchar(100) DEFAULT ''default'' COMMENT ''配置分组（如 contact/points/system）'',
  `config_type`  varchar(20) DEFAULT ''text'' COMMENT ''输入类型（text/textarea/json）'',
  `sort_order`   int(11) DEFAULT 0 COMMENT ''排序序号'',
  `remark`       varchar(500) DEFAULT NULL COMMENT ''备注说明'',
  `create_by`    varchar(50) DEFAULT NULL COMMENT ''创建人'',
  `create_time`  varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  `update_by`    varchar(50) DEFAULT NULL COMMENT ''修改人'',
  `update_time`  varchar(20) DEFAULT NULL COMMENT ''修改时间'',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_config_key` (`config_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''系统配置表''', 'SELECT 1');
PREPARE stmt_sc FROM @sql_sc;
EXECUTE stmt_sc;
DEALLOCATE PREPARE stmt_sc;

-- 2026-07-15: 使用反馈联系方式初始配置
INSERT INTO `bbs_system_config` (`config_key`, `config_value`, `config_label`, `config_group`, `config_type`, `sort_order`, `remark`, `create_by`, `create_time`)
SELECT 'feedback_contact', '{\"name\":\"\",\"email\":\"\"}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', '2026-07-15 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_system_config` WHERE `config_key` = 'feedback_contact');

-- 2026-07-24: 组织管理 — bbs_sa_org 增加 is_display_selected 字段
SELECT COUNT(*) INTO @col_display_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_sa_org' AND COLUMN_NAME = 'is_display_selected';
SET @sql_display = IF(@col_display_exists = 0, 'ALTER TABLE `bbs_sa_org` ADD COLUMN `is_display_selected` tinyint(1) DEFAULT 1 COMMENT ''是否显示在用户前台(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_display FROM @sql_display;
EXECUTE stmt_display;
DEALLOCATE PREPARE stmt_display;

-- ============================================
-- 2026-08-25: 组织架构梳理 — 排名分组调整
-- 变更内容：
--   1. "地市支撑机构及原集体企业" 重命名为 "公司所属各单位"
--   2. "内江星原公司" 从独立二级排名组降级为 "公司所属各单位" 下的单位
--   3. 县级供电企业的包含组织新增内江星原公司XX分公司
--
-- 回滚 SQL 见本文件末尾 "rollback" 区域
-- ============================================

-- Step 1: 重命名 "地市支撑机构及原集体企业" → "公司所属各单位"
UPDATE `bbs_sa_org` SET `org_name` = '公司所属各单位' WHERE `org_name` = '地市支撑机构及原集体企业';

-- Step 2: 更新 init 脚本中的组织机构名称（保持 init 与 upgrade 同步）
-- 注意：init-pg.sql / init-mysql.sql 中的 INSERT 使用 ON CONFLICT DO NOTHING /
-- WHERE NOT EXISTS，重命名后旧名称不再匹配，需同步修改 init 文件中的 org_name 值。
-- 此步骤仅更新 upgrade 中可见的名称，init 文件需手动同步。

-- Step 3: 删除项目管理中心节点（org_no=51404011701），合并到建设部（项目管理中心）（org_no=514040117）
UPDATE `bbs_user` SET `org_no` = '514040117' WHERE `org_no` = '51404011701';
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` = '51404011701';

-- ============================================
-- Step 4-7: 组织架构树重组
-- ============================================

-- Step 4: 移动5140404（内江星原公司）从51404到5140403，更新所有后代路径
UPDATE `bbs_sa_org` SET `p_org_no` = '5140403' WHERE `org_no` = '5140404' AND `p_org_no` = '51404';
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|5140404', SUBSTRING(`org_tree`, LENGTH('51404|5140404') + 1))
WHERE `org_tree` LIKE '51404|5140404%';

-- Step 5: 将原内江星原公司下属的独立部门/分公司上移到公司所属各单位（5140403）下
UPDATE `bbs_sa_org`
SET `p_org_no` = '5140403',
    `org_tree` = CONCAT('51404|5140403', SUBSTRING(`org_tree`, LENGTH('5140404') + 1))
WHERE `org_no` IN (
    '514040425','514040401','514040426',
    '514040412','514040413',
    '514040402','514040407','514040406',
    '514040408','514040410',
    '514040404','514040403','514040409',
    '514040411','514040405'
) AND `org_tree` LIKE '5140404|5140404%';
-- 更新有后代的节点的后代路径
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|', `org_tree`)
WHERE (`org_no` LIKE '5140404010%' AND `org_tree` LIKE '514040401|%')
   OR (`org_no` LIKE '5140404250%' AND `org_tree` LIKE '514040425|%')
   OR (`org_no` LIKE '5140404260%' AND `org_tree` LIKE '514040426|%');

-- Step 6: 移动514040422（运检分公司）到变电运检中心（514040304）下
UPDATE `bbs_sa_org`
SET `p_org_no` = '514040304',
    `org_tree` = '51404|5140403|514040304|514040422'
WHERE `org_no` = '514040422' AND `org_tree` LIKE '5140404|514040422%';
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|514040304|514040422', SUBSTRING(`org_tree`, LENGTH('514040422') + 1))
WHERE `org_no` LIKE '5140404220%' AND `org_tree` LIKE '514040422|514040422%';

-- Step 7: 软删内江星原公司（三新）及其后代（已合并到内江三新公司）
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` = '514040424' AND `is_delete` = 0;
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` LIKE '5140404240%' AND `is_delete` = 0;

-- ============================================
-- 2026-08-XX: 积分调整日志表（管理员手动增减用户积分，幂等安全）
-- ============================================
SELECT COUNT(*) INTO @tbl_pl_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log';
SET @sql_pl = IF(@tbl_pl_exists = 0, 'CREATE TABLE `bbs_points_log` (
  `id`               int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`          int(11) NOT NULL COMMENT ''用户ID'',
  `points_change`    int(11) NOT NULL COMMENT ''积分变动（正数加分，负数扣分）'',
  `reason`           varchar(500) DEFAULT NULL COMMENT ''调整原因'',
  `related_type`     varchar(20) DEFAULT NULL COMMENT ''关联类型（article/comment/reply/manual/undo）'',
  `related_id`       int(11) DEFAULT NULL COMMENT ''关联ID'',
  `operator_id`      int(11) DEFAULT NULL COMMENT ''操作人ID'',
  `create_time`      varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  `is_reversed`      tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否已被撤销(0=否,1=是)'',
  `reversed_by`      int(11) DEFAULT NULL COMMENT ''被哪条撤销记录撤销（记录ID）'',
  `reversing_record` int(11) DEFAULT NULL COMMENT ''此记录撤销了哪条原始记录（记录ID）'',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''积分调整日志''', 'SELECT 1');
PREPARE stmt_pl FROM @sql_pl;
EXECUTE stmt_pl;
DEALLOCATE PREPARE stmt_pl;

-- 检查索引是否存在
SELECT COUNT(*) INTO @idx_pl_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND INDEX_NAME = 'idx_points_log_user_id';
SET @idx_pl_sql = IF(@idx_pl_exists = 0, 'ALTER TABLE `bbs_points_log` ADD INDEX `idx_points_log_user_id` (`user_id`)', 'SELECT 1');
PREPARE idx_pl_stmt FROM @idx_pl_sql;
EXECUTE idx_pl_stmt;
DEALLOCATE PREPARE idx_pl_stmt;

-- 积分调整日志支持撤销（对已存在的旧表补列，幂等安全）
SELECT COUNT(*) INTO @col_reversed_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND COLUMN_NAME = 'is_reversed';
SET @sql_reversed = IF(@col_reversed_exists = 0, 'ALTER TABLE `bbs_points_log` ADD COLUMN `is_reversed` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否已被撤销(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_reversed FROM @sql_reversed;
EXECUTE stmt_reversed;
DEALLOCATE PREPARE stmt_reversed;

SELECT COUNT(*) INTO @col_rb_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND COLUMN_NAME = 'reversed_by';
SET @sql_rb = IF(@col_rb_exists = 0, 'ALTER TABLE `bbs_points_log` ADD COLUMN `reversed_by` int(11) DEFAULT NULL COMMENT ''被哪条撤销记录撤销''', 'SELECT 1');
PREPARE stmt_rb FROM @sql_rb;
EXECUTE stmt_rb;
DEALLOCATE PREPARE stmt_rb;

SELECT COUNT(*) INTO @col_rr_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND COLUMN_NAME = 'reversing_record';
SET @sql_rr = IF(@col_rr_exists = 0, 'ALTER TABLE `bbs_points_log` ADD COLUMN `reversing_record` int(11) DEFAULT NULL COMMENT ''此记录撤销了哪条原始记录''', 'SELECT 1');
PREPARE stmt_rr FROM @sql_rr;
EXECUTE stmt_rr;
DEALLOCATE PREPARE stmt_rr;

-- 检查 is_reversed 索引是否存在
SELECT COUNT(*) INTO @idx_pr_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND INDEX_NAME = 'idx_points_log_is_reversed';
SET @idx_pr_sql = IF(@idx_pr_exists = 0, 'ALTER TABLE `bbs_points_log` ADD INDEX `idx_points_log_is_reversed` (`is_reversed`)', 'SELECT 1');
PREPARE idx_pr_stmt FROM @idx_pr_sql;
EXECUTE idx_pr_stmt;
DEALLOCATE PREPARE idx_pr_stmt;

-- ============================================
-- 回滚 SQL（如需撤销上述变更，取消注释执行）
-- ============================================
-- Step 7 回滚
-- UPDATE `bbs_sa_org` SET `is_delete` = 0 WHERE `org_no` LIKE '5140404240%';
-- UPDATE `bbs_sa_org` SET `is_delete` = 0 WHERE `org_no` = '514040424';
-- Step 6 回滚
-- UPDATE `bbs_sa_org` SET `p_org_no` = '5140404', `org_tree` = CONCAT('51404|5140404|', SUBSTRING(`org_tree`, LENGTH('51404|5140403|514040304|') + 1)) WHERE `org_no` = '514040422';
-- Step 3 回滚
-- UPDATE `bbs_sa_org` SET `is_delete` = 0 WHERE `org_no` = '51404011701';
-- Step 1 回滚
-- UPDATE `bbs_sa_org` SET `org_name` = '地市支撑机构及原集体企业' WHERE `org_name` = '公司所属各单位';
-- 注意：Step 4-5 的完整回滚较复杂，建议从备份恢复
