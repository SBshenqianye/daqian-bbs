-- ============================================
-- BBS PostgreSQL 增量升级脚本
-- 用法：psql -U work_flow -d bbs -f upgrade-pg.sql
-- ============================================

-- ============================================
-- 2026-06-30: bbs_user 唯一约束
-- 为 username、personnel_id、id_card 添加 UNIQUE 约束
-- ============================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_bbs_user_username') THEN
    ALTER TABLE bbs_user ADD CONSTRAINT uk_bbs_user_username UNIQUE (username);
    RAISE NOTICE '已添加: uk_bbs_user_username';
  ELSE
    RAISE NOTICE '已存在: uk_bbs_user_username，跳过';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_bbs_user_personnel_id') THEN
    ALTER TABLE bbs_user ADD CONSTRAINT uk_bbs_user_personnel_id UNIQUE (personnel_id);
    RAISE NOTICE '已添加: uk_bbs_user_personnel_id';
  ELSE
    RAISE NOTICE '已存在: uk_bbs_user_personnel_id，跳过';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_bbs_user_id_card') THEN
    ALTER TABLE bbs_user ADD CONSTRAINT uk_bbs_user_id_card UNIQUE (id_card);
    RAISE NOTICE '已添加: uk_bbs_user_id_card';
  ELSE
    RAISE NOTICE '已存在: uk_bbs_user_id_card，跳过';
  END IF;
END $$;
