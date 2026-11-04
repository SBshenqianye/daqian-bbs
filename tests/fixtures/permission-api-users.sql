-- 权限组 API 冒烟的固定账号（仅用于隔离测试库）
-- 适用：bbs_e2e_test（本地副本）或 bbs_ci_test（CI 从 init-mysql.sql 初始化）
-- 安全阀：连接的不是测试库时立即报错中断，避免误写生产库。
SET NAMES utf8mb4;
SET @guard := (SELECT CASE WHEN DATABASE() IN ('bbs_e2e_test', 'bbs_ci_test') THEN 1
                           ELSE (SELECT 1 FROM (SELECT 1 UNION ALL SELECT 2) must_be_test_db) END);

-- 密码复用 id=1 超管的 BCrypt 哈希；账号只用于测试，不参与真实业务。
INSERT INTO bbs_user
  (id, username, password, nickname, gender, fans, attention, good,
   is_alive, is_delete, create_time, user_type, primary_group_id, is_first_login)
VALUES
  (9101, 'testadmin',
   (SELECT password FROM (SELECT password FROM bbs_user WHERE id = 1) p),
   'E2E-ADMIN', '1', 0, 0, 0, 0, 0, '2026-01-01 00:00:00', '2',
   (SELECT id FROM bbs_permission_group WHERE group_code = 'role_admin' LIMIT 1), 0),
  (9102, 'testuser',
   (SELECT password FROM (SELECT password FROM bbs_user WHERE id = 1) p),
   'E2E-USER', '1', 0, 0, 0, 0, 0, '2026-01-01 00:00:00', '1',
   (SELECT id FROM bbs_permission_group WHERE group_code = 'role_user' LIMIT 1), 0)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  user_type = VALUES(user_type),
  primary_group_id = VALUES(primary_group_id),
  is_alive = 0,
  is_delete = 0,
  is_first_login = 0;
