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

-- @migration: v012-ops-v2 运营方案V2功能（登录积分/热度/采纳/举报/违规/等级/版主/限制/申诉）

-- === 1. 新增表 ===

-- bbs_login_log — 每日登录浏览记录
CREATE TABLE IF NOT EXISTS bbs_login_log (
    id              SERIAL PRIMARY KEY,
    user_id         integer NOT NULL,
    login_date      varchar(10) NOT NULL,
    login_time      varchar(20),
    browse_minutes  integer DEFAULT 0,
    points_awarded  smallint DEFAULT 0,
    create_time     varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_login_log_user_date ON bbs_login_log (user_id, login_date);

-- bbs_report — 实名举报记录
CREATE TABLE IF NOT EXISTS bbs_report (
    id              SERIAL PRIMARY KEY,
    reporter_id     integer NOT NULL,
    target_type     varchar(20) NOT NULL,
    target_id       integer NOT NULL,
    reason          varchar(500),
    status          varchar(20) DEFAULT 'pending',
    reviewer_id     integer,
    review_time     varchar(20),
    review_remark   varchar(500),
    points_awarded  smallint DEFAULT 0,
    create_time     varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_report_reporter ON bbs_report (reporter_id);
CREATE INDEX IF NOT EXISTS idx_report_target ON bbs_report (target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_report_status ON bbs_report (status);

-- bbs_violation — 违规记录
CREATE TABLE IF NOT EXISTS bbs_violation (
    id              SERIAL PRIMARY KEY,
    user_id         integer NOT NULL,
    violation_type  varchar(50) NOT NULL,
    points_deducted integer NOT NULL,
    related_type    varchar(20),
    related_id      integer,
    operator_id     integer NOT NULL,
    remark          varchar(500),
    create_time     varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_violation_user ON bbs_violation (user_id);
CREATE INDEX IF NOT EXISTS idx_violation_type ON bbs_violation (violation_type);

-- bbs_appeal — 申诉记录
CREATE TABLE IF NOT EXISTS bbs_appeal (
    id              SERIAL PRIMARY KEY,
    user_id         integer NOT NULL,
    appeal_type     varchar(20) NOT NULL,
    related_id      integer,
    content         text NOT NULL,
    status          varchar(20) DEFAULT 'pending',
    reviewer_id     integer,
    review_remark   varchar(500),
    review_time     varchar(20),
    create_time     varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_appeal_user ON bbs_appeal (user_id);
CREATE INDEX IF NOT EXISTS idx_appeal_status ON bbs_appeal (status);

-- bbs_board_moderator — 版块管理员
CREATE TABLE IF NOT EXISTS bbs_board_moderator (
    id           SERIAL PRIMARY KEY,
    user_id      integer NOT NULL,
    label_id     integer NOT NULL,
    role_type    varchar(20) DEFAULT 'moderator',
    status       smallint DEFAULT 1,
    appoint_time varchar(20),
    create_time  varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_board_mod_user_label ON bbs_board_moderator (user_id, label_id);
CREATE INDEX IF NOT EXISTS idx_board_mod_label ON bbs_board_moderator (label_id);

-- === 2. 修改现有表 ===

-- bbs_reply 增加 is_adopted 字段
ALTER TABLE bbs_reply ADD COLUMN IF NOT EXISTS is_adopted smallint DEFAULT 0;

-- bbs_user 增加发帖限制字段
ALTER TABLE bbs_user ADD COLUMN IF NOT EXISTS post_restricted smallint DEFAULT 0;
ALTER TABLE bbs_user ADD COLUMN IF NOT EXISTS post_restricted_until varchar(20);

-- bbs_article 增加热度奖励标记字段
ALTER TABLE bbs_article ADD COLUMN IF NOT EXISTS is_hot_bonus smallint DEFAULT 0;

-- === 3. 数据字典新增 ===

-- 违规类型字典
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'illegal', '违法违规内容', 1, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣15分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'illegal');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'attack', '人身攻击/争吵引战', 2, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣10分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'attack');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'spam', '恶意灌水/刷屏', 3, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣4分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'spam');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'plagiarism', '抄袭剽窃', 4, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣12分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'plagiarism');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'false_report', '虚假恶意举报', 5, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣3分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'false_report');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'violation', 'leak', '泄露企业秘密', 6, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '扣20分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'violation' AND dict_value = 'leak');

-- 帖子热度阈值
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'hot_threshold', '10', '帖子热度回复阈值', 10, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '回复数超过此值触发热度奖励'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'hot_threshold');

