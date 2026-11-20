-- 清理旧的违规字典条目（dict_value 为字符串 key 的旧记录）
-- 只删除 dict_type='violation' 且 dict_value 不是数字的旧行

DELETE FROM `bbs_dict` WHERE `dict_type` = 'violation' AND `dict_value` IN ('illegal', 'attack', 'spam', 'plagiarism', 'false_report', 'leak');
