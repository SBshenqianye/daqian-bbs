-- E2E / API 测试固定数据（仅用于 bbs_e2e_test 副本库，可重复执行）
-- 账号密码统一为 1234@abcD（复用 id=1 超管的 BCrypt 哈希）
--
-- 用法：
--   mysql -uroot -p bbs_e2e_test < tests/fixtures/moderator-seed.sql
--
-- 安全阀：当前连接的库名不是 bbs_e2e_test 时，下面的 CASE 会触发
-- "Subquery returns more than 1 row" 并立即中断，避免误删/误写其他库。
SET NAMES utf8mb4;
SET @guard := (SELECT CASE WHEN DATABASE() = 'bbs_e2e_test' THEN 1
                           ELSE (SELECT 1 FROM (SELECT 1 UNION ALL SELECT 2) must_be_bbs_e2e_test) END);

-- 清理测试用户相关动态数据，保证每次执行基线一致
DELETE FROM bbs_notification WHERE user_id IN (9001,9002,9003,9004,9005,9006,9007,9008);
DELETE FROM bbs_points_log WHERE user_id IN (9001,9002,9003,9004,9005,9006,9007,9008);
DELETE FROM bbs_report WHERE id BETWEEN 9000 AND 9999;
DELETE FROM bbs_appeal WHERE id BETWEEN 9000 AND 9999;
DELETE FROM bbs_featured_recommendation WHERE id BETWEEN 9000 AND 9999;
DELETE FROM bbs_violation WHERE id BETWEEN 9000 AND 9999;
DELETE FROM bbs_comment WHERE comment_id IN (9001,9002);
DELETE FROM bbs_reply WHERE reply_id IN (9001,9002);
DELETE FROM bbs_board_moderator WHERE user_id IN (9001,9002,9003,9004,9005,9006,9007,9008);

-- 测试账号
INSERT INTO bbs_user
  (id, username, password, nickname, portrait, gender, fans, attention, good, is_alive, is_delete,
   create_time, phone, org_no, user_type, is_first_login, post_restricted)
