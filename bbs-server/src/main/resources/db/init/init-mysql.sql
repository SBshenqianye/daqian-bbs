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
  PRIMARY KEY (`article_id`) USING BTREE,
  KEY `idx_article_featured_time` (`is_featured`, `create_time`)
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
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
-- Table: bbs_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `bbs_sensitive_word`;
CREATE TABLE `bbs_sensitive_word` (
  `id`      int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `keyword` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '敏感词',
  PRIMARY KEY (`id`) USING BTREE
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
  `user_type`      varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户类型（1：普通用户，2：管理员，3：超级管理员）',
  `personnel_id`   varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '人员编号（Excel B列）',
  `id_card`        varchar(18) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `is_first_login` tinyint(1) NULL DEFAULT 1 COMMENT '是否首次登录(1=需改密码,0=已修改)',
  `post_restricted` tinyint(1) NULL DEFAULT 0 COMMENT '是否限制发帖(0=否,1=是)',
  `post_restricted_until` varchar(20) NULL DEFAULT NULL COMMENT '限制发帖截止时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bbs_user_username` (`username`),
  UNIQUE KEY `uk_bbs_user_personnel_id` (`personnel_id`),
  UNIQUE KEY `uk_bbs_user_id_card` (`id_card`)
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
INSERT INTO `bbs_article_label` (`label_id`, `label_name`, `enabled`, `icon`, `description`) VALUES
(1, '技术交流', 0, 'thumb_up', ''),
(2, '求助问答', 1, 'help', ''),
(3, '资源共享', 0, 'folder_open', ''),
(4, '建议反馈', 1, 'lightbulb', '提交建议并被采纳获得+5积分');

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
-- 数据字典
-- ----------------------------
INSERT INTO `bbs_dict` (`id`, `dict_type`, `dict_value`, `dict_label`, `dict_sort`, `create_by`, `create_time`, `remark`) VALUES
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
(12, 'login_browse_minutes', '10', '每日登录有效浏览分钟数', 11, '系统', '2026-08-25 00:00:00', '登录后需浏览满此分钟数才计分');

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
  `title`          varchar(255) DEFAULT NULL COMMENT '通知标题',
  `related_type`   varchar(20) DEFAULT NULL COMMENT '关联类型(article/comment/reply)',
  `related_id`     int(11) DEFAULT NULL COMMENT '关联ID',
  `is_read`        tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读(0=未读,1=已读)',
  `create_time`    varchar(20) DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_user_id` (`user_id`),
  INDEX `idx_notification_user_read` (`user_id`, `is_read`)
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
  `status`          varchar(20) DEFAULT 'pending' COMMENT '状态(pending/confirmed/rejected)',
  `reviewer_id`     int(11) DEFAULT NULL COMMENT '审核人ID',
  `review_time`     varchar(20) DEFAULT NULL COMMENT '审核时间',
  `review_remark`   varchar(500) DEFAULT NULL COMMENT '审核备注',
  `points_awarded`  tinyint(1) DEFAULT 0 COMMENT '是否已给举报人发积分(0=否,1=是)',
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

SET FOREIGN_KEY_CHECKS = 1;
