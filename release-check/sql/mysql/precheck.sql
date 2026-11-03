-- 部署前只读检查（不会修改任何数据）
-- 输出格式：key<TAB>value，供 check.ps1 解析
SELECT 'schema_version', COALESCE(MAX(version), '(none)') FROM bbs_schema_version;
SELECT 'has_v039', COUNT(1) FROM bbs_schema_version WHERE version = 'v039-moderator-role';
SELECT 'has_v040', COUNT(1) FROM bbs_schema_version WHERE version = 'v040-audit-log';
SELECT 'has_v041', COUNT(1) FROM bbs_schema_version WHERE version = 'v041-permission-group';
SELECT 'has_v042', COUNT(1) FROM bbs_schema_version WHERE version = 'v042-permission-group-base-role';
SELECT 'has_v043', COUNT(1) FROM bbs_schema_version WHERE version = 'v043-user-primary-group';
SELECT 'primary_group_column_exists', COUNT(1) FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_user' AND COLUMN_NAME = 'primary_group_id';
SELECT 'audit_table_exists', COUNT(1) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_audit_log';
SELECT 'permission_group_table_exists', COUNT(1) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_permission_group';
SELECT 'total_users', COUNT(1) FROM bbs_user;
SELECT CONCAT('type_', user_type), COUNT(1) FROM bbs_user GROUP BY user_type ORDER BY user_type;
SELECT 'invalid_type', COUNT(1) FROM bbs_user
WHERE user_type IS NULL OR TRIM(user_type) = '' OR user_type NOT IN ('1','2','3','4');
SELECT 'active_moderators', COUNT(1) FROM bbs_board_moderator WHERE status = 1;
SELECT 'admin_super', COUNT(1) FROM bbs_user WHERE user_type IN ('2','3');
SELECT 'candidates', COUNT(1) FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1);
SELECT CONCAT('candidate_user:', u.id, ':', u.username) FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1)
ORDER BY u.id;
SELECT 'comment_has_4',
       CASE WHEN COLUMN_COMMENT LIKE '%4：版主%' THEN 1 ELSE 0 END
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_user' AND COLUMN_NAME = 'user_type';
