-- ============================================
-- BBS 数据库升级脚本 - PostgreSQL 版
-- 可重复执行：使用 IF NOT EXISTS 保证幂等
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS icon varchar(50);
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS description varchar(200);
