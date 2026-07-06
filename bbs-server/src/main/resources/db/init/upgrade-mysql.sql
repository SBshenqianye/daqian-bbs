-- ============================================
-- BBS 数据库升级脚本 - MySQL 版
-- 可重复执行：错误会被 catch 并记录为 warning
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN `icon` varchar(50) DEFAULT NULL COMMENT '标签图标';
ALTER TABLE bbs_article_label ADD COLUMN `description` varchar(200) DEFAULT NULL COMMENT '标签描述';

-- 2026-07-06: 支持评论和回复内容中的 Emoji 表情（utf8mb4 支持 4 字节 UTF-8）
ALTER TABLE bbs_comment MODIFY COLUMN `comment_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论的内容';
ALTER TABLE bbs_reply MODIFY COLUMN `reply_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回复的具体内容';
