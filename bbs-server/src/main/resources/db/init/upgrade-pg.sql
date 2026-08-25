-- ============================================
-- BBS 数据库升级脚本 - PostgreSQL 版
-- 可重复执行：使用 IF NOT EXISTS 保证幂等
-- ============================================

-- 2026-07-06: 标签表增加图标与描述字段
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS icon varchar(50);
ALTER TABLE bbs_article_label ADD COLUMN IF NOT EXISTS description varchar(200);

-- 2026-07-13: 精华帖功能 — 文章表增加 is_featured 字段
ALTER TABLE bbs_article ADD COLUMN IF NOT EXISTS is_featured smallint NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_article_featured_time ON bbs_article (is_featured, create_time);

-- 2026-07-13: 精华帖积分配置（存在则跳过）
INSERT INTO bbs_dict (dict_type, dict_value, dict_label, dict_sort, create_by, create_time, remark)
SELECT 'featured', '10', '精华帖积分', 2, '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS'), '被设为精华帖额外获得的积分'
WHERE NOT EXISTS (SELECT 1 FROM bbs_dict WHERE dict_type = 'featured');

-- 2026-07-15: 新增系统配置表
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

-- 2026-07-15: 使用反馈联系方式初始配置
INSERT INTO bbs_system_config (config_key, config_value, config_label, config_group, config_type, sort_order, remark, create_by, create_time)
SELECT 'feedback_contact', '{"name":"","email":""}', '使用反馈联系方式', 'contact', 'json', 0, '配置使用反馈弹窗中的联系人信息，格式：{"name":"联系人姓名","email":"联系邮箱"}', '系统', TO_CHAR(NOW(), 'YYYY-MM-DD HH24:MI:SS')
WHERE NOT EXISTS (SELECT 1 FROM bbs_system_config WHERE config_key = 'feedback_contact');

-- 2026-07-24: 组织管理 — bbs_sa_org 增加 is_display_selected 字段
ALTER TABLE bbs_sa_org ADD COLUMN IF NOT EXISTS is_display_selected smallint DEFAULT 1;

-- ============================================
-- 2026-08-25: 组织架构梳理 — 排名分组调整
-- 变更内容：
--   1. "地市支撑机构及原集体企业" 重命名为 "公司所属各单位"
--   2. "内江星原公司" 从独立二级排名组降级为 "公司所属各单位" 下的单位
--   3. 县级供电企业的包含组织新增内江星原公司XX分公司
--
-- 回滚 SQL 见本文件末尾 "rollback" 区域
-- ============================================

-- Step 1: 重命名 "地市支撑机构及原集体企业" → "公司所属各单位"
UPDATE bbs_sa_org SET org_name = '公司所属各单位' WHERE org_name = '地市支撑机构及原集体企业';

-- Step 2: 更新 init 脚本中的组织机构名称（保持 init 与 upgrade 同步）
-- 注意：init-pg.sql / init-mysql.sql 中的 INSERT 使用 ON CONFLICT DO NOTHING，
-- 重命名后旧名称不再匹配，需同步修改 init 文件中的 org_name 值。
-- 此步骤仅更新 upgrade 中可见的名称，init 文件需手动同步。

-- Step 3: 删除项目管理中心节点（org_no=51404011701），合并到建设部（项目管理中心）（org_no=514040117）
UPDATE bbs_user SET org_no = '514040117' WHERE org_no = '51404011701';
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no = '51404011701';

-- ============================================
-- Step 4-7: 组织架构树重组
-- 将内江星原公司（5140404）从市公司（51404）下移到公司所属各单位（5140403）下
-- ============================================

-- Step 4: 移动5140404（内江星原公司）从51404到5140403，更新所有后代路径
-- 4a: 更新5140404自身的父节点
UPDATE bbs_sa_org SET p_org_no = '5140403' WHERE org_no = '5140404' AND p_org_no = '51404';
-- 4b: 更新5140404及其所有后代的org_tree路径
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|5140404' || SUBSTRING(org_tree, LENGTH('51404|5140404') + 1)
WHERE org_tree LIKE '51404|5140404%';

-- Step 5: 将原内江星原公司下属的独立部门/分公司上移到公司所属各单位（5140403）下
-- 这些节点不再是内江星原公司的子节点，而是公司所属各单位的直接子节点
-- 注意：子节点的 org_tree 是相对路径格式（如 5140404|514040401），不是全路径
-- 5a: 更新节点自身的p_org_no和org_tree（相对路径：5140404|xxx → 51404|5140403|xxx）
UPDATE bbs_sa_org
SET p_org_no = '5140403',
    org_tree = '51404|5140403' || SUBSTRING(org_tree, LENGTH('5140404') + 1)
