/*
 Navicat Premium Data Transfer

 Source Server         : 3.88
 Source Server Type    : MySQL
 Source Server Version : 50736
 Source Host           : 192.168.3.88:13306
 Source Schema         : bbs

 Target Server Type    : MySQL
 Target Server Version : 50736
 File Encoding         : 65001

 Date: 27/04/2026 13:52:25
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bbs_admin
-- ----------------------------
DROP TABLE IF EXISTS `bbs_admin`;
CREATE TABLE `bbs_admin`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '管理员id',
  `username` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员名称',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员密码',
  `portrait` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '管理员头像',
  `is_alive` int(11) NULL DEFAULT 0 COMMENT '是否禁用',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_admin
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_area
-- ----------------------------
DROP TABLE IF EXISTS `bbs_area`;
CREATE TABLE `bbs_area`  (
  `area_id` int(11) NOT NULL AUTO_INCREMENT,
  `image_title` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片上的文字叙述',
  `image_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片路径',
  PRIMARY KEY (`area_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_area
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_article
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article`;
CREATE TABLE `bbs_article`  (
  `article_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '文章id',
  `article_label_id` int(11) NULL DEFAULT NULL COMMENT '标签id',
  `article_author` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章作者',
  `article_title` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章标题',
  `article_summary` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章摘要',
  `article_type_id` int(11) NULL DEFAULT NULL COMMENT '文章类型',
  `article_content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章内容',
  `article_content_html` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章内容html',
  `article_image` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '文章中的图片',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '发布文章的用户的id',
  `article_good_num` int(11) NULL DEFAULT 0 COMMENT '文章获赞数量',
  `article_view_num` int(11) NULL DEFAULT 0 COMMENT '文章的浏览量',
  `article_community_id` int(11) NULL DEFAULT NULL COMMENT '文章所属的社区id',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章发布的时间',
  `recommend` tinyint(1) UNSIGNED ZEROFILL NULL DEFAULT NULL COMMENT '是否推荐该文章',
  `enable` int(11) NULL DEFAULT 0 COMMENT '是否审核通过',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`article_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_article
-- ----------------------------
INSERT INTO `bbs_article` VALUES (50, 1, '国网四川内江东兴供电公司-内江东兴星马豪', '第一个发帖', '神鼎飞丹砂', 0, '# 是的方法\n我是第一个\n![封面.png](http://192.168.3.36:8081/user-api/files/User/id_27/article/2026-03-12/1773305782931_.png)', '<h1><a id=\"_0\"></a>是的方法</h1>\n<p>我是第一个<br />\n<img src=\"http://192.168.3.36:8081/user-api/files/User/id_27/article/2026-03-12/1773305782931_.png\" alt=\"封面.png\" /></p>\n', '', 27, 0, 4, 0, '2026-03-12 16:56:36', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (51, 1, '国网四川内江东兴供电公司-内江东兴星马豪', '测试', '测试', 0, '测试', '<p>测试</p>\n', '', 27, 0, 3, 0, '2026-03-12 17:20:37', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (52, 2, '国网四川内江东兴供电公司-内江东兴星马豪', '测试附件限制exe', '测试限制附件不允许传exe等可执行文件', 0, '打发斯蒂芬萨芬![封面.png](http://192.168.3.36:8081/user-api/files/User/id_27/article/2026-03-12/1773307529550_.png)', '<p>打发斯蒂芬萨芬<img src=\"http://192.168.3.36:8081/user-api/files/User/id_27/article/2026-03-12/1773307529550_.png\" alt=\"封面.png\" /></p>\n', '', 27, 0, 0, 0, '2026-03-12 17:26:52', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (53, 2, '超级管理员', '测试头像', '士大夫撒', 0, '11111![优质服务提升工单流程图.jpg](http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773384211609_.jpg)', '<p>11111<img src=\"http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773384211609_.jpg\" alt=\"优质服务提升工单流程图.jpg\" /></p>\n', '', 1, 0, 26, 0, '2026-03-13 14:43:36', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (54, 2, '超级管理员', '输入', '第三方', 0, '试试 ![20230414105320.jpg](http://192.168.3.36:8081/user-api/files/User/id_1/article/2026-03-13/1773386301078_.jpg)', '<p>试试 <img src=\"http://192.168.3.36:8081/user-api/files/User/id_1/article/2026-03-13/1773386301078_.jpg\" alt=\"20230414105320.jpg\" /></p>\n', '', 1, 0, 3, 0, '2026-03-13 15:18:29', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (55, 2, '超级管理员', '是的冯绍峰', '是的冯绍峰', 0, '说法康师傅\n![20230414105338.jpg](http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773386735535_.jpg)', '<p>说法康师傅<br />\n<img src=\"http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773386735535_.jpg\" alt=\"20230414105338.jpg\" /></p>\n', '', 1, 0, 6, 0, '2026-03-13 15:25:44', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (56, 2, '超级管理员', '测试', 'c', 0, '789![222.png](http://192.168.3.36:8081/user-api/files/User/id_1/article/2026-03-13/1773388138101_.png)', '<p>789<img src=\"http://192.168.3.36:8081/user-api/files/User/id_1/article/2026-03-13/1773388138101_.png\" alt=\"222.png\" /></p>\n', '', 1, 0, 5, 0, '2026-03-13 15:49:06', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (57, 2, '超级管理员', 'sad', 'asda', 0, 'asda![22222.png](http://192.168.3.36:8083/files/User/id_1/article/2026-03-13/1773388710008_.png)', '<p>asda<img src=\"http://192.168.3.36:8083/files/User/id_1/article/2026-03-13/1773388710008_.png\" alt=\"22222.png\" /></p>\n', '', 1, 0, 2, 0, '2026-03-13 15:58:37', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (58, 3, '超级管理员', 'da', 'sad', 0, 'dasda![微信图片_20210326153911.jpg](http://192.168.3.36:8083/files/User/id_1/article/2026-03-13/1773388876937_.jpg)', '<p>dasda<img src=\"http://192.168.3.36:8083/files/User/id_1/article/2026-03-13/1773388876937_.jpg\" alt=\"微信图片_20210326153911.jpg\" /></p>\n', '', 1, 0, 2, 0, '2026-03-13 16:01:28', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (59, 3, '超级管理员', '第三方很反感', '打份饭', 0, '大法师发的\n![20230414105320.jpg](http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773392830312_.jpg)', '<p>大法师发的<br />\n<img src=\"http://192.168.3.36:8080/bbs-server/files/User/id_1/article/2026-03-13/1773392830312_.jpg\" alt=\"20230414105320.jpg\" /></p>\n', '', 1, 0, 7, 0, '2026-03-13 17:07:25', NULL, 1, 1);
INSERT INTO `bbs_article` VALUES (60, 3, '超级管理员', '水电费水电费', '大法师', 0, '士大夫撒', '<p>士大夫撒</p>\n', '/files/User/id_1/article/2026-03-13/cover/1773394577427_.png', 1, 0, 0, 0, '2026-03-13 17:36:17', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (61, 12, '国网四川内江东兴供电公司-内江东兴星马豪', '味儿', '沙发发', 0, '测试批量删除', '<p>测试批量删除</p>\n', '', 27, 0, 2, 0, '2026-03-16 10:52:19', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (62, 2, '国网四川内江东兴供电公司-内江东兴星马豪', '考核口径', '是的冯绍峰', 0, '似懂非懂分隔符父工单', '<p>似懂非懂分隔符父工单</p>\n', '', 27, 0, 17, 0, '2026-03-16 10:52:43', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (63, 1, '国网四川内江东兴供电公司-内江东兴星马豪', '地方规范化发个', '胜多负少的森岛帆高', 0, '# 胜多负少的\n东方闪电富可敌国\n![Capture001.png](http://192.168.3.36:8083/files/User/id_27/article/2026-03-16/1773630772545_.png)', '<h1><a id=\"_0\"></a>胜多负少的</h1>\n<p>东方闪电富可敌国<br />\n<img src=\"http://192.168.3.36:8083/files/User/id_27/article/2026-03-16/1773630772545_.png\" alt=\"Capture001.png\" /></p>\n', '/files/User/id_27/article/2026-03-16/cover/1773630841378_.png', 27, 0, 6, 0, '2026-03-16 11:14:01', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (64, 1, '超级管理员', '是的冯绍峰', '是的冯绍峰', 0, '是的冯绍峰\n士大夫撒![屏幕截图 20250327 180149.png](http://192.168.3.36:8083/files/User/id_1/article/2026-03-16/1773645098220_.png)', '<p>是的冯绍峰<br />\n士大夫撒<img src=\"http://192.168.3.36:8083/files/User/id_1/article/2026-03-16/1773645098220_.png\" alt=\"屏幕截图 20250327 180149.png\" /></p>\n', '', 1, 0, 1, 0, '2026-03-16 15:12:23', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (65, 12, '超级管理员', '水电费水电费', '水电费水电费', 0, '水电费水电费', '<p>水电费水电费</p>\n', '', 1, 0, 1, 0, '2026-03-16 15:33:20', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (66, 1, '超级管理员', '是的冯绍峰', '大范甘迪', 0, '沙发发', '<p>沙发发</p>\n', '', 1, 0, 1, 0, '2026-03-16 15:46:00', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (67, 12, '超级管理员', '法国恢复规划', '各个', 0, '是的冯绍峰', '<p>是的冯绍峰</p>\n', '', 1, 0, 2, 0, '2026-03-16 16:02:13', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (68, 1, '国网四川内江东兴供电公司-内江东兴星马豪', '玩泥巴', '玩泥巴呀', 0, '爱老虎油', '<p>爱老虎油</p>\n', '', 27, 0, 6, 0, '2026-04-08 14:37:18', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (69, 2, '国网四川内江东兴供电公司-内江东兴星马豪', '脱敏测试', '女孩问你赌博吗', 0, '你是一个老司机吗？你天天开车', '<p>你是一个老司机吗？你天天开车</p>\n', '', 27, 0, 9, 0, '2026-04-09 15:10:09', NULL, 1, 0);
INSERT INTO `bbs_article` VALUES (70, 1, '国网四川内江资中县供电公司-程总', '阿德给对方', '阿斯顿发生', 0, '收到大幅度', '<p>收到大幅度</p>\n', '', 29, 0, 6, 0, '2026-04-27 09:55:01', NULL, 1, 0);

-- ----------------------------
-- Table structure for bbs_article_file
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_file`;
CREATE TABLE `bbs_article_file`  (
  `file_id` int(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件名称',
  `file_type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件类型',
  `file_path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '附件存储地址',
  `article_id` int(20) NULL DEFAULT NULL COMMENT '关联文章id',
  PRIMARY KEY (`file_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_article_file
-- ----------------------------
INSERT INTO `bbs_article_file` VALUES (58, '111.csv', 'text/csv', '/files/User/id_27/file/2026-03-12/1773305789614/111.csv', 50);
INSERT INTO `bbs_article_file` VALUES (59, '111.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/files/User/id_27/file/2026-03-12/1773305789614/111.xlsx', 50);
INSERT INTO `bbs_article_file` VALUES (60, 'ddd.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/files/User/id_27/file/2026-03-12/1773307197531/ddd.xlsx', 51);
INSERT INTO `bbs_article_file` VALUES (61, '2.txt', 'text/plain', '/files/User/id_27/file/2026-03-12/1773307220231/2.txt', 51);
INSERT INTO `bbs_article_file` VALUES (62, '111.csv', 'text/csv', '/files/User/id_27/file/2026-03-12/1773307588569/111.csv', 52);
INSERT INTO `bbs_article_file` VALUES (63, 'testHead.jpg', 'image/jpeg', '/files/User/id_1/file/2026-03-13/1773388881325/testHead.jpg', 58);
INSERT INTO `bbs_article_file` VALUES (64, '20230414105338.jpg', 'image/jpeg', '/files/User/id_1/file/2026-03-13/1773392838565/20230414105338.jpg', 59);
INSERT INTO `bbs_article_file` VALUES (65, '测试pdf.pdf', 'application/pdf', '/files/User/id_1/file/2026-03-13/1773392838565/测试pdf.pdf', 59);
INSERT INTO `bbs_article_file` VALUES (66, '微信截图_20230524180351.png', 'image/png', '/files/User/id_1/file/2026-03-13/1773392838562/微信截图_20230524180351.png', 59);
INSERT INTO `bbs_article_file` VALUES (67, '111.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/files/User/id_27/file/2026-03-16/1773630794961/111.xlsx', 63);
INSERT INTO `bbs_article_file` VALUES (68, 'bbs0304.sql', 'application/octet-stream', '/files/User/id_27/file/2026-03-16/1773630802038/bbs0304.sql', 63);
INSERT INTO `bbs_article_file` VALUES (69, '封面.png', 'image/png', '/files/User/id_27/file/2026-03-16/1773630817362/封面.png', 63);
INSERT INTO `bbs_article_file` VALUES (70, '111 (6).csv', 'text/csv', '/files/User/id_1/file/2026-03-16/1773645110864/111 (6).csv', 64);
INSERT INTO `bbs_article_file` VALUES (71, '111 (4).csv', 'text/csv', '/files/User/id_1/file/2026-03-16/1773645110864/111 (4).csv', 64);
INSERT INTO `bbs_article_file` VALUES (72, '111 (5).csv', 'text/csv', '/files/User/id_1/file/2026-03-16/1773645110864/111 (5).csv', 64);
INSERT INTO `bbs_article_file` VALUES (73, 'diagram.bpmn', 'application/octet-stream', '/files/User/id_1/file/2026-03-16/1773646394918/diagram.bpmn', 65);
INSERT INTO `bbs_article_file` VALUES (74, '1122.bmp', 'image/bmp', '/files/User/id_1/file/2026-03-16/1773647153172/1122.bmp', 66);
INSERT INTO `bbs_article_file` VALUES (75, '11222.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/files/User/id_1/file/2026-03-16/1773648126805/11222.xlsx', 67);
INSERT INTO `bbs_article_file` VALUES (76, '雅安bigmap各乡镇街道.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '/files/User/id_27/file/2026-04-08/1775630203120/雅安bigmap各乡镇街道.xlsx', 68);

-- ----------------------------
-- Table structure for bbs_article_label
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_label`;
CREATE TABLE `bbs_article_label`  (
  `label_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '文章标签的id',
  `label_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标签名称',
  `enabled` tinyint(4) NULL DEFAULT NULL COMMENT '标签是否禁用',
  `icon` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标签图标',
  `description` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标签描述',
  PRIMARY KEY (`label_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_article_label
-- ----------------------------
INSERT INTO `bbs_article_label` VALUES (1, '技术交流', 0, 'thumb_up', '');
INSERT INTO `bbs_article_label` VALUES (2, '求助问答', 1, 'help', '');
INSERT INTO `bbs_article_label` VALUES (3, '资源共享', 0, 'folder_open', '');

-- ----------------------------
-- Table structure for bbs_article_type
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_type`;
CREATE TABLE `bbs_article_type`  (
  `type_id` int(11) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文章类型名称',
  PRIMARY KEY (`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_article_type
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_article_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_article_user`;
CREATE TABLE `bbs_article_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  `article_id` int(11) NULL DEFAULT NULL COMMENT '文章id',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_article_user
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_comment
-- ----------------------------
DROP TABLE IF EXISTS `bbs_comment`;
CREATE TABLE `bbs_comment`  (
  `comment_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '评论的id',
  `comment_content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论的内容',
  `comment_article_id` int(11) NULL DEFAULT NULL COMMENT '评论的文章id',
  `comment_user_id` int(11) NULL DEFAULT NULL COMMENT '评论的用户id',
  `comment_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论时间',
  `enable` int(11) NULL DEFAULT 1 COMMENT '是否被审核通过',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`comment_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_comment
-- ----------------------------
INSERT INTO `bbs_comment` VALUES (50, '士大夫撒', 53, 1, '2026-03-13 15:01:17', 1, 0);
INSERT INTO `bbs_comment` VALUES (51, '1122', 53, 1, '2026-03-13 15:03:21', 1, 0);
INSERT INTO `bbs_comment` VALUES (52, '111', 53, NULL, '2026-03-13 15:08:02', 1, 0);
INSERT INTO `bbs_comment` VALUES (53, '大法师', 54, 1, '2026-03-13 15:18:34', 1, 0);
INSERT INTO `bbs_comment` VALUES (54, '士大夫撒', 54, 1, '2026-03-13 15:18:37', 1, 0);
INSERT INTO `bbs_comment` VALUES (55, '一样', 54, 1, '2026-03-13 15:18:46', 1, 0);
INSERT INTO `bbs_comment` VALUES (56, '水电费水电费', 55, 1, '2026-03-13 15:25:55', 1, 0);
INSERT INTO `bbs_comment` VALUES (57, '水电费水电费', 55, 1, '2026-03-13 15:25:58', 1, 0);
INSERT INTO `bbs_comment` VALUES (58, 'asda', 57, 1, '2026-03-13 15:58:42', 1, 0);
INSERT INTO `bbs_comment` VALUES (59, 'asd', 57, 1, '2026-03-13 15:58:45', 1, 0);
INSERT INTO `bbs_comment` VALUES (60, '水电费水电费', 59, 1, '2026-03-13 17:07:35', 1, 1);
INSERT INTO `bbs_comment` VALUES (61, '士大夫撒否', 59, 1, '2026-03-13 17:07:38', 1, 1);
INSERT INTO `bbs_comment` VALUES (62, '似懂非懂', 59, 1, '2026-03-16 10:05:12', 1, 1);
INSERT INTO `bbs_comment` VALUES (63, '是的冯绍峰', 62, 27, '2026-03-16 10:58:22', 1, 0);
INSERT INTO `bbs_comment` VALUES (64, '是的冯绍峰', 62, 27, '2026-03-16 10:58:30', 1, 0);
INSERT INTO `bbs_comment` VALUES (65, '水电费水电费水电费', 62, 1, '2026-03-16 11:10:56', 1, 0);
INSERT INTO `bbs_comment` VALUES (66, '胜多负少的', 62, 1, '2026-03-16 11:11:23', 1, 0);
INSERT INTO `bbs_comment` VALUES (67, '沙发发', 64, 1, '2026-03-16 15:13:17', 1, 0);
INSERT INTO `bbs_comment` VALUES (68, '第三方', 68, 27, '2026-04-08 15:29:04', 1, 0);
INSERT INTO `bbs_comment` VALUES (69, '士大夫撒', 70, 30, '2026-04-27 10:20:43', 1, 0);

-- ----------------------------
-- Table structure for bbs_community
-- ----------------------------
DROP TABLE IF EXISTS `bbs_community`;
CREATE TABLE `bbs_community`  (
  `community_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '社区id',
  `community_name` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区名称',
  `community_introduce` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区介绍',
  `community_image` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区的照片',
  `community_user_num` int(11) NULL DEFAULT 0 COMMENT '社区用户数量',
  `create_user_id` int(11) NULL DEFAULT NULL COMMENT '创建改社区的用户id',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '社区的创建时间',
  `enable` int(11) NULL DEFAULT 1 COMMENT '是否审核通过',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`community_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_community
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_community_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_community_user`;
CREATE TABLE `bbs_community_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `community_id` int(11) NULL DEFAULT NULL COMMENT '社区的id',
  `user_id` int(11) NULL DEFAULT NULL COMMENT '社区用户的id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_community_user
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_dict
-- ----------------------------
DROP TABLE IF EXISTS `bbs_dict`;
CREATE TABLE `bbs_dict`  (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `dict_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典类型-键',
  `dict_value` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '值',
  `dict_label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '中文翻译',
  `dict_sort` int(20) NULL DEFAULT NULL COMMENT '排序序号',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_dict
-- ----------------------------
INSERT INTO `bbs_dict` VALUES (2, 'post', '3', '发帖积分', 1, '京茶吉鹿', '2026-03-09 10:20:10', '81964008@qq.com', '2026-03-10 17:00:57', '发一个帖子所得积分');
INSERT INTO `bbs_dict` VALUES (3, 'reply', '1', '回帖积分', 0, '京茶吉鹿', '2026-03-09 10:35:15', '京茶吉鹿', '2026-03-09 11:03:44', '回帖一次所得积分');
INSERT INTO `bbs_dict` VALUES (4, 'switch', '1', '排名功能是否开启', 1, '京茶吉鹿', '2026-03-09 10:40:45', 'nj_xingmahao', '2026-03-13 09:20:16', '值：积分排名开关（0不开放，1开放）');
INSERT INTO `bbs_dict` VALUES (5, 'points-start-time', '2026-01-22', '累计排名开始时间', 1, '京茶吉鹿', '2026-03-09 11:36:43', '152847345@qq.com', '2026-03-11 16:15:31', '积分开始时间，格式（YYYY-MM-DD）');
INSERT INTO `bbs_dict` VALUES (6, 'point-end-time', '2026-03-19', '累计排名结束时间', 2, '京茶吉鹿', '2026-03-09 11:37:36', '152847345@qq.com', '2026-03-11 16:15:39', '积分结束时间，格式（YYYY-MM-DD）');

-- ----------------------------
-- Table structure for bbs_fans
-- ----------------------------
DROP TABLE IF EXISTS `bbs_fans`;
CREATE TABLE `bbs_fans`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) NULL DEFAULT NULL COMMENT '用户id',
  `attention_id` int(11) NULL DEFAULT NULL COMMENT '关注用户的id',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '关注的时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_fans
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_inventory
-- ----------------------------
DROP TABLE IF EXISTS `bbs_inventory`;
CREATE TABLE `bbs_inventory`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `area` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地区',
  `category` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类别',
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '内容',
  `snumber` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '编号',
  `department` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位',
  `time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_inventory
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_reply
-- ----------------------------
DROP TABLE IF EXISTS `bbs_reply`;
CREATE TABLE `bbs_reply`  (
  `reply_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '回复的id',
  `reply_content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '回复的具体内容',
  `reply_to_user_id` int(11) NULL DEFAULT NULL COMMENT '回复的对象的Id',
  `comment_id` int(11) NULL DEFAULT NULL COMMENT '回复评论的ID',
  `reply_user_id` int(11) NULL DEFAULT NULL COMMENT '回复的用户id',
  `reply_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '回复时间',
  `enable` int(11) NULL DEFAULT 1 COMMENT '是否被审核通过',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`reply_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_reply
-- ----------------------------
INSERT INTO `bbs_reply` VALUES (34, '在哪儿呀，在哪儿啊', 1, 60, 27, '2026-03-16 10:05:08', 1, 1);
INSERT INTO `bbs_reply` VALUES (35, '你说啥啊', 1, 61, 27, '2026-03-16 10:05:49', 1, 1);
INSERT INTO `bbs_reply` VALUES (36, '测试一下', 27, 63, 1, '2026-03-16 11:11:04', 1, 0);
INSERT INTO `bbs_reply` VALUES (37, '是的冯绍峰发个', 27, 64, 1, '2026-03-16 11:11:16', 1, 0);
INSERT INTO `bbs_reply` VALUES (38, '大地飞歌电饭锅', 30, 69, 29, '2026-04-27 11:10:52', 1, 0);

-- ----------------------------
-- Table structure for bbs_sa_org
-- ----------------------------
DROP TABLE IF EXISTS `bbs_sa_org`;
CREATE TABLE `bbs_sa_org`  (
  `id` int(11) NOT NULL COMMENT '唯一标识',
  `org_no` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '单位编号',
  `org_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位名称',
  `p_org_no` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父级单位编号',
  `org_tree` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位树',
  `is_delete` tinyint(1) NULL DEFAULT NULL COMMENT '是否删除',
  `is_ranking_selected` tinyint(1) NULL DEFAULT 0 COMMENT '是否参与排名(0=否,1=是)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_sa_org
-- ----------------------------
INSERT INTO `bbs_sa_org` VALUES (1, '51404', '国网四川内江供电公司', '51101', '51101|51404', 0);
INSERT INTO `bbs_sa_org` VALUES (2, '5140400', '国网四川内江市区供电中心', '51404', '51101|51404|5140400', 0);
INSERT INTO `bbs_sa_org` VALUES (3, '514040003', '国网四川内江市区凌家供电所', '5140400', '51101|51404|5140400|514040003', 0);
INSERT INTO `bbs_sa_org` VALUES (4, '514040006', '国网四川内江市区白马供电所', '5140400', '51101|51404|5140400|514040006', 0);
INSERT INTO `bbs_sa_org` VALUES (5, '514040008', '国网四川内江市区靖民供电所', '5140400', '51101|51404|5140400|514040008', 0);
INSERT INTO `bbs_sa_org` VALUES (6, '514040009', '国网四川内江市区城郊供电所', '5140400', '51101|51404|5140400|514040009', 0);
INSERT INTO `bbs_sa_org` VALUES (7, '5140403', '国网四川内江东兴供电公司', '51404', '51101|51404|5140403', 0);
INSERT INTO `bbs_sa_org` VALUES (8, '514040302', '国网四川内江东兴高梁供电所', '5140403', '51101|51404|5140403|514040302', 0);
INSERT INTO `bbs_sa_org` VALUES (9, '514040303', '国网四川内江东兴白合供电所', '5140403', '51101|51404|5140403|514040303', 0);
INSERT INTO `bbs_sa_org` VALUES (10, '514040304', '国网四川内江东兴双才供电所', '5140403', '51101|51404|5140403|514040304', 0);
INSERT INTO `bbs_sa_org` VALUES (11, '514040305', '国网四川内江东兴田家供电所', '5140403', '51101|51404|5140403|514040305', 0);
INSERT INTO `bbs_sa_org` VALUES (12, '514040306', '国网四川内江东兴椑木供电所', '5140403', '51101|51404|5140403|514040306', 0);
INSERT INTO `bbs_sa_org` VALUES (13, '5140424', '国网四川内江威远县供电公司', '51404', '51101|51404|5140424', 0);
INSERT INTO `bbs_sa_org` VALUES (14, '514042401', '国网四川内江威远连界供电所', '5140424', '51101|51404|5140424|514042401', 0);
INSERT INTO `bbs_sa_org` VALUES (15, '514042402', '国网四川内江威远龙会供电所', '5140424', '51101|51404|5140424|514042402', 0);
INSERT INTO `bbs_sa_org` VALUES (16, '514042404', '国网四川内江威远山王供电所', '5140424', '51101|51404|5140424|514042404', 0);
INSERT INTO `bbs_sa_org` VALUES (17, '514042408', '国网四川内江威远严陵供电所', '5140424', '51101|51404|5140424|514042408', 0);
INSERT INTO `bbs_sa_org` VALUES (18, '514042410', '国网四川内江威远镇西供电所', '5140424', '51101|51404|5140424|514042410', 0);
INSERT INTO `bbs_sa_org` VALUES (19, '514042411', '国网四川内江威远越溪供电所', '5140424', '51101|51404|5140424|514042411', 0);
INSERT INTO `bbs_sa_org` VALUES (20, '514042412', '国网四川内江威远新店供电所', '5140424', '51101|51404|5140424|514042412', 0);
INSERT INTO `bbs_sa_org` VALUES (21, '514042413', '国网四川内江威远界牌供电所', '5140424', '51101|51404|5140424|514042413', 0);
INSERT INTO `bbs_sa_org` VALUES (22, '5140425', '国网四川内江资中县供电公司', '51404', '51101|51404|5140425', 0);
INSERT INTO `bbs_sa_org` VALUES (23, '514042501', '国网四川内江资中归德供电所', '5140425', '51101|51404|5140425|514042501', 0);
INSERT INTO `bbs_sa_org` VALUES (24, '514042502', '国网四川内江资中水南供电所', '5140425', '51101|51404|5140425|514042502', 0);
INSERT INTO `bbs_sa_org` VALUES (25, '514042503', '国网四川内江资中银山供电所', '5140425', '51101|51404|5140425|514042503', 0);
INSERT INTO `bbs_sa_org` VALUES (26, '514042507', '国网四川内江资中双河供电所', '5140425', '51101|51404|5140425|514042507', 0);
INSERT INTO `bbs_sa_org` VALUES (27, '514042508', '国网四川内江资中鱼溪供电所', '5140425', '51101|51404|5140425|514042508', 0);
INSERT INTO `bbs_sa_org` VALUES (28, '5140428', '国网四川内江隆昌市供电公司', '51404', '51101|51404|5140428', 0);
INSERT INTO `bbs_sa_org` VALUES (29, '514042804', '国网四川内江隆昌金鹅供电所', '5140428', '51101|51404|5140428|514042804', 0);
INSERT INTO `bbs_sa_org` VALUES (30, '514042806', '国网四川内江隆昌黄家供电所', '5140428', '51101|51404|5140428|514042806', 0);
INSERT INTO `bbs_sa_org` VALUES (31, '514042807', '国网四川内江隆昌石碾供电所', '5140428', '51101|51404|5140428|514042807', 0);
INSERT INTO `bbs_sa_org` VALUES (32, '514042812', '国网四川内江隆昌云顶供电所', '5140428', '51101|51404|5140428|514042812', 0);
INSERT INTO `bbs_sa_org` VALUES (33, '51404000601', '测试', '514040006', '51101|51404|5140400|514040006|51404000601', 0);

-- ----------------------------
-- Table structure for bbs_sensitive_word
-- ----------------------------
DROP TABLE IF EXISTS `bbs_sensitive_word`;
CREATE TABLE `bbs_sensitive_word`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '唯一标识',
  `keyword` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '敏感词',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_sensitive_word
-- ----------------------------
INSERT INTO `bbs_sensitive_word` VALUES (1, '赌博');
INSERT INTO `bbs_sensitive_word` VALUES (2, '肉票');
INSERT INTO `bbs_sensitive_word` VALUES (4, '抢劫');
INSERT INTO `bbs_sensitive_word` VALUES (5, '莎莎舞');
INSERT INTO `bbs_sensitive_word` VALUES (6, '老司机');

-- ----------------------------
-- Table structure for bbs_slideshow
-- ----------------------------
DROP TABLE IF EXISTS `bbs_slideshow`;
CREATE TABLE `bbs_slideshow`  (
  `slideshow_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '轮播图的id',
  `image_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '轮播图的地址',
  `successive` int(11) NULL DEFAULT NULL COMMENT '顺序（数字越大，越靠前）',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`slideshow_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_slideshow
-- ----------------------------

-- ----------------------------
-- Table structure for bbs_user
-- ----------------------------
DROP TABLE IF EXISTS `bbs_user`;
CREATE TABLE `bbs_user`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `username` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户登录名',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户密码',
  `nickname` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `portrait` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户头像',
  `gender` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户性别',
  `introduce` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户介绍',
  `city` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户城市',
  `fans` int(11) NULL DEFAULT 0 COMMENT '粉丝数量',
  `attention` int(11) NULL DEFAULT NULL COMMENT '关注数量',
  `good` int(11) NULL DEFAULT NULL COMMENT '获赞数量',
  `is_alive` int(11) NULL DEFAULT 0 COMMENT '是否被禁',
  `is_delete` int(11) NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '添加时间',
  `phone` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电话',
  `org_no` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '单位编号',
  `user_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户类型（1：普通用户，2：管理员，3：超级管理员）',
  `personnel_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '人员编号（Excel B列）',
  `id_card` varchar(18) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '身份证号',
  `is_first_login` tinyint(1) NULL DEFAULT 1 COMMENT '是否首次登录(1=需改密码,0=已修改)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bbs_user
-- ----------------------------
INSERT INTO `bbs_user` (`id`, `username`, `password`, `nickname`, `portrait`, `gender`, `introduce`, `city`, `fans`, `attention`, `good`, `is_alive`, `is_delete`, `create_time`, `phone`, `org_no`, `user_type`, `personnel_id`, `id_card`, `is_first_login`) VALUES
(1, 'asiayak', '$2a$10$VAj33hwXHrlqUjpxxjVbDuG/rx3DLKoNhWR6HhVX7KKJSr5JG9jkO', '超级管理员', '/files/User/id_1/portrait/1773383668826_.png', '1', 'Go big or go home!', '河北省-秦皇岛市', 1201, 9, 521, 0, 0, '2022-05-20 00:00:00', NULL, '51404', '3', NULL, NULL, 0),
(27, 'nj_xingmahao', '$2a$10$AnRCvMtWSEmZ.8qI8tbapul1oj3tUk7GpgDvr60rLmFnDJiCX6ZRq', '国网四川内江东兴供电公司-内江东兴星马豪', '/files/User/id_27/portrait/1773629671056_.png', '0', NULL, NULL, 0, 0, 0, 0, 0, '2026-03-12 16:55:41', '15826457845', '5140403', '2', NULL, NULL, 0),
(28, 'cheng', '$2a$10$OIRkbKfkfElh4v312LVWYOMeV5KAcjF.qfhT2okMFpEYaYzQo4mu6', '国网四川内江威远县供电公司-程司令', NULL, '0', NULL, NULL, 0, 0, 0, 0, 0, '2026-04-08 14:01:56', '15983568745', '5140424', '1', NULL, NULL, 0),
(29, 'nj_chengqing', '$2a$10$YvljsOG3DmLWCO3RJHvhIul5hi49RWPqpi1Z8joLSB4uPF3CblFOu', '国网四川内江资中县供电公司-程总', '/files/User/id_29/portrait/1777259410841_.png', '0', NULL, NULL, 0, 0, 0, 0, 0, '2026-04-27 09:54:11', '18383380954', '5140425', '2', NULL, NULL, 0),
(30, 'nj_chengqing01', '$2a$10$a7gageaaX4CT05HMVWX5N.Q71A/Jj1e2SyapueSiCggTsE7H1CDqu', '国网四川内江隆昌市供电公司-程厅', NULL, '0', NULL, NULL, 0, 0, 0, 0, 0, '2026-04-27 10:20:28', '15983456547', '5140428', '1', NULL, NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
