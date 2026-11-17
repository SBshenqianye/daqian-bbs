-- ============================================
-- BBS 数据库初始化脚本 - MySQL 版
-- 用于首次部署时自动建表 + 基础数据
-- 不包含测试数据
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table: bbs_admin
-- ----------------------------
DROP TABLE IF EXISTS `bbs_admin`;
CREATE TABLE `bbs_admin` (
  `id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '管理员id',
  `username`    varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员名称',
  `password`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员密码',
  `portrait`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员头像',
  `is_alive`    int(11) NULL DEFAULT 0 COMMENT '是否禁用',
  `is_delete`   int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_area
-- ----------------------------
DROP TABLE IF EXISTS `bbs_area`;
CREATE TABLE `bbs_area` (
  `area_id`     int(11) NOT NULL AUTO_INCREMENT,
  `image_title` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片上的文字叙述',
  `image_url`   varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片路径',
  PRIMARY KEY (`area_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article`;
CREATE TABLE `bbs_article` (
  `article_id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '文章id',
  `article_label_id`    int(11) NULL DEFAULT NULL COMMENT '标签id',
  `article_author`      varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章作者',
  `article_title`       varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章标题',
  `article_summary`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章摘要',
  `article_type_id`     int(11) NULL DEFAULT NULL COMMENT '文章类型',
  `article_content`     longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章内容',
  `article_content_html` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章内容html',
  `article_image`       text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章中的图片',
  `user_id`             int(11) NULL DEFAULT NULL COMMENT '发布文章的用户的id',
  `article_good_num`    int(11) NULL DEFAULT 0 COMMENT '文章获赞数量',
  `article_view_num`    int(11) NULL DEFAULT 0 COMMENT '文章的浏览量',
  `article_community_id` int(11) NULL DEFAULT NULL COMMENT '文章所属的社区id',
  `create_time`         varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章发布的时间',
  `recommend`           tinyint(1) UNSIGNED ZEROFILL NULL DEFAULT NULL COMMENT '是否推荐该文章',
  `enable`              int(11) NULL DEFAULT 0 COMMENT '是否审核通过',
  `is_delete`           int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `is_featured`         tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为精华帖(0=否,1=是)',
  `is_hot_bonus`        tinyint(1) NULL DEFAULT 0 COMMENT '热度奖励是否已发放(0=否,1=是)',
  `last_reply_time`     varchar(20) DEFAULT NULL COMMENT '最后回复时间',
  `last_activity_time`  varchar(20) DEFAULT NULL COMMENT '最后活跃时间',
  `comment_num`         int(11) NOT NULL DEFAULT 0 COMMENT '评论+回复数',
  `favorite_num`        int(11) NOT NULL DEFAULT 0 COMMENT '收藏数',
  `share_num`           int(11) NOT NULL DEFAULT 0 COMMENT '分享数',
  `is_anonymous`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名发布',
  PRIMARY KEY (`article_id`) USING BTREE,
  KEY `idx_article_featured_time` (`is_featured`, `create_time`),
  KEY `idx_article_last_reply` (`last_reply_time`),
  KEY `idx_article_last_activity` (`last_activity_time`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article_file
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_file`;
CREATE TABLE `bbs_article_file` (
  `file_id`    int(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `file_name`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件名称',
  `file_type`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件类型',
  `file_path`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件存储地址',
  `article_id` int(20) NULL DEFAULT NULL COMMENT '关联文章id',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article_label
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_label`;
CREATE TABLE `bbs_article_label` (
  `label_id`   int(11) NOT NULL AUTO_INCREMENT COMMENT '文章标签的id',
  `label_name`  varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标签名称',
  `enabled`     tinyint(4) NULL DEFAULT NULL COMMENT '标签是否禁用',
  `icon`        varchar(50) DEFAULT NULL COMMENT '标签图标',
  `description` varchar(200) DEFAULT NULL COMMENT '标签描述',
  `label_type`  varchar(20) NOT NULL DEFAULT 'normal' COMMENT '标签用途类型(normal普通/question问题求助/suggestion建议反馈)',
  `allow_anonymous` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许匿名',
  `no_points`   tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否不计分',
  PRIMARY KEY (`label_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article_type
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_type`;
CREATE TABLE `bbs_article_type` (
  `type_id`   int(11) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章类型名称',
  PRIMARY KEY (`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_user`;
CREATE TABLE `bbs_article_user` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `user_id`     int(11) NULL DEFAULT NULL COMMENT '用户id',
  `article_id`  int(11) NULL DEFAULT NULL COMMENT '文章id',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_article_user_favorite` (`user_id`, `article_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_article_like
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_like`;
CREATE TABLE `bbs_article_like` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `user_id`     int(11) NOT NULL,
  `article_id`  int(11) NOT NULL,
  `create_time` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_article_like` (`user_id`, `article_id`),
  KEY `idx_article_like_article` (`article_id`, `create_time`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '帖子点赞';

-- ----------------------------
-- Table: bbs_comment
-- ----------------------------
DROP TABLE IF EXISTS `bbs_comment`;
CREATE TABLE `bbs_comment` (
  `comment_id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '评论的id',
  `comment_content`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '评论的内容',
  `comment_article_id`  int(11) NULL DEFAULT NULL COMMENT '评论的文章id',
  `comment_user_id`     int(11) NULL DEFAULT NULL COMMENT '评论的用户id',
  `comment_time`        varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论时间',
  `enable`              int(11) NULL DEFAULT 1 COMMENT '是否被审核通过',
  `is_delete`           int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `adopt_status`        tinyint(1) NULL DEFAULT 0 COMMENT '采纳审批状态(0=未采纳,1=待审批,2=已确认,3=已拒绝)',
  `is_anonymous`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `comment_good_num`    int(11) NOT NULL DEFAULT 0 COMMENT '评论点赞数',
  PRIMARY KEY (`comment_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_community
-- ----------------------------
DROP TABLE IF EXISTS `bbs_community`;
CREATE TABLE `bbs_community` (
  `community_id`         int(11) NOT NULL AUTO_INCREMENT COMMENT '社区id',
  `community_name`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区名称',
  `community_introduce`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区介绍',
  `community_image`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区的照片',
  `community_user_num`   int(11) NULL DEFAULT 0 COMMENT '社区用户数量',
  `create_user_id`       int(11) NULL DEFAULT NULL COMMENT '创建改社区的用户id',
  `create_time`          varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区的创建时间',
  `enable`               int(11) NULL DEFAULT 1 COMMENT '是否审核通过',
  `is_delete`            int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`community_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_community_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_community_user`;
CREATE TABLE `bbs_community_user` (
  `id`            int(11) NOT NULL AUTO_INCREMENT,
  `community_id`  int(11) NULL DEFAULT NULL COMMENT '社区的id',
  `user_id`       int(11) NULL DEFAULT NULL COMMENT '社区用户的id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_dict
-- ----------------------------
DROP TABLE IF EXISTS `bbs_dict`;
CREATE TABLE `bbs_dict` (
  `id`          int(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `dict_type`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典类型-键',
  `dict_value`  varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '值',
  `dict_label`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '中文翻译',
  `dict_sort`   int(20) NULL DEFAULT NULL COMMENT '排序序号',
  `create_by`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建时间',
  `update_by`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新时间',
  `remark`      varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  `dict_key`    varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '键(违规类型标识)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_system_config
-- ----------------------------
DROP TABLE IF EXISTS `bbs_system_config`;
CREATE TABLE `bbs_system_config` (
  `id`           int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `config_key`   varchar(100) NOT NULL COMMENT '配置键',
  `config_value` longtext COMMENT '配置值（支持任意长度文本/JSON）',
  `config_label` varchar(255) DEFAULT NULL COMMENT '配置名称/说明',
  `config_group` varchar(100) DEFAULT 'default' COMMENT '配置分组（如 contact/points/system）',
  `config_type`  varchar(20) DEFAULT 'text' COMMENT '输入类型（text/textarea/json）',
  `sort_order`   int(11) DEFAULT 0 COMMENT '排序序号',
  `remark`       varchar(500) DEFAULT NULL COMMENT '备注说明',
  `create_by`    varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time`  varchar(20) DEFAULT NULL COMMENT '创建时间',
  `update_by`    varchar(50) DEFAULT NULL COMMENT '修改人',
  `update_time`  varchar(20) DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_config_key` (`config_key`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- Table: bbs_fans
-- ----------------------------
DROP TABLE IF EXISTS `bbs_fans`;
CREATE TABLE `bbs_fans` (
  `id`            int(11) NOT NULL AUTO_INCREMENT,
  `user_id`       int(11) NULL DEFAULT NULL COMMENT '用户id',
  `attention_id`  int(11) NULL DEFAULT NULL COMMENT '关注用户的id',
  `create_time`   varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '关注的时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_inventory
-- ----------------------------
DROP TABLE IF EXISTS `bbs_inventory`;
CREATE TABLE `bbs_inventory` (
  `id`         int(11) NOT NULL AUTO_INCREMENT,
  `area`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地区',
  `category`   varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类别',
  `type`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型',
  `name`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `content`    longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '内容',
  `snumber`    varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '编号',
  `department` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位',
  `time`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_reply
-- ----------------------------
DROP TABLE IF EXISTS `bbs_reply`;
CREATE TABLE `bbs_reply` (
  `reply_id`         int(11) NOT NULL AUTO_INCREMENT COMMENT '回复的id',
  `reply_content`    varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回复的具体内容',
  `reply_to_user_id` int(11) NULL DEFAULT NULL COMMENT '回复的对象的Id',
  `comment_id`       int(11) NULL DEFAULT NULL COMMENT '回复评论的ID',
  `reply_user_id`    int(11) NULL DEFAULT NULL COMMENT '回复的用户id',
  `reply_time`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '回复时间',
  `enable`           int(11) NULL DEFAULT 1 COMMENT '是否被审核通过',
  `is_delete`        int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `is_adopted`       tinyint(1) NULL DEFAULT 0 COMMENT '是否被采纳(0=否,1=是)',
  `adopt_status`     tinyint(1) NULL DEFAULT 0 COMMENT '采纳审批状态(0=未采纳,1=待审批,2=已确认,3=已拒绝)',
  `is_anonymous`     tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名',
  `reply_good_num`   int(11) NOT NULL DEFAULT 0 COMMENT '回复点赞数',
  PRIMARY KEY (`reply_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_sa_org
-- ----------------------------
DROP TABLE IF EXISTS `bbs_sa_org`;
CREATE TABLE `bbs_sa_org` (
  `id`                 int(11) NOT NULL COMMENT '唯一标识',
  `org_no`             varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '单位编号',
  `org_name`           varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位名称',
  `p_org_no`           varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父级单位编号',
  `org_tree`           varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位树',
  `is_delete`          tinyint(1) NULL DEFAULT NULL COMMENT '是否删除',
  `is_ranking_selected` tinyint(1) NULL DEFAULT 0 COMMENT '是否参与排名(0=否,1=是)',
  `is_display_selected` tinyint(1) NULL DEFAULT 1 COMMENT '是否显示在用户前台(0=否,1=是)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_ranking_category（排行榜分类）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_ranking_category`;
CREATE TABLE `bbs_ranking_category` (
  `id`            int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_code` varchar(50) NOT NULL COMMENT '分类编码',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `description`   varchar(500) DEFAULT NULL COMMENT '分类说明',
  `sort_order`    int(11) NOT NULL DEFAULT 0 COMMENT '排序序号',
  `enabled`       tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用(0=否,1=是)',
  `create_time`   varchar(20) DEFAULT NULL COMMENT '创建时间',
  `update_time`   varchar(20) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ranking_category_code` (`category_code`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '排行榜分类';

-- ----------------------------
-- Table: bbs_ranking_category_org（排行榜分类成员）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_ranking_category_org`;
CREATE TABLE `bbs_ranking_category_org` (
  `id`                  int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_id`         int(11) NOT NULL COMMENT '分类ID',
  `org_no`              varchar(20) NOT NULL COMMENT '组织编号',
  `include_descendants` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否包含下级(0=否,1=是)',
  `sort_order`          int(11) NOT NULL DEFAULT 0 COMMENT '排序序号',
  `create_time`         varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_ranking_category_org` (`category_id`, `org_no`),
  KEY `idx_ranking_category` (`category_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '排行榜分类成员';

-- ----------------------------
-- Table: bbs_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `bbs_sensitive_word`;
CREATE TABLE `bbs_sensitive_word` (
  `id`      int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `keyword` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '敏感词',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sensitive_word_keyword` (`keyword`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_slideshow
-- ----------------------------
DROP TABLE IF EXISTS `bbs_slideshow`;
CREATE TABLE `bbs_slideshow` (
  `slideshow_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '轮播图的id',
  `image_url`    varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '轮播图的地址',
  `successive`   int(11) NULL DEFAULT NULL COMMENT '顺序（数字越大，越靠前）',
  `create_time`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`slideshow_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table: bbs_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_user`;
CREATE TABLE `bbs_user` (
  `id`             int(11) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户登录名',
  `password`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户密码',
  `nickname`       varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `portrait`       varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户头像',
  `gender`         varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户性别',
  `introduce`      varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户介绍',
  `city`           varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户城市',
  `fans`           int(11) NULL DEFAULT 0 COMMENT '粉丝数量',
  `attention`      int(11) NULL DEFAULT NULL COMMENT '关注数量',
  `good`           int(11) NULL DEFAULT NULL COMMENT '获赞数量',
  `is_alive`       int(11) NULL DEFAULT 0 COMMENT '是否被禁',
  `is_delete`      int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`    varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '添加时间',
  `phone`          varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电话',
  `org_no`         varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位编号',
  `user_type`      varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '1' COMMENT '用户类型（1：普通用户，2：管理员，3：超级管理员，4：版主）',
  `primary_group_id` int(11) NULL DEFAULT NULL COMMENT '主权限组ID（必须指向系统角色组）',
  `personnel_id`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '人员编号（Excel B列）',
  `id_card`        varchar(18) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `is_first_login` tinyint(1) NULL DEFAULT 1 COMMENT '是否首次登录(1=需改密码,0=已修改)',
  `post_restricted` tinyint(1) NULL DEFAULT 0 COMMENT '是否限制发帖(0=否,1=是)',
  `post_restricted_until` varchar(20) NULL DEFAULT NULL COMMENT '限制发帖截止时间',
  `is_test_account` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否测试账号(0=否,1=是)，测试账号不参与排行榜统计',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bbs_user_username` (`username`),
  UNIQUE KEY `uk_bbs_user_personnel_id` (`personnel_id`),
  UNIQUE KEY `uk_bbs_user_id_card` (`id_card`),
  KEY `idx_user_primary_group` (`primary_group_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ============================================
-- 基础数据
-- ============================================

-- ----------------------------
-- 超级管理员（密码由 DatabaseInitializer 启动时用 BCrypt 加密写入）
-- ----------------------------
INSERT INTO `bbs_user` (`id`, `username`, `password`, `nickname`, `gender`, `city`, `fans`, `attention`, `good`, `is_alive`, `is_delete`, `create_time`, `org_no`, `user_type`, `is_first_login`)
VALUES (1, 'asiayak', '$2a$10$hpTQLGhUicOwSbSgLa2kyuQYMXhnWCZhi/CR/v6cyc2JcNOs2rk3O', '超级管理员', '1', '河北省-秦皇岛市', 0, 0, 0, 0, 0, '2026-06-26 00:00:00', '51404', '3', 0);

-- ----------------------------
-- 文章标签
-- ----------------------------
INSERT INTO `bbs_article_label` (`label_id`, `label_name`, `enabled`, `icon`, `description`, `label_type`) VALUES
(1, '技术交流', 0, 'thumb_up', '', 'normal'),
(2, '求助问答', 1, 'help', '', 'question'),
(3, '资源共享', 0, 'folder_open', '', 'normal'),
(4, '建议反馈', 1, 'lightbulb', '提交建议并被采纳获得+5积分', 'suggestion');

-- ----------------------------
-- 组织机构（国网四川内江供电公司）
-- ----------------------------
INSERT INTO `bbs_sa_org` (`id`, `org_no`, `org_name`, `p_org_no`, `org_tree`, `is_delete`) VALUES
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
(32, '514042812', '国网四川内江隆昌云顶供电所', '5140428', '51101|51404|5140428|514042812', 0);

-- ----------------------------
-- 排行榜分类（“内江供电公司本部”为系统内置 LEGACY，不落库）
-- ----------------------------
INSERT INTO `bbs_ranking_category` (`id`, `category_code`, `category_name`, `description`, `sort_order`, `enabled`, `create_time`) VALUES
(1, 'county_units', '县级供电企业', '县级供电公司/供电中心，含下级供电所', 20, 1, '2026-11-13 00:00:00'),
(2, 'company_units', '公司所属各单位', '内江公司根节点下直属单位，含下级', 30, 1, '2026-11-13 00:00:00'),
(3, 'support_units', '地市支撑机构', '变电运检、输电运检、客户服务等支撑机构', 40, 1, '2026-11-13 00:00:00'),
(4, 'collective_units', '集体企业及相关单位', '集体企业及相关单位', 50, 1, '2026-11-13 00:00:00');

INSERT IGNORE INTO `bbs_ranking_category_org` (`category_id`, `org_no`, `include_descendants`, `sort_order`, `create_time`)
SELECT 1, o.org_no, 1, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_sa_org` o
WHERE o.is_delete = 0
  AND (
    o.p_org_no = (SELECT org_no FROM `bbs_sa_org` WHERE org_name = '县级供电企业' AND is_delete = 0 LIMIT 1)
    OR (o.p_org_no = '51404' AND (o.org_name LIKE '%供电分公司%' OR o.org_name = '市区供电中心'))
  );

INSERT IGNORE INTO `bbs_ranking_category_org` (`category_id`, `org_no`, `include_descendants`, `sort_order`, `create_time`)
SELECT 2, o.org_no, 1, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_sa_org` o
WHERE o.is_delete = 0
  AND o.p_org_no = (SELECT org_no FROM `bbs_sa_org` WHERE org_name = '公司所属各单位' AND is_delete = 0 LIMIT 1);

INSERT IGNORE INTO `bbs_ranking_category_org` (`category_id`, `org_no`, `include_descendants`, `sort_order`, `create_time`)
SELECT 3, o.org_no, 1, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_sa_org` o
WHERE o.is_delete = 0 AND (
  o.org_name IN ('变电运检中心', '输电运检中心', '客户服务中心', '经济技术研究所',
                 '信息通信分公司', '综合服务中心', '供电服务指挥中心')
  OR o.org_name LIKE '%项目管理中心%'
);

INSERT IGNORE INTO `bbs_ranking_category_org` (`category_id`, `org_no`, `include_descendants`, `sort_order`, `create_time`)
SELECT 4, o.org_no, 1, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_sa_org` o
WHERE o.is_delete = 0 AND o.p_org_no = '5140403' AND (
  o.org_name LIKE '%星原%' OR o.org_name LIKE '%三新%'
  OR o.org_name LIKE '%乐山城电%' OR o.org_name LIKE '%东祥%'
  OR o.org_name LIKE '%西星%'
);

-- ----------------------------
-- 数据字典
-- ----------------------------
INSERT INTO `bbs_dict` (`id`, `dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`, `dict_key`) VALUES
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
(19, 'featured_final_review_mode', 'ADMIN_FINAL', '精华终审模式', 19, '系统', '2026-11-13 00:00:00', 'ADMIN_FINAL管理员终审 / MODERATOR_FINAL版主终审即通过', 'featured_final_review_mode');

-- ----------------------------
-- 系统配置（使用反馈联系方式）
-- ----------------------------
INSERT INTO `bbs_system_config` (`config_key`, `config_value`, `config_label`, `config_group`, `config_type`, `sort_order`, `remark`, `create_by`, `create_time`)
SELECT 'feedback_contact', '{\"name\":\"\",\"email\":\"\"}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', '2026-07-15 00:00:00'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_system_config` WHERE `config_key` = 'feedback_contact');

-- ----------------------------
-- Table: bbs_points_log（积分调整日志）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_points_log`;
CREATE TABLE `bbs_points_log` (
  `id`               int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`          int(11) NOT NULL COMMENT '用户ID',
  `points_change`    int(11) NOT NULL COMMENT '积分变动（正数加分，负数扣分）',
  `reason`           varchar(500) DEFAULT NULL COMMENT '调整原因',
  `related_type`     varchar(20) DEFAULT NULL COMMENT '关联类型（article/comment/reply/manual/undo）',
  `related_id`       int(11) DEFAULT NULL COMMENT '关联ID（帖子ID/评论ID/回复ID）',
  `operator_id`      int(11) DEFAULT NULL COMMENT '操作人ID（管理员）',
  `create_time`      varchar(20) DEFAULT NULL COMMENT '创建时间',
  `is_reversed`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已被撤销(0=否,1=是)',
  `reversed_by`      int(11) DEFAULT NULL COMMENT '被哪条撤销记录撤销（记录ID）',
  `reversing_record` int(11) DEFAULT NULL COMMENT '此记录撤销了哪条原始记录（记录ID）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_points_log_user_id` (`user_id`),
  INDEX `idx_points_log_is_reversed` (`is_reversed`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '积分调整日志';

-- ----------------------------
-- Table: bbs_points_alert（积分预警去重）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_points_alert`;
CREATE TABLE `bbs_points_alert` (
  `id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     int(11) NOT NULL COMMENT '用户ID',
  `alert_type`  varchar(30) NOT NULL COMMENT '预警类型',
  `period`      varchar(10) NOT NULL COMMENT '统计周期(yyyy-MM)',
  `threshold`   int(11) NOT NULL COMMENT '触发阈值',
  `points`      int(11) NOT NULL DEFAULT 0 COMMENT '当期累计扣分',
  `create_time` varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_points_alert` (`user_id`, `alert_type`, `period`, `threshold`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '积分预警去重';

-- ----------------------------
-- 敏感词
-- ----------------------------
INSERT INTO `bbs_sensitive_word` (`id`, `keyword`) VALUES
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
(33, 'testtest');

-- ----------------------------
-- Table: bbs_notification（通知表）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_notification`;
CREATE TABLE `bbs_notification` (
  `id`             int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        int(11) NOT NULL COMMENT '被通知的用户ID',
  `from_user_id`   int(11) DEFAULT NULL COMMENT '触发通知的用户ID',
  `type`           varchar(20) NOT NULL COMMENT '通知类型(reply/comment/favorite)',
  `category`       varchar(20) NOT NULL DEFAULT 'system' COMMENT '通知分类(interaction=互动消息,system=系统通知)',
  `title`          varchar(255) DEFAULT NULL COMMENT '通知标题',
  `related_type`   varchar(20) DEFAULT NULL COMMENT '关联类型(article/comment/reply)',
  `related_id`     int(11) DEFAULT NULL COMMENT '关联ID',
  `is_read`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读(0=未读,1=已读)',
  `create_time`    varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_user_id` (`user_id`),
  INDEX `idx_notification_user_read` (`user_id`, `is_read`),
  INDEX `idx_notification_user_cat_read` (`user_id`, `category`, `is_read`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '通知表';

-- ----------------------------
-- Table: bbs_schema_version（迁移版本追踪）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_schema_version`;
CREATE TABLE `bbs_schema_version` (
  `version`     varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '迁移版本标识',
  `description` varchar(255) DEFAULT NULL COMMENT '迁移描述',
  `applied_at`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '执行时间',
  PRIMARY KEY (`version`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic COMMENT = '数据库迁移版本追踪';

-- ----------------------------
-- Table: bbs_login_log（每日登录浏览记录）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_login_log`;
CREATE TABLE `bbs_login_log` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         int(11) NOT NULL COMMENT '用户ID',
  `login_date`      varchar(10) NOT NULL COMMENT '登录日期(YYYY-MM-DD)',
  `login_time`      varchar(20) DEFAULT NULL COMMENT '登录时间',
  `browse_minutes`  int(11) DEFAULT 0 COMMENT '有效浏览分钟数',
  `points_awarded`  tinyint(1) DEFAULT 0 COMMENT '是否已发积分(0=否,1=是)',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_login_log_user_date` (`user_id`, `login_date`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '每日登录浏览记录';

-- ----------------------------
-- Table: bbs_report（实名举报记录）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_report`;
CREATE TABLE `bbs_report` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reporter_id`     int(11) NOT NULL COMMENT '举报人ID',
  `target_type`     varchar(20) NOT NULL COMMENT '举报目标类型(article/comment/reply)',
  `target_id`       int(11) NOT NULL COMMENT '被举报内容ID',
  `reason`          varchar(500) DEFAULT NULL COMMENT '举报原因',
  `violation_type`  varchar(50) DEFAULT NULL COMMENT '违规类型(spam/plagiarism/illegal/attack/leak)',
  `status`          varchar(20) DEFAULT 'pending' COMMENT '状态(pending/confirmed/rejected)',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT '审核人ID',
  `review_time`     varchar(20) DEFAULT NULL COMMENT '审核时间',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT '审核备注',
  `points_awarded`  tinyint(1) DEFAULT 0 COMMENT '是否已给举报人发积分(0=否,1=是)',
  `is_anonymous`    tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否匿名举报',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_reporter` (`reporter_id`),
  INDEX `idx_report_target` (`target_type`, `target_id`),
  INDEX `idx_report_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '实名举报记录';

-- ----------------------------
-- Table: bbs_violation（违规记录）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_violation`;
CREATE TABLE `bbs_violation` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         int(11) NOT NULL COMMENT '违规用户ID',
  `violation_type`  varchar(50) NOT NULL COMMENT '违规类型(对应字典violation)',
  `points_deducted` int(11) NOT NULL COMMENT '扣减积分',
  `related_type`    varchar(20) DEFAULT NULL COMMENT '关联类型(article/comment/reply)',
  `related_id`      int(11) DEFAULT NULL COMMENT '关联ID',
  `operator_id`     int(11) NOT NULL COMMENT '操作管理员ID',
  `remark`          varchar(500) DEFAULT NULL COMMENT '备注说明',
  `status`          varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态(active=生效中/cancelled=已取消)',
  `cancel_reason`   varchar(500) DEFAULT NULL COMMENT '取消原因',
  `cancel_operator_id` int(11) DEFAULT NULL COMMENT '取消操作人ID',
  `cancel_time`     varchar(20) DEFAULT NULL COMMENT '取消时间',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_violation_user` (`user_id`),
  INDEX `idx_violation_type` (`violation_type`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '违规记录';

-- ----------------------------
-- Table: bbs_appeal（申诉记录）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_appeal`;
CREATE TABLE `bbs_appeal` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`         int(11) NOT NULL COMMENT '申诉人ID',
  `appeal_type`     varchar(20) NOT NULL COMMENT '申诉类型(violation/points/other)',
  `related_id`      int(11) DEFAULT NULL COMMENT '关联的违规/积分记录ID',
  `content`         text NOT NULL COMMENT '申诉内容',
  `status`          varchar(20) DEFAULT 'pending' COMMENT '状态(pending/accepted/rejected)',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT '审核人ID',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT '审核备注',
  `review_time`     varchar(20) DEFAULT NULL COMMENT '审核时间',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appeal_user` (`user_id`),
  INDEX `idx_appeal_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '申诉记录';

-- ----------------------------
-- Table: bbs_board_moderator（版块管理员）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_board_moderator`;
CREATE TABLE `bbs_board_moderator` (
  `id`           int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`      int(11) NOT NULL COMMENT '用户ID',
  `label_id`     int(11) NOT NULL COMMENT '关联标签(版块)ID',
  `role_type`    varchar(20) DEFAULT 'moderator' COMMENT '角色类型(moderator/admin)',
  `status`       tinyint(1) DEFAULT 1 COMMENT '状态(1=有效,0=撤销)',
  `appoint_time` varchar(20) DEFAULT NULL COMMENT '任命时间',
  `create_time`  varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_board_mod_user_label` (`user_id`, `label_id`),
  INDEX `idx_board_mod_label` (`label_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '版块管理员';

-- ----------------------------
-- Table: bbs_moderator_complaint（版主投诉）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_moderator_complaint`;
CREATE TABLE `bbs_moderator_complaint` (
  `id`            int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reporter_id`   int(11) NOT NULL COMMENT '投诉人用户ID',
  `moderator_id`  int(11) NULL DEFAULT NULL COMMENT '被投诉版主用户ID',
  `label_id`      int(11) DEFAULT NULL COMMENT '被投诉版主管理的标签ID',
  `content`       text NOT NULL COMMENT '投诉内容',
  `status`        varchar(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态(pending/accepted/rejected)',
  `reviewer_id`   int(11) DEFAULT NULL COMMENT '审核人ID',
  `review_remark` varchar(500) DEFAULT NULL COMMENT '审核备注',
  `review_time`   varchar(20) DEFAULT NULL COMMENT '审核时间',
  `create_time`   varchar(20) NOT NULL COMMENT '投诉时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mc_moderator` (`moderator_id`),
  INDEX `idx_mc_reporter` (`reporter_id`),
  INDEX `idx_mc_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '版主投诉表';

-- ----------------------------
-- Table: bbs_featured_recommendation（精华帖推荐审批）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_featured_recommendation`;
CREATE TABLE `bbs_featured_recommendation` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id`      int(11) NOT NULL COMMENT '推荐文章ID',
  `recommender_id`  int(11) NOT NULL COMMENT '推荐人(版主)ID',
  `label_id`        int(11) DEFAULT NULL COMMENT '推荐人管理的标签ID',
  `status`          varchar(20) NOT NULL DEFAULT 'pending' COMMENT '审批状态(pending/approved/rejected)',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT '审核人(总运营)ID',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT '审核备注',
  `review_time`     varchar(20) DEFAULT NULL COMMENT '审核时间',
  `create_time`     varchar(20) NOT NULL COMMENT '推荐时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_fr_article` (`article_id`),
  INDEX `idx_fr_status` (`status`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '精华帖推荐审批表';

-- ----------------------------
-- Table: bbs_moderator_reward_cancel（版主履职奖励取消记录）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_moderator_reward_cancel`;
CREATE TABLE `bbs_moderator_reward_cancel` (
  `id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ym`        varchar(7) NOT NULL COMMENT '年月(yyyy-MM)',
  `user_id`     int(11) NOT NULL COMMENT '被取消奖励的版主用户ID',
  `operator_id` int(11) DEFAULT NULL COMMENT '操作人ID',
  `remark`      varchar(500) DEFAULT NULL COMMENT '取消原因',
  `create_time` varchar(20) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_mrc_ym` (`ym`),
  UNIQUE KEY `uk_mrc_ym_user` (`ym`, `user_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '版主履职奖励取消记录';

-- ----------------------------
-- Table: bbs_audit_log（关键操作审计日志）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_audit_log`;
CREATE TABLE `bbs_audit_log` (
  `id`             bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id`    int(11) DEFAULT NULL COMMENT '操作人用户ID；系统任务可为空',
  `operator_name`  varchar(64) DEFAULT NULL COMMENT '操作人用户名（冗余快照）',
  `module`         varchar(32) NOT NULL COMMENT '业务模块：ARTICLE/REPORT/...',
  `action`         varchar(64) NOT NULL COMMENT '操作：ARTICLE_AUDIT/REPORT_REVIEW/...',
  `target_type`    varchar(32) DEFAULT NULL COMMENT '目标类型：ARTICLE/USER/...',
  `target_id`      varchar(128) DEFAULT NULL COMMENT '目标ID；批量操作存 ALL 或逗号分隔',
  `before_json`    longtext COMMENT '变更前快照（已脱敏）',
  `after_json`     longtext COMMENT '变更后快照（已脱敏）',
  `request_method` varchar(10) DEFAULT NULL COMMENT 'HTTP 方法',
  `request_path`   varchar(500) DEFAULT NULL COMMENT '请求路径（含 context-path）',
  `ip`             varchar(64) DEFAULT NULL COMMENT '客户端 IP',
  `user_agent`     varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `result`         varchar(16) NOT NULL DEFAULT 'success' COMMENT '结果：success/fail',
  `error_msg`      varchar(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time`    varchar(20) NOT NULL COMMENT '创建时间 yyyy-MM-dd HH:mm:ss',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_audit_create_time` (`create_time`),
  KEY `idx_audit_module_time` (`module`, `create_time`),
  KEY `idx_audit_operator_time` (`operator_id`, `create_time`),
  KEY `idx_audit_result_time` (`result`, `create_time`),
  KEY `idx_audit_target` (`target_type`, `target_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '关键操作审计日志';

-- ----------------------------
-- Table: bbs_permission_group（权限组）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_permission_group`;
CREATE TABLE `bbs_permission_group` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_code`      varchar(64) NOT NULL COMMENT '权限组编码',
  `group_name`      varchar(100) NOT NULL COMMENT '权限组名称',
  `description`     varchar(500) DEFAULT NULL COMMENT '描述',
  `scope_type`      varchar(20) NOT NULL DEFAULT 'ADMIN' COMMENT '范围：ADMIN/FRONT/ALL',
  `data_scope_type` varchar(20) NOT NULL DEFAULT 'ROLE_DEFAULT' COMMENT '数据范围：ROLE_DEFAULT/BOARD_MODERATOR',
  `base_role`       varchar(20) DEFAULT NULL COMMENT '角色基线：USER/MODERATOR/ADMIN/SUPER_ADMIN',
  `enabled`         tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `sort_order`      int(11) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_by`       varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  `update_by`       varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time`     varchar(20) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_permission_group_code` (`group_code`),
  KEY `idx_permission_group_enabled` (`enabled`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '权限组';

-- ----------------------------
-- Table: bbs_permission_group_item（权限组-权限点）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_permission_group_item`;
CREATE TABLE `bbs_permission_group_item` (
  `id`              int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id`        int(11) NOT NULL COMMENT '权限组ID',
  `permission_code` varchar(64) NOT NULL COMMENT '权限点编码',
  `effect`          varchar(10) NOT NULL DEFAULT 'grant' COMMENT '效果：grant（授予）/revoke（收回）',
  `create_time`     varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_group_permission` (`group_id`, `permission_code`),
  KEY `idx_pgi_code` (`permission_code`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '权限组-权限点';

-- ----------------------------
-- Table: bbs_user_permission_group（用户-权限组）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_user_permission_group`;
CREATE TABLE `bbs_user_permission_group` (
  `id`          int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`     int(11) NOT NULL COMMENT '用户ID',
  `group_id`    int(11) NOT NULL COMMENT '权限组ID',
  `priority`    int(11) NOT NULL DEFAULT 0 COMMENT '优先级（预留给后续 revoke/冲突规则）',
  `create_by`   varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_permission_group` (`user_id`, `group_id`),
  KEY `idx_upg_user` (`user_id`),
  KEY `idx_upg_group` (`group_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '用户-权限组';

-- ----------------------------
-- Table: bbs_permission_preset（权限预设方案）
-- ----------------------------
DROP TABLE IF EXISTS `bbs_permission_preset`;
CREATE TABLE `bbs_permission_preset` (
  `id`            int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `preset_code`   varchar(64) NOT NULL COMMENT '预设编码',
  `preset_name`   varchar(100) NOT NULL COMMENT '预设名称',
  `snapshot_json` longtext COMMENT '权限组快照（名称/编码/权限点）',
  `create_by`     varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time`   varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_permission_preset_code` (`preset_code`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic COMMENT = '权限预设方案';

SET FOREIGN_KEY_CHECKS = 1;

-- @migration: v050-p2-level-achievement P2 等级、成就与勋章
-- 目标：可配置等级（8×3=24）、成就规则、用户成就、勋章 SVG。
-- 幂等：CREATE TABLE IF NOT EXISTS；INSERT IGNORE；information_schema 检查。
CREATE TABLE IF NOT EXISTS `bbs_level_profile` (
  `id`              int(11) NOT NULL AUTO_INCREMENT,
  `profile_name`    varchar(100) NOT NULL,
  `major_count`     int(11) NOT NULL DEFAULT 8,
  `minor_per_major` int(11) NOT NULL DEFAULT 3,
  `status`          tinyint(1) NOT NULL DEFAULT 1,
  `create_time`     varchar(20) DEFAULT NULL,
  `update_time`     varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='等级体系';

CREATE TABLE IF NOT EXISTS `bbs_level_item` (
  `id`               int(11) NOT NULL AUTO_INCREMENT,
  `profile_id`       int(11) NOT NULL,
  `level_index`      int(11) NOT NULL,
  `major_index`      int(11) NOT NULL,
  `major_name`       varchar(50) NOT NULL,
  `minor_index`      int(11) NOT NULL,
  `display_name`     varchar(50) NOT NULL,
  `points_threshold` int(11) NOT NULL DEFAULT 0,
  `icon_key`         varchar(50) DEFAULT NULL,
  `color_token`      varchar(30) DEFAULT NULL,
  `enabled`          tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_level_profile_index` (`profile_id`, `level_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='等级项';

CREATE TABLE IF NOT EXISTS `bbs_achievement` (
  `id`               int(11) NOT NULL AUTO_INCREMENT,
  `achievement_code` varchar(50) NOT NULL,
  `achievement_name` varchar(100) NOT NULL,
  `description`      varchar(500) DEFAULT NULL,
  `icon_svg`         longtext,
  `category`         varchar(50) DEFAULT '成长',
  `rule_json`        text,
  `rule_logic`       varchar(20) DEFAULT 'ALL',
  `preset`           tinyint(1) DEFAULT 0,
  `enabled`          tinyint(1) DEFAULT 1,
  `sort_order`       int(11) DEFAULT 0,
  `create_time`      varchar(20) DEFAULT NULL,
  `update_time`      varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_achievement_code` (`achievement_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='成就';

CREATE TABLE IF NOT EXISTS `bbs_user_achievement` (
  `id`             int(11) NOT NULL AUTO_INCREMENT,
  `user_id`        int(11) NOT NULL,
  `achievement_id` int(11) NOT NULL,
  `progress_json`  text,
  `unlocked`       tinyint(1) NOT NULL DEFAULT 0,
  `unlocked_at`    varchar(20) DEFAULT NULL,
  `source`         varchar(30) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户成就';

INSERT IGNORE INTO `bbs_level_profile` (`id`, `profile_name`, `major_count`, `minor_per_major`, `status`, `create_time`, `update_time`) VALUES
(1, '默认等级体系', 8, 3, 1, '2026-11-13 00:00:00', '2026-11-13 00:00:00');

INSERT IGNORE INTO `bbs_level_item` (`id`, `profile_id`, `level_index`, `major_index`, `major_name`, `minor_index`, `display_name`, `points_threshold`, `icon_key`, `color_token`, `enabled`) VALUES
(1,1,1,1,'学徒工',1,'学徒工-1',0,'level-1','green',1),
(2,1,2,1,'学徒工',2,'学徒工-2',100,'level-1','green',1),
(3,1,3,1,'学徒工',3,'学徒工-3',200,'level-1','green',1),
(4,1,4,2,'初级工',1,'初级工-1',300,'level-2','teal',1),
(5,1,5,2,'初级工',2,'初级工-2',400,'level-2','teal',1),
(6,1,6,2,'初级工',3,'初级工-3',500,'level-2','teal',1),
(7,1,7,3,'中级工',1,'中级工-1',600,'level-3','blue',1),
(8,1,8,3,'中级工',2,'中级工-2',700,'level-3','blue',1),
(9,1,9,3,'中级工',3,'中级工-3',800,'level-3','blue',1),
(10,1,10,4,'高级工',1,'高级工-1',900,'level-4','indigo',1),
(11,1,11,4,'高级工',2,'高级工-2',1000,'level-4','indigo',1),
(12,1,12,4,'高级工',3,'高级工-3',1100,'level-4','indigo',1),
(13,1,13,5,'技师',1,'技师-1',1200,'level-5','purple',1),
(14,1,14,5,'技师',2,'技师-2',1300,'level-5','purple',1),
(15,1,15,5,'技师',3,'技师-3',1400,'level-5','purple',1),
(16,1,16,6,'高级技师',1,'高级技师-1',1500,'level-6','amber',1),
(17,1,17,6,'高级技师',2,'高级技师-2',1600,'level-6','amber',1),
(18,1,18,6,'高级技师',3,'高级技师-3',1700,'level-6','amber',1),
(19,1,19,7,'特级技师',1,'特级技师-1',1800,'level-7','orange',1),
(20,1,20,7,'特级技师',2,'特级技师-2',1900,'level-7','orange',1),
(21,1,21,7,'特级技师',3,'特级技师-3',2000,'level-7','orange',1),
(22,1,22,8,'首席技师',1,'首席技师-1',2100,'level-8','red',1),
(23,1,23,8,'首席技师',2,'首席技师-2',2200,'level-8','red',1),
(24,1,24,8,'首席技师',3,'首席技师-3',2300,'level-8','red',1);

INSERT IGNORE INTO `bbs_achievement` (`achievement_code`, `achievement_name`, `description`, `icon_svg`, `category`, `rule_json`, `rule_logic`, `preset`, `enabled`, `sort_order`, `create_time`, `update_time`) VALUES
('first_post', '初来乍到', '发布第一篇帖子', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#1f6f5c" stroke="#fff" stroke-width="6"/><circle cx="128" cy="112" r="28" fill="none" stroke="#fff" stroke-width="4"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":1}]}', 'ALL', 1, 1, 10, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('writer_50', '笔耕不辍', '累计发布 50 篇帖子', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#2563eb" stroke="#fff" stroke-width="6"/><path d="M100 96 H156 M100 120 H156 M100 144 H136" stroke="#fff" stroke-width="6" stroke-linecap="round"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":50}]}', 'ALL', 1, 1, 20, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('popular_100', '人气新星', '累计获得 100 个赞', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#d97706" stroke="#fff" stroke-width="6"/><path d="M128 84 L138 108 L164 110 L144 126 L150 152 L128 138 L106 152 L112 126 L92 110 L118 108 Z" fill="#fff"/></svg>', '人气', '{"logic":"ALL","conditions":[{"metric":"like_received","op":">=","value":100}]}', 'ALL', 1, 1, 30, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('favorite_50', '收藏达人', '累计被收藏 50 次', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#7c3aed" stroke="#fff" stroke-width="6"/><path d="M112 80 H144 V152 L128 142 L112 152 Z" fill="#fff"/></svg>', '人气', '{"logic":"ALL","conditions":[{"metric":"favorite_received","op":">=","value":50}]}', 'ALL', 1, 1, 40, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('featured_5', '精华作者', '累计 5 篇帖子被评为精华', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#ca8a04" stroke="#fff" stroke-width="6"/><path d="M128 78 L138 104 L166 106 L144 124 L151 152 L128 136 L105 152 L112 124 L90 106 L118 104 Z" fill="#fff"/></svg>', '内容', '{"logic":"ALL","conditions":[{"metric":"featured_count","op":">=","value":5}]}', 'ALL', 1, 1, 50, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('adopt_10', '答疑专家', '累计被采纳 10 次', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#0891b2" stroke="#fff" stroke-width="6"/><path d="M100 100 H156 V128 H128 L112 144 V128 H100 Z" fill="#fff"/></svg>', '内容', '{"logic":"ALL","conditions":[{"metric":"adopted_count","op":">=","value":10}]}', 'ALL', 1, 1, 60, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('login_30', '全勤标兵', '累计登录 30 天', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#059669" stroke="#fff" stroke-width="6"/><rect x="96" y="88" width="64" height="56" rx="6" fill="none" stroke="#fff" stroke-width="5"/><path d="M96 104 H160" stroke="#fff" stroke-width="5"/></svg>', '活跃', '{"logic":"ALL","conditions":[{"metric":"login_days","op":">=","value":30}]}', 'ALL', 1, 1, 70, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('level_24', '满级工匠', '达到最高等级', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#dc2626" stroke="#fff" stroke-width="6"/><path d="M96 128 L112 144 L160 96" fill="none" stroke="#fff" stroke-width="8" stroke-linecap="round"/></svg>', '成长', '{"logic":"ALL","conditions":[{"metric":"level_index","op":">=","value":24}]}', 'ALL', 1, 1, 80, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('allround_10_50_100', '三好学生', '发帖 10、回复 50、获赞 100', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#4f46e5" stroke="#fff" stroke-width="6"/><path d="M128 80 L134 100 L154 100 L138 112 L144 132 L128 120 L112 132 L118 112 L102 100 L122 100 Z" fill="#fff"/></svg>', '综合', '{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":10},{"metric":"reply_count","op":">=","value":50},{"metric":"like_received","op":">=","value":100}]}', 'ALL', 1, 1, 90, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('allround_any', '全能达人', '发帖 50 或精华 10 或积分 5000', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256"><path d="M44 36 H212 V214 L128 170 L44 214 Z" fill="#be123c" stroke="#fff" stroke-width="6"/><circle cx="128" cy="112" r="30" fill="none" stroke="#fff" stroke-width="5"/><path d="M128 94 L132 106 L144 106 L134 114 L138 126 L128 118 L118 126 L122 114 L112 106 L124 106 Z" fill="#fff"/></svg>', '综合', '{"logic":"ANY","groups":[{"logic":"ALL","conditions":[{"metric":"article_count","op":">=","value":50}]},{"logic":"ALL","conditions":[{"metric":"featured_count","op":">=","value":10}]},{"logic":"ALL","conditions":[{"metric":"points_total","op":">=","value":5000}]}]}', 'ANY', 1, 1, 100, '2026-11-13 00:00:00', '2026-11-13 00:00:00');

SET @col_featured_time := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_article' AND COLUMN_NAME='featured_time');
SET @sql_featured_time := IF(@col_featured_time=0, 'ALTER TABLE `bbs_article` ADD COLUMN `featured_time` varchar(20) DEFAULT NULL COMMENT ''设为精华时间''', 'SELECT 1');
PREPARE s20 FROM @sql_featured_time; EXECUTE s20; DEALLOCATE PREPARE s20;

SET @col_reason := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_featured_recommendation' AND COLUMN_NAME='featured_reason');
SET @sql_reason := IF(@col_reason=0, 'ALTER TABLE `bbs_featured_recommendation` ADD COLUMN `featured_reason` varchar(500) DEFAULT NULL COMMENT ''推荐理由''', 'SELECT 1');
PREPARE s21 FROM @sql_reason; EXECUTE s21; DEALLOCATE PREPARE s21;

SET @col_public_start := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_featured_recommendation' AND COLUMN_NAME='public_start_time');
SET @sql_public_start := IF(@col_public_start=0, 'ALTER TABLE `bbs_featured_recommendation` ADD COLUMN `public_start_time` varchar(20) DEFAULT NULL COMMENT ''公示开始时间''', 'SELECT 1');
PREPARE s22 FROM @sql_public_start; EXECUTE s22; DEALLOCATE PREPARE s22;

SET @col_public_end := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_featured_recommendation' AND COLUMN_NAME='public_end_time');
SET @sql_public_end := IF(@col_public_end=0, 'ALTER TABLE `bbs_featured_recommendation` ADD COLUMN `public_end_time` varchar(20) DEFAULT NULL COMMENT ''公示结束时间''', 'SELECT 1');
PREPARE s23 FROM @sql_public_end; EXECUTE s23; DEALLOCATE PREPARE s23;

SET @col_public_status := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_featured_recommendation' AND COLUMN_NAME='public_status');
SET @sql_public_status := IF(@col_public_status=0, 'ALTER TABLE `bbs_featured_recommendation` ADD COLUMN `public_status` varchar(20) DEFAULT ''NONE'' COMMENT ''公示状态''', 'SELECT 1');
PREPARE s24 FROM @sql_public_status; EXECUTE s24; DEALLOCATE PREPARE s24;

-- @migration: v051-p2-featured-public P2 精华公示配置
-- 幂等：WHERE NOT EXISTS。
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`, `dict_key`)
SELECT 'featured_public_enabled', '1', '精华公示开关', 20, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '1=启用公示，0=终审后直接发布', 'featured_public_enabled'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_key` = 'featured_public_enabled');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`, `dict_key`)
SELECT 'featured_public_days', '5', '精华公示工作日数', 21, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '默认 5 个工作日', 'featured_public_days'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_key` = 'featured_public_days');
INSERT INTO `bbs_dict` (`dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`, `dict_key`)
SELECT 'featured_showcase_count', '3', '精华展示卡数量', 22, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), '首页展示卡数量', 'featured_showcase_count'
WHERE NOT EXISTS (SELECT 1 FROM `bbs_dict` WHERE `dict_key` = 'featured_showcase_count');

-- @migration: v052-p3-operation-cycle 年度运营周期
-- 幂等：CREATE TABLE IF NOT EXISTS；information_schema 检查；INSERT IGNORE。
CREATE TABLE IF NOT EXISTS `bbs_operation_cycle` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `cycle_code`  varchar(50) NOT NULL,
  `cycle_name`  varchar(100) NOT NULL,
  `start_date`  varchar(10) NOT NULL,
  `end_date`    varchar(10) NOT NULL,
  `status`      tinyint(1) NOT NULL DEFAULT 0 COMMENT '0草稿/1启用/2归档',
  `create_time` varchar(20) DEFAULT NULL,
  `update_time` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cycle_code` (`cycle_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='运营周期';

CREATE TABLE IF NOT EXISTS `bbs_cycle_config` (
  `id`           int(11) NOT NULL AUTO_INCREMENT,
  `cycle_id`     int(11) NOT NULL,
  `config_key`   varchar(100) NOT NULL,
  `config_value` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cycle_config` (`cycle_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='运营周期配置';

SET @col_cycle_id := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_points_log' AND COLUMN_NAME='cycle_id');
SET @sql_cycle_id := IF(@col_cycle_id=0, 'ALTER TABLE `bbs_points_log` ADD COLUMN `cycle_id` int(11) DEFAULT NULL COMMENT ''运营周期ID''', 'SELECT 1');
PREPARE s30 FROM @sql_cycle_id; EXECUTE s30; DEALLOCATE PREPARE s30;

SET @idx_cycle_id := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_points_log' AND INDEX_NAME='idx_points_cycle');
SET @sql_cycle_idx := IF(@idx_cycle_id=0, 'ALTER TABLE `bbs_points_log` ADD INDEX `idx_points_cycle` (`cycle_id`, `create_time`)', 'SELECT 1');
PREPARE s31 FROM @sql_cycle_idx; EXECUTE s31; DEALLOCATE PREPARE s31;

INSERT IGNORE INTO `bbs_operation_cycle` (`cycle_code`, `cycle_name`, `start_date`, `end_date`, `status`, `create_time`, `update_time`) VALUES
('CYCLE2026', '2026 年度运营周期', '2026-01-01', '2026-12-31', 1, '2026-11-13 00:00:00', '2026-11-13 00:00:00');

UPDATE `bbs_points_log` p
JOIN `bbs_operation_cycle` c ON c.status = 1
SET p.cycle_id = c.id
WHERE p.cycle_id IS NULL
  AND p.create_time >= c.start_date
  AND p.create_time < CONCAT(c.end_date, ' 23:59:59');

-- @migration: v053-p3-reminder 待办提醒
-- 幂等：CREATE TABLE IF NOT EXISTS；INSERT IGNORE。
CREATE TABLE IF NOT EXISTS `bbs_reminder_rule` (
  `id`                int(11) NOT NULL AUTO_INCREMENT,
  `rule_code`         varchar(50) NOT NULL,
  `rule_name`         varchar(100) NOT NULL,
  `event_type`        varchar(50) NOT NULL,
  `permission_code`   varchar(64) NOT NULL,
  `scope_type`        varchar(20) NOT NULL DEFAULT 'ALL',
  `enabled`           tinyint(1) NOT NULL DEFAULT 1,
  `delay_minutes`     int(11) DEFAULT 10,
  `repeat_minutes`    int(11) DEFAULT 60,
  `max_per_day`       int(11) DEFAULT 8,
  `title_template`    varchar(255) DEFAULT NULL,
  `content_template`  varchar(500) DEFAULT NULL,
  `sort_order`        int(11) DEFAULT 0,
  `create_time`       varchar(20) DEFAULT NULL,
  `update_time`       varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_reminder_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='待办提醒规则';

CREATE TABLE IF NOT EXISTS `bbs_reminder_log` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `rule_id`     int(11) NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `target_id`   int(11) NOT NULL DEFAULT 0,
  `user_id`     int(11) NOT NULL,
  `batch_key`   varchar(20) NOT NULL,
  `sent_at`     varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_reminder_log` (`rule_id`, `target_type`, `target_id`, `user_id`, `batch_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='待办提醒去重';

INSERT IGNORE INTO `bbs_reminder_rule` (`rule_code`, `rule_name`, `event_type`, `permission_code`, `scope_type`, `enabled`, `delay_minutes`, `repeat_minutes`, `max_per_day`, `title_template`, `sort_order`, `create_time`, `update_time`) VALUES
('article_pending', '待审核帖子', 'article_pending', 'article:review', 'BOARD', 1, 10, 60, 8, '您有 {count} 条待审核帖子', 10, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('report_pending', '待审核举报', 'report_pending', 'report:review', 'BOARD', 1, 10, 60, 8, '您有 {count} 条待审核举报', 20, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('appeal_pending', '待审核申诉', 'appeal_pending', 'appeal:review', 'ALL', 1, 30, 120, 8, '您有 {count} 条待审核申诉', 30, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('featured_pending', '精华待终审', 'featured_pending', 'featured:review', 'BOARD', 1, 30, 120, 8, '您有 {count} 条精华推荐待终审', 40, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('adopt_pending', '采纳待审批', 'adopt_pending', 'adopt:review', 'BOARD', 1, 30, 120, 8, '您有 {count} 条采纳待审批', 50, '2026-11-13 00:00:00', '2026-11-13 00:00:00'),
('complaint_pending', '版主投诉待处理', 'complaint_pending', 'moderator:complaint:review', 'ALL', 1, 10, 60, 8, '您有 {count} 条版主投诉待处理', 60, '2026-11-13 00:00:00', '2026-11-13 00:00:00');

-- @migration: v054-p3-org-import 组织自动导入
-- 幂等：CREATE TABLE IF NOT EXISTS；information_schema 检查。
CREATE TABLE IF NOT EXISTS `bbs_org_import_batch` (
  `id`            int(11) NOT NULL AUTO_INCREMENT,
  `file_name`     varchar(255) DEFAULT NULL,
  `file_hash`     varchar(64) DEFAULT NULL,
  `status`        varchar(20) NOT NULL DEFAULT 'PENDING',
  `total_count`   int(11) DEFAULT 0,
  `add_count`     int(11) DEFAULT 0,
  `update_count`  int(11) DEFAULT 0,
  `move_count`    int(11) DEFAULT 0,
  `disable_count` int(11) DEFAULT 0,
  `error_count`   int(11) DEFAULT 0,
  `error_json`    longtext,
  `safe_flag`     tinyint(1) DEFAULT 1,
  `operator_id`   int(11) DEFAULT NULL,
  `start_time`    varchar(20) DEFAULT NULL,
  `end_time`      varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织导入批次';

CREATE TABLE IF NOT EXISTS `bbs_org_import_item` (
  `id`        int(11) NOT NULL AUTO_INCREMENT,
  `batch_id`  int(11) NOT NULL,
  `action`    varchar(20) NOT NULL,
  `org_no`    varchar(20) NOT NULL,
  `old_json`  longtext,
  `new_json`  longtext,
  `status`    varchar(20) DEFAULT 'PENDING',
  `message`   varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_org_import_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='组织导入明细';

SET @col_org_source := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_sa_org' AND COLUMN_NAME='source');
SET @sql_org_source := IF(@col_org_source=0, 'ALTER TABLE `bbs_sa_org` ADD COLUMN `source` varchar(20) DEFAULT ''manual'' COMMENT ''来源''', 'SELECT 1');
PREPARE s40 FROM @sql_org_source; EXECUTE s40; DEALLOCATE PREPARE s40;

SET @col_org_batch := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_sa_org' AND COLUMN_NAME='import_batch_id');
SET @sql_org_batch := IF(@col_org_batch=0, 'ALTER TABLE `bbs_sa_org` ADD COLUMN `import_batch_id` int(11) DEFAULT NULL COMMENT ''导入批次''', 'SELECT 1');
PREPARE s41 FROM @sql_org_batch; EXECUTE s41; DEALLOCATE PREPARE s41;

-- @migration: v055-comment-reply-thumb 评论/回帖点赞
-- 目标：评论与楼中楼回复支持点赞；新增统一互动表 bbs_thumb。
-- 幂等：information_schema 检查；CREATE TABLE IF NOT EXISTS。
SET @col_comment_good := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_comment' AND COLUMN_NAME='comment_good_num');
SET @sql_comment_good := IF(@col_comment_good=0, 'ALTER TABLE `bbs_comment` ADD COLUMN `comment_good_num` int(11) NOT NULL DEFAULT 0 COMMENT ''评论点赞数''', 'SELECT 1');
PREPARE s50 FROM @sql_comment_good; EXECUTE s50; DEALLOCATE PREPARE s50;

SET @col_reply_good := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_reply' AND COLUMN_NAME='reply_good_num');
SET @sql_reply_good := IF(@col_reply_good=0, 'ALTER TABLE `bbs_reply` ADD COLUMN `reply_good_num` int(11) NOT NULL DEFAULT 0 COMMENT ''回复点赞数''', 'SELECT 1');
PREPARE s51 FROM @sql_reply_good; EXECUTE s51; DEALLOCATE PREPARE s51;

CREATE TABLE IF NOT EXISTS `bbs_thumb` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `user_id`     int(11) NOT NULL,
  `target_type` varchar(20) NOT NULL COMMENT 'comment/reply',
  `target_id`   int(11) NOT NULL,
  `create_time` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_thumb` (`user_id`, `target_type`, `target_id`),
  KEY `idx_thumb_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评论/回复点赞';

-- @migration: v056-legacy-ranking-category 内江供电公司本部改为可配置分类
-- 目标：原“本部”虚拟分类（id=0）落为真实分类 legacy_root，成员可像其它分类一样增删。
-- 幂等：WHERE NOT EXISTS。
INSERT INTO `bbs_ranking_category` (`category_code`, `category_name`, `description`, `sort_order`, `enabled`, `create_time`, `update_time`)
SELECT 'legacy_root', '内江供电公司本部', '原“本部”排名，已改为普通分类，可自由配置成员与含下级', 0, 1,
       DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM `bbs_ranking_category` WHERE `category_code` = 'legacy_root');

INSERT INTO `bbs_ranking_category_org` (`category_id`, `org_no`, `include_descendants`, `sort_order`, `create_time`)
SELECT c.id, o.org_no, 1, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM `bbs_ranking_category` c
JOIN `bbs_sa_org` o ON o.p_org_no = '51404' AND o.is_delete = 0
WHERE c.category_code = 'legacy_root'
  AND NOT EXISTS (
    SELECT 1 FROM `bbs_ranking_category_org` x
    WHERE x.category_id = c.id AND x.org_no = o.org_no
  );

-- @migration: v057-level-icon-custom 等级图标自定义
-- 目标：等级项支持自定义图标（图形/颜色/SVG），管理员可在等级管理中绘制或选择。
-- 幂等：information_schema 检查。
SET @col_level_icon_svg := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_level_item' AND COLUMN_NAME='icon_svg');
SET @sql_level_icon_svg := IF(@col_level_icon_svg=0, 'ALTER TABLE `bbs_level_item` ADD COLUMN `icon_svg` longtext COMMENT ''自定义图标SVG''', 'SELECT 1');
PREPARE s60 FROM @sql_level_icon_svg; EXECUTE s60; DEALLOCATE PREPARE s60;

-- @migration: v058-svg-asset SVG 素材库
-- 目标：勋章与等级图标共用绘制成果；管理端独立 SVG 工作台保存/引用。
-- 幂等：CREATE TABLE IF NOT EXISTS。
CREATE TABLE IF NOT EXISTS `bbs_svg_asset` (
  `id`          int(11) NOT NULL AUTO_INCREMENT,
  `asset_name`  varchar(100) NOT NULL COMMENT '素材名称',
  `category`    varchar(50) NOT NULL DEFAULT 'other' COMMENT '分类(medal/level/other)',
  `svg`         longtext NOT NULL COMMENT 'SVG 内容',
  `created_by`  varchar(50) DEFAULT NULL COMMENT '创建人',
  `create_time` varchar(20) DEFAULT NULL,
  `update_time` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_svg_asset_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='SVG素材库';
-- @migration: v059-level-schemes 等级方案系统
-- 目标：等级项补充徽章与定位说明；内置两套 21 级方案（参考《大千智荟论坛等级积分对照表》）。
-- 幂等：information_schema 检查；WHERE NOT EXISTS。
SET @col_badge := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_level_item' AND COLUMN_NAME='badge_name');
SET @sql_badge := IF(@col_badge=0, 'ALTER TABLE `bbs_level_item` ADD COLUMN `badge_name` varchar(50) DEFAULT NULL COMMENT ''徽章名称''', 'SELECT 1');
PREPARE s70 FROM @sql_badge; EXECUTE s70; DEALLOCATE PREPARE s70;
SET @col_desc := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='bbs_level_item' AND COLUMN_NAME='description');
SET @sql_desc := IF(@col_desc=0, 'ALTER TABLE `bbs_level_item` ADD COLUMN `description` varchar(500) DEFAULT NULL COMMENT ''定位说明''', 'SELECT 1');
PREPARE s71 FROM @sql_desc; EXECUTE s71; DEALLOCATE PREPARE s71;

INSERT INTO `bbs_level_profile` (`profile_name`, `major_count`, `minor_per_major`, `status`, `create_time`, `update_time`)
SELECT '方案一 · 智荟成长', 7, 3, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM `bbs_level_profile` WHERE `profile_name`='方案一 · 智荟成长');
INSERT INTO `bbs_level_profile` (`profile_name`, `major_count`, `minor_per_major`, `status`, `create_time`, `update_time`)
SELECT '方案二 · 电网匠心', 7, 3, 0, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM `bbs_level_profile` WHERE `profile_name`='方案二 · 电网匠心');

INSERT INTO `bbs_level_item` (`profile_id`, `level_index`, `major_index`, `major_name`, `minor_index`, `display_name`, `points_threshold`, `icon_key`, `color_token`, `enabled`, `badge_name`, `description`)
SELECT p.id, t.lv, CEIL(t.lv/3), t.major_name, ((t.lv-1)%3)+1, t.title, (t.lv-1)*100, NULL, NULL, 1, NULLIF(t.badge,''), t.descr
FROM `bbs_level_profile` p JOIN ( SELECT 1 AS lv, '初级工' AS major_name, '智荟新芽·观' AS title, '新芽徽章' AS badge, '刚注册入场，多看多浏览，熟悉论坛规则与业务分享帖' AS descr UNION ALL SELECT 2 AS lv, '初级工' AS major_name, '智荟新芽·悟' AS title, '' AS badge, '常浏览、正常回帖交流，开始留意业务中的小问题' AS descr UNION ALL SELECT 3 AS lv, '初级工' AS major_name, '智荟新芽·行' AS title, '' AS badge, '主动发言讨论，敢于抛出疑问、交换想法' AS descr UNION ALL SELECT 4 AS lv, '中级工' AS major_name, '拾光新锐·观' AS title, '耕耘徽章' AS badge, '开始自主发帖，分享岗位实操经验、台账与小技巧' AS descr UNION ALL SELECT 5 AS lv, '中级工' AS major_name, '拾光新锐·悟' AS title, '' AS badge, '积极建言献策，参与痛点研讨，提炼优化思路' AS descr UNION ALL SELECT 6 AS lv, '中级工' AS major_name, '拾光新锐·行' AS title, '' AS badge, '稳定输出内容，主动答疑，带动板块交流氛围' AS descr UNION ALL SELECT 7 AS lv, '高级工' AS major_name, '精锐先锋·观' AS title, '创见徽章' AS badge, '聚焦业务堵点，尝试提出改良办法、减负小方案' AS descr UNION ALL SELECT 8 AS lv, '高级工' AS major_name, '精锐先锋·悟' AS title, '' AS badge, '擅长拆解难题，多次给出高质量解决方案' AS descr UNION ALL SELECT 9 AS lv, '高级工' AS major_name, '精锐先锋·行' AS title, '' AS badge, '产出精华帖，内容可跨班组复用推广' AS descr UNION ALL SELECT 10 AS lv, '技师' AS major_name, '破局造浪人·观' AS title, '砺新徽章' AS badge, '持续产出高质量创意，挖掘QC创新点子' AS descr UNION ALL SELECT 11 AS lv, '技师' AS major_name, '破局造浪人·悟' AS title, '' AS badge, '深挖隐性痛点，牵头组织专题研讨碰撞思路' AS descr UNION ALL SELECT 12 AS lv, '技师' AS major_name, '破局造浪人·行' AS title, '' AS badge, '牵头共创，把零散想法打磨成可落地方案' AS descr UNION ALL SELECT 13 AS lv, '高级技师' AS major_name, '知灯引路人·观' AS title, '传知徽章' AS badge, '创新成果落地见效，复盘沉淀可复制经验' AS descr UNION ALL SELECT 14 AS lv, '高级技师' AS major_name, '知灯引路人·悟' AS title, '' AS badge, '帮带新人，指导梳理创意、打磨帖子' AS descr UNION ALL SELECT 15 AS lv, '高级技师' AS major_name, '知灯引路人·行' AS title, '' AS badge, '参与精华帖初评，协助甄别优质内容' AS descr UNION ALL SELECT 16 AS lv, '特级技师' AS major_name, '云衢领航者·观' AS title, '星引徽章' AS badge, '跨专业协同，指导多个创新项目推进' AS descr UNION ALL SELECT 17 AS lv, '特级技师' AS major_name, '云衢领航者·悟' AS title, '' AS badge, '创新成果在单位内部推广，形成标准化指引' AS descr UNION ALL SELECT 18 AS lv, '特级技师' AS major_name, '云衢领航者·行' AS title, '' AS badge, '参与论坛生态建设，提出高质量运营优化建议' AS descr UNION ALL SELECT 19 AS lv, '首席技师' AS major_name, '大千执炬者·观' AS title, '大成徽章' AS badge, '产出标杆型创新成果，在专业领域示范引领' AS descr UNION ALL SELECT 20 AS lv, '首席技师' AS major_name, '大千执炬者·悟' AS title, '' AS badge, '成果具备跨地市、跨单位借鉴价值' AS descr UNION ALL SELECT 21 AS lv, '首席技师' AS major_name, '大千执炬者·行' AS title, '' AS badge, '论坛最高荣誉，引领创新方向，是平台标杆人物' AS descr ) t
WHERE p.profile_name='方案一 · 智荟成长'
  AND NOT EXISTS (SELECT 1 FROM `bbs_level_item` x WHERE x.profile_id=p.id AND x.level_index=t.lv);

INSERT INTO `bbs_level_item` (`profile_id`, `level_index`, `major_index`, `major_name`, `minor_index`, `display_name`, `points_threshold`, `icon_key`, `color_token`, `enabled`, `badge_name`, `description`)
SELECT p.id, t.lv, CEIL(t.lv/3), t.major_name, ((t.lv-1)%3)+1, t.title, (t.lv-1)*100, NULL, NULL, 1, NULLIF(t.badge,''), t.descr
FROM `bbs_level_profile` p JOIN ( SELECT 1 AS lv, '初级工' AS major_name, '银线学徒·观' AS title, '线芯徽章' AS badge, '刚注册入场，多看多浏览，熟悉论坛规则与业务分享帖' AS descr UNION ALL SELECT 2 AS lv, '初级工' AS major_name, '银线学徒·悟' AS title, '' AS badge, '常浏览、正常回帖交流，开始留意业务中的小问题' AS descr UNION ALL SELECT 3 AS lv, '初级工' AS major_name, '银线学徒·行' AS title, '' AS badge, '主动发言讨论，敢于抛出疑问、交换想法' AS descr UNION ALL SELECT 4 AS lv, '中级工' AS major_name, '线网知客·观' AS title, '杆塔徽章' AS badge, '开始自主发帖，分享岗位实操经验、台账与小技巧' AS descr UNION ALL SELECT 5 AS lv, '中级工' AS major_name, '线网知客·悟' AS title, '' AS badge, '积极建言献策，参与痛点研讨，提炼优化思路' AS descr UNION ALL SELECT 6 AS lv, '中级工' AS major_name, '线网知客·行' AS title, '' AS badge, '稳定输出内容，主动答疑，带动板块交流氛围' AS descr UNION ALL SELECT 7 AS lv, '高级工' AS major_name, '送电革新者·观' AS title, '革新徽章' AS badge, '聚焦业务堵点，尝试提出改良办法、减负小方案' AS descr UNION ALL SELECT 8 AS lv, '高级工' AS major_name, '送电革新者·悟' AS title, '' AS badge, '擅长拆解难题，多次给出高质量解决方案' AS descr UNION ALL SELECT 9 AS lv, '高级工' AS major_name, '送电革新者·行' AS title, '' AS badge, '产出精华帖，内容可跨班组复用推广' AS descr UNION ALL SELECT 10 AS lv, '技师' AS major_name, '电光造梦师·观' AS title, '电芒徽章' AS badge, '持续产出高质量创意，挖掘QC创新点子' AS descr UNION ALL SELECT 11 AS lv, '技师' AS major_name, '电光造梦师·悟' AS title, '' AS badge, '深挖隐性痛点，牵头组织专题研讨碰撞思路' AS descr UNION ALL SELECT 12 AS lv, '技师' AS major_name, '电光造梦师·行' AS title, '' AS badge, '牵头共创，把零散想法打磨成可落地方案' AS descr UNION ALL SELECT 13 AS lv, '高级技师' AS major_name, '熔光传习师·观' AS title, '传灯徽章' AS badge, '创新成果落地见效，复盘沉淀可复制经验' AS descr UNION ALL SELECT 14 AS lv, '高级技师' AS major_name, '熔光传习师·悟' AS title, '' AS badge, '帮带新人，指导梳理创意、打磨帖子' AS descr UNION ALL SELECT 15 AS lv, '高级技师' AS major_name, '熔光传习师·行' AS title, '' AS badge, '参与精华帖初评，协助甄别优质内容' AS descr UNION ALL SELECT 16 AS lv, '特级技师' AS major_name, '经纬领航者·观' AS title, '母线徽章' AS badge, '跨专业协同，指导多个创新项目推进' AS descr UNION ALL SELECT 17 AS lv, '特级技师' AS major_name, '经纬领航者·悟' AS title, '' AS badge, '创新成果在单位内部推广，形成标准化指引' AS descr UNION ALL SELECT 18 AS lv, '特级技师' AS major_name, '经纬领航者·行' AS title, '' AS badge, '参与论坛生态建设，提出高质量运营优化建议' AS descr UNION ALL SELECT 19 AS lv, '首席技师' AS major_name, '万伏执炬人·观' AS title, '金炬徽章' AS badge, '产出标杆型创新成果，在专业领域示范引领' AS descr UNION ALL SELECT 20 AS lv, '首席技师' AS major_name, '万伏执炬人·悟' AS title, '' AS badge, '成果具备跨地市、跨单位借鉴价值' AS descr UNION ALL SELECT 21 AS lv, '首席技师' AS major_name, '万伏执炬人·行' AS title, '' AS badge, '论坛最高荣誉，引领创新方向，是平台标杆人物' AS descr ) t
WHERE p.profile_name='方案二 · 电网匠心'
  AND NOT EXISTS (SELECT 1 FROM `bbs_level_item` x WHERE x.profile_id=p.id AND x.level_index=t.lv);
-- @migration: v060-level-major-labels-and-assets 方案标签修正 + 内置图标入库
-- 目标：大阶段显示论坛头衔前缀（不再显示对标技能层级）；已在用的等级图标、成就勋章写入素材库作为默认数据。
-- 幂等：WHERE NOT EXISTS / NOT LIKE '对标 %'。
SET collation_connection = 'utf8mb4_general_ci';
UPDATE bbs_level_item i JOIN bbs_level_profile p ON p.id = i.profile_id
SET i.major_name = CASE major_index WHEN 1 THEN '智荟新芽' WHEN 2 THEN '拾光新锐' WHEN 3 THEN '精锐先锋' WHEN 4 THEN '破局造浪人' WHEN 5 THEN '知灯引路人' WHEN 6 THEN '云衢领航者' WHEN 7 THEN '大千执炬者' ELSE major_name END,
    i.description = CONCAT('对标 ', CASE major_index WHEN 1 THEN '初级工' WHEN 2 THEN '中级工' WHEN 3 THEN '高级工' WHEN 4 THEN '技师' WHEN 5 THEN '高级技师' WHEN 6 THEN '特级技师' WHEN 7 THEN '首席技师' ELSE major_name END, i.minor_index, '档；', COALESCE(i.description, ''))
WHERE p.profile_name = '方案一 · 智荟成长' AND i.major_index BETWEEN 1 AND 7
  AND (i.description IS NULL OR i.description NOT LIKE '对标 %');

UPDATE bbs_level_item i JOIN bbs_level_profile p ON p.id = i.profile_id
SET i.major_name = CASE major_index WHEN 1 THEN '银线学徒' WHEN 2 THEN '线网知客' WHEN 3 THEN '送电革新者' WHEN 4 THEN '电光造梦师' WHEN 5 THEN '熔光传习师' WHEN 6 THEN '经纬领航者' WHEN 7 THEN '万伏执炬人' ELSE major_name END,
    i.description = CONCAT('对标 ', CASE major_index WHEN 1 THEN '初级工' WHEN 2 THEN '中级工' WHEN 3 THEN '高级工' WHEN 4 THEN '技师' WHEN 5 THEN '高级技师' WHEN 6 THEN '特级技师' WHEN 7 THEN '首席技师' ELSE major_name END, i.minor_index, '档；', COALESCE(i.description, ''))
WHERE p.profile_name = '方案二 · 电网匠心' AND i.major_index BETWEEN 1 AND 7
  AND (i.description IS NULL OR i.description NOT LIKE '对标 %');

-- 指数型升级门槛：按“当前大等级每升一小档所需积分”累计（与产品对照表一致）
UPDATE bbs_level_item i
JOIN bbs_level_profile p ON p.id = i.profile_id
SET i.points_threshold = CASE i.level_index
  WHEN 1 THEN 0    WHEN 2 THEN 20   WHEN 3 THEN 40
  WHEN 4 THEN 60   WHEN 5 THEN 90   WHEN 6 THEN 120
  WHEN 7 THEN 150  WHEN 8 THEN 200  WHEN 9 THEN 250
  WHEN 10 THEN 300 WHEN 11 THEN 400 WHEN 12 THEN 500
  WHEN 13 THEN 600 WHEN 14 THEN 700 WHEN 15 THEN 800
  WHEN 16 THEN 900 WHEN 17 THEN 1100 WHEN 18 THEN 1300
  WHEN 19 THEN 1500 WHEN 20 THEN 1700 WHEN 21 THEN 1900
  ELSE i.points_threshold END
WHERE p.profile_name IN ('方案一 · 智荟成长', '方案二 · 电网匠心')
  AND i.level_index BETWEEN 1 AND 21;
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 星', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#16a34a" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 星' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 月', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#0284c7" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 月' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 日', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><circle cx="12" cy="12" r="4.7" fill="#d97706"/><g stroke="#d97706" stroke-width="2.1" stroke-linecap="round"><line x1="12" y1="1.7" x2="12" y2="4.7"/><line x1="12" y1="19.3" x2="12" y2="22.3"/><line x1="1.7" y1="12" x2="4.7" y2="12"/><line x1="19.3" y1="12" x2="22.3" y2="12"/><line x1="4.6" y1="4.6" x2="6.8" y2="6.8"/><line x1="17.2" y1="17.2" x2="19.4" y2="19.4"/><line x1="19.4" y1="4.6" x2="17.2" y2="6.8"/><line x1="6.8" y1="17.2" x2="4.6" y2="19.4"/></g></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 日' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#ca8a04" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#ca8a04"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+星', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#7c3aed" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#7c3aed"/></g><g transform="translate(14.2 0) scale(0.4)"><path fill="#f59e0b" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+星' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+月', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#4f46e5" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#4f46e5"/></g><g transform="translate(14.2 0) scale(0.4)"><path fill="#f59e0b" d="M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+月' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 皇冠+日', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(0.4 3.2) scale(0.8)"><path fill="#db2777" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#db2777"/></g><g transform="translate(14.2 0) scale(0.4)"><circle cx="12" cy="12" r="4.7" fill="#f59e0b"/><g stroke="#f59e0b" stroke-width="2.1" stroke-linecap="round"><line x1="12" y1="1.7" x2="12" y2="4.7"/><line x1="12" y1="19.3" x2="12" y2="22.3"/><line x1="1.7" y1="12" x2="4.7" y2="12"/><line x1="19.3" y1="12" x2="22.3" y2="12"/><line x1="4.6" y1="4.6" x2="6.8" y2="6.8"/><line x1="17.2" y1="17.2" x2="19.4" y2="19.4"/><line x1="19.4" y1="4.6" x2="17.2" y2="6.8"/><line x1="6.8" y1="17.2" x2="4.6" y2="19.4"/></g></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 皇冠+日' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT '等级图标 · 至尊冠', 'level', '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><g transform="translate(1.5 1.5) scale(0.875)"><path fill="#dc2626" d="M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z"/><rect x="3.3" y="19" width="17.4" height="2.5" rx="1.25" fill="#dc2626"/><circle cx="12" cy="10.4" r="1.85" fill="#fff" opacity="0.92"/><circle cx="12" cy="10.4" r="0.85" fill="#dc2626" opacity="0.55"/></g><g transform="translate(15.4 1) scale(0.34)"><path fill="#fbbf24" d="M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z"/></g></svg>', '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
WHERE NOT EXISTS (SELECT 1 FROM bbs_svg_asset WHERE asset_name='等级图标 · 至尊冠' COLLATE utf8mb4_general_ci);
INSERT INTO bbs_svg_asset (asset_name, category, svg, created_by, create_time, update_time)
SELECT CONCAT('勋章 · ', a.achievement_name) COLLATE utf8mb4_general_ci, 'medal', a.icon_svg, '系统', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'), DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM bbs_achievement a
WHERE a.icon_svg IS NOT NULL AND a.icon_svg <> ''
  AND NOT EXISTS (SELECT 1 FROM bbs_svg_asset s WHERE s.asset_name = CONCAT('勋章 · ', a.achievement_name) COLLATE utf8mb4_general_ci);