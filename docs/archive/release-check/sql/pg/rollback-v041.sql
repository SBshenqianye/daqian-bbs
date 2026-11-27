-- v041 回滚（PostgreSQL）：删除权限组相关表并移除迁移记录
-- 警告：会丢失权限组、用户绑定和预设；执行前请确认已备份。
DROP TABLE IF EXISTS bbs_permission_preset;
DROP TABLE IF EXISTS bbs_user_permission_group;
DROP TABLE IF EXISTS bbs_permission_group_item;
DROP TABLE IF EXISTS bbs_permission_group;
DELETE FROM bbs_schema_version WHERE version = 'v041-permission-group';
