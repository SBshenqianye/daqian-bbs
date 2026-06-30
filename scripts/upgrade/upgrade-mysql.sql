-- ============================================
-- BBS MySQL 增量升级脚本
-- 用法：mysql -u root -p bbs_db < upgrade-mysql.sql
-- ============================================

-- ============================================
-- 2026-06-30: bbs_user 唯一约束
-- 为 username、personnel_id、id_card 添加 UNIQUE 约束
-- 使用 IF NOT EXISTS 跳过已存在的约束，可重复执行
-- ============================================
ALTER TABLE bbs_user
    ADD CONSTRAINT uk_bbs_user_username UNIQUE (username),
    ADD CONSTRAINT uk_bbs_user_personnel_id UNIQUE (personnel_id),
    ADD CONSTRAINT uk_bbs_user_id_card UNIQUE (id_card);
