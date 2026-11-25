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
    article_view_num     integer DEFAULT 0,    create_time          varchar(20),
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
    selection_type      varchar(10) NOT NULL DEFAULT 'include',
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
    mask_enabled smallint NOT NULL DEFAULT 1,
    category varchar(16) NOT NULL DEFAULT 'sensitive',
    CONSTRAINT uk_sensitive_word_keyword UNIQUE (keyword)
);

-- ----------------------------
-- Table: bbs_sensitive_hit
-- ----------------------------
CREATE TABLE IF NOT EXISTS bbs_sensitive_hit (
    id          BIGSERIAL PRIMARY KEY,
    user_id     integer NOT NULL,
    target_type varchar(16) NOT NULL,
    target_id   integer NOT NULL,
    label_id    integer,
    hit_words   varchar(500),
    hit_count   integer DEFAULT 0,
    create_time varchar(20),
    CONSTRAINT uk_sensitive_hit_target UNIQUE (target_type, target_id)
);
CREATE INDEX IF NOT EXISTS idx_sensitive_hit_user_time ON bbs_sensitive_hit (user_id, create_time);
CREATE INDEX IF NOT EXISTS idx_sensitive_hit_time ON bbs_sensitive_hit (create_time);

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
                 '信息通信分公司', '综合服务中心')
  OR o.org_name LIKE '%供电服务指挥中心%'
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
(19, 'featured_final_review_mode', 'MODERATOR_FINAL', '精华终审模式', 19, '系统', '2026-11-13 00:00:00', 'MODERATOR_FINAL版主终审即通过（默认） / ADMIN_FINAL管理员终审', 'featured_final_review_mode')
ON CONFLICT (id) DO NOTHING;

-- ----------------------------
-- 系统配置（使用反馈联系方式）
-- ----------------------------
-- @migration: v078-home-hero-images 首页 Hero 影像带可配置
-- 背景：影像带 4 张图此前写死在 bbs-ui 打包资源里，换一张图要改代码重新发版（2026-11-25 领导要求后台可配）。
-- 处理：新增 config_group=home 的 5 个配置：
--       hero_image_1..4 = 各格图片地址（后台「系统配置」上传后自动写入；留空则该格使用内置默认图）；
--       hero_caption    = 右下角来源标注（留空用默认文案）。
-- 幂等：WHERE NOT EXISTS 按 config_key 判断。图片地址建议用 /bbs-server/files/common/upload/... 相对路径。
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'hero_image_1', '', 'Hero 第1格（左一）', 'home', 'image', 10, '留空=内置默认图；建议横图 ≥1600×900', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'hero_image_1');
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'hero_image_2', '', 'Hero 第2格', 'home', 'image', 20, '留空=内置默认图；建议横图 ≥1600×900', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'hero_image_2');
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'hero_image_3', '', 'Hero 第3格（移动端隐藏）', 'home', 'image', 30, '留空=内置默认图；建议横图 ≥1600×900', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'hero_image_3');
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'hero_image_4', '', 'Hero 第4格（最右）', 'home', 'image', 40, '留空=内置默认图；当前默认图是竖幅大楼，换成横幅也不会变形（自动 cover 裁切）', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'hero_image_4');
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'hero_caption', '俯瞰内江·圣水寺·隆昌石牌坊·国网大楼', 'Hero 右下角来源标注', 'home', 'text', 50, '留空则隐藏该行；各图用“·”分隔', '系统', to_char(now(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'hero_caption');