VALUES
  (9001, 'SUPER1',  (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-SUPER1',  NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '3', 0, 0),
  (9002, 'ADMIN1',  (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-ADMIN1',  NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '2', 0, 0),
  (9003, 'MOD1',    (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-MOD1',    NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '4', 0, 0),
  (9004, 'MOD2',    (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-MOD2',    NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '4', 0, 0),
  (9005, 'MOD0',    (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-MOD0',    NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '4', 0, 0),
  (9006, 'NORMAL1', (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-NORMAL1', NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '1', 0, 0),
  (9007, 'NORMAL2', (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-NORMAL2', NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '1', 0, 0),
  (9008, 'NORMAL3', (SELECT password FROM (SELECT password FROM bbs_user WHERE id=1) p), 'E2E-NORMAL3', NULL, '1', 0, 0, 0, 0, 0, '2026-10-01 10:00:00', NULL, NULL, '1', 0, 0)
ON DUPLICATE KEY UPDATE
  password=VALUES(password), nickname=VALUES(nickname), user_type=VALUES(user_type),
  is_delete=0, is_first_login=0, post_restricted=0;

-- 测试板块：9001 为建议类型（便于验证建议采纳）
INSERT INTO bbs_article_label (label_id, label_name, enabled, icon, description, label_type)
VALUES
  (9001, 'E2E板块A', 1, NULL, 'E2E 测试板块A', 'suggestion'),
  (9002, 'E2E板块B', 1, NULL, 'E2E 测试板块B', 'normal'),
  (9003, 'E2E板块C', 1, NULL, 'E2E 测试板块C', 'normal'),
  (9005, 'E2E板块Q9', 1, NULL, 'E2E Q9 删除拦截专用', 'normal')
ON DUPLICATE KEY UPDATE label_name=VALUES(label_name), enabled=1, description=VALUES(description), label_type=VALUES(label_type);

-- 初始任职：admin1(全局管理员，同时挂板块验证 Q1) / mod1 一个板块 / mod2 两个板块 / normal2 用于 Q9
INSERT INTO bbs_board_moderator (user_id, label_id, role_type, status, appoint_time, create_time)
VALUES
  (9002, 9001, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9003, 9001, 'moderator', 1, '2026-10-01 10:11:00', '2026-10-01 10:11:00'),
  (9004, 9002, 'moderator', 1, '2026-10-01 10:12:00', '2026-10-01 10:12:00'),
  (9004, 9003, 'moderator', 1, '2026-10-01 10:13:00', '2026-10-01 10:13:00'),
  (9007, 9005, 'moderator', 1, '2026-10-01 10:14:00', '2026-10-01 10:14:00');

-- 初始积分（任命版主需累计 >= 300）
INSERT INTO bbs_points_log (user_id, points_change, reason, related_type, related_id, operator_id, create_time)
VALUES
  (9002, 300, 'E2E初始积分', 'e2e', NULL, 9001, '2026-10-01 10:20:00'),
  (9006, 300, 'E2E初始积分', 'e2e', NULL, 9001, '2026-10-01 10:20:00'),
  (9007, 300, 'E2E初始积分', 'e2e', NULL, 9001, '2026-10-01 10:20:00');

-- 文章：A 板块 3 篇（含 1 篇建议、1 篇待审、1 篇精华推荐用），B/C 板块各 1 篇
INSERT INTO bbs_article
  (article_id, article_label_id, article_author, article_title, article_summary, article_type_id,
   article_content, article_content_html, user_id, article_good_num, article_view_num,
   create_time, recommend, enable, is_delete, is_featured, is_hot_bonus)
VALUES
  (9001, 9001, 'NORMAL1', 'E2E-A文章1', 'A板块文章1', 1, 'A板块文章1正文', '<p>A板块文章1正文</p>', 9006, 0, 0, '2026-10-01 11:00:00', 0, 1, 0, 0, 0),
  (9002, 9001, 'NORMAL1', 'E2E-A文章2建议', 'A板块建议', 1, 'A板块建议正文', '<p>A板块建议正文</p>', 9006, 0, 0, '2026-10-01 11:01:00', 0, 1, 0, 0, 0),
  (9003, 9002, 'NORMAL2', 'E2E-B文章1', 'B板块文章1', 1, 'B板块文章1正文', '<p>B板块文章1正文</p>', 9007, 0, 0, '2026-10-01 11:02:00', 0, 1, 0, 0, 0),
  (9004, 9003, 'NORMAL2', 'E2E-C文章1', 'C板块文章1', 1, 'C板块文章1正文', '<p>C板块文章1正文</p>', 9007, 0, 0, '2026-10-01 11:03:00', 0, 1, 0, 0, 0),
  (9005, 9002, 'NORMAL2', 'E2E-B未审', 'B板块待审', 1, 'B板块待审正文', '<p>B板块待审正文</p>', 9007, 0, 0, '2026-10-01 11:04:00', 0, 0, 0, 0, 0),
  (9006, 9001, 'NORMAL1', 'E2E-A未审', 'A板块待审', 1, 'A板块待审正文', '<p>A板块待审正文</p>', 9006, 0, 0, '2026-10-01 11:05:00', 0, 0, 0, 0, 0),
  (9007, 9001, 'NORMAL2', 'E2E-A精华推荐', 'A板块精华推荐用', 1, 'A板块精华推荐正文', '<p>A板块精华推荐正文</p>', 9007, 0, 0, '2026-10-01 11:06:00', 0, 1, 0, 0, 0)
ON DUPLICATE KEY UPDATE
  article_label_id=VALUES(article_label_id), user_id=VALUES(user_id), article_title=VALUES(article_title),
  enable=VALUES(enable), is_delete=0, is_featured=0;

-- 待审批采纳（A/B 各 1 条评论 + 1 条回复）
INSERT INTO bbs_comment (comment_id, comment_content, comment_article_id, comment_user_id, comment_time, enable, is_delete, adopt_status)
VALUES
  (9001, 'E2E-A评论待采纳', 9001, 9007, '2026-10-01 12:00:00', 1, 0, 1),
  (9002, 'E2E-B评论待采纳', 9003, 9006, '2026-10-01 12:01:00', 1, 0, 1)
ON DUPLICATE KEY UPDATE comment_article_id=VALUES(comment_article_id), adopt_status=1, enable=1, is_delete=0;

INSERT INTO bbs_reply (reply_id, reply_content, reply_to_user_id, comment_id, reply_user_id, reply_time, enable, is_delete, is_adopted, adopt_status)
VALUES
  (9001, 'E2E-A回复待采纳', 9007, 9001, 9007, '2026-10-01 12:10:00', 1, 0, 0, 1),
  (9002, 'E2E-B回复待采纳', 9006, 9002, 9006, '2026-10-01 12:11:00', 1, 0, 0, 1)
ON DUPLICATE KEY UPDATE comment_id=VALUES(comment_id), adopt_status=1, is_adopted=0, enable=1, is_delete=0;

-- 举报：A/B 各 1 条待审
INSERT INTO bbs_report (id, reporter_id, target_type, target_id, reason, status, create_time, violation_type)
VALUES
  (9001, 9007, 'article', 9001, 'E2E-A举报', 'pending', '2026-10-01 13:00:00', 'spam'),
  (9002, 9006, 'article', 9003, 'E2E-B举报', 'pending', '2026-10-01 13:01:00', 'spam');

-- 违规：A 板块 9001/9003，B 板块 9002；9004 为 A 板块可取消样本
INSERT INTO bbs_violation (id, user_id, violation_type, points_deducted, related_type, related_id, operator_id, remark, create_time, status)
VALUES
  (9001, 9006, 'spam', 1, 'article', 9001, 9002, 'E2E-A违规(申诉关联)', '2026-10-01 14:00:00', 'active'),
  (9002, 9007, 'spam', 1, 'article', 9003, 9002, 'E2E-B违规', '2026-10-01 14:01:00', 'active'),
  (9003, 9006, 'spam', 1, 'article', 9002, 9002, 'E2E-A违规(可取消)', '2026-10-01 14:02:00', 'active'),
  (9004, 9006, 'spam', 1, 'article', 9001, 9002, 'E2E-A违规(新增样本)', '2026-10-01 14:03:00', 'cancelled');

-- 申诉：A/B 各 1 条待审
INSERT INTO bbs_appeal (id, user_id, appeal_type, related_id, content, status, create_time)
VALUES
  (9001, 9006, 'violation', 9001, 'E2E-A申诉', 'pending', '2026-10-01 15:00:00'),
  (9002, 9007, 'violation', 9002, 'E2E-B申诉', 'pending', '2026-10-01 15:01:00');

-- 精华推荐：A 板块 1 条待审（用于验证 Q5 MODERATOR 不能终审）
INSERT INTO bbs_featured_recommendation (id, article_id, recommender_id, label_id, status, create_time)
VALUES
  (9001, 9007, 9006, 9001, 'pending', '2026-10-01 16:00:00');