-- 每日登录浏览阈值（分钟）
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'login_browse_minutes', '10', '每日登录有效浏览分钟数', 11, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '登录后需浏览满此分钟数才计分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'login_browse_minutes');

-- ============================================
-- 回滚 SQL（如需撤销上述变更，取消注释执行）
-- ============================================
-- DROP TABLE IF EXISTS bbs_board_moderator;
-- DROP TABLE IF EXISTS bbs_appeal;
-- DROP TABLE IF EXISTS bbs_violation;
-- DROP TABLE IF EXISTS bbs_report;
-- DROP TABLE IF EXISTS bbs_login_log;
-- ALTER TABLE bbs_article DROP COLUMN IF EXISTS is_hot_bonus;
-- ALTER TABLE bbs_user DROP COLUMN IF EXISTS post_restricted_until;
-- ALTER TABLE bbs_user DROP COLUMN IF EXISTS post_restricted;
-- ALTER TABLE bbs_reply DROP COLUMN IF EXISTS is_adopted;
-- DELETE FROM bbs_dict WHERE dict_type IN ('violation', 'hot_threshold', 'login_browse_minutes');

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

-- @migration: v013-backfill-points-log 回填历史积分到 bbs_points_log
-- 将历史发帖、评论、回复、登录浏览积分写入积分日志，使积分明细完整
-- 幂等：通过 NOT EXISTS 检查 related_type + related_id 避免重复

-- 1. 历史发帖积分
INSERT INTO bbs_points_log (user_id, points_change, reason, related_type, related_id, operator_id, create_time, is_reversed)
SELECT a.user_id,
       COALESCE(d.dict_value::integer, 3),
       '发帖积分',
       'article',
       a.article_id,
       NULL,
       a.create_time,
       0
FROM bbs_article a
LEFT JOIN bbs_dict d ON d.dict_type = 'post'
WHERE a.is_delete = 0
  AND a.enable = 1
  AND NOT EXISTS (
    SELECT 1 FROM bbs_points_log p
    WHERE p.related_type = 'article' AND p.related_id = a.article_id
  );

-- 2. 历史评论积分（楼层）
INSERT INTO bbs_points_log (user_id, points_change, reason, related_type, related_id, operator_id, create_time, is_reversed)
SELECT c.comment_user_id,
       COALESCE(d.dict_value::integer, 1),
       '评论积分',
       'comment',
       c.comment_id,
       NULL,
       c.comment_time,
       0
FROM bbs_comment c
LEFT JOIN bbs_dict d ON d.dict_type = 'reply'
WHERE c.is_delete = 0
  AND c.enable = 1
  AND NOT EXISTS (
    SELECT 1 FROM bbs_points_log p
    WHERE p.related_type = 'comment' AND p.related_id = c.comment_id
  );

-- 3. 历史回复积分（楼中楼）
INSERT INTO bbs_points_log (user_id, points_change, reason, related_type, related_id, operator_id, create_time, is_reversed)
SELECT r.reply_user_id,
       COALESCE(d.dict_value::integer, 1),
       '回复积分',
       'reply',
       r.reply_id,
       NULL,
       r.reply_time,
       0
FROM bbs_reply r
LEFT JOIN bbs_dict d ON d.dict_type = 'reply'
WHERE r.is_delete = 0
  AND r.enable = 1
  AND NOT EXISTS (
    SELECT 1 FROM bbs_points_log p
    WHERE p.related_type = 'reply' AND p.related_id = r.reply_id
  );

-- 4. 历史登录浏览积分
INSERT INTO bbs_points_log (user_id, points_change, reason, related_type, related_id, operator_id, create_time, is_reversed)
SELECT ll.user_id,
       1,
       '每日有效登录浏览积分',
       'login',
       ll.id,
       NULL,
       ll.create_time,
       0
FROM bbs_login_log ll
WHERE ll.points_awarded = 1
  AND NOT EXISTS (
    SELECT 1 FROM bbs_points_log p
    WHERE p.related_type = 'login' AND p.related_id = ll.id
  );

