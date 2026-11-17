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
    is_hot_bonus         smallint DEFAULT 0,
    last_reply_time      varchar(20),
    last_activity_time   varchar(20),
    comment_num          integer NOT NULL DEFAULT 0,
    favorite_num         integer NOT NULL DEFAULT 0,
    share_num            integer NOT NULL DEFAULT 0,
    is_anonymous         smallint NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_article_featured_time ON bbs_article (is_featured, create_time);
CREATE INDEX IF NOT EXISTS idx_article_last_reply ON bbs_article (last_reply_time);
CREATE INDEX IF NOT EXISTS idx_article_last_activity ON bbs_article (last_activity_time);

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
    description varchar(200),
    label_type  varchar(20) NOT NULL DEFAULT 'normal',
    allow_anonymous smallint NOT NULL DEFAULT 0,
    no_points       smallint NOT NULL DEFAULT 0
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
CREATE UNIQUE INDEX IF NOT EXISTS uk_article_user_favorite ON bbs_article_user (user_id, article_id);

CREATE TABLE IF NOT EXISTS bbs_article_like (
    id          SERIAL PRIMARY KEY,
    user_id     integer NOT NULL,
    article_id  integer NOT NULL,
    create_time varchar(20),
    CONSTRAINT uk_article_like UNIQUE (user_id, article_id)
);
CREATE INDEX IF NOT EXISTS idx_article_like_article ON bbs_article_like (article_id, create_time);

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
    is_delete           integer DEFAULT 0,
    adopt_status        smallint DEFAULT 0,
    is_anonymous        smallint NOT NULL DEFAULT 0,
    comment_good_num    integer NOT NULL DEFAULT 0
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
    remark      varchar(256),
    dict_key    varchar(100)
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
    is_adopted       smallint DEFAULT 0,
    adopt_status     smallint DEFAULT 0,
    is_anonymous     smallint NOT NULL DEFAULT 0,
    reply_good_num   integer NOT NULL DEFAULT 0
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
-- Table: bbs_ranking_category（排行榜分类）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_ranking_category (
    id            SERIAL PRIMARY KEY,
    category_code varchar(50) NOT NULL UNIQUE,
    category_name varchar(100) NOT NULL,
    description   varchar(500),
    sort_order    integer NOT NULL DEFAULT 0,
    enabled       smallint NOT NULL DEFAULT 1,
    create_time   varchar(20),
    update_time   varchar(20)
);

