-- ============================================
-- BBS 数据库升级脚本 - PostgreSQL 版
-- 每个迁移块以 -- @migration: <id> <description> 开头
-- 由 DatabaseInitializer 解析并只执行未记录的迁移
-- ============================================

-- @migration: v001-article-label-icon-desc 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS icon varchar(50);
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS description varchar(200);

-- @migration: v002-emoji-utf8mb4 评论和回复内容支持 Emoji（PG 原生支持，无需变更）
-- 此版本在 PG 中为空操作，保留标记以保持 MySQL/PG 版本号一致

-- @migration: v003-featured-post 精华帖功能（is_featured 字段 + 索引 + 积分配置）
ALTER TABLE bbs_article ADD COLUMN IF NOT EXISTS is_featured smallint NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_article_featured_time ON bbs_article (is_featured, create_time);

INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'featured', '10', '精华帖积分', 2, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '被设为精华帖额外获得的积分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'featured');

-- @migration: v004-system-config 系统配置表 bbs_system_config
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

-- @migration: v005-feedback-contact 使用反馈联系方式初始配置
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'feedback_contact', '{"name":"","email":""}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'feedback_contact');

-- @migration: v006-display-selected 组织管理 bbs_sa_org 增加 is_display_selected 字段
ALTER TABLE bbs_sa_org ADD COLUMN IF NOT EXISTS is_display_selected smallint DEFAULT 1;

-- @migration: v007-org-restructure 组织架构梳理（重命名+删除+树重组）
-- Step 1: 重命名
UPDATE bbs_sa_org SET org_name = '公司所属各单位' WHERE org_name = '地市支撑机构及原集体企业';

-- Step 3: 删除项目管理中心节点
UPDATE bbs_user SET org_no = '514040117' WHERE org_no = '51404011701';
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no = '51404011701';

-- Step 4: 移动5140404从51404到5140403
UPDATE bbs_sa_org SET p_org_no = '5140403' WHERE org_no = '5140404' AND p_org_no = '51404';
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|5140404' || SUBSTRING(org_tree, LENGTH('51404|5140404') + 1)
WHERE org_tree LIKE '51404|5140404%';

-- Step 5: 独立部门/分公司上移到公司所属各单位
UPDATE bbs_sa_org
SET p_org_no = '5140403',
    org_tree = '51404|5140403' || SUBSTRING(org_tree, LENGTH('5140404') + 1)
WHERE org_no IN (
    '514040425','514040401','514040426',
    '514040412','514040413',
    '514040402','514040407','514040406',
    '514040408','514040410',
    '514040404','514040403','514040409',
    '514040411','514040405'
) AND org_tree LIKE '5140404|5140404%';
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|' || org_tree
WHERE (org_no LIKE '5140404010%' AND org_tree LIKE '514040401|%')
   OR (org_no LIKE '5140404250%' AND org_tree LIKE '514040425|%')
   OR (org_no LIKE '5140404260%' AND org_tree LIKE '514040426|%');

-- Step 6: 运检分公司移到变电运检中心
UPDATE bbs_sa_org
SET p_org_no = '514040304',
    org_tree = '51404|5140403|514040304|514040422'
WHERE org_no = '514040422' AND org_tree LIKE '5140404|514040422%';
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|514040304|514040422' || SUBSTRING(org_tree, LENGTH('514040422') + 1)
WHERE org_no LIKE '5140404220%' AND org_tree LIKE '514040422|514040422%';

-- Step 7: 软删内江星原公司（三新）及迁移用户
UPDATE bbs_user SET org_no = '514040303' WHERE org_no = '514040424';
UPDATE bbs_user SET org_no = '514040303' WHERE org_no IN ('51404042401','51404042402','51404042403','51404042404','51404042405');
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no = '514040424' AND is_delete = 0;
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no LIKE '5140404240%' AND is_delete = 0;

