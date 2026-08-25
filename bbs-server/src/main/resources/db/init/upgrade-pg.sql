-- ============================================
-- BBS 数据库升级脚本 - PostgreSQL 版
-- 可重复执行：使用 IF NOT EXISTS 保证幂等
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS icon varchar(50);
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS description varchar(200);

-- 2026-07-13: 精华帖功能 — 文章表增加 is_featured 字段
ALTER TABLE bbs_article ADD COLUMN IF NOT EXISTS is_featured smallint NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_article_featured_time ON bbs_article (is_featured, create_time);

-- 2026-07-13: 精华帖积分配置（存在则跳过）
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'featured', '10', '精华帖积分', 2, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '被设为精华帖额外获得的积分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'featured');

-- 2026-07-15: 新增系统配置表
CREATE TABLE IF NOT EXISTS bbs_system_config (
    id           SERIAL PRIMARY KEY,
    config_key   varchar(100) NOT NULL,
    config_value text,
    config_label varchar(255),
    config_group varchar(100) DEFAULT 'default',
    config_type  varchar(20) DEFAULT 'text',
    sort_order   integer DEFAULT 0,
    remark       varchar(500),
    create_by    varchar(50),
    create_time  varchar(20),
    update_by    varchar(50),
    update_time  varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key ON bbs_system_config (config_key);

-- 2026-07-15: 使用反馈联系方式初始配置
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'feedback_contact', '{"name":"","email":""}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'feedback_contact');

-- 2026-07-24: 组织管理 — bbs_sa_org 增加 is_display_selected 字段
ALTER TABLE bbs_sa_org ADD COLUMN IF NOT EXISTS is_display_selected smallint DEFAULT 1;
