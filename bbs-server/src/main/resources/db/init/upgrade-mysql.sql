-- ============================================
-- BBS 数据库升级脚本 - MySQL 版
-- 每个迁移块以 -- @migration: <id> <description> 开头
-- 由 DatabaseInitializer 解析并只执行未记录的迁移
-- ============================================

-- @migration: v001-article-label-icon-desc 标签表增加图标与描述字段
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

-- @migration: v002-emoji-utf8mb4 评论和回复内容支持 Emoji 表情
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

-- @migration: v003-featured-post 精华帖功能（is_featured 字段 + 索引 + 积分配置）
SELECT COUNT(*) INTO @col_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article' AND COLUMN_NAME = 'is_featured';
SET @col_sql = IF(@col_exists = 0, 'ALTER TABLE `bbs_article` ADD COLUMN `is_featured` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否为精华帖(0=否,1=是)''', 'SELECT 1');
PREPARE col_stmt FROM @col_sql;
EXECUTE col_stmt;
DEALLOCATE PREPARE col_stmt;

SELECT COUNT(*) INTO @idx_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article' AND INDEX_NAME = 'idx_article_featured_time';
SET @idx_sql = IF(@idx_exists = 0, 'ALTER TABLE `bbs_article` ADD INDEX `idx_article_featured_time` (`is_featured`, `create_time`)', 'SELECT 1');
PREPARE idx_stmt FROM @idx_sql;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;

INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `remark`)
SELECT 'featured', '10', '精华帖积分', 2, '被设为精华帖额外获得的积分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'featured');

-- @migration: v004-system-config 系统配置表 bbs_system_config
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

-- @migration: v005-feedback-contact 使用反馈联系方式初始配置
INSERT INTO `bbs_system_config` (`config_key`, `config_value`, `config_label`, `config_group`, `config_type`, `sort_order`, `remark`, `create_by`, `create_time`)
SELECT 'feedback_contact', '{\"name\":\"\",\"email\":\"\"}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', '2026-07-15 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_system_config` WHERE `config_key` = 'feedback_contact');

