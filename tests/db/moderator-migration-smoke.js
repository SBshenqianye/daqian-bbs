#!/usr/bin/env node
/**
 * v039-moderator-role 迁移 → 校验 → 回滚 → 再迁移 烟测（仅副本库 bbs_moderator_test）
 *
 * 运行：node tests/db/moderator-migration-smoke.js
 * 不会连接/写入 bbs_db。
 */
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const DB = 'bbs_moderator_test';
const ROOT = path.resolve(__dirname, '..', '..');
const UPGRADE_SQL = path.join(ROOT, 'bbs-server', 'src', 'main', 'resources', 'db', 'init', 'upgrade-mysql.sql');
const TMP = path.join(ROOT, 'tmp');
const checks = [];
const evidence = {};

function runMysql(args, input) {
  return execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '--default-character-set=utf8mb4', ...args,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, input, encoding: 'utf8' });
}

function sql(statement) {
  runMysql([DB], statement);
}

function query(statement) {
  const out = runMysql(['-N', '-B', DB], statement);
  return out.trim() ? out.trim().split(/\r?\n/).map(line => line.split('\t')) : [];
}

function check(id, name, fn) {
  try {
    const detail = fn();
    checks.push({ id, name, status: 'PASS', detail: detail || '' });
    console.log(`[PASS] ${id} ${name}${detail ? ' :: ' + detail : ''}`);
  } catch (e) {
    checks.push({ id, name, status: 'FAIL', detail: e.message });
    console.log(`[FAIL] ${id} ${name} :: ${e.message}`);
  }
}
function assert(cond, message) {
  if (!cond) throw new Error(message);
}
function dump(file) {
  const out = execFileSync('mysqldump', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '--single-transaction', '--quick',
    '--default-character-set=utf8mb4', DB,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  fs.writeFileSync(file, out, 'utf8');
}

const EXPECTED_NEW_COMMENT = '用户类型（1：普通用户，2：管理员，3：超级管理员，4：版主）';
const EXPECTED_OLD_COMMENT = '用户类型（1：普通用户，2：管理员，3：超级管理员）';

function counts() {
  const rows = query("SELECT user_type, COUNT(*) FROM bbs_user GROUP BY user_type ORDER BY user_type");
  const map = {};
  for (const [k, v] of rows) map[k] = Number(v);
  return map;
}
function columnComment() {
  return query("SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='bbs_moderator_test' AND TABLE_NAME='bbs_user' AND COLUMN_NAME='user_type'")[0][0];
}
function versionExists() {
  return Number(query("SELECT COUNT(*) FROM bbs_schema_version WHERE version='v039-moderator-role'")[0][0]) > 0;
}

function setup() {
  sql(`
CREATE TABLE IF NOT EXISTS bbs_user_type_v039_backup (
  user_id int(11) NOT NULL,
  old_user_type varchar(20) NOT NULL,
  backed_up_at varchar(20) NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DELETE FROM bbs_board_moderator WHERE user_id BETWEEN 9101 AND 9110;
DELETE FROM bbs_user WHERE id BETWEEN 9101 AND 9110;
DELETE FROM bbs_user_type_v039_backup WHERE user_id BETWEEN 9101 AND 9110;
DELETE FROM bbs_schema_version WHERE version='v039-moderator-role';
UPDATE bbs_user SET user_type='1' WHERE user_type='4';

INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9101, 'NORMAL_MIG', password, '迁移-普通用户有板块', '1', 0, 0, 0, '2026-10-01 10:00:00', '1', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9102, 'ADMIN_MIG', password, '迁移-管理员有板块', '1', 0, 0, 0, '2026-10-01 10:00:00', '2', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9103, 'SUPER_MIG', password, '迁移-超管有板块', '1', 0, 0, 0, '2026-10-01 10:00:00', '3', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9104, 'NORMAL_NOBOARD', password, '迁移-普通用户无板块', '1', 0, 0, 0, '2026-10-01 10:00:00', '1', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9106, 'MANUAL_MOD', password, '迁移-人工版主', '1', 0, 0, 0, '2026-10-01 10:00:00', '4', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9107, 'DELETED_MOD', password, '迁移-已注销有板块', '1', 0, 0, 1, '2026-10-01 10:00:00', '1', 0 FROM bbs_user WHERE id=1;
INSERT INTO bbs_user (id, username, password, nickname, gender, fans, is_alive, is_delete, create_time, user_type, is_first_login)
SELECT 9108, 'INVALID_TYPE', password, '迁移-非法类型', '1', 0, 0, 0, '2026-10-01 10:00:00', '9', 0 FROM bbs_user WHERE id=1;

INSERT INTO bbs_board_moderator (user_id, label_id, role_type, status, appoint_time, create_time) VALUES
  (9101, 2, 'moderator', 1, '2026-10-01 10:10:00', '2026-10-01 10:10:00'),
  (9102, 3, 'moderator', 1, '2026-10-01 10:11:00', '2026-10-01 10:11:00'),
  (9103, 4, 'moderator', 1, '2026-10-01 10:12:00', '2026-10-01 10:12:00'),
  (9107, 5, 'moderator', 1, '2026-10-01 10:13:00', '2026-10-01 10:13:00');
  `);
}

function backupCandidates() {
  sql(`
CREATE TABLE IF NOT EXISTS bbs_user_type_v039_backup (
  user_id int(11) NOT NULL,
  old_user_type varchar(20) NOT NULL,
  backed_up_at varchar(20) NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO bbs_user_type_v039_backup (user_id, old_user_type, backed_up_at)
SELECT u.id, u.user_type, DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')
FROM bbs_user u
WHERE u.user_type = '1' AND u.is_delete = 0 AND u.id <> 1
  AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id = u.id AND m.status = 1)
ON DUPLICATE KEY UPDATE old_user_type=VALUES(old_user_type), backed_up_at=VALUES(backed_up_at);
  `);
}

function extractV039() {
  const text = fs.readFileSync(UPGRADE_SQL, 'utf8');
  const idx = text.indexOf('-- @migration: v039-moderator-role');
  if (idx < 0) throw new Error('upgrade-mysql.sql 中未找到 v039 块');
  const block = text.slice(idx);
  const out = path.join(TMP, 'v039-block.generated.sql');
  fs.writeFileSync(out, block, 'utf8');
  return { block, out };
}

function applyV039(block) {
  runMysql([DB], block);
  sql(`INSERT INTO bbs_schema_version (version, description, applied_at)
       VALUES ('v039-moderator-role', '版主账号角色（user_type=4）', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'))
       ON DUPLICATE KEY UPDATE applied_at=VALUES(applied_at);`);
}

function rollback() {
  sql(`
UPDATE bbs_user u
JOIN bbs_user_type_v039_backup b ON b.user_id = u.id
SET u.user_type = b.old_user_type
WHERE u.user_type = '4';

ALTER TABLE bbs_user
  MODIFY COLUMN user_type varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci
  NULL DEFAULT '1'
  COMMENT '用户类型（1：普通用户，2：管理员，3：超级管理员）';

DELETE FROM bbs_schema_version WHERE version='v039-moderator-role';
  `);
}

function main() {
  if (!fs.existsSync(TMP)) fs.mkdirSync(TMP, { recursive: true });
  const dbExists = query("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='bbs_moderator_test'");
  if (!dbExists.length) throw new Error('副本库 bbs_moderator_test 不存在，请先创建');

  const { block, out } = extractV039();
  evidence.generatedSql = out;
  setup();

  const baseline = {
    counts: counts(),
    candidates: query(`SELECT u.id FROM bbs_user u WHERE u.user_type='1' AND u.is_delete=0 AND u.id<>1
      AND EXISTS (SELECT 1 FROM bbs_board_moderator m WHERE m.user_id=u.id AND m.status=1) ORDER BY u.id`)
      .map(r => Number(r[0])),
    type4: query("SELECT id FROM bbs_user WHERE user_type='4' ORDER BY id").map(r => Number(r[0])),
    adminSuperWithBoard: query(`SELECT u.id, u.user_type FROM bbs_user u
      JOIN bbs_board_moderator m ON m.user_id=u.id AND m.status=1
      WHERE u.user_type IN ('2','3') ORDER BY u.id`).map(r => `${r[0]}:${r[1]}`),
    invalid: query("SELECT id FROM bbs_user WHERE user_type IS NULL OR TRIM(user_type)='' OR user_type NOT IN ('1','2','3','4') ORDER BY id")
      .map(r => Number(r[0])),
    comment: columnComment(),
    version: versionExists(),
  };
  evidence.baseline = baseline;
  dump(path.join(TMP, 'bbs_moderator_test_before_v039.sql'));

  check('G1', '迁移前基线：仅 9101 命中升级条件，版本未记录', () => {
    assert(baseline.candidates.length === 1 && baseline.candidates[0] === 9101, `candidates=${baseline.candidates}`);
    assert(baseline.type4.length === 1 && baseline.type4[0] === 9106, `type4=${baseline.type4}`);
    assert(baseline.adminSuperWithBoard.includes('9102:2') && baseline.adminSuperWithBoard.includes('9103:3'), `adminSuper=${baseline.adminSuperWithBoard}`);
    assert(baseline.invalid.join(',') === '9108', `invalid=${baseline.invalid}`);
    assert(baseline.version === false, 'v039 不应已记录');
    return `counts=${JSON.stringify(baseline.counts)}, candidates=[9101]`;
  });

  backupCandidates();
  const backupRows = query("SELECT user_id, old_user_type FROM bbs_user_type_v039_backup WHERE user_id BETWEEN 9101 AND 9110 ORDER BY user_id");
  evidence.backupRows = backupRows;
  check('G2', '迁移前备份表记录升级候选', () => {
    assert(backupRows.length === 1 && backupRows[0][0] === '9101' && backupRows[0][1] === '1', `backup=${JSON.stringify(backupRows)}`);
    return '9101 old_user_type=1';
  });

  applyV039(block);
  const after1 = {
    counts: counts(),
    upgraded: query("SELECT id, user_type FROM bbs_user WHERE id IN (9101,9102,9103,9104,9106,9107,9108) ORDER BY id"),
    type4: query("SELECT id FROM bbs_user WHERE user_type='4' ORDER BY id").map(r => Number(r[0])),
    invalid: query("SELECT id FROM bbs_user WHERE user_type IS NULL OR TRIM(user_type)='' OR user_type NOT IN ('1','2','3','4') ORDER BY id").map(r => Number(r[0])),
    adminSuperWithBoard: query(`SELECT u.id, u.user_type FROM bbs_user u
      JOIN bbs_board_moderator m ON m.user_id=u.id AND m.status=1
      WHERE u.user_type IN ('2','3') ORDER BY u.id`).map(r => `${r[0]}:${r[1]}`),
    comment: columnComment(),
    version: versionExists(),
  };
  evidence.afterFirstMigration = after1;

  check('G3', '第一次迁移：普通用户有板块升级为 4，管理员/超管保持 2/3', () => {
    const type1 = Number(after1.counts['1'] || 0);
    const type2 = Number(after1.counts['2'] || 0);
    const type3 = Number(after1.counts['3'] || 0);
    const type4 = Number(after1.counts['4'] || 0);
    const b1 = Number(baseline.counts['1'] || 0);
    const b2 = Number(baseline.counts['2'] || 0);
    const b3 = Number(baseline.counts['3'] || 0);
    const b4 = Number(baseline.counts['4'] || 0);
    // 9101: 1→4；9108: 非法 9→1；因此 1 的数量不变
    assert(type1 === b1, `type1 ${b1}→${type1}`);
    assert(type2 === b2 && type3 === b3, `2/3 变化 ${b2}/${b3}→${type2}/${type3}`);
    assert(type4 === b4 + 1, `type4 ${b4}→${type4}`);
    assert(after1.type4.includes(9101) && after1.type4.includes(9106), `type4 名单=${after1.type4}`);
    assert(after1.invalid.length === 0, `非法类型=${after1.invalid}`);
    assert(after1.adminSuperWithBoard.join(',') === baseline.adminSuperWithBoard.join(','), `管理员/超管变化 ${baseline.adminSuperWithBoard}→${after1.adminSuperWithBoard}`);
    const map = Object.fromEntries(after1.upgraded.map(r => [r[0], r[1]]));
    assert(map['9101'] === '4' && map['9102'] === '2' && map['9103'] === '3', `明细=${JSON.stringify(map)}`);
    assert(map['9107'] === '1', '已注销用户不应升级');
    assert(map['9108'] === '1', '非法类型应归一为 1');
    assert(map['9106'] === '4', '人工版主应保留');
    return `type4=${after1.type4}, 管理员/超管不变, 非法值=0`;
  });

  check('G4', '第一次迁移：列注释更新且 v039 已记录', () => {
    assert(after1.comment === EXPECTED_NEW_COMMENT, `comment=${after1.comment}`);
    assert(after1.version === true, 'v039 未记录');
    return `comment 含“4：版主”，version=v039-moderator-role`;
  });

  dump(path.join(TMP, 'bbs_moderator_test_before_rollback.sql'));
  rollback();
  const afterRollback = {
    counts: counts(),
    users: query("SELECT id, user_type FROM bbs_user WHERE id IN (9101,9102,9103,9104,9106,9107,9108) ORDER BY id"),
    type4: query("SELECT id FROM bbs_user WHERE user_type='4' ORDER BY id").map(r => Number(r[0])),
    invalid: query("SELECT id FROM bbs_user WHERE user_type IS NULL OR TRIM(user_type)='' OR user_type NOT IN ('1','2','3','4') ORDER BY id").map(r => Number(r[0])),
    comment: columnComment(),
    version: versionExists(),
  };
  evidence.afterRollback = afterRollback;

  check('G5', '回滚：9101 回到 1，人工版主 9106 保留，列注释与版本记录恢复', () => {
    const map = Object.fromEntries(afterRollback.users.map(r => [r[0], r[1]]));
    assert(map['9101'] === '1', `9101=${map['9101']}`);
    assert(map['9102'] === '2' && map['9103'] === '3', `admin/super=${map['9102']}/${map['9103']}`);
    assert(map['9106'] === '4', `人工版主 9106=${map['9106']}`);
    assert(afterRollback.invalid.length === 0, `非法类型=${afterRollback.invalid}`);
    assert(afterRollback.comment === EXPECTED_OLD_COMMENT, `comment=${afterRollback.comment}`);
    assert(afterRollback.version === false, 'v039 版本记录未删除');
    return `type4=${afterRollback.type4}, comment 已恢复, version 已删除`;
  });

  applyV039(block);
  const after2 = {
    counts: counts(),
    type4: query("SELECT id FROM bbs_user WHERE user_type='4' ORDER BY id").map(r => Number(r[0])),
    users: query("SELECT id, user_type FROM bbs_user WHERE id IN (9101,9102,9103,9104,9106,9107,9108) ORDER BY id"),
    invalid: query("SELECT id FROM bbs_user WHERE user_type IS NULL OR TRIM(user_type)='' OR user_type NOT IN ('1','2','3','4') ORDER BY id").map(r => Number(r[0])),
    comment: columnComment(),
    version: versionExists(),
  };
  evidence.afterSecondMigration = after2;

  check('G6', '再次迁移：结果与第一次一致（幂等/可重跑）', () => {
    assert(JSON.stringify(after2.counts) === JSON.stringify(after1.counts), `counts ${JSON.stringify(after1.counts)} vs ${JSON.stringify(after2.counts)}`);
    assert(JSON.stringify(after2.type4) === JSON.stringify(after1.type4), `type4 ${after1.type4} vs ${after2.type4}`);
    assert(JSON.stringify(after2.users) === JSON.stringify(after1.upgraded), '用户明细不一致');
    assert(after2.invalid.length === 0, `非法类型=${after2.invalid}`);
    assert(after2.comment === EXPECTED_NEW_COMMENT, `comment=${after2.comment}`);
    assert(after2.version === true, 'v039 未记录');
    return `counts=${JSON.stringify(after2.counts)}, type4=${after2.type4}`;
  });

  const passed = checks.filter(c => c.status === 'PASS').length;
  const failed = checks.filter(c => c.status === 'FAIL');
  const summary = { db: DB, generatedSql: out, total: checks.length, passed, failed: failed.length, checks, evidence };
  const resPath = path.join(TMP, 'moderator-migration-results.json');
  fs.writeFileSync(resPath, JSON.stringify(summary, null, 2), 'utf8');
  console.log('\n================ SUMMARY ================');
  console.log(`TOTAL=${checks.length} PASS=${passed} FAIL=${failed.length}`);
  for (const f of failed) console.log(`${f.id} ${f.name} :: ${f.detail}`);
  console.log(`RESULT_JSON=${resPath}`);
  process.exitCode = failed.length ? 1 : 0;
}

main();