-- Step 8: 县公司星原分公司移到对应国网XX供电分公司
UPDATE bbs_sa_org SET p_org_no = '514040204', org_tree = '514040204|514040423' WHERE org_no = '514040423' AND p_org_no = '5140404';
UPDATE bbs_sa_org SET org_tree = '514040204|514040423' || SUBSTRING(org_tree, LENGTH('514040423') + 1) WHERE org_no LIKE '5140404230%' AND org_tree LIKE '514040423|514040423%';
UPDATE bbs_sa_org SET p_org_no = '514040201', org_tree = '514040201|514040414' WHERE org_no = '514040414' AND p_org_no = '5140404';
UPDATE bbs_sa_org SET org_tree = '514040201|514040414' || SUBSTRING(org_tree, LENGTH('514040414') + 1) WHERE org_no LIKE '5140404140%' AND org_tree LIKE '514040414|514040414%';
UPDATE bbs_sa_org SET p_org_no = '514040202', org_tree = '514040202|514040415' WHERE org_no = '514040415' AND p_org_no = '5140404';
UPDATE bbs_sa_org SET org_tree = '514040202|514040415' || SUBSTRING(org_tree, LENGTH('514040415') + 1) WHERE org_no LIKE '5140404150%' AND org_tree LIKE '514040415|514040415%';
UPDATE bbs_sa_org SET p_org_no = '514040203', org_tree = '514040203|514040421' WHERE org_no = '514040421' AND p_org_no = '5140404';
UPDATE bbs_sa_org SET org_tree = '514040203|514040421' || SUBSTRING(org_tree, LENGTH('514040421') + 1) WHERE org_no LIKE '5140404210%' AND org_tree LIKE '514040421|514040421%';
UPDATE bbs_sa_org SET p_org_no = '514040205', org_tree = '514040205|514040417' WHERE org_no = '514040417' AND p_org_no = '5140404';
UPDATE bbs_sa_org SET org_tree = '514040205|514040417' || SUBSTRING(org_tree, LENGTH('514040417') + 1) WHERE org_no LIKE '5140404170%' AND org_tree LIKE '514040417|514040417%';

-- @migration: v008-points-log 积分调整日志表
CREATE TABLE IF NOT EXISTS bbs_points_log (
    id               SERIAL PRIMARY KEY,
    user_id          integer NOT NULL,
    points_change    integer NOT NULL,
    reason           varchar(500),
    related_type     varchar(20),
    related_id       integer,
    operator_id      integer,
    create_time      varchar(20),
    is_reversed      smallint NOT NULL DEFAULT 0,
    reversed_by      integer,
    reversing_record integer
);
CREATE INDEX IF NOT EXISTS idx_points_log_user_id ON bbs_points_log (user_id);
CREATE INDEX IF NOT EXISTS idx_points_log_is_reversed ON bbs_points_log (is_reversed);

ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS is_reversed smallint NOT NULL DEFAULT 0;
ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS reversed_by integer;
ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS reversing_record integer;

-- @migration: v009-sensitive-words 扩展垃圾/灌水关键词库
INSERT INTO bbs_sensitive_word (keyword) VALUES
('哈哈哈'),('嘻嘻嘻'),('嘿嘿嘿'),('啊啊啊'),('嗯嗯嗯'),('哦哦哦'),('呵呵呵'),
('啦啦啦'),('呜呜呜'),
('沙发'),('占位'),('占楼'),('路过'),('马克'),('mark'),('mark一下'),
('顶贴'),('灌水'),('水水水'),('水帖'),
('来了'),('看看'),('打卡'),('签到'),
('666666'),('8888'),('11111'),('123456'),
('测试测试'),('测试一下'),('testtest')
ON CONFLICT DO NOTHING;

-- @migration: v010-notification 通知表（回复提醒、未读计数）
CREATE TABLE IF NOT EXISTS bbs_notification (
    id             SERIAL PRIMARY KEY,
    user_id        integer NOT NULL,
    from_user_id   integer,
    type           varchar(20) NOT NULL,
    title          varchar(255),
    related_type   varchar(20),
    related_id     integer,
    is_read        smallint NOT NULL DEFAULT 0,
    create_time    varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON bbs_notification (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON bbs_notification (user_id, is_read);

-- @migration: v011-lingdao-display 领导干部默认不展示排名
UPDATE bbs_sa_org SET is_display_selected = 0
  WHERE org_name = '领导干部' AND is_delete = 0 AND is_display_selected = 1;

-- ============================================
-- 回滚 SQL（如需撤销上述变更，取消注释执行）
-- ============================================
-- Step 7 回滚
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no LIKE '514040424%';
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no = '514040424';
-- Step 6 回滚
-- UPDATE bbs_sa_org SET p_org_no = '5140404', org_tree = '51404|5140404|514040422' WHERE org_no = '514040422';
-- UPDATE bbs_sa_org SET org_tree = '51404|5140404|514040422' || SUBSTRING(org_tree, LENGTH('51404|5140403|514040304|514040422') + 1) WHERE org_tree LIKE '51404|5140403|514040304|514040422%';
-- Step 5 回滚: 需逐个恢复
-- Step 4 回滚
-- UPDATE bbs_sa_org SET org_tree = '51404|5140404' || SUBSTRING(org_tree, LENGTH('51404|5140403|5140404') + 1) WHERE org_tree LIKE '51404|5140403|5140404%';
-- UPDATE bbs_sa_org SET p_org_no = '51404' WHERE org_no = '5140404';
-- Step 3 回滚
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no = '51404011701';
-- Step 1 回滚
-- UPDATE bbs_sa_org SET org_name = '地市支撑机构及原集体企业' WHERE org_name = '公司所属各单位';