-- @migration: v006-display-selected 组织管理 bbs_sa_org 增加 is_display_selected 字段
SELECT COUNT(*) INTO @col_display_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_sa_org' AND COLUMN_NAME = 'is_display_selected';
SET @sql_display = IF(@col_display_exists = 0, 'ALTER TABLE `bbs_sa_org` ADD COLUMN `is_display_selected` tinyint(1) DEFAULT 1 COMMENT ''是否显示在用户前台(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_display FROM @sql_display;
EXECUTE stmt_display;
DEALLOCATE PREPARE stmt_display;

-- @migration: v007-org-restructure 组织架构梳理（重命名+删除+树重组）
-- Step 1: 重命名
UPDATE `bbs_sa_org` SET `org_name` = '公司所属各单位' WHERE `org_name` = '地市支撑机构及原集体企业';

-- Step 3: 删除项目管理中心节点
UPDATE `bbs_user` SET `org_no` = '514040117' WHERE `org_no` = '51404011701';
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` = '51404011701';

-- Step 4: 移动5140404从51404到5140403
UPDATE `bbs_sa_org` SET `p_org_no` = '5140403' WHERE `org_no` = '5140404' AND `p_org_no` = '51404';
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|5140404', SUBSTRING(`org_tree`, LENGTH('51404|5140404') + 1))
WHERE `org_tree` LIKE '51404|5140404%';

-- Step 5: 独立部门/分公司上移到公司所属各单位
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
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|', `org_tree`)
WHERE (`org_no` LIKE '5140404010%' AND `org_tree` LIKE '514040401|%')
   OR (`org_no` LIKE '5140404250%' AND `org_tree` LIKE '514040425|%')
   OR (`org_no` LIKE '5140404260%' AND `org_tree` LIKE '514040426|%');

-- Step 6: 运检分公司移到变电运检中心
UPDATE `bbs_sa_org`
SET `p_org_no` = '514040304',
    `org_tree` = '51404|5140403|514040304|514040422'
WHERE `org_no` = '514040422' AND `org_tree` LIKE '5140404|514040422%';
UPDATE `bbs_sa_org`
SET `org_tree` = CONCAT('51404|5140403|514040304|514040422', SUBSTRING(`org_tree`, LENGTH('514040422') + 1))
WHERE `org_no` LIKE '5140404220%' AND `org_tree` LIKE '514040422|514040422%';

-- Step 7: 软删内江星原公司（三新）及迁移用户
UPDATE `bbs_user` SET `org_no` = '514040303' WHERE `org_no` = '514040424';
UPDATE `bbs_user` SET `org_no` = '514040303' WHERE `org_no` IN ('51404042401','51404042402','51404042403','51404042404','51404042405');
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` = '514040424' AND `is_delete` = 0;
UPDATE `bbs_sa_org` SET `is_delete` = 1 WHERE `org_no` LIKE '5140404240%' AND `is_delete` = 0;

-- Step 8: 县公司星原分公司移到对应国网XX供电分公司
UPDATE `bbs_sa_org` SET `p_org_no` = '514040204', `org_tree` = CONCAT('514040204|', `org_no`) WHERE `org_no` = '514040423' AND `p_org_no` = '5140404';
UPDATE `bbs_sa_org` SET `org_tree` = CONCAT('514040204|514040423', SUBSTRING(`org_tree`, LENGTH('514040423') + 1)) WHERE `org_no` LIKE '5140404230%' AND `org_tree` LIKE '514040423|514040423%';
UPDATE `bbs_sa_org` SET `p_org_no` = '514040201', `org_tree` = CONCAT('514040201|', `org_no`) WHERE `org_no` = '514040414' AND `p_org_no` = '5140404';
UPDATE `bbs_sa_org` SET `org_tree` = CONCAT('514040201|514040414', SUBSTRING(`org_tree`, LENGTH('514040414') + 1)) WHERE `org_no` LIKE '5140404140%' AND `org_tree` LIKE '514040414|514040414%';
UPDATE `bbs_sa_org` SET `p_org_no` = '514040202', `org_tree` = CONCAT('514040202|', `org_no`) WHERE `org_no` = '514040415' AND `p_org_no` = '5140404';
UPDATE `bbs_sa_org` SET `org_tree` = CONCAT('514040202|514040415', SUBSTRING(`org_tree`, LENGTH('514040415') + 1)) WHERE `org_no` LIKE '5140404150%' AND `org_tree` LIKE '514040415|514040415%';
UPDATE `bbs_sa_org` SET `p_org_no` = '514040203', `org_tree` = CONCAT('514040203|', `org_no`) WHERE `org_no` = '514040421' AND `p_org_no` = '5140404';
UPDATE `bbs_sa_org` SET `org_tree` = CONCAT('514040203|514040421', SUBSTRING(`org_tree`, LENGTH('514040421') + 1)) WHERE `org_no` LIKE '5140404210%' AND `org_tree` LIKE '514040421|514040421%';
UPDATE `bbs_sa_org` SET `p_org_no` = '514040205', `org_tree` = CONCAT('514040205|', `org_no`) WHERE `org_no` = '514040417' AND `p_org_no` = '5140404';
UPDATE `bbs_sa_org` SET `org_tree` = CONCAT('514040205|514040417', SUBSTRING(`org_tree`, LENGTH('514040417') + 1)) WHERE `org_no` LIKE '5140404170%' AND `org_tree` LIKE '514040417|514040417%';

-- @migration: v008-points-log 积分调整日志表
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

SELECT COUNT(*) INTO @idx_pl_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND INDEX_NAME = 'idx_points_log_user_id';
SET @idx_pl_sql = IF(@idx_pl_exists = 0, 'ALTER TABLE `bbs_points_log` ADD INDEX `idx_points_log_user_id` (`user_id`)', 'SELECT 1');
PREPARE idx_pl_stmt FROM @idx_pl_sql;
EXECUTE idx_pl_stmt;
DEALLOCATE PREPARE idx_pl_stmt;

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

SELECT COUNT(*) INTO @idx_pr_exists FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_points_log' AND INDEX_NAME = 'idx_points_log_is_reversed';
SET @idx_pr_sql = IF(@idx_pr_exists = 0, 'ALTER TABLE `bbs_points_log` ADD INDEX `idx_points_log_is_reversed` (`is_reversed`)', 'SELECT 1');
PREPARE idx_pr_stmt FROM @idx_pr_sql;
EXECUTE idx_pr_stmt;
DEALLOCATE PREPARE idx_pr_stmt;

-- @migration: v009-sensitive-words 扩展垃圾/灌水关键词库
INSERT INTO `bbs_sensitive_word` (`keyword`) VALUES
('哈哈哈'),('嘻嘻嘻'),('嘿嘿嘿'),('啊啊啊'),('嗯嗯嗯'),('哦哦哦'),('呵呵呵'),
('啦啦啦'),('呜呜呜'),
('沙发'),('占位'),('占楼'),('路过'),('马克'),('mark'),('mark一下'),
('顶贴'),('灌水'),('水水水'),('水帖'),
('来了'),('看看'),('打卡'),('签到'),
('666666'),('8888'),('11111'),('123456'),
('测试测试'),('测试一下'),('testtest')
ON DUPLICATE KEY UPDATE `keyword` = `keyword`;

-- @migration: v010-notification 通知表（回复提醒、未读计数）
SELECT COUNT(*) INTO @tbl_notif_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_notification';
SET @sql_notif = IF(@tbl_notif_exists = 0, 'CREATE TABLE `bbs_notification` (
  `id`             int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`        int(11) NOT NULL COMMENT ''被通知的用户ID'',
  `from_user_id`   int(11) DEFAULT NULL COMMENT ''触发通知的用户ID'',
  `type`           varchar(20) NOT NULL COMMENT ''通知类型(reply/comment/favorite)'',
  `title`          varchar(255) DEFAULT NULL COMMENT ''通知标题'',
  `related_type`   varchar(20) DEFAULT NULL COMMENT ''关联类型(article/comment/reply)'',
  `related_id`     int(11) DEFAULT NULL COMMENT ''关联ID'',
  `is_read`        tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否已读(0=未读,1=已读)'',
  `create_time`    varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_user_id` (`user_id`),
  INDEX `idx_notification_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''通知表''', 'SELECT 1');
PREPARE stmt_notif FROM @sql_notif;
EXECUTE stmt_notif;
DEALLOCATE PREPARE stmt_notif;

-- @migration: v011-lingdao-display 领导干部默认不展示排名
UPDATE `bbs_sa_org` SET `is_display_selected` = 0
  WHERE `org_name` = '领导干部' AND `is_delete` = 0 AND `is_display_selected` = 1;

-- @migration: v012-ops-v2 运营方案V2功能（登录积分/热度/采纳/举报/违规/等级/版主/限制/申诉）

-- === 1. 新增表 ===

-- bbs_login_log — 每日登录浏览记录
SELECT COUNT(*) INTO @tbl_ll_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_login_log';
SET @sql_ll = IF(@tbl_ll_exists = 0, 'CREATE TABLE `bbs_login_log` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`         int(11) NOT NULL COMMENT ''用户ID'',
  `login_date`      varchar(10) NOT NULL COMMENT ''登录日期(YYYY-MM-DD)'',
  `login_time`      varchar(20) DEFAULT NULL COMMENT ''登录时间'',
  `browse_minutes`  int(11) DEFAULT 0 COMMENT ''有效浏览分钟数'',
  `points_awarded`  tinyint(1) DEFAULT 0 COMMENT ''是否已发积分(0=否,1=是)'',
  `create_time`     varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_login_log_user_date` (`user_id`, `login_date`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''每日登录浏览记录''', 'SELECT 1');
PREPARE stmt_ll FROM @sql_ll;
EXECUTE stmt_ll;
DEALLOCATE PREPARE stmt_ll;

-- bbs_report — 实名举报记录
SELECT COUNT(*) INTO @tbl_rpt_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_report';
SET @sql_rpt = IF(@tbl_rpt_exists = 0, 'CREATE TABLE `bbs_report` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `reporter_id`     int(11) NOT NULL COMMENT ''举报人ID'',
  `target_type`     varchar(20) NOT NULL COMMENT ''举报目标类型(article/comment/reply)'',
  `target_id`       int(11) NOT NULL COMMENT ''被举报内容ID'',
  `reason`          varchar(500) DEFAULT NULL COMMENT ''举报原因'',
  `status`          varchar(20) DEFAULT ''pending'' COMMENT ''状态(pending/confirmed/rejected)'',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT ''审核人ID'',
  `review_time`     varchar(20) DEFAULT NULL COMMENT ''审核时间'',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT ''审核备注'',
  `points_awarded`  tinyint(1) DEFAULT 0 COMMENT ''是否已给举报人发积分(0=否,1=是)'',
  `create_time`     varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_reporter` (`reporter_id`),
  INDEX `idx_report_target` (`target_type`, `target_id`),
  INDEX `idx_report_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''实名举报记录''', 'SELECT 1');
PREPARE stmt_rpt FROM @sql_rpt;
EXECUTE stmt_rpt;
DEALLOCATE PREPARE stmt_rpt;

-- bbs_violation — 违规记录
SELECT COUNT(*) INTO @tbl_vio_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_violation';
SET @sql_vio = IF(@tbl_vio_exists = 0, 'CREATE TABLE `bbs_violation` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`         int(11) NOT NULL COMMENT ''违规用户ID'',
  `violation_type`  varchar(50) NOT NULL COMMENT ''违规类型(对应字典violation)'',
  `points_deducted` int(11) NOT NULL COMMENT ''扣减积分'',
  `related_type`    varchar(20) DEFAULT NULL COMMENT ''关联类型(article/comment/reply)'',
  `related_id`      int(11) DEFAULT NULL COMMENT ''关联ID'',
  `operator_id`     int(11) NOT NULL COMMENT ''操作管理员ID'',
  `remark`          varchar(500) DEFAULT NULL COMMENT ''备注说明'',
  `create_time`     varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_violation_user` (`user_id`),
  INDEX `idx_violation_type` (`violation_type`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''违规记录''', 'SELECT 1');
PREPARE stmt_vio FROM @sql_vio;
EXECUTE stmt_vio;
DEALLOCATE PREPARE stmt_vio;

-- bbs_appeal — 申诉记录
SELECT COUNT(*) INTO @tbl_apl_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_appeal';
SET @sql_apl = IF(@tbl_apl_exists = 0, 'CREATE TABLE `bbs_appeal` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`         int(11) NOT NULL COMMENT ''申诉人ID'',
  `appeal_type`     varchar(20) NOT NULL COMMENT ''申诉类型(violation/points/other)'',
  `related_id`      int(11) DEFAULT NULL COMMENT ''关联的违规/积分记录ID'',
  `content`         text NOT NULL COMMENT ''申诉内容'',
  `status`          varchar(20) DEFAULT ''pending'' COMMENT ''状态(pending/accepted/rejected)'',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT ''审核人ID'',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT ''审核备注'',
  `review_time`     varchar(20) DEFAULT NULL COMMENT ''审核时间'',
  `create_time`     varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appeal_user` (`user_id`),
  INDEX `idx_appeal_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''申诉记录''', 'SELECT 1');
PREPARE stmt_apl FROM @sql_apl;
EXECUTE stmt_apl;
DEALLOCATE PREPARE stmt_apl;

-- bbs_board_moderator — 版块管理员
SELECT COUNT(*) INTO @tbl_bm_exists FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_board_moderator';
SET @sql_bm = IF(@tbl_bm_exists = 0, 'CREATE TABLE `bbs_board_moderator` (
  `id`           int(11) NOT NULL AUTO_INCREMENT COMMENT ''主键ID'',
  `user_id`      int(11) NOT NULL COMMENT ''用户ID'',
  `label_id`     int(11) NOT NULL COMMENT ''关联标签(版块)ID'',
  `role_type`    varchar(20) DEFAULT ''moderator'' COMMENT ''角色类型(moderator/admin)'',
  `status`       tinyint(1) DEFAULT 1 COMMENT ''状态(1=有效,0=撤销)'',
  `appoint_time` varchar(20) DEFAULT NULL COMMENT ''任命时间'',
  `create_time`  varchar(20) DEFAULT NULL COMMENT ''创建时间'',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_board_mod_user_label` (`user_id`, `label_id`),
  INDEX `idx_board_mod_label` (`label_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT=''版块管理员''', 'SELECT 1');
PREPARE stmt_bm FROM @sql_bm;
EXECUTE stmt_bm;
DEALLOCATE PREPARE stmt_bm;

-- === 2. 修改现有表 ===

-- bbs_reply 增加 is_adopted 字段
SELECT COUNT(*) INTO @col_adopted_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_reply' AND COLUMN_NAME = 'is_adopted';
SET @sql_adopted = IF(@col_adopted_exists = 0, 'ALTER TABLE `bbs_reply` ADD COLUMN `is_adopted` tinyint(1) DEFAULT 0 COMMENT ''是否被采纳(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_adopted FROM @sql_adopted;
EXECUTE stmt_adopted;
DEALLOCATE PREPARE stmt_adopted;

-- bbs_user 增加发帖限制字段
SELECT COUNT(*) INTO @col_pr_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_user' AND COLUMN_NAME = 'post_restricted';
SET @sql_pr = IF(@col_pr_exists = 0, 'ALTER TABLE `bbs_user` ADD COLUMN `post_restricted` tinyint(1) DEFAULT 0 COMMENT ''是否限制发帖(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_pr FROM @sql_pr;
EXECUTE stmt_pr;
DEALLOCATE PREPARE stmt_pr;

SELECT COUNT(*) INTO @col_pru_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_user' AND COLUMN_NAME = 'post_restricted_until';
SET @sql_pru = IF(@col_pru_exists = 0, 'ALTER TABLE `bbs_user` ADD COLUMN `post_restricted_until` varchar(20) DEFAULT NULL COMMENT ''限制发帖截止时间''', 'SELECT 1');
PREPARE stmt_pru FROM @sql_pru;
EXECUTE stmt_pru;
DEALLOCATE PREPARE stmt_pru;

-- bbs_article 增加热度奖励标记字段
SELECT COUNT(*) INTO @col_hb_exists FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_article' AND COLUMN_NAME = 'is_hot_bonus';
SET @sql_hb = IF(@col_hb_exists = 0, 'ALTER TABLE `bbs_article` ADD COLUMN `is_hot_bonus` tinyint(1) DEFAULT 0 COMMENT ''热度奖励是否已发放(0=否,1=是)''', 'SELECT 1');
PREPARE stmt_hb FROM @sql_hb;
EXECUTE stmt_hb;
DEALLOCATE PREPARE stmt_hb;

-- === 3. 数据字典新增 ===

-- 违规类型字典
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'illegal', '违法违规内容', 1, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣15分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'illegal');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'attack', '人身攻击/争吵引战', 2, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣10分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'attack');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'spam', '恶意灌水/刷屏', 3, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣4分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'spam');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'plagiarism', '抄袭剽窃', 4, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣12分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'plagiarism');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'false_report', '虚假恶意举报', 5, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣3分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'false_report');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'violation', 'leak', '泄露企业秘密', 6, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '扣20分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` = 'leak');

