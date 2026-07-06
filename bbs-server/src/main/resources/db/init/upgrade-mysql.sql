-- ============================================
-- BBS 数据库升级脚本 - MySQL 版
-- 可重复执行：错误会被 catch 并记录为 warning
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN `icon` varchar(50) DEFAULT NULL COMMENT '标签图标';
ALTER TABLE bbs_article_label ADD COLUMN `description` varchar(200) DEFAULT NULL COMMENT '标签描述';