WHERE org_no IN (
    '514040425','514040401','514040426',
    '514040412','514040413',
    '514040402','514040407','514040406',
    '514040408','514040410',
    '514040404','514040403','514040409',
    '514040411','514040405'
) AND org_tree LIKE '5140404|5140404%';
-- 5b: 更新有后代的节点的后代路径（乐山城电、四川东祥、西星招标）
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|' || org_tree
WHERE (org_no LIKE '5140404010%' OR org_no LIKE '5140404250%' OR org_no LIKE '5140404260%')
  AND org_tree LIKE '514040401|%' OR org_tree LIKE '514040425|%' OR org_tree LIKE '514040426|%';
-- 5b修正：用括号确保OR逻辑正确
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|' || org_tree
WHERE (org_no LIKE '5140404010%' AND org_tree LIKE '514040401|%')
   OR (org_no LIKE '5140404250%' AND org_tree LIKE '514040425|%')
   OR (org_no LIKE '5140404260%' AND org_tree LIKE '514040426|%');

-- Step 6: 移动514040422（内江星原公司运检分公司）到变电运检中心（514040304）下
-- 6a: 更新运检分公司自身（相对路径：5140404|514040422 → 51404|5140403|514040304|514040422）
UPDATE bbs_sa_org
SET p_org_no = '514040304',
    org_tree = '51404|5140403|514040304|514040422'
WHERE org_no = '514040422' AND org_tree LIKE '5140404|514040422%';
-- 6b: 更新运检分公司所有后代路径（如51404042201等）
UPDATE bbs_sa_org
SET org_tree = '51404|5140403|514040304|514040422' || SUBSTRING(org_tree, LENGTH('514040422') + 1)
WHERE org_no LIKE '5140404220%' AND org_tree LIKE '514040422|514040422%';

-- Step 7: 软删内江星原公司（三新）及其后代（已合并到内江三新公司）
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no = '514040424' AND is_delete = 0;
UPDATE bbs_sa_org SET is_delete = 1 WHERE org_no LIKE '5140404240%' AND is_delete = 0;

-- 2026-08-XX: 积分调整日志表（管理员手动增减用户积分）
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

-- 2026-08-XX: 积分调整日志支持撤销（对已存在的旧表补列，幂等）
ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS is_reversed smallint NOT NULL DEFAULT 0;
ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS reversed_by integer;
ALTER TABLE bbs_points_log ADD COLUMN IF NOT EXISTS reversing_record integer;
CREATE INDEX IF NOT EXISTS idx_points_log_is_reversed ON bbs_points_log (is_reversed);

-- ============================================
-- 回滚 SQL（如需撤销上述变更，取消注释执行）
-- 注意：需要按逆序执行，先回滚后面的步骤再回滚前面的
-- ============================================
-- Step 7 回滚: 取消内江星原公司（三新）的软删
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no LIKE '514040424%';
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no = '514040424';
-- Step 6 回滚: 将运检分公司移回内江星原公司下
-- UPDATE bbs_sa_org SET p_org_no = '5140404', org_tree = '51404|5140404|514040422' WHERE org_no = '514040422';
-- UPDATE bbs_sa_org SET org_tree = '51404|5140404|514040422' || SUBSTRING(org_tree, LENGTH('51404|5140403|514040304|514040422') + 1) WHERE org_tree LIKE '51404|5140403|514040304|514040422%';
-- Step 5 回滚: 将独立部门/分公司移回内江星原公司下（需逐个恢复p_org_no和org_tree）
-- Step 4 回滚: 将5140404移回51404下
-- UPDATE bbs_sa_org SET org_tree = '51404|5140404' || SUBSTRING(org_tree, LENGTH('51404|5140403|5140404') + 1) WHERE org_tree LIKE '51404|5140403|5140404%';
-- UPDATE bbs_sa_org SET p_org_no = '51404' WHERE org_no = '5140404';
-- Step 3 回滚
-- UPDATE bbs_sa_org SET is_delete = 0 WHERE org_no = '51404011701';
-- Step 1 回滚
-- UPDATE bbs_sa_org SET org_name = '地市支撑机构及原集体企业' WHERE org_name = '公司所属各单位';