-- 帖子热度阈值
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'hot_threshold', '10', '帖子热度回复阈值', 10, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '回复数超过此值触发热度奖励'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'hot_threshold');

-- 每日登录浏览阈值（分钟）
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`)
SELECT 'login_browse_minutes', '10', '每日登录有效浏览分钟数', 11, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '登录后需浏览满此分钟数才计分'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_type` = 'login_browse_minutes');

-- ============================================
-- 回滚 SQL（如需撤销上述变更，取消注释执行）
-- ============================================
-- DROP TABLE IF EXISTS `bbs_board_moderator`;
-- DROP TABLE IF EXISTS `bbs_appeal`;
-- DROP TABLE IF EXISTS `bbs_violation`;
-- DROP TABLE IF EXISTS `bbs_report`;
-- DROP TABLE IF EXISTS `bbs_login_log`;
-- ALTER TABLE `bbs_article` DROP COLUMN `is_hot_bonus`;
-- ALTER TABLE `bbs_user` DROP COLUMN `post_restricted_until`;
-- ALTER TABLE `bbs_user` DROP COLUMN `post_restricted`;
-- ALTER TABLE `bbs_reply` DROP COLUMN `is_adopted`;
-- DELETE FROM `bbs_dict` WHERE `dict_type` IN ('violation', 'hot_threshold', 'login_browse_minutes');

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
