-- 手动修复：为 bbs_dict 表添加 dict_key 列
-- 执行方式：mysql -u用户名 -p密码 数据库名 < fix-dict-key-column.sql

ALTER TABLE `bbs_dict` ADD COLUMN `dict_key` varchar(100) DEFAULT NULL COMMENT '键(违规类型标识)' AFTER `remark`;

-- 回填所有字典条目的 dict_key
UPDATE `bbs_dict` SET `dict_key` = 'post'                 WHERE `dict_type` = 'post' AND `dict_key` IS NULL;
UPDATE `bbs_dict` SET `dict_key` = 'reply'                WHERE `dict_type` = 'reply' AND `dict_key` IS NULL;
UPDATE `bbs_dict` SET `dict_key` = 'switch'               WHERE `dict_type` = 'switch' AND `dict_key` IS NULL;
UPDATE `bbs_dict` SET `dict_key` = 'featured'             WHERE `dict_type` = 'featured' AND `dict_key` IS NULL;
UPDATE `bbs_dict` SET `dict_key` = 'hot_threshold'        WHERE `dict_type` = 'hot_threshold' AND `dict_key` IS NULL;
UPDATE `bbs_dict` SET `dict_key` = 'login_browse_minutes' WHERE `dict_type` = 'login_browse_minutes' AND `dict_key` IS NULL;

-- 违规类型：dict_key = 类型标识，dict_value = 扣分值
UPDATE `bbs_dict` SET `dict_key` = 'illegal',      `dict_value` = '15'  WHERE `dict_type` = 'violation' AND `dict_value` = 'illegal';
UPDATE `bbs_dict` SET `dict_key` = 'attack',       `dict_value` = '10'  WHERE `dict_type` = 'violation' AND `dict_value` = 'attack';
UPDATE `bbs_dict` SET `dict_key` = 'spam',         `dict_value` = '4'   WHERE `dict_type` = 'violation' AND `dict_value` = 'spam';
UPDATE `bbs_dict` SET `dict_key` = 'plagiarism',   `dict_value` = '12'  WHERE `dict_type` = 'violation' AND `dict_value` = 'plagiarism';
UPDATE `bbs_dict` SET `dict_key` = 'false_report', `dict_value` = '3'   WHERE `dict_type` = 'violation' AND `dict_value` = 'false_report';
UPDATE `bbs_dict` SET `dict_key` = 'leak',         `dict_value` = '20'  WHERE `dict_type` = 'violation' AND `dict_value` = 'leak';
