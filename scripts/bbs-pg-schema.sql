-- ============================================
-- BBS PostgreSQL Schema
-- Converted from MySQL (bbs.sql) to PostgreSQL
-- ============================================

-- 禁用外键检查以便重建表
SET session_replication_role = 'replica';

-- ============================================
-- Table: bbs_admin
-- ============================================
DROP TABLE IF EXISTS bbs_admin;
CREATE TABLE bbs_admin (
    id          SERIAL PRIMARY KEY,
    username    varchar(20),
    password    varchar(255),
    portrait    varchar(255),
    is_alive    integer DEFAULT 0,
    is_delete   integer DEFAULT 0,
    create_time varchar(20)
);

-- ============================================
-- Table: bbs_area
-- ============================================
DROP TABLE IF EXISTS bbs_area;
CREATE TABLE bbs_area (
    area_id     SERIAL PRIMARY KEY,
    image_title varchar(20),
    image_url   varchar(255)
);

-- ============================================
-- Table: bbs_article
-- ============================================
DROP TABLE IF EXISTS bbs_article;
CREATE TABLE bbs_article (
    article_id          SERIAL PRIMARY KEY,
    article_label_id    integer,
    article_author      varchar(20),
    article_title       varchar(30),
    article_summary     varchar(255),
    article_type_id     integer,
    article_content     text,
    article_content_html text,
    article_image       text,
    user_id             integer,
    article_good_num    integer DEFAULT 0,
    article_view_num    integer DEFAULT 0,
    article_community_id integer,
    create_time         varchar(20),
    recommend           smallint,
    enable              integer DEFAULT 0,
    is_delete           integer DEFAULT 0
);

-- ============================================
-- Table: bbs_article_file
-- ============================================
DROP TABLE IF EXISTS bbs_article_file;
CREATE TABLE bbs_article_file (
    file_id     SERIAL PRIMARY KEY,
    file_name   varchar(255),
    file_type   varchar(255),
    file_path   varchar(255),
    article_id  integer
);

-- ============================================
-- Table: bbs_article_label
-- ============================================
DROP TABLE IF EXISTS bbs_article_label;
CREATE TABLE bbs_article_label (
    label_id    SERIAL PRIMARY KEY,
    label_name  varchar(10),
    enabled     smallint
);

-- ============================================
-- Table: bbs_article_type
-- ============================================
DROP TABLE IF EXISTS bbs_article_type;
CREATE TABLE bbs_article_type (
    type_id     SERIAL PRIMARY KEY,
    type_name   varchar(10)
);

-- ============================================
-- Table: bbs_article_user
-- ============================================
DROP TABLE IF EXISTS bbs_article_user;
CREATE TABLE bbs_article_user (
    id          SERIAL PRIMARY KEY,
    user_id     integer,
    article_id  integer,
    create_time varchar(20)
);

-- ============================================
-- Table: bbs_comment
-- ============================================
DROP TABLE IF EXISTS bbs_comment;
CREATE TABLE bbs_comment (
    comment_id          SERIAL PRIMARY KEY,
    comment_content     varchar(255),
    comment_article_id  integer,
    comment_user_id     integer,
    comment_time        varchar(20),
    enable              integer DEFAULT 1,
    is_delete           integer DEFAULT 0
);

-- ============================================
-- Table: bbs_community
-- ============================================
DROP TABLE IF EXISTS bbs_community;
CREATE TABLE bbs_community (
    community_id        SERIAL PRIMARY KEY,
    community_name      varchar(20),
    community_introduce varchar(255),
    community_image     varchar(255),
    community_user_num  integer DEFAULT 0,
    create_user_id      integer,
    create_time         varchar(20),
    enable              integer DEFAULT 1,
    is_delete           integer DEFAULT 0
);

-- ============================================
-- Table: bbs_community_user
-- ============================================
DROP TABLE IF EXISTS bbs_community_user;
CREATE TABLE bbs_community_user (
    id            SERIAL PRIMARY KEY,
    community_id  integer,
    user_id       integer
);

-- ============================================
-- Table: bbs_dict
-- ============================================
DROP TABLE IF EXISTS bbs_dict;
CREATE TABLE bbs_dict (
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

-- ============================================
-- Table: bbs_fans
-- ============================================
DROP TABLE IF EXISTS bbs_fans;
CREATE TABLE bbs_fans (
    id            SERIAL PRIMARY KEY,
    user_id       integer,
    attention_id  integer,
    create_time   varchar(20)
);

-- ============================================
-- Table: bbs_inventory
-- ============================================
DROP TABLE IF EXISTS bbs_inventory;
CREATE TABLE bbs_inventory (
    id          SERIAL PRIMARY KEY,
    area        varchar(255),
    category    varchar(255),
    type        varchar(255),
    name        varchar(255),
    content     text,
    snumber     varchar(20),
    department  varchar(255),
    time        varchar(20)
);

-- ============================================
-- Table: bbs_reply
-- ============================================
DROP TABLE IF EXISTS bbs_reply;
CREATE TABLE bbs_reply (
    reply_id        SERIAL PRIMARY KEY,
    reply_content   varchar(255),
    reply_to_user_id integer,
    comment_id      integer,
    reply_user_id   integer,
    reply_time      varchar(20) NOT NULL,
    enable          integer DEFAULT 1,
    is_delete       integer DEFAULT 0
);

-- ============================================
-- Table: bbs_sa_org
-- ============================================
DROP TABLE IF EXISTS bbs_sa_org;
CREATE TABLE bbs_sa_org (
    id          integer PRIMARY KEY,
    org_no      varchar(20) NOT NULL,
    org_name    varchar(255),
    p_org_no    varchar(20),
    org_tree    varchar(255),
    is_delete   smallint
);

-- ============================================
-- Table: bbs_sensitive_word
-- ============================================
DROP TABLE IF EXISTS bbs_sensitive_word;
CREATE TABLE bbs_sensitive_word (
    id      SERIAL PRIMARY KEY,
    keyword varchar(255)
);

-- ============================================
-- Table: bbs_slideshow
-- ============================================
DROP TABLE IF EXISTS bbs_slideshow;
CREATE TABLE bbs_slideshow (
    slideshow_id  SERIAL PRIMARY KEY,
    image_url     varchar(255),
    successive    integer,
    create_time   varchar(20)
);

-- ============================================
-- Table: bbs_user
-- ============================================
DROP TABLE IF EXISTS bbs_user;
CREATE TABLE bbs_user (
    id          SERIAL PRIMARY KEY,
    username    varchar(20),
    password    varchar(255),
    nickname    varchar(20),
    portrait    varchar(255),
    gender      varchar(2),
    introduce   varchar(255),
    city        varchar(100),
    fans        integer DEFAULT 0,
    attention   integer,
    good        integer,
    is_alive    integer DEFAULT 0,
    is_delete   integer DEFAULT 0,
    create_time varchar(20),
    phone       varchar(20),
    org_no      varchar(20),
    user_type   varchar(20)
);

-- 重新启用外键检查
SET session_replication_role = 'origin';

-- ============================================
-- 导入数据（INSERT 语句）
-- 从 MySQL 导出数据：去掉反引号和 MySQL 特定语法即可
-- 数据文件脚本: bbs-pg-data.sql
-- ============================================
