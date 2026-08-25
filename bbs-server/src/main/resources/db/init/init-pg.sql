-- ============================================
-- BBS 数据库初始化脚本 - PostgreSQL 版
-- 安全模式：不会覆盖已有数据
-- 可重复执行：CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING
-- ============================================

SET session_replication_role = 'replica';

-- ----------------------------
-- Table: bbs_admin
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_admin (
    id          SERIAL PRIMARY KEY,
    username    varchar(20),
    password    varchar(255),
    portrait    varchar(255),
    is_alive    integer DEFAULT 0,
    is_delete   integer DEFAULT 0,
    create_time varchar(20)
);

-- ----------------------------
-- Table: bbs_area
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_area (
    area_id     SERIAL PRIMARY KEY,
    image_title varchar(20),
    image_url   varchar(255)
);

-- ----------------------------
-- Table: bbs_article
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_article (
    article_id           SERIAL PRIMARY KEY,
    article_label_id     integer,
    article_author       varchar(20),
    article_title        varchar(30),
    article_summary      varchar(255),
    article_type_id      integer,
    article_content      text,
    article_content_html text,
    article_image        text,
    user_id              integer,
    article_good_num     integer DEFAULT 0,
    article_view_num     integer DEFAULT 0,
    article_community_id integer,
    create_time          varchar(20),
    recommend            smallint,
    enable               integer DEFAULT 0,
    is_delete            integer DEFAULT 0,
    is_featured          smallint NOT NULL DEFAULT 0,
    is_hot_bonus         smallint DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_article_featured_time ON bbs_article (is_featured, create_time);

-- ----------------------------
-- Table: bbs_article_file
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_article_file (
    file_id    SERIAL PRIMARY KEY,
    file_name  varchar(255),
    file_type  varchar(255),
    file_path  varchar(255),
    article_id integer
);

-- ----------------------------
-- Table: bbs_article_label
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_article_label (
    label_id    SERIAL PRIMARY KEY,
    label_name  varchar(10),
    enabled     smallint,
    icon        varchar(50),
    description varchar(200)
);

-- ----------------------------
-- Table: bbs_article_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_article_type (
    type_id   SERIAL PRIMARY KEY,
    type_name varchar(10)
);

-- ----------------------------
-- Table: bbs_article_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_article_user (
    id          SERIAL PRIMARY KEY,
    user_id     integer,
    article_id  integer,
    create_time varchar(20)
);

-- ----------------------------
-- Table: bbs_comment
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_comment (
    comment_id          SERIAL PRIMARY KEY,
    comment_content     varchar(255),
    comment_article_id  integer,
    comment_user_id     integer,
    comment_time        varchar(20),
    enable              integer DEFAULT 1,
    is_delete           integer DEFAULT 0
);

-- ----------------------------
-- Table: bbs_community
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_community (
    community_id         SERIAL PRIMARY KEY,
    community_name       varchar(20),
    community_introduce  varchar(255),
    community_image      varchar(255),
    community_user_num   integer DEFAULT 0,
    create_user_id       integer,
    create_time          varchar(20),
    enable               integer DEFAULT 1,
    is_delete            integer DEFAULT 0
);

-- ----------------------------
-- Table: bbs_community_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_community_user (
    id            SERIAL PRIMARY KEY,
    community_id  integer,
    user_id       integer
);

-- ----------------------------
-- Table: bbs_dict
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_dict (
    id          SERIAL PRIMARY KEY,
    dict_type   varchar(50),
    dict_value  varchar(50),
    dict_label  varchar(255),
    dict_sort   integer,
    create_by   varchar(50),
    create_time varchar(20),
    update_by   varchar(50),
    update_time varchar(20),
    remark      varchar(256)
);

-- ----------------------------
-- Table: bbs_system_config
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_fans
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_fans (
    id            SERIAL PRIMARY KEY,
    user_id       integer,
    attention_id  integer,
    create_time   varchar(20)
);

-- ----------------------------
-- Table: bbs_inventory
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_inventory (
    id         SERIAL PRIMARY KEY,
    area       varchar(255),
    category   varchar(255),
    type       varchar(255),
    name       varchar(255),
    content    text,
    snumber    varchar(20),
    department varchar(255),
    time       varchar(20)
);

-- ----------------------------
-- Table: bbs_reply
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_reply (
    reply_id         SERIAL PRIMARY KEY,
    reply_content    varchar(255),
    reply_to_user_id integer,
    comment_id       integer,
    reply_user_id    integer,
    reply_time       varchar(20) NOT NULL,
    enable           integer DEFAULT 1,
    is_delete        integer DEFAULT 0,
    is_adopted       smallint DEFAULT 0
);

-- ----------------------------
-- Table: bbs_sa_org
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_sa_org (
    id                 integer PRIMARY KEY,
    org_no             varchar(20) NOT NULL,
    org_name           varchar(255),
    p_org_no           varchar(20),
    org_tree           varchar(255),
    is_delete          smallint,
    is_ranking_selected smallint DEFAULT 0,
    is_display_selected smallint DEFAULT 1
);

-- ----------------------------
-- Table: bbs_sensitive_word
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_sensitive_word (
    id      SERIAL PRIMARY KEY,
    keyword varchar(255)
);

-- ----------------------------
-- Table: bbs_slideshow
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_slideshow (
    slideshow_id SERIAL PRIMARY KEY,
    image_url    varchar(255),
    successive   integer,
    create_time  varchar(20)
);

-- ----------------------------
-- Table: bbs_user
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_user (
    id              SERIAL PRIMARY KEY,
    username        varchar(20),
    password        varchar(255),
    nickname        varchar(20),
    portrait        varchar(255),
    gender          varchar(2),
    introduce       varchar(255),
    city            varchar(100),
    fans            integer DEFAULT 0,
    attention       integer,
    good            integer,
    is_alive        integer DEFAULT 0,
    is_delete       integer DEFAULT 0,
    create_time     varchar(20),
    phone           varchar(20),
    org_no          varchar(20),
    user_type       varchar(20),
    personnel_id    varchar(50),
    id_card         varchar(18),
    is_first_login  smallint DEFAULT 1,
    post_restricted smallint DEFAULT 0,
    post_restricted_until varchar(20),
    CONSTRAINT uk_bbs_user_username UNIQUE (username),
    CONSTRAINT uk_bbs_user_personnel_id UNIQUE (personnel_id),
    CONSTRAINT uk_bbs_user_id_card UNIQUE (id_card)
);

-- 重新启用外键检查
SET session_replication_role = 'origin';

-- ============================================
-- 基础数据（不会覆盖已有数据）
-- ============================================

-- ----------------------------
-- 超级管理员（密码由 DatabaseInitializer 启动时用 BCrypt 加密写入）
-- ----------------------------
INSERT INTO bbs_user (id, username, password, nickname, gender, city, fans, attention, good, is_alive, is_delete, create_time, org_no, user_type, is_first_login)
VALUES (1, 'asiayak', '$2a$10$hpTQLGhUicOwSbSgLa2kyuQYMXhnWCZhi/CR/v6cyc2JcNOs2rk3O', '超级管理员', '1', '河北省-秦皇岛市', 0, 0, 0, 0, 0, '2026-06-26 00:00:00', '51404', '3', 0)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 文章标签
-- ----------------------------
INSERT INTO bbs_article_label (label_id, label_name, enabled, icon, description) VALUES
(1, '技术交流', 0, 'thumb_up', ''),
(2, '求助问答', 1, 'help', ''),
(3, '资源共享', 0, 'folder_open', '')
ON CONFLICT (label_id) DO NOTHING;

-- ----------------------------
-- 组织机构（国网四川内江供电公司）
-- ----------------------------
INSERT INTO bbs_sa_org (id, org_no, org_name, p_org_no, org_tree, is_delete) VALUES
(1, '51404', '国网四川内江供电公司', '51101', '51101|51404', 0),
(2, '5140400', '国网四川内江市区供电中心', '51404', '51101|51404|5140400', 0),
(3, '514040003', '国网四川内江市区凌家供电所', '5140400', '51101|51404|5140400|514040003', 0),
(4, '514040006', '国网四川内江市区白马供电所', '5140400', '51101|51404|5140400|514040006', 0),
(5, '514040008', '国网四川内江市区靖民供电所', '5140400', '51101|51404|5140400|514040008', 0),
(6, '514040009', '国网四川内江市区城郊供电所', '5140400', '51101|51404|5140400|514040009', 0),
(7, '5140403', '国网四川内江东兴供电公司', '51404', '51101|51404|5140403', 0),
(8, '514040302', '国网四川内江东兴高梁供电所', '5140403', '51101|51404|5140403|514040302', 0),
(9, '514040303', '国网四川内江东兴白合供电所', '5140403', '51101|51404|5140403|514040303', 0),
(10, '514040304', '国网四川内江东兴双才供电所', '5140403', '51101|51404|5140403|514040304', 0),
(11, '514040305', '国网四川内江东兴田家供电所', '5140403', '51101|51404|5140403|514040305', 0),
(12, '514040306', '国网四川内江东兴椑木供电所', '5140403', '51101|51404|5140403|514040306', 0),
(13, '5140424', '国网四川内江威远县供电公司', '51404', '51101|51404|5140424', 0),
(14, '514042401', '国网四川内江威远连界供电所', '5140424', '51101|51404|5140424|514042401', 0),
(15, '514042402', '国网四川内江威远龙会供电所', '5140424', '51101|51404|5140424|514042402', 0),
(16, '514042404', '国网四川内江威远山王供电所', '5140424', '51101|51404|5140424|514042404', 0),
(17, '514042408', '国网四川内江威远严陵供电所', '5140424', '51101|51404|5140424|514042408', 0),
(18, '514042410', '国网四川内江威远镇西供电所', '5140424', '51101|51404|5140424|514042410', 0),
(19, '514042411', '国网四川内江威远越溪供电所', '5140424', '51101|51404|5140424|514042411', 0),
(20, '514042412', '国网四川内江威远新店供电所', '5140424', '51101|51404|5140424|514042412', 0),
(21, '514042413', '国网四川内江威远界牌供电所', '5140424', '51101|51404|5140424|514042413', 0),
(22, '5140425', '国网四川内江资中县供电公司', '51404', '51101|51404|5140425', 0),
(23, '514042501', '国网四川内江资中归德供电所', '5140425', '51101|51404|5140425|514042501', 0),
(24, '514042502', '国网四川内江资中水南供电所', '5140425', '51101|51404|5140425|514042502', 0),
(25, '514042503', '国网四川内江资中银山供电所', '5140425', '51101|51404|5140425|514042503', 0),
(26, '514042507', '国网四川内江资中双河供电所', '5140425', '51101|51404|5140425|514042507', 0),
(27, '514042508', '国网四川内江资中鱼溪供电所', '5140425', '51101|51404|5140425|514042508', 0),
(28, '5140428', '国网四川内江隆昌市供电公司', '51404', '51101|51404|5140428', 0),
(29, '514042804', '国网四川内江隆昌金鹅供电所', '5140428', '51101|51404|5140428|514042804', 0),
(30, '514042806', '国网四川内江隆昌黄家供电所', '5140428', '51101|51404|5140428|514042806', 0),
(31, '514042807', '国网四川内江隆昌石碾供电所', '5140428', '51101|51404|5140428|514042807', 0),
(32, '514042812', '国网四川内江隆昌云顶供电所', '5140428', '51101|51404|5140428|514042812', 0)
ON CONFLICT (id) DO NOTHING;

-- 注：init 中的组织机构名称已同步更新（2026-08-25 组织架构梳理）
-- "地市支撑机构及原集体企业" 已在 upgrade 中重命名为 "公司所属各单位"

-- ----------------------------
-- 数据字典
-- ----------------------------
INSERT INTO bbs_dict (id, dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark) VALUES
(1, 'post', '3', '发帖积分', 1, '系统', '2026-06-26 00:00:00', '发一个帖子所得积分'),
(2, 'reply', '1', '回帖积分', 0, '系统', '2026-06-26 00:00:00', '回帖一次所得积分'),
(3, 'switch', '1', '排名功能是否开启', 1, '系统', '2026-06-26 00:00:00', '值：积分排名开关（0不开放，1开放）'),
(4, 'featured', '10', '精华帖积分', 2, '系统', '2026-07-13 00:00:00', '被设为精华帖额外获得的积分'),
(5, 'violation', 'illegal', '违法违规内容', 1, '系统', '2026-08-25 00:00:00', '扣15分'),
(6, 'violation', 'attack', '人身攻击/争吵引战', 2, '系统', '2026-08-25 00:00:00', '扣10分'),
(7, 'violation', 'spam', '恶意灌水/刷屏', 3, '系统', '2026-08-25 00:00:00', '扣4分'),
(8, 'violation', 'plagiarism', '抄袭剽窃', 4, '系统', '2026-08-25 00:00:00', '扣12分'),
(9, 'violation', 'false_report', '虚假恶意举报', 5, '系统', '2026-08-25 00:00:00', '扣3分'),
(10, 'violation', 'leak', '泄露企业秘密', 6, '系统', '2026-08-25 00:00:00', '扣20分'),
(11, 'hot_threshold', '10', '帖子热度回复阈值', 10, '系统', '2026-08-25 00:00:00', '回复数超过此值触发热度奖励'),
(12, 'login_browse_minutes', '10', '每日登录有效浏览分钟数', 11, '系统', '2026-08-25 00:00:00', '登录后需浏览满此分钟数才计分')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 系统配置（使用反馈联系方式）
-- ----------------------------
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'feedback_contact', '{"name":"","email":""}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'feedback_contact');

-- ----------------------------
-- 敏感词
-- ----------------------------
INSERT INTO bbs_sensitive_word (id, keyword) VALUES
(1, '赌博'),
(2, '肉票'),
(3, '抢劫'),
(4, '莎莎舞'),
(5, '老司机'),
(6, '哈哈哈'),
(7, '嘻嘻嘻'),
(8, '嘿嘿嘿'),
(9, '啊啊啊'),
(10, '嗯嗯嗯'),
(11, '哦哦哦'),
(12, '呵呵呵'),
(13, '啦啦啦'),
(14, '呜呜呜'),
(15, '沙发'),
(16, '占位'),
(17, '占楼'),
(18, '路过'),
(19, '马克'),
(20, 'mark'),
(21, '顶贴'),
(22, '灌水'),
(23, '水水水'),
(24, '水帖'),
(25, '打卡'),
(26, '签到'),
(27, '666666'),
(28, '8888'),
(29, '11111'),
(30, '123456'),
(31, '测试测试'),
(32, '测试一下'),
(33, 'testtest')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- Table: bbs_points_log（积分调整日志）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_notification（通知表）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_login_log（每日登录浏览记录）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_report（实名举报记录）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_violation（违规记录）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_appeal（申诉记录）
-- ----------------------------
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

-- ----------------------------
-- Table: bbs_board_moderator（版块管理员）
-- ----------------------------
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

-- ----------------------------
-- 重置序列，使后续自增从正确值开始
-- 仅当序列当前值小于 max(id) 时才更新
-- ----------------------------
SELECT setval('bbs_user_id_seq', GREATEST(nextval('bbs_user_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_user)));
SELECT setval('bbs_article_label_label_id_seq', GREATEST(nextval('bbs_article_label_label_id_seq'), (SELECT COALESCE(max(label_id), 0) FROM bbs_article_label)));
SELECT setval('bbs_sensitive_word_id_seq', GREATEST(nextval('bbs_sensitive_word_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_sensitive_word)));
SELECT setval('bbs_dict_id_seq', GREATEST(nextval('bbs_dict_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_dict)));

-- ----------------------------
-- Table: bbs_schema_version（迁移版本追踪）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_schema_version (
    version     varchar(50) PRIMARY KEY,
    description varchar(255),
    applied_at  varchar(20) NOT NULL
);
