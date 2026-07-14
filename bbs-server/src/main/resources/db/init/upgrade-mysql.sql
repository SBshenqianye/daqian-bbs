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
