-- 部署后只读检查（PostgreSQL，不会修改任何数据）
SELECT 'schema_version', COALESCE(MAX(version), '(none)') FROM bbs_schema_version;
SELECT 'has_v039', COUNT(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role';
SELECT 'has_v040', COUNT(1) FROM bbs_schema_version WHERE version = 'v040-audit-log';
SELECT 'audit_table_exists', COUNT(1) FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'bbs_audit_log';
SELECT 'audit_rows', COUNT(1) FROM bbs_audit_log;
SELECT 'total_users', COUNT(1) FROM bbs_user;
SELECT 'type_' || user_type, COUNT(1) FROM bbs_user GROUP BY user_type ORDER BY user_type;
SELECT 'invalid_type', COUNT(1) FROM bbs_user
WHERE user_type IS NULL OR TRIM(user_type) = '' OR user_type NOT IN ('1','2','3','4');
SELECT 'active_moderators', COUNT(1) FROM bbs_board_moderator WHERE status = 1;
SELECT 'admin_super', COUNT(1) FROM bbs_user WHERE user_type IN ('2','3');
SELECT 'candidates_after', COUNT(1) FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1);
SELECT 'candidate_after_user:' || u.id || ':' || u.username FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1)
ORDER BY u.id;
SELECT 'moderator_accounts', COUNT(1) FROM bbs_user WHERE user_type = '4';
SELECT 'moderator_user:' || u.id || ':' || u.username FROM bbs_user u
WHERE u.user_type = '4' ORDER BY u.id;
SELECT 'comment_has_4',
       CASE WHEN COALESCE(col_description('bbs_user'::regclass,
            (SELECT attnum FROM pg_attribute
             WHERE attrelid = 'bbs_user'::regclass AND attname = 'user_type')), '')
            LIKE '%4：版主%' THEN 1 ELSE 0 END;
