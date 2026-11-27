-- 部署前只读检查（PostgreSQL，不会修改任何数据）
-- 输出 key<TAB>value，value 尽量为数字/ASCII，避免客户端编码差异
SELECT 'schema_version', COALESCE(MAX(version), '(none)') FROM bbs_schema_version;
SELECT 'has_v039', COUNT(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role';
SELECT 'has_v040', COUNT(1) FROM bbs_schema_version WHERE version = 'v040-audit-log';
SELECT 'has_v041', COUNT(1) FROM bbs_schema_version WHERE version = 'v041-permission-group';
SELECT 'has_v042', COUNT(1) FROM bbs_schema_version WHERE version = 'v042-permission-group-base-role';
SELECT 'has_v043', COUNT(1) FROM bbs_schema_version WHERE version = 'v043-user-primary-group';
SELECT 'primary_group_column_exists', COUNT(1) FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'bbs_user' AND column_name = 'primary_group_id';
SELECT 'audit_table_exists', COUNT(1) FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'bbs_audit_log';
SELECT 'permission_group_table_exists', COUNT(1) FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'bbs_permission_group';
SELECT 'total_users', COUNT(1) FROM bbs_user;
SELECT 'type_' || user_type, COUNT(1) FROM bbs_user GROUP BY user_type ORDER BY user_type;
SELECT 'invalid_type', COUNT(1) FROM bbs_user
WHERE user_type IS NULL OR TRIM(user_type) = '' OR user_type NOT IN ('1','2','3','4');
SELECT 'active_moderators', COUNT(1) FROM bbs_board_moderator WHERE status = 1;
SELECT 'admin_super', COUNT(1) FROM bbs_user WHERE user_type IN ('2','3');
SELECT 'candidates', COUNT(1) FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1);
SELECT 'candidate_user:' || u.id || ':' || u.username FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1)
ORDER BY u.id;
SELECT 'comment_has_4',
       CASE WHEN COALESCE(col_description('bbs_user'::regclass,
            (SELECT attnum FROM pg_attribute
             WHERE attrelid = 'bbs_user'::regclass AND attname = 'user_type')), '')
            LIKE '%4：版主%' THEN 1 ELSE 0 END;