-- @migration: v014-fix-backfill-reason 去除回填记录中的"历史回填"字样
UPDATE bbs_points_log SET reason = '发帖积分' WHERE reason = '发帖积分（历史回填）';
UPDATE bbs_points_log SET reason = '评论积分' WHERE reason = '评论积分（历史回填）';
UPDATE bbs_points_log SET reason = '回复积分' WHERE reason = '回复积分（历史回填）';
UPDATE bbs_points_log SET reason = '每日有效登录浏览积分' WHERE reason = '每日有效登录浏览积分（历史回填）';

-- @migration: v015-adopt-status 采纳审批状态（问题求助最佳解答）
-- bbs_reply 新增 adopt_status：0=未采纳 1=待审批 2=已确认 3=已拒绝
ALTER TABLE bbs_reply ADD COLUMN IF NOT EXISTS adopt_status smallint DEFAULT 0;

-- 回填：已采纳的记录设为已确认(2)
UPDATE bbs_reply SET adopt_status = 2 WHERE is_adopted = 1 AND (adopt_status IS NULL OR adopt_status = 0);

-- bbs_comment 同步新增 adopt_status（评论也可被采纳为最佳解答）
ALTER TABLE bbs_comment ADD COLUMN IF NOT EXISTS adopt_status smallint DEFAULT 0;

-- @migration: v016-adopt-comment-status 补充 bbs_comment.adopt_status
-- 早期 v015 只加了 bbs_reply.adopt_status，此迁移确保 bbs_comment 也有该列
ALTER TABLE bbs_comment ADD COLUMN IF NOT EXISTS adopt_status smallint DEFAULT 0;

-- @migration: v017-suggestion-label 添加"建议反馈"标签
INSERT INTO bbs_article_label (label_id, label_name, enabled, icon, description)
VALUES (4, '建议反馈', 1, 'lightbulb', '提交建议并被采纳获得+5积分')
ON CONFLICT (label_id) DO NOTHING;

-- @migration: v018-notification-category 通知分类字段，支持分类独立未读计数
-- interaction=互动消息(reply/comment，对应"回复我的")；system=系统通知(其余类型，对应"消息通知")
ALTER TABLE bbs_notification ADD COLUMN IF NOT EXISTS category varchar(20) DEFAULT 'system';

-- 回填历史数据：互动类通知归入 interaction，其余保持 system
UPDATE bbs_notification SET category = 'interaction'
WHERE type IN ('reply', 'comment') AND (category IS NULL OR category = '');

UPDATE bbs_notification SET category = 'system' WHERE category IS NULL OR category = '';

-- 回填完成后收紧为 NOT NULL（与 init 建表定义一致）
ALTER TABLE bbs_notification ALTER COLUMN category SET NOT NULL;

-- 分类维度的未读计数索引
CREATE INDEX IF NOT EXISTS idx_notification_user_cat_read ON bbs_notification (user_id, category, is_read);

-- @migration: v019-report-violation-type 举报表增加违规类型字段，支持管理员"确认并扣分"时自动填写
ALTER TABLE bbs_report ADD COLUMN IF NOT EXISTS violation_type varchar(50);

-- 回填历史数据：从 reason 字段解析 【label】 格式的违规类型
UPDATE bbs_report SET violation_type = 'spam'      WHERE violation_type IS NULL AND reason LIKE '【恶意灌水%';
UPDATE bbs_report SET violation_type = 'plagiarism' WHERE violation_type IS NULL AND reason LIKE '【抄袭剽窃%';
UPDATE bbs_report SET violation_type = 'illegal'    WHERE violation_type IS NULL AND reason LIKE '【违规%';
UPDATE bbs_report SET violation_type = 'attack'     WHERE violation_type IS NULL AND reason LIKE '【人身攻击%';
UPDATE bbs_report SET violation_type = 'leak'       WHERE violation_type IS NULL AND reason LIKE '【泄露%';

-- @migration: v020-dict-extra 字典表增加键字段dict_key，违规扣分统一存入dict_value
ALTER TABLE bbs_dict ADD COLUMN IF NOT EXISTS dict_key varchar(100);