-- ----------------------------
-- Table: bbs_ranking_category_org（排行榜分类成员）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_ranking_category_org (
    id                  SERIAL PRIMARY KEY,
    category_id         integer NOT NULL,
    org_no              varchar(20) NOT NULL,
    include_descendants smallint NOT NULL DEFAULT 1,
    sort_order          integer NOT NULL DEFAULT 0,
    create_time         varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ranking_category_org
    ON bbs_ranking_category_org (category_id, org_no);
CREATE INDEX IF NOT EXISTS idx_ranking_category
    ON bbs_ranking_category_org (category_id);

-- ----------------------------
-- Table: bbs_sensitive_word
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_sensitive_word (
    id      SERIAL PRIMARY KEY,
    keyword varchar(255) NOT NULL,
    CONSTRAINT uk_sensitive_word_keyword UNIQUE (keyword)
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
    user_type       varchar(20) DEFAULT '1',
    primary_group_id integer,
    personnel_id    varchar(50),
    id_card         varchar(18),
    is_first_login  smallint DEFAULT 1,
    post_restricted smallint DEFAULT 0,
    post_restricted_until varchar(20),
    is_test_account smallint NOT NULL DEFAULT 0,
    CONSTRAINT uk_bbs_user_username UNIQUE (username),
    CONSTRAINT uk_bbs_user_personnel_id UNIQUE (personnel_id),
    CONSTRAINT uk_bbs_user_id_card UNIQUE (id_card)
);

COMMENT ON COLUMN bbs_user.user_type
    IS '用户类型（1：普通用户，2：管理员，3：超级管理员，4：版主）';
COMMENT ON COLUMN bbs_user.primary_group_id
    IS '主权限组ID（必须指向系统角色组）';
CREATE INDEX IF NOT EXISTS idx_user_primary_group ON bbs_user (primary_group_id);

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
INSERT INTO bbs_article_label (label_id, label_name, enabled, icon, description, label_type) VALUES
(1, '技术交流', 0, 'thumb_up', '', 'normal'),
(2, '求助问答', 1, 'help', '', 'question'),
(3, '资源共享', 0, 'folder_open', '', 'normal'),
(4, '建议反馈', 1, 'lightbulb', '提交建议并被采纳获得+5积分', 'suggestion')
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

-- ----------------------------
-- 排行榜分类（“内江供电公司本部”为系统内置 LEGACY，不落库）
-- ----------------------------
INSERT INTO bbs_ranking_category (category_code, category_name, description, sort_order, enabled, create_time) VALUES
('county_units', '县级供电企业', '县级供电公司/供电中心，含下级供电所', 20, 1, '2026-11-13 00:00:00'),
('company_units', '公司所属各单位', '内江公司根节点下直属单位，含下级', 30, 1, '2026-11-13 00:00:00'),
('support_units', '地市支撑机构', '变电运检、输电运检、客户服务等支撑机构', 40, 1, '2026-11-13 00:00:00'),
('collective_units', '集体企业及相关单位', '集体企业及相关单位', 50, 1, '2026-11-13 00:00:00')
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO bbs_ranking_category_org (category_id, org_no, include_descendants, sort_order, create_time)
SELECT c.id, o.org_no, 1, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_ranking_category c
JOIN bbs_sa_org o ON o.is_delete = 0
WHERE c.category_code = 'county_units'
  AND (
    o.p_org_no = (SELECT org_no FROM bbs_sa_org WHERE org_name = '县级供电企业' AND is_delete = 0 LIMIT 1)
    OR (o.p_org_no = '51404' AND (o.org_name LIKE '%供电分公司%' OR o.org_name = '市区供电中心'))
  )
ON CONFLICT (category_id, org_no) DO NOTHING;

INSERT INTO bbs_ranking_category_org (category_id, org_no, include_descendants, sort_order, create_time)
SELECT c.id, o.org_no, 1, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_ranking_category c
JOIN bbs_sa_org o ON o.is_delete = 0
  AND o.p_org_no = (SELECT org_no FROM bbs_sa_org WHERE org_name = '公司所属各单位' AND is_delete = 0 LIMIT 1)
WHERE c.category_code = 'company_units'
ON CONFLICT (category_id, org_no) DO NOTHING;

INSERT INTO bbs_ranking_category_org (category_id, org_no, include_descendants, sort_order, create_time)
SELECT c.id, o.org_no, 1, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_ranking_category c
JOIN bbs_sa_org o ON o.is_delete = 0 AND (
  o.org_name IN ('变电运检中心', '输电运检中心', '客户服务中心', '经济技术研究所',
                 '信息通信分公司', '综合服务中心', '供电服务指挥中心')
  OR o.org_name LIKE '%项目管理中心%'
)
WHERE c.category_code = 'support_units'
ON CONFLICT (category_id, org_no) DO NOTHING;

INSERT INTO bbs_ranking_category_org (category_id, org_no, include_descendants, sort_order, create_time)
SELECT c.id, o.org_no, 1, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_ranking_category c
JOIN bbs_sa_org o ON o.is_delete = 0 AND o.p_org_no = '5140403' AND (
  o.org_name LIKE '%星原%' OR o.org_name LIKE '%三新%'
  OR o.org_name LIKE '%乐山城电%' OR o.org_name LIKE '%东祥%'
  OR o.org_name LIKE '%西星%'
)
WHERE c.category_code = 'collective_units'
ON CONFLICT (category_id, org_no) DO NOTHING;

-- 注：init 中的组织机构名称已同步更新（2026-08-25 组织架构梳理）
-- "地市支撑机构及原集体企业" 已在 upgrade 中重命名为 "公司所属各单位"

-- ----------------------------
-- 数据字典
-- ----------------------------
INSERT INTO bbs_dict (id, dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark, dict_key) VALUES
(1, 'post', '3', '发帖积分', 1, '系统', '2026-06-26 00:00:00', '发一个帖子所得积分', 'post'),
(2, 'reply', '1', '回帖积分', 0, '系统', '2026-06-26 00:00:00', '回帖一次所得积分', 'reply'),
(3, 'switch', '1', '排名功能是否开启', 1, '系统', '2026-06-26 00:00:00', '值：积分排名开关（0不开放，1开放）', 'switch'),
(4, 'featured', '10', '精华帖积分', 2, '系统', '2026-07-13 00:00:00', '被设为精华帖额外获得的积分', 'featured'),
(5, 'violation', '15', '违法违规内容', 1, '系统', '2026-08-25 00:00:00', '扣15分', 'illegal'),
(6, 'violation', '10', '人身攻击/争吵引战', 2, '系统', '2026-08-25 00:00:00', '扣10分', 'attack'),
(7, 'violation', '4', '恶意灌水/刷屏', 3, '系统', '2026-08-25 00:00:00', '扣4分', 'spam'),
(8, 'violation', '12', '抄袭剽窃', 4, '系统', '2026-08-25 00:00:00', '扣12分', 'plagiarism'),
(9, 'violation', '3', '虚假恶意举报', 5, '系统', '2026-08-25 00:00:00', '扣3分', 'false_report'),
(10, 'violation', '20', '泄露企业秘密', 6, '系统', '2026-08-25 00:00:00', '扣20分', 'leak'),
(11, 'hot_threshold', '10', '帖子热度回复阈值', 10, '系统', '2026-08-25 00:00:00', '回复数超过此值触发热度奖励', 'hot_threshold'),
(12, 'login_browse_minutes', '1', '每日登录有效浏览分钟数', 11, '系统', '2026-08-25 00:00:00', '登录后需浏览满此分钟数才计分', 'login_browse_minutes'),
(13, 'moderator_reward_auto', '0', '版主奖励自动发放开关', 13, '系统', '2026-09-01 00:00:00', '值：0关闭，1开启', 'moderator_reward_auto'),
(14, 'moderator_reward_day', '1', '版主奖励发放日', 14, '系统', '2026-09-01 00:00:00', '每月发放日（1-28）', 'moderator_reward_day'),
(15, 'moderator_reward_points', '400', '版主月度履职奖励积分', 15, '系统', '2026-11-13 00:00:00', '每位版主每月一次性奖励积分', 'moderator_reward_points'),
(16, 'monthly_deduction_alert_enabled', '1', '月累计扣分提醒开关', 16, '系统', '2026-11-13 00:00:00', '值：0关闭，1开启', 'monthly_deduction_alert_enabled'),
(17, 'monthly_deduction_alert_threshold', '10', '月累计扣分提醒阈值', 17, '系统', '2026-11-13 00:00:00', '当月累计扣分达到该值提醒用户', 'monthly_deduction_alert_threshold'),
(18, 'article_publish_mode', 'AUTO', '发帖审核模式', 18, '系统', '2026-11-13 00:00:00', 'AUTO自动通过 / PRE_REVIEW前置审核 / POST_REVIEW先发后审', 'article_publish_mode'),
(19, 'featured_final_review_mode', 'ADMIN_FINAL', '精华终审模式', 19, '系统', '2026-11-13 00:00:00', 'ADMIN_FINAL管理员终审 / MODERATOR_FINAL版主终审即通过', 'featured_final_review_mode')
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
-- Table: bbs_points_alert（积分预警去重）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_points_alert (
    id          SERIAL PRIMARY KEY,
    user_id     integer NOT NULL,
    alert_type  varchar(30) NOT NULL,
    period      varchar(10) NOT NULL,
    threshold   integer NOT NULL,
    points      integer NOT NULL DEFAULT 0,
    create_time varchar(20),
    CONSTRAINT uk_points_alert UNIQUE (user_id, alert_type, period, threshold)
);

-- ----------------------------
-- Table: bbs_notification（通知表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_notification (
    id             SERIAL PRIMARY KEY,
    user_id        integer NOT NULL,
    from_user_id   integer,
    type           varchar(20) NOT NULL,
    category       varchar(20) NOT NULL DEFAULT 'system',
    title          varchar(255),
    related_type   varchar(20),
    related_id     integer,
    is_read        smallint NOT NULL DEFAULT 0,
    create_time    varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON bbs_notification (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON bbs_notification (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notification_user_cat_read ON bbs_notification (user_id, category, is_read);

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
    violation_type  varchar(50),
    status          varchar(20) DEFAULT 'pending',
    reviewer_id     integer,
    review_time     varchar(20),
    review_remark   varchar(500),
    points_awarded  smallint DEFAULT 0,
    is_anonymous    smallint NOT NULL DEFAULT 0,
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
    status          varchar(20) NOT NULL DEFAULT 'active',
    cancel_reason   varchar(500),
    cancel_operator_id integer,
    cancel_time     varchar(20),
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
-- Table: bbs_moderator_complaint（版主投诉）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_moderator_complaint (
    id            SERIAL PRIMARY KEY,
    reporter_id   integer NOT NULL,
    moderator_id  integer DEFAULT NULL,
    label_id      integer DEFAULT NULL,
    content       text NOT NULL,
    status        varchar(20) NOT NULL DEFAULT 'pending',
    reviewer_id   integer DEFAULT NULL,
    review_remark varchar(500) DEFAULT NULL,
    review_time   varchar(20) DEFAULT NULL,
    create_time   varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mc_moderator ON bbs_moderator_complaint (moderator_id);
CREATE INDEX IF NOT EXISTS idx_mc_reporter ON bbs_moderator_complaint (reporter_id);
CREATE INDEX IF NOT EXISTS idx_mc_status ON bbs_moderator_complaint (status);

-- ----------------------------
-- Table: bbs_featured_recommendation（精华帖推荐审批）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_featured_recommendation (
    id              SERIAL PRIMARY KEY,
    article_id      integer NOT NULL,
    recommender_id  integer NOT NULL,
    label_id        integer DEFAULT NULL,
    status          varchar(20) NOT NULL DEFAULT 'pending',
    reviewer_id     integer DEFAULT NULL,
    review_remark   varchar(500) DEFAULT NULL,
    review_time     varchar(20) DEFAULT NULL,
    create_time     varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fr_article ON bbs_featured_recommendation (article_id);
CREATE INDEX IF NOT EXISTS idx_fr_status ON bbs_featured_recommendation (status);

-- ----------------------------
-- Table: bbs_moderator_reward_cancel（版主履职奖励取消记录）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_moderator_reward_cancel (
    id          SERIAL PRIMARY KEY,
    ym          varchar(7) NOT NULL,
    user_id     integer NOT NULL,
    operator_id integer DEFAULT NULL,
    remark      varchar(500) DEFAULT NULL,
    create_time varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mrc_ym ON bbs_moderator_reward_cancel (ym);
CREATE UNIQUE INDEX IF NOT EXISTS uk_mrc_ym_user ON bbs_moderator_reward_cancel (ym, user_id);

-- ----------------------------
-- 重置序列，使后续自增从正确值开始
-- 仅当序列当前值小于 max(id) 时才更新
-- ----------------------------
SELECT setval('bbs_user_id_seq', GREATEST(nextval('bbs_user_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_user)));
SELECT setval('bbs_article_label_label_id_seq', GREATEST(nextval('bbs_article_label_label_id_seq'), (SELECT COALESCE(max(label_id), 0) FROM bbs_article_label)));
SELECT setval('bbs_sensitive_word_id_seq', GREATEST(nextval('bbs_sensitive_word_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_sensitive_word)));
SELECT setval('bbs_dict_id_seq', GREATEST(nextval('bbs_dict_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_dict)));

-- ----------------------------
-- Table: bbs_audit_log（关键操作审计日志）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_audit_log (
    id             BIGSERIAL PRIMARY KEY,
    operator_id    integer,
    operator_name  varchar(64),
    module         varchar(32) NOT NULL,
    action         varchar(64) NOT NULL,
    target_type    varchar(32),
    target_id      varchar(128),
    before_json    text,
    after_json     text,
    request_method varchar(10),
    request_path   varchar(500),
    ip             varchar(64),
    user_agent     varchar(500),
    result         varchar(16) NOT NULL DEFAULT 'success',
    error_msg      varchar(1000),
    create_time    varchar(20) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_create_time ON bbs_audit_log (create_time);
CREATE INDEX IF NOT EXISTS idx_audit_module_time ON bbs_audit_log (module, create_time);
CREATE INDEX IF NOT EXISTS idx_audit_operator_time ON bbs_audit_log (operator_id, create_time);
CREATE INDEX IF NOT EXISTS idx_audit_result_time ON bbs_audit_log (result, create_time);
CREATE INDEX IF NOT EXISTS idx_audit_target ON bbs_audit_log (target_type, target_id);

COMMENT ON TABLE bbs_audit_log IS '关键操作审计日志';
COMMENT ON COLUMN bbs_audit_log.operator_id IS '操作人用户ID；系统任务可为空';
COMMENT ON COLUMN bbs_audit_log.operator_name IS '操作人用户名（冗余快照）';
COMMENT ON COLUMN bbs_audit_log.module IS '业务模块：ARTICLE/REPORT/...';
COMMENT ON COLUMN bbs_audit_log.action IS '操作：ARTICLE_AUDIT/REPORT_REVIEW/...';
COMMENT ON COLUMN bbs_audit_log.target_type IS '目标类型：ARTICLE/USER/...';
COMMENT ON COLUMN bbs_audit_log.target_id IS '目标ID；批量操作存 ALL 或逗号分隔';
COMMENT ON COLUMN bbs_audit_log.before_json IS '变更前快照（已脱敏）';
COMMENT ON COLUMN bbs_audit_log.after_json IS '变更后快照（已脱敏）';
COMMENT ON COLUMN bbs_audit_log.request_method IS 'HTTP 方法';
COMMENT ON COLUMN bbs_audit_log.request_path IS '请求路径（含 context-path）';
COMMENT ON COLUMN bbs_audit_log.ip IS '客户端 IP';
COMMENT ON COLUMN bbs_audit_log.user_agent IS 'User-Agent';
COMMENT ON COLUMN bbs_audit_log.result IS '结果：success/fail';
COMMENT ON COLUMN bbs_audit_log.error_msg IS '失败原因';
COMMENT ON COLUMN bbs_audit_log.create_time IS '创建时间 yyyy-MM-dd HH:mm:ss';

-- ----------------------------
-- Table: bbs_permission_group（权限组）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_permission_group (
    id              BIGSERIAL PRIMARY KEY,
    group_code      varchar(64) NOT NULL UNIQUE,
    group_name      varchar(100) NOT NULL,
    description     varchar(500),
    scope_type      varchar(20) NOT NULL DEFAULT 'ADMIN',
    data_scope_type varchar(20) NOT NULL DEFAULT 'ROLE_DEFAULT',
    base_role       varchar(20),
    enabled         smallint NOT NULL DEFAULT 1,
    sort_order      integer NOT NULL DEFAULT 0,
    create_by       varchar(64),
    create_time     varchar(20),
    update_by       varchar(64),
    update_time     varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_permission_group_enabled ON bbs_permission_group (enabled);

CREATE TABLE IF NOT EXISTS bbs_permission_group_item (
    id              BIGSERIAL PRIMARY KEY,
    group_id        integer NOT NULL,
    permission_code varchar(64) NOT NULL,
    effect          varchar(10) NOT NULL DEFAULT 'grant',
    create_time     varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_group_permission
    ON bbs_permission_group_item (group_id, permission_code);
CREATE INDEX IF NOT EXISTS idx_pgi_code ON bbs_permission_group_item (permission_code);

CREATE TABLE IF NOT EXISTS bbs_user_permission_group (
    id          BIGSERIAL PRIMARY KEY,
    user_id     integer NOT NULL,
    group_id    integer NOT NULL,
    priority    integer NOT NULL DEFAULT 0,
    create_by   varchar(64),
    create_time varchar(20)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_permission_group
    ON bbs_user_permission_group (user_id, group_id);
CREATE INDEX IF NOT EXISTS idx_upg_user ON bbs_user_permission_group (user_id);
CREATE INDEX IF NOT EXISTS idx_upg_group ON bbs_user_permission_group (group_id);

CREATE TABLE IF NOT EXISTS bbs_permission_preset (
    id            BIGSERIAL PRIMARY KEY,
    preset_code   varchar(64) NOT NULL UNIQUE,
    preset_name   varchar(100) NOT NULL,
    snapshot_json text,
    create_by     varchar(64),
    create_time   varchar(20)
);

COMMENT ON TABLE bbs_permission_group IS '权限组';
COMMENT ON TABLE bbs_permission_group_item IS '权限组-权限点';
COMMENT ON COLUMN bbs_permission_group_item.effect IS '效果：grant（授予）/revoke（收回）';
COMMENT ON TABLE bbs_user_permission_group IS '用户-权限组';
COMMENT ON TABLE bbs_permission_preset IS '权限预设方案';

-- ----------------------------
-- Table: bbs_schema_version（迁移版本追踪）
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_schema_version (
    version     varchar(50) PRIMARY KEY,
    description varchar(255),
    applied_at  varchar(20) NOT NULL
);

-- @migration: v050-p2-level-achievement P2 等级、成就与勋章
-- 目标：可配置等级（8×3=24）、成就规则、用户成就、勋章 SVG。
-- 幂等：CREATE TABLE IF NOT EXISTS；ON CONFLICT DO NOTHING。
CREATE TABLE IF NOT EXISTS bbs_level_profile (
    id              SERIAL PRIMARY KEY,
    profile_name    varchar(100) NOT NULL,
    major_count     integer NOT NULL DEFAULT 8,
    minor_per_major integer NOT NULL DEFAULT 3,
    status          smallint NOT NULL DEFAULT 1,
    create_time     varchar(20),
    update_time     varchar(20)
);

CREATE TABLE IF NOT EXISTS bbs_level_item (
    id               SERIAL PRIMARY KEY,
    profile_id       integer NOT NULL,
    level_index      integer NOT NULL,
    major_index      integer NOT NULL,
    major_name       varchar(50) NOT NULL,
    minor_index      integer NOT NULL,
    display_name     varchar(50) NOT NULL,
    points_threshold integer NOT NULL DEFAULT 0,
    icon_key         varchar(50),
    color_token      varchar(30),
    enabled          smallint NOT NULL DEFAULT 1,
    CONSTRAINT uk_level_profile_index UNIQUE (profile_id, level_index)
);

CREATE TABLE IF NOT EXISTS bbs_achievement (
    id               SERIAL PRIMARY KEY,
    achievement_code varchar(50) NOT NULL UNIQUE,
    achievement_name varchar(100) NOT NULL,
    description      varchar(500),
    icon_svg         text,
    category         varchar(50) DEFAULT '成长',
    rule_json        text,
    rule_logic       varchar(20) DEFAULT 'ALL',
    preset           smallint DEFAULT 0,
    enabled          smallint DEFAULT 1,
    sort_order       integer DEFAULT 0,
    create_time      varchar(20),
    update_time      varchar(20)
);

CREATE TABLE IF NOT EXISTS bbs_user_achievement (
    id             SERIAL PRIMARY KEY,
    user_id        integer NOT NULL,
    achievement_id integer NOT NULL,
    progress_json  text,
    unlocked       smallint NOT NULL DEFAULT 0,
    unlocked_at    varchar(20),
    source         varchar(30),
    CONSTRAINT uk_user_achievement UNIQUE (user_id, achievement_id)
);

INSERT INTO bbs_level_profile (id, profile_name, major_count, minor_per_major, status, create_time, update_time)
VALUES (1, '默认等级体系', 8, 3, 1, '2026-11-13 00:00:00', '2026-11-13 00:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO bbs_level_item (id, profile_id, level_index, major_index, major_name, minor_index, display_name, points_threshold, icon_key, color_token, enabled) VALUES
(1,1,1,1,'学徒工',1,'学徒工-1',0,'level-1','green',1),(2,1,2,1,'学徒工',2,'学徒工-2',100,'level-1','green',1),(3,1,3,1,'学徒工',3,'学徒工-3',200,'level-1','green',1),
(4,1,4,2,'初级工',1,'初级工-1',300,'level-2','teal',1),(5,1,5,2,'初级工',2,'初级工-2',400,'level-2','teal',1),(6,1,6,2,'初级工',3,'初级工-3',500,'level-2','teal',1),
(7,1,7,3,'中级工',1,'中级工-1',600,'level-3','blue',1),(8,1,8,3,'中级工',2,'中级工-2',700,'level-3','blue',1),(9,1,9,3,'中级工',3,'中级工-3',800,'level-3','blue',1),
(10,1,10,4,'高级工',1,'高级工-1',900,'level-4','indigo',1),(11,1,11,4,'高级工',2,'高级工-2',1000,'level-4','indigo',1),(12,1,12,4,'高级工',3,'高级工-3',1100,'level-4','indigo',1),
(13,1,13,5,'技师',1,'技师-1',1200,'level-5','purple',1),(14,1,14,5,'技师',2,'技师-2',1300,'level-5','purple',1),(15,1,15,5,'技师',3,'技师-3',1400,'level-5','purple',1),
(16,1,16,6,'高级技师',1,'高级技师-1',1500,'level-6','amber',1),(17,1,17,6,'高级技师',2,'高级技师-2',1600,'level-6','amber',1),(18,1,18,6,'高级技师',3,'高级技师-3',1700,'level-6','amber',1),
(19,1,19,7,'特级技师',1,'特级技师-1',1800,'level-7','orange',1),(20,1,20,7,'特级技师',2,'特级技师-2',1900,'level-7','orange',1),(21,1,21,7,'特级技师',3,'特级技师-3',2000,'level-7','orange',1),
(22,1,22,8,'首席技师',1,'首席技师-1',2100,'level-8','red',1),(23,1,23,8,'首席技师',2,'首席技师-2',2200,'level-8','red',1),(24,1,24,8,'首席技师',3,'首席技师-3',2300,'level-8','red',1)
ON CONFLICT (id) DO NOTHING;

-- 等级方案/等级项使用了显式 id 写入种子；PostgreSQL 的 SERIAL 不会随显式 id 推进，
-- 必须在这里（种子插入之后）同步，否则后续方案/等级项的自增插入会撞上已存在的 id。
SELECT setval('bbs_level_profile_id_seq', GREATEST(nextval('bbs_level_profile_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_level_profile)));
SELECT setval('bbs_level_item_id_seq', GREATEST(nextval('bbs_level_item_id_seq'), (SELECT COALESCE(max(id), 0) FROM bbs_level_item)));

INSERT INTO bbs_achievement (achievement_code, achievement_name, description, icon_svg, category, rule_json, rule_logic, preset, enabled, sort_order, create_time, update_time) VALUES
('first_post', '初来乍到', '发布第一篇帖子', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#1f6f5c" stroke="#fff" stroke-width="6"/><circle cx="128" cy="112" r="28" fill="none" stroke="#fff" stroke-width="4"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":1}]}', 'ALL', 1, 1, 10, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('writer_50', '笔耕不辍', '累计发布 50 篇帖子', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#2563eb" stroke="#fff" stroke-width="6"/><path d="M100 96 H156 M100 120 H156 M100 144 H136" stroke="#fff" stroke-width="6" stroke-linecap="round"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":50}]}', 'ALL', 1, 1, 20, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('popular_100', '人气新星', '累计获得 100 个赞', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#d97706" stroke="#fff" stroke-width="6"/><path d="M128 84 L138 108 L164 110 L144 126 L150 152 L128 138 L106 152 L112 126 L92 110 L118 108 Z" fill="#fff"/></svg>', '人气', '{"logic":"ALL","conditions":[{"metric":"like_received","op":">=","value":100}]}', 'ALL', 1, 1, 30, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('favorite_50', '收藏达人', '累计被收藏 50 次', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#7c3aed" stroke="#fff" stroke-width="6"/><path d="M112 80 H144 V152 L128 142 L112 152 Z" fill="#fff"/></svg>', '人气', '{"logic":"ALL","conditions":[{"metric":"favorite_received","op":">=","value":50}]}', 'ALL', 1, 1, 40, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('featured_5', '精华作者', '累计 5 篇帖子被评为精华', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#ca8a04" stroke="#fff" stroke-width="6"/><path d="M128 78 L138 104 L166 106 L144 124 L151 152 L128 136 L105 152 L112 124 L90 106 L118 104 Z" fill="#fff"/></svg>', '内容', '{"logic":"ALL","conditions":[{"metric":"featured_count","op":">=","value":5}]}', 'ALL', 1, 1, 50, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('adopt_10', '答疑专家', '累计被采纳 10 次', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#0891b2" stroke="#fff" stroke-width="6"/><path d="M100 100 H156 V128 H128 L112 144 V128 H100 Z" fill="#fff"/></svg>', '内容', '{"logic":"ALL","conditions":[{"metric":"adopted_count","op":">=","value":10}]}', 'ALL', 1, 1, 60, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('login_30', '全勤标兵', '累计登录 30 天', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#059669" stroke="#fff" stroke-width="6"/><rect x="96" y="88" width="64" height="56" rx="6" fill="none" stroke="#fff" stroke-width="5"/><path d="M96 104 H160" stroke="#fff" stroke-width="5"/></svg>', '活跃', '{"logic":"ALL","conditions":[{"metric":"login_days","op":">=","value":30}]}', 'ALL', 1, 1, 70, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('level_24', '满级工匠', '达到最高等级', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#dc2626" stroke="#fff" stroke-width="6"/><path d="M96 128 L112 144 L160 96" fill="none" stroke="#fff" stroke-width="8" stroke-linecap="round"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"level_index","op":">=","value":24}]}', 'ALL', 1, 1, 80, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('allround_10_50_100', '三好学生', '发帖 10、回复 50、获赞 100', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#4f46e5" stroke="#fff" stroke-width="6"/><path d="M128 80 L134 100 L154 100 L138 112 L144 132 L128 120 L112 132 L118 112 L102 100 L122 100 Z" fill="#fff"/></svg>', '综合', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":10},{"metric":"reply_count","op":">=","value":50},{"metric":"like_received","op":">=","value":100}]}', 'ALL', 1, 1, 90, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('allround_any', '全能达人', '发帖 50 或精华 10 或积分 5000', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#be123c" stroke="#fff" stroke-width="6"/><circle cx="128" cy="112" r="30" fill="none" stroke="#fff" stroke-width="5"/><path d="M128 94 L132 106 L144 106 L134 114 L138 126 L128 118 L118 126 L122 114 L112 106 L124 106 Z" fill="#fff"/></svg>', '综合', '{"logic":"ANY","groups":[{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":50}]},{"logic":"ALL","conditions":[{"metric":"featured_count","op":">=","value":10}]},{"logic":"ALL","conditions":[{"metric":"points_total","op":">=","value":5000}]}]}', 'ANY', 1, 1, 100, '2026-11-13 00:00:00', '2026-11-13 00:00:00')
ON CONFLICT (achievement_code) DO NOTHING;

ALTER TABLE bbs_article ADD COLUMN IF NOT EXISTS featured_time varchar(20);
ALTER TABLE bbs_featured_recommendation ADD COLUMN IF NOT EXISTS featured_reason varchar(500);
ALTER TABLE bbs_featured_recommendation ADD COLUMN IF NOT EXISTS public_start_time varchar(20);
ALTER TABLE bbs_featured_recommendation ADD COLUMN IF NOT EXISTS public_end_time varchar(20);
ALTER TABLE bbs_featured_recommendation ADD COLUMN IF NOT EXISTS public_status varchar(20) DEFAULT 'NONE';

-- @migration: v051-p2-featured-public P2 精华公示配置
-- 幂等：WHERE NOT EXISTS。
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark, dict_key)
SELECT 'featured_public_enabled', '1', '精华公示开关', 20, '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), '1=启用公示，0=终审后直接发布', 'featured_public_enabled'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_key = 'featured_public_enabled');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark, dict_key)
SELECT 'featured_public_days', '5', '精华公示工作日数', 21, '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), '默认 5 个工作日', 'featured_public_days'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_key = 'featured_public_days');
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark, dict_key)
SELECT 'featured_showcase_count', '3', '精华展示卡数量', 22, '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), '首页展示卡数量', 'featured_showcase_count'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_key = 'featured_showcase_count');

-- @migration: v052-p3-operation-cycle 年度运营周期
-- 幂等：CREATE TABLE IF NOT EXISTS；ADD COLUMN IF NOT EXISTS；ON CONFLICT DO NOTHING。
CREATE TABLE IF NOT EXISTS bbs_operation_cycle (
    id          SERIAL PRIMARY KEY,
    cycle_code  varchar(50) NOT NULL UNIQUE,
    cycle_name  varchar(100) NOT NULL,
    start_date  varchar(10) NOT NULL,
    end_date    varchar(10) NOT NULL,
    status      smallint NOT NULL DEFAULT 0,
    create_time varchar(20),
    update_time varchar(20)
);

CREATE TABLE IF NOT EXISTS bbs_cycle_config (
    id           SERIAL PRIMARY KEY,
    cycle_id     integer NOT NULL,
    config_key   varchar(100) NOT NULL,
    config_value varchar(2000),
    CONSTRAINT uk_cycle_config UNIQUE (cycle_id, config_key)
);

ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS cycle_id integer;
CREATE INDEX IF NOT EXISTS idx_points_cycle ON bbs_points_log (cycle_id, create_time);

INSERT INTO bbs_operation_cycle (cycle_code, cycle_name, start_date, end_date, status, create_time, update_time)
VALUES ('CYCLE2026', '2026 年度运营周期', '2026-01-01', '2026-12-31', 1, '2026-11-13 00:00:00', '2026-11-13 00:00:00')
ON CONFLICT (cycle_code) DO NOTHING;

UPDATE bbs_points_log p
SET cycle_id = c.id
FROM bbs_operation_cycle c
WHERE c.status = 1
  AND p.cycle_id IS NULL
  AND p.create_time >= c.start_date
  AND p.create_time < c.end_date || ' 23:59:59';

-- @migration: v053-p3-reminder 待办提醒
-- 幂等：CREATE TABLE IF NOT EXISTS；ON CONFLICT DO NOTHING。
CREATE TABLE IF NOT EXISTS bbs_reminder_rule (
    id                SERIAL PRIMARY KEY,
    rule_code         varchar(50) NOT NULL UNIQUE,
    rule_name         varchar(100) NOT NULL,
    event_type        varchar(50) NOT NULL,
    permission_code   varchar(64) NOT NULL,
    scope_type        varchar(20) NOT NULL DEFAULT 'ALL',
    enabled           smallint NOT NULL DEFAULT 1,
    delay_minutes     integer DEFAULT 10,
    repeat_minutes    integer DEFAULT 60,
    max_per_day       integer DEFAULT 8,
    title_template    varchar(255),
    content_template  varchar(500),
    sort_order        integer DEFAULT 0,
    create_time       varchar(20),
    update_time       varchar(20)
);

CREATE TABLE IF NOT EXISTS bbs_reminder_log (
    id          SERIAL PRIMARY KEY,
    rule_id     integer NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id   integer NOT NULL DEFAULT 0,
    user_id     integer NOT NULL,
    batch_key   varchar(20) NOT NULL,
    sent_at     varchar(20),
    CONSTRAINT uk_reminder_log UNIQUE (rule_id, target_type, target_id, user_id, batch_key)
);

INSERT INTO bbs_reminder_rule (rule_code, rule_name, event_type, permission_code, scope_type, enabled, delay_minutes, repeat_minutes, max_per_day, title_template, sort_order, create_time, update_time) VALUES
('article_pending', '待审核帖子', 'article_pending', 'article:review', 'BOARD', 1, 10, 60, 8, '您有 {count} 条待审核帖子', 10, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('report_pending', '待审核举报', 'report_pending', 'report:review', 'BOARD', 1, 10, 60, 8, '您有 {count} 条待审核举报', 20, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('appeal_pending', '待审核申诉', 'appeal_pending', 'appeal:review', 'ALL', 1, 30, 120, 8, '您有 {count} 条待审核申诉', 30, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('featured_pending', '精华待终审', 'featured_pending', 'featured:review', 'BOARD', 1, 30, 120, 8, '您有 {count} 条精华推荐待终审', 40, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('adopt_pending', '采纳待审批', 'adopt_pending', 'adopt:review', 'BOARD', 1, 30, 120, 8, '您有 {count} 条采纳待审批', 50, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('complaint_pending', '版主投诉待处理', 'complaint_pending', 'moderator:complaint:review', 'ALL', 1, 10, 60, 8, '您有 {count} 条版主投诉待处理', 60, '2026-11-13 00:00:00', '2026-11-13 00:00:00')
ON CONFLICT (rule_code) DO NOTHING;

-- @migration: v054-p3-org-import 组织自动导入
-- 幂等：CREATE TABLE IF NOT EXISTS；ADD COLUMN IF NOT EXISTS。
CREATE TABLE IF NOT EXISTS bbs_org_import_batch (
    id            SERIAL PRIMARY KEY,
    file_name     varchar(255),
    file_hash     varchar(64),
    status        varchar(20) NOT NULL DEFAULT 'PENDING',
    total_count   integer DEFAULT 0,
    add_count     integer DEFAULT 0,
    update_count  integer DEFAULT 0,
    move_count    integer DEFAULT 0,
    disable_count integer DEFAULT 0,
    error_count   integer DEFAULT 0,
    error_json    text,
    safe_flag     smallint DEFAULT 1,
    operator_id   integer,
    start_time    varchar(20),
    end_time      varchar(20)
);

CREATE TABLE IF NOT EXISTS bbs_org_import_item (
    id        SERIAL PRIMARY KEY,
    batch_id  integer NOT NULL,
    action    varchar(20) NOT NULL,
    org_no    varchar(20) NOT NULL,
    old_json  text,
    new_json  text,
    status    varchar(20) DEFAULT 'PENDING',
    message   varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_org_import_batch ON bbs_org_import_item (batch_id);

ALTER TABLE bbs_sa_org ADD COLUMN IF NOT EXISTS source varchar(20) DEFAULT 'manual';
ALTER TABLE bbs_sa_org ADD COLUMN IF NOT EXISTS import_batch_id integer;

-- @migration: v055-comment-reply-thumb 评论/回帖点赞
-- 目标：评论与楼中楼回复支持点赞；新增统一互动表 bbs_thumb。
-- 幂等：ADD COLUMN IF NOT EXISTS；CREATE TABLE IF NOT EXISTS。
ALTER TABLE bbs_comment ADD COLUMN IF NOT EXISTS comment_good_num integer NOT NULL DEFAULT 0;
ALTER TABLE bbs_reply ADD COLUMN IF NOT EXISTS reply_good_num integer NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS bbs_thumb (
    id          SERIAL PRIMARY KEY,
    user_id     integer NOT NULL,
    target_type varchar(20) NOT NULL,
    target_id   integer NOT NULL,
    create_time varchar(20),
    CONSTRAINT uk_thumb UNIQUE (user_id, target_type, target_id)
);
CREATE INDEX IF NOT EXISTS idx_thumb_target ON bbs_thumb (target_type, target_id);

-- @migration: v056-legacy-ranking-category 内江供电公司本部改为可配置分类
-- 目标：原“本部”虚拟分类（id=0）落为真实分类 legacy_root，成员可像其它分类一样增删。
-- 幂等：ON CONFLICT DO NOTHING。
INSERT INTO bbs_ranking_category (category_code, category_name, description, sort_order, enabled, create_time, update_time)
VALUES ('legacy_root', '内江供电公司本部', '原“本部”排名，已改为普通分类，可自由配置成员与含下级', 0, 1,
        to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS'))
ON CONFLICT (category_code) DO NOTHING;

INSERT INTO bbs_ranking_category_org (category_id, org_no, include_descendants, sort_order, create_time)
SELECT c.id, o.org_no, 1, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_ranking_category c
JOIN bbs_sa_org o ON o.p_org_no = '51404' AND o.is_delete = 0
WHERE c.category_code = 'legacy_root'
  AND NOT EXISTS (
    SELECT 1 FROM bbs_ranking_category_org x
    WHERE x.category_id = c.id AND x.org_no = o.org_no
  );

-- @migration: v057-level-icon-custom 等级图标自定义
-- 目标：等级项支持自定义图标（图形/颜色/SVG），管理员可在等级管理中绘制或选择。
-- 幂等：ADD COLUMN IF NOT EXISTS。
ALTER TABLE bbs_level_item ADD COLUMN IF NOT EXISTS icon_svg text;

-- @migration: v058-svg-asset SVG 素材库
-- 目标：勋章与等级图标共用绘制成果；管理端独立 SVG 工作台保存/引用。
-- 幂等：CREATE TABLE IF NOT EXISTS。
CREATE TABLE IF NOT EXISTS bbs_svg_asset (
    id          SERIAL PRIMARY KEY,
    asset_name  varchar(100) NOT NULL,
    category    varchar(50) NOT NULL DEFAULT 'other',
    svg         text NOT NULL,
    created_by  varchar(50),
    create_time varchar(20),
    update_time varchar(20)
);
CREATE INDEX IF NOT EXISTS idx_svg_asset_category ON bbs_svg_asset (category);
-- @migration: v059-level-schemes 等级方案系统
-- 目标：等级项补充徽章与定位说明；内置两套 21 级方案（参考《大千智荟论坛等级积分对照表》）。
-- 幂等：ADD COLUMN IF NOT EXISTS；WHERE NOT EXISTS。
ALTER TABLE bbs_level_item ADD COLUMN IF NOT EXISTS badge_name varchar(50);
ALTER TABLE bbs_level_item ADD COLUMN IF NOT EXISTS description varchar(500);

INSERT INTO bbs_level_profile (profile_name, major_count, minor_per_major, status, create_time, update_time)
SELECT '方案一 · 智荟成长', 7, 3, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_level_profile WHERE profile_name='方案一 · 智荟成长');
INSERT INTO bbs_level_profile (profile_name, major_count, minor_per_major, status, create_time, update_time)
SELECT '方案二 · 电网匠心', 7, 3, 0, to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_level_profile WHERE profile_name='方案二 · 电网匠心');

INSERT INTO bbs_level_item (profile_id, level_index, major_index, major_name, minor_index, display_name, points_threshold, icon_key, color_token, enabled, badge_name, description)
SELECT p.id, t.lv, CEIL(t.lv::numeric/3), t.major_name, ((t.lv-1)%3)+1, t.title, (t.lv-1)*100, NULL, NULL, 1, NULLIF(t.badge,''), t.descr
FROM bbs_level_profile p JOIN ( SELECT 1 AS lv, '初级工' AS major_name, '智荟新芽·观' AS title, '新芽徽章' AS badge, '刚注册入场，多看多浏览，熟悉论坛规则与业务分享帖' AS descr UNION ALL SELECT 2 AS lv, '初级工' AS major_name, '智荟新芽·悟' AS title, '' AS badge, '常浏览、正常回帖交流，开始留意业务中的小问题' AS descr UNION ALL SELECT 3 AS lv, '初级工' AS major_name, '智荟新芽·行' AS title, '' AS badge, '主动发言讨论，敢于抛出疑问、交换想法' AS descr UNION ALL SELECT 4 AS lv, '中级工' AS major_name, '拾光新锐·观' AS title, '耕耘徽章' AS badge, '开始自主发帖，分享岗位实操经验、台账与小技巧' AS descr UNION ALL SELECT 5 AS lv, '中级工' AS major_name, '拾光新锐·悟' AS title, '' AS badge, '积极建言献策，参与痛点研讨，提炼优化思路' AS descr UNION ALL SELECT 6 AS lv, '中级工' AS major_name, '拾光新锐·行' AS title, '' AS badge, '稳定输出内容，主动答疑，带动板块交流氛围' AS descr UNION ALL SELECT 7 AS lv, '高级工' AS major_name, '精锐先锋·观' AS title, '创见徽章' AS badge, '聚焦业务堵点，尝试提出改良办法、减负小方案' AS descr UNION ALL SELECT 8 AS lv, '高级工' AS major_name, '精锐先锋·悟' AS title, '' AS badge, '擅长拆解难题，多次给出高质量解决方案' AS descr UNION ALL SELECT 9 AS lv, '高级工' AS major_name, '精锐先锋·行' AS title, '' AS badge, '产出精华帖，内容可跨班组复用推广' AS descr UNION ALL SELECT 10 AS lv, '技师' AS major_name, '破局造浪人·观' AS title, '砺新徽章' AS badge, '持续产出高质量创意，挖掘QC创新点子' AS descr UNION ALL SELECT 11 AS lv, '技师' AS major_name, '破局造浪人·悟' AS title, '' AS badge, '深挖隐性痛点，牵头组织专题研讨碰撞思路' AS descr UNION ALL SELECT 12 AS lv, '技师' AS major_name, '破局造浪人·行' AS title, '' AS badge, '牵头共创，把零散想法打磨成可落地方案' AS descr UNION ALL SELECT 13 AS lv, '高级技师' AS major_name, '知灯引路人·观' AS title, '传知徽章' AS badge, '创新成果落地见效，复盘沉淀可复制经验' AS descr UNION ALL SELECT 14 AS lv, '高级技师' AS major_name, '知灯引路人·悟' AS title, '' AS badge, '帮带新人，指导梳理创意、打磨帖子' AS descr UNION ALL SELECT 15 AS lv, '高级技师' AS major_name, '知灯引路人·行' AS title, '' AS badge, '参与精华帖初评，协助甄别优质内容' AS descr UNION ALL SELECT 16 AS lv, '特级技师' AS major_name, '云衢领航者·观' AS title, '星引徽章' AS badge, '跨专业协同，指导多个创新项目推进' AS descr UNION ALL SELECT 17 AS lv, '特级技师' AS major_name, '云衢领航者·悟' AS title, '' AS badge, '创新成果在单位内部推广，形成标准化指引' AS descr UNION ALL SELECT 18 AS lv, '特级技师' AS major_name, '云衢领航者·行' AS title, '' AS badge, '参与论坛生态建设，提出高质量运营优化建议' AS descr UNION ALL SELECT 19 AS lv, '首席技师' AS major_name, '大千执炬者·观' AS title, '大成徽章' AS badge, '产出标杆型创新成果，在专业领域示范引领' AS descr UNION ALL SELECT 20 AS lv, '首席技师' AS major_name, '大千执炬者·悟' AS title, '' AS badge, '成果具备跨地市、跨单位借鉴价值' AS descr UNION ALL SELECT 21 AS lv, '首席技师' AS major_name, '大千执炬者·行' AS title, '' AS badge, '论坛最高荣誉，引领创新方向，是平台标杆人物' AS descr ) t
WHERE p.profile_name='方案一 · 智荟成长'
  AND NOT EXISTS (SELECT 1 FROM bbs_level_item x WHERE x.profile_id=p.id AND x.level_index=t.lv);

INSERT INTO bbs_level_item (profile_id, level_index, major_index, major_name, minor_index, display_name, points_threshold, icon_key, color_token, enabled, badge_name, description)
SELECT p.id, t.lv, CEIL(t.lv::numeric/3), t.major_name, ((t.lv-1)%3)+1, t.title, (t.lv-1)*100, NULL, NULL, 1, NULLIF(t.badge,''), t.descr
FROM bbs_level_profile p JOIN ( SELECT 1 AS lv, '初级工' AS major_name, '银线学徒·观' AS title, '线芯徽章' AS badge, '刚注册入场，多看多浏览，熟悉论坛规则与业务分享帖' AS descr UNION ALL SELECT 2 AS lv, '初级工' AS major_name, '银线学徒·悟' AS title, '' AS badge, '常浏览、正常回帖交流，开始留意业务中的小问题' AS descr UNION ALL SELECT 3 AS lv, '初级工' AS major_name, '银线学徒·行' AS title, '' AS badge, '主动发言讨论，敢于抛出疑问、交换想法' AS descr UNION ALL SELECT 4 AS lv, '中级工' AS major_name, '线网知客·观' AS title, '杆塔徽章' AS badge, '开始自主发帖，分享岗位实操经验、台账与小技巧' AS descr UNION ALL SELECT 5 AS lv, '中级工' AS major_name, '线网知客·悟' AS title, '' AS badge, '积极建言献策，参与痛点研讨，提炼优化思路' AS descr UNION ALL SELECT 6 AS lv, '中级工' AS major_name, '线网知客·行' AS title, '' AS badge, '稳定输出内容，主动答疑，带动板块交流氛围' AS descr UNION ALL SELECT 7 AS lv, '高级工' AS major_name, '送电革新者·观' AS title, '革新徽章' AS badge, '聚焦业务堵点，尝试提出改良办法、减负小方案' AS descr UNION ALL SELECT 8 AS lv, '高级工' AS major_name, '送电革新者·悟' AS title, '' AS badge, '擅长拆解难题，多次给出高质量解决方案' AS descr UNION ALL SELECT 9 AS lv, '高级工' AS major_name, '送电革新者·行' AS title, '' AS badge, '产出精华帖，内容可跨班组复用推广' AS descr UNION ALL SELECT 10 AS lv, '技师' AS major_name, '电光造梦师·观' AS title, '电芒徽章' AS badge, '持续产出高质量创意，挖掘QC创新点子' AS descr UNION ALL SELECT 11 AS lv, '技师' AS major_name, '电光造梦师·悟' AS title, '' AS badge, '深挖隐性痛点，牵头组织专题研讨碰撞思路' AS descr UNION ALL SELECT 12 AS lv, '技师' AS major_name, '电光造梦师·行' AS title, '' AS badge, '牵头共创，把零散想法打磨成可落地方案' AS descr UNION ALL SELECT 13 AS lv, '高级技师' AS major_name, '熔光传习师·观' AS title, '传灯徽章' AS badge, '创新成果落地见效，复盘沉淀可复制经验' AS descr UNION ALL SELECT 14 AS lv, '高级技师' AS major_name, '熔光传习师·悟' AS title, '' AS badge, '帮带新人，指导梳理创意、打磨帖子' AS descr UNION ALL SELECT 15 AS lv, '高级技师' AS major_name, '熔光传习师·行' AS title, '' AS badge, '参与精华帖初评，协助甄别优质内容' AS descr UNION ALL SELECT 16 AS lv, '特级技师' AS major_name, '经纬领航者·观' AS title, '母线徽章' AS badge, '跨专业协同，指导多个创新项目推进' AS descr UNION ALL SELECT 17 AS lv, '特级技师' AS major_name, '经纬领航者·悟' AS title, '' AS badge, '创新成果在单位内部推广，形成标准化指引' AS descr UNION ALL SELECT 18 AS lv, '特级技师' AS major_name, '经纬领航者·行' AS title, '' AS badge, '参与论坛生态建设，提出高质量运营优化建议' AS descr UNION ALL SELECT 19 AS lv, '首席技师' AS major_name, '万伏执炬人·观' AS title, '金炬徽章' AS badge, '产出标杆型创新成果，在专业领域示范引领' AS descr UNION ALL SELECT 20 AS lv, '首席技师' AS major_name, '万伏执炬人·悟' AS title, '' AS badge, '成果具备跨地市、跨单位借鉴价值' AS descr UNION ALL SELECT 21 AS lv, '首席技师' AS major_name, '万伏执炬人·行' AS title, '' AS badge, '论坛最高荣誉，引领创新方向，是平台标杆人物' AS descr ) t
WHERE p.profile_name='方案二 · 电网匠心'
  AND NOT EXISTS (SELECT 1 FROM bbs_level_item x WHERE x.profile_id=p.id AND x.level_index=t.lv);
-- @migration: v060-level-major-labels-and-assets 方案标签修正 + 内置图标入库
-- 目标：大阶段显示论坛头衔前缀（不再显示对标技能层级）；已在用的等级图标、成就勋章写入素材库作为默认数据。
-- 幂等：WHERE NOT EXISTS / NOT LIKE '对标 %'。
UPDATE bbs_level_item i SET major_name = CASE major_index WHEN 1 THEN '智荟新芽' WHEN 2 THEN '拾光新锐' WHEN 3 THEN '精锐先锋' WHEN 4 THEN '破局造浪人' WHEN 5 THEN '知灯引路人' WHEN 6 THEN '云衢领航者' WHEN 7 THEN '大千执炬者' ELSE major_name END,
    description = CONCAT('对标 ', CASE major_index WHEN 1 THEN '初级工' WHEN 2 THEN '中级工' WHEN 3 THEN '高级工' WHEN 4 THEN '技师' WHEN 5 THEN '高级技师' WHEN 6 THEN '特级技师' WHEN 7 THEN '首席技师' ELSE major_name END, i.minor_index, '档；', COALESCE(i.description, ''))
FROM bbs_level_profile p
WHERE p.id = i.profile_id AND p.profile_name = '方案一 · 智荟成长' AND i.major_index BETWEEN 1 AND 7
  AND (i.description IS NULL OR i.description NOT LIKE '对标 %');

UPDATE bbs_level_item i SET major_name = CASE major_index WHEN 1 THEN '银线学徒' WHEN 2 THEN '线网知客' WHEN 3 THEN '送电革新者' WHEN 4 THEN '电光造梦师' WHEN 5 THEN '熔光传习师' WHEN 6 THEN '经纬领航者' WHEN 7 THEN '万伏执炬人' ELSE major_name END,
    description = CONCAT('对标 ', CASE major_index WHEN 1 THEN '初级工' WHEN 2 THEN '中级工' WHEN 3 THEN '高级工' WHEN 4 THEN '技师' WHEN 5 THEN '高级技师' WHEN 6 THEN '特级技师' WHEN 7 THEN '首席技师' ELSE major_name END, i.minor_index, '档；', COALESCE(i.description, ''))
FROM bbs_level_profile p
WHERE p.id = i.profile_id AND p.profile_name = '方案二 · 电网匠心' AND i.major_index BETWEEN 1 AND 7
  AND (i.description IS NULL OR i.description NOT LIKE '对标 %');

-- 指数型升级门槛：按“当前大等级每升一小档所需积分”累计（与产品对照表一致）
UPDATE bbs_level_item i
SET points_threshold = CASE i.level_index
  WHEN 1 THEN 0    WHEN 2 THEN 20   WHEN 3 THEN 40
  WHEN 4 THEN 60   WHEN 5 THEN 90   WHEN 6 THEN 120
  WHEN 7 THEN 150  WHEN 8 THEN 200  WHEN 9 THEN 250
  WHEN 10 THEN 300 WHEN 11 THEN 400 WHEN 12 THEN 500
  WHEN 13 THEN 600 WHEN 14 THEN 700 WHEN 15 THEN 800
  WHEN 16 THEN 900 WHEN 17 THEN 1100 WHEN 18 THEN 1300
  WHEN 19 THEN 1500 WHEN 20 THEN 1700 WHEN 21 THEN 1900
  ELSE i.points_threshold END
FROM bbs_level_profile p
WHERE p.id = i.profile_id
  AND p.profile_name IN ('方案一 · 智荟成长', '方案二 · 电网匠心')
  AND i.level_index BETWEEN 1 AND 21;
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 星', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#16a34a" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 星');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 月', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#0284c7" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 月');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 日', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><circle cx="12" cy="12" r="4.7" fill="#d97706"/><g stroke="#d97706" stroke-width="2.1" stroke-linecap="round"><line x1="12" y1="1.7" x2="12" y2="4.7"/><line x1="12" y1="19.3" x2="12" y2="22.3"/><line x1="1.7" y1="12" x2="4.7" y2="12"/><line x1="19.3" y1="12" x2="22.3" y2="12"/><line x1="4.6" y1="4.6" x2="6.8" y2="6.8"/><line x1="17.2" y1="17.2" x2="19.4" y2="19.4"/><line x1="19.4" y1="4.6" x2="17.2" y2="6.8"/><line x1="6.8" y1="17.2" x2="4.6" y2="19.4"/></g></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 日');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#ca8a04" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#ca8a04"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+星', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#7c3aed" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#7c3aed"/></g><g transform="translate(14.2 0) scale(0.4)"><path fill="#f59e0b" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+星');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+月', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#4f46e5" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#4f46e5"/></g><g transform="translate(14.2 0) scale(0.4)"><path fill="#f59e0b" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+月');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+日', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#db2777" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#db2777"/></g><g transform="translate(14.2 0) scale(0.4)"><circle cx="12" cy="12" r="4.7" fill="#f59e0b"/><g stroke="#f59e0b" stroke-width="2.1" stroke-linecap="round"><line x1="12" y1="1.7" x2="12" y2="4.7"/><line x1="12" y1="19.3" x2="12" y2="22.3"/><line x1="1.7" y1="12" x2="4.7" y2="12"/><line x1="19.3" y1="12" x2="22.3" y2="12"/><line x1="4.6" y1="4.6" x2="6.8" y2="6.8"/><line x1="17.2" y1="17.2" x2="19.4" y2="19.4"/><line x1="19.4" y1="4.6" x2="17.2" y2="6.8"/><line x1="6.8" y1="17.2" x2="4.6" y2="19.4"/></g></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+日');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 至尊冠', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#dc2626" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#dc2626"/><circle cx="12" cy="10.4" r="1.85" fill="#fff" opacity="0.92"/><circle cx="12" cy="10.4" r="0.85" fill="#dc2626" opacity="0.55"/></g><g transform="translate(15.4 1) scale(0.34)"><path fill="#fbbf24" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 至尊冠');
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT CONCAT('勋章 · ', a.achievement_name), 'medal', a.icon_svg, '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS'), to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
FROM bbs_achievement a
WHERE a.icon_svg IS NOT NULL AND a.icon_svg <> ''
  AND NOT EXISTS (SELECT 1 FROM bbs_svg_asset s WHERE s.asset_name = CONCAT('勋章 · ', a.achievement_name));