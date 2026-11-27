-- v040 回滚（PostgreSQL）：删除审计日志表并移除迁移记录
-- 警告：会丢失全部审计日志；执行前请确认已备份。
DROP TABLE IF EXISTS bbs_audit_log;
DELETE FROM bbs_schema_version WHERE version = 'v040-audit-log';