-- 回填：所有字典条目的 dict_key
UPDATE bbs_dict SET dict_key = 'post'                 WHERE dict_type = 'post' AND dict_key IS NULL;
UPDATE bbs_dict SET dict_key = 'reply'                WHERE dict_type = 'reply' AND dict_key IS NULL;
UPDATE bbs_dict SET dict_key = 'switch'               WHERE dict_type = 'switch' AND dict_key IS NULL;
UPDATE bbs_dict SET dict_key = 'featured'             WHERE dict_type = 'featured' AND dict_key IS NULL;
UPDATE bbs_dict SET dict_key = 'hot_threshold'        WHERE dict_type = 'hot_threshold' AND dict_key IS NULL;
UPDATE bbs_dict SET dict_key = 'login_browse_minutes' WHERE dict_type = 'login_browse_minutes' AND dict_key IS NULL;

-- 违规类型：dict_key = 类型标识，dict_value = 扣分值
UPDATE bbs_dict SET dict_key = 'illegal',      dict_value = '15'  WHERE dict_type = 'violation' AND dict_value = 'illegal';
UPDATE bbs_dict SET dict_key = 'attack',       dict_value = '10'  WHERE dict_type = 'violation' AND dict_value = 'attack';
UPDATE bbs_dict SET dict_key = 'spam',         dict_value = '4'   WHERE dict_type = 'violation' AND dict_value = 'spam';
UPDATE bbs_dict SET dict_key = 'plagiarism',   dict_value = '12'  WHERE dict_type = 'violation' AND dict_value = 'plagiarism';
UPDATE bbs_dict SET dict_key = 'false_report', dict_value = '3'   WHERE dict_type = 'violation' AND dict_value = 'false_report';
UPDATE bbs_dict SET dict_key = 'leak',         dict_value = '20'  WHERE dict_type = 'violation' AND dict_value = 'leak';

-- 清理旧的违规字典条目（dict_value 为旧格式字符串 key 的重复行）
DELETE FROM bbs_dict WHERE dict_type = 'violation' AND dict_value IN ('illegal', 'attack', 'spam', 'plagiarism', 'false_report', 'leak');

-- 清理 dict_extra 列（如已存在）
ALTER TABLE bbs_dict DROP COLUMN IF EXISTS dict_extra;

-- @migration: v021-dedup-sensitive-words 清理敏感词重复数据并加唯一约束
DELETE FROM bbs_sensitive_word WHERE id NOT IN (
    SELECT MIN(id) FROM bbs_sensitive_word GROUP BY keyword
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sensitive_word_keyword ON bbs_sensitive_word (keyword);

-- @migration: v022-moderator-complaint 版主投诉表（用户可投诉版主违规操作）
CREATE TABLE IF NOT EXISTS bbs_moderator_complaint (
    id SERIAL PRIMARY KEY,
    reporter_id integer NOT NULL,
    moderator_id integer NOT NULL,
    label_id integer DEFAULT NULL,
    content text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'pending',
    reviewer_id integer DEFAULT NULL,
    review_remark varchar(500) DEFAULT NULL,
    review_time varchar(20) DEFAULT NULL,
    create_time varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_moderator_id ON bbs_moderator_complaint (moderator_id);
CREATE INDEX IF NOT EXISTS idx_reporter_id ON bbs_moderator_complaint (reporter_id);
CREATE INDEX IF NOT EXISTS idx_mc_status ON bbs_moderator_complaint (status);

-- @migration: v023-featured-recommendation 精华帖推荐审批流程（版主初审→总运营终审）
CREATE TABLE IF NOT EXISTS bbs_featured_recommendation (
    id SERIAL PRIMARY KEY,
    article_id integer NOT NULL,
    recommender_id integer NOT NULL,
    label_id integer DEFAULT NULL,
    status varchar(20) NOT NULL DEFAULT 'pending',
    reviewer_id integer DEFAULT NULL,
    review_remark varchar(500) DEFAULT NULL,
    review_time varchar(20) DEFAULT NULL,
    create_time varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fr_article ON bbs_featured_recommendation (article_id);
CREATE INDEX IF NOT EXISTS idx_fr_status ON bbs_featured_recommendation (status);

-- @migration: v024-moderator-reward-auto 版主履职奖励自动发放+取消机制
CREATE TABLE IF NOT EXISTS bbs_moderator_reward_cancel (
    id SERIAL PRIMARY KEY,
    year_month varchar(7) NOT NULL,
    user_id integer NOT NULL,
    operator_id integer DEFAULT NULL,
    remark varchar(500) DEFAULT NULL,
    create_time varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mrc_ym ON bbs_moderator_reward_cancel (year_month);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mrc_ym_user ON bbs_moderator_reward_cancel (year_month, user_id);
