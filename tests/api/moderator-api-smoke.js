#!/usr/bin/env node
/**
 * 版主角色化 API 权限 / 数据范围冒烟测试
 *
 * 前置：
 *   - MySQL bbs_e2e_test 已执行 tests/fixtures/moderator-seed.sql
 *   - 后端以 e2e profile 启动（默认 http://127.0.0.1:9084/bbs-server）
 *
 * 运行：
 *   node tests/api/moderator-api-smoke.js
 *
 * 说明：接口权限失败统一返回 HTTP 200 + body.code=403（GlobalExceptionHandler），
 * 因此断言兼容 HTTP 403 与 body.code=403 两种形态。
 */
const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const BASE = process.env.BBS_E2E_BASE || 'http://127.0.0.1:9084/bbs-server';
const PASSWORD = '1234@abcD';
const ARTICLE_BOARD = {
  9001: 9001, 9002: 9001, 9003: 9002, 9004: 9003, 9005: 9002, 9006: 9001, 9007: 9001,
};

const results = [];
let currentGroup = '';

function group(name) { currentGroup = name; }

async function call(method, urlPath, { token, body, query } = {}) {
  let url = BASE + urlPath;
  if (query) {
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v !== undefined && v !== null) qs.append(k, String(v));
    }
    const s = qs.toString();
    if (s) url += (url.includes('?') ? '&' : '?') + s;
  }
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = token;
  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = { raw: text }; }
  return { status: res.status, data };
}

function isForbidden(r) {
  return r.status === 403 || (r.data && Number(r.data.code) === 403);
}
function msgOf(r) {
  return (r.data && (r.data.message || r.data.msg)) || '';
}
function assert(cond, message) {
  if (!cond) throw new Error(message);
}

async function test(id, name, fn) {
  try {
    const detail = await fn();
    results.push({ id, group: currentGroup, name, status: 'PASS', detail: detail || '' });
    console.log(`[PASS] ${id} ${name}${detail ? ' :: ' + detail : ''}`);
  } catch (e) {
    results.push({ id, group: currentGroup, name, status: 'FAIL', detail: e.message });
    console.log(`[FAIL] ${id} ${name} :: ${e.message}`);
  }
}

async function login(username) {
  const r = await call('POST', '/common/login', {
    body: { username, password: PASSWORD, channel: '02' },
  });
  return r;
}
async function loginOk(username) {
  const r = await login(username);
  assert(r.data && Number(r.data.code) === 200, `${username} 登录失败: ${r.status} ${msgOf(r)}`);
  const obj = r.data.obj;
  const token = obj.tokenHead + obj.token;
  return { token, user: obj.user };
}

function mysqlQuery(sql, db = 'bbs_e2e_test') {
  const out = execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '-N', '-B', db, '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8' });
  return out.trim() ? out.trim().split(/\r?\n/).map(line => line.split('\t')) : [];
}

function mysqlExec(sql, db = 'bbs_e2e_test') {
  execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '-N', '-B', db, '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8' });
}

function systemGroupId(baseRole) {
  const rows = mysqlQuery(`SELECT id FROM bbs_permission_group WHERE base_role='${baseRole}' ORDER BY id LIMIT 1`);
  assert(rows.length === 1, `缺少系统角色组 base_role=${baseRole}`);
  return Number(rows[0][0]);
}

function revokeGroupCodes(groupId, codes) {
  const values = codes.map(code => `(${groupId}, '${code}', 'revoke', NOW())`).join(',');
  mysqlExec(`INSERT INTO bbs_permission_group_item (group_id, permission_code, effect, create_time)
             VALUES ${values}
             ON DUPLICATE KEY UPDATE effect='revoke'`);
}

function clearGroupRevokes(groupId, codes) {
  if (!codes || codes.length === 0) return;
  const inList = codes.map(code => `'${code}'`).join(',');
  mysqlExec(`DELETE FROM bbs_permission_group_item
             WHERE group_id=${groupId} AND effect='revoke' AND permission_code IN (${inList})`);
}

const ALL_PERMISSION_CODES = [
  'admin:access', 'user:read', 'user:create', 'user:update', 'user:delete', 'user:grant:admin',
  'config:read', 'config:write', 'permission:manage', 'audit:read',
  'article:read', 'article:review', 'article:delete', 'article:feature', 'article:suggestion:adopt',
  'report:review', 'appeal:review', 'violation:review', 'violation:add', 'violation:cancel',
  'moderator:manage', 'moderator:complaint:review', 'adopt:review',
  'featured:recommend', 'featured:review', 'points:read', 'points:adjust', 'dashboard:read',
  'post:create', 'post:update:own', 'post:delete:own', 'comment:create', 'comment:delete:own',
  'reply:create', 'reply:delete:own', 'report:submit', 'appeal:submit', 'moderator:complain',
  'upload:file', 'profile:update', 'password:update'
];

async function main() {
  // ---------- A. 登录 ----------
  group('A. 登录');

  await test('A1', 'normal1 用 channel=02 登录失败', async () => {
    const r = await login('NORMAL1');
    assert(r.data && Number(r.data.code) !== 200, `期望失败，实际 code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  let mod0Token;
  await test('A2', 'mod0 登录成功且角色/权限/板块范围正确', async () => {
    const { token, user } = await loginOk('MOD0');
    mod0Token = token;
    assert(user.role === 'MODERATOR', `role=${user.role}`);
    assert(Array.isArray(user.permissions), 'permissions 非数组');
    assert(user.permissions.includes('article:read'), '缺少 article:read');
    assert(!user.permissions.includes('user:read'), '不应包含 user:read');
    assert(!user.permissions.includes('featured:review'), '不应包含 featured:review');
    assert(Array.isArray(user.boardScopes) && user.boardScopes.length === 0, `boardScopes=${JSON.stringify(user.boardScopes)}`);
    return `role=MODERATOR, permissions=${user.permissions.length}, boardScopes=[]`;
  });

  let mod1Token;
  await test('A3', 'mod1 登录成功且 boardScopes 含自己的板块', async () => {
    const { token, user } = await loginOk('MOD1');
    mod1Token = token;
    assert(user.role === 'MODERATOR', `role=${user.role}`);
    assert((user.boardScopes || []).some(b => Number(b.labelId) === 9001), `boardScopes=${JSON.stringify(user.boardScopes)}`);
    return `boardScopes=${JSON.stringify((user.boardScopes || []).map(b => b.labelId))}`;
  });

  let superToken;
  await test('A4', 'super1 登录成功 role=SUPER_ADMIN', async () => {
    const { token, user } = await loginOk('SUPER1');
    superToken = token;
    assert(user.role === 'SUPER_ADMIN', `role=${user.role}`);
    return `permissions=${user.permissions.length}`;
  });

  let admin1Token;
  await test('A5', 'admin1 登录成功 role=ADMIN 且有板块任职', async () => {
    const { token, user } = await loginOk('ADMIN1');
    admin1Token = token;
    assert(user.role === 'ADMIN', `role=${user.role}`);
    assert((user.boardScopes || []).some(b => Number(b.labelId) === 9001), 'admin1 板块任职未返回');
    return 'role=ADMIN, boardScopes 含 9001';
  });

  await test('A6', '/common/user/info 返回角色/权限/板块范围（刷新会话不丢）', async () => {
    const r = await call('GET', '/common/user/info', { token: mod1Token });
    assert(r.data && r.data.role === 'MODERATOR', `role=${r.data && r.data.role}`);
    assert(Array.isArray(r.data.permissions) && r.data.permissions.includes('article:read'), 'permissions 缺失');
    assert(!r.data.permissions.includes('featured:review'), '不应包含 featured:review');
    assert((r.data.boardScopes || []).some(b => Number(b.labelId) === 9001), 'boardScopes 缺失');
    return 'role/permissions/boardScopes 齐全';
  });

  // ---------- B. 数据范围 ----------
  group('B. 数据范围');
  await test('B1', 'mod1 文章列表只返回自己板块', async () => {
    const r = await call('POST', '/admin/article/list', { token: mod1Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const list = r.data.obj.list || [];
    assert(list.length > 0, '列表为空');
    assert(list.every(a => Number(a.articleLabelId) === 9001), '包含非本板块文章');
    return `total=${r.data.obj.total}, 全部为板块 9001`;
  });

  await test('B2', 'mod1 传其他 labelId 返回空', async () => {
    const r = await call('POST', '/admin/article/list', { token: mod1Token, body: { page: 1, size: 100, labelId: 9002 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    assert(Number(r.data.obj.total) === 0, `total=${r.data.obj.total}`);
    assert((r.data.obj.list || []).length === 0, '列表非空');
    return 'total=0';
  });

  await test('B3', 'mod1 看其他板块文章详情 403', async () => {
    const r = await call('GET', '/admin/getArticleByArticleId/9003', { token: mod1Token });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B4', 'mod1 看本板块文章详情成功', async () => {
    const r = await call('GET', '/admin/getArticleByArticleId/9001', { token: mod1Token });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    return '200';
  });

  await test('B5', 'mod1 举报列表只返回自己板块', async () => {
    const r = await call('POST', '/admin/report/list', { token: mod1Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const records = r.data.obj.records || [];
    assert(records.length > 0, '列表为空');
    assert(records.every(x => Number(x.targetId) === 9001), `targetIds=${records.map(x => x.targetId)}`);
    return `total=${r.data.obj.total}`;
  });

  await test('B6', 'mod1 违规列表只返回自己板块', async () => {
    const r = await call('POST', '/admin/violation/list', { token: mod1Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const records = r.data.obj.records || [];
    assert(records.length > 0, '列表为空');
    const bad = records.filter(x => ARTICLE_BOARD[Number(x.relatedId)] !== 9001);
    assert(bad.length === 0, `跨板块记录=${JSON.stringify(bad.map(x => x.relatedId))}`);
    return `total=${r.data.obj.total}`;
  });

  await test('B7', 'mod1 申诉列表只返回自己板块', async () => {
    const r = await call('POST', '/admin/appeal/list', { token: mod1Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const records = r.data.obj.records || [];
    assert(records.some(x => Number(x.id) === 9001), '缺少本板块申诉 9001');
    assert(!records.some(x => Number(x.id) === 9002), '包含跨板块申诉 9002');
    return `total=${r.data.obj.total}`;
  });

  await test('B8', 'mod1 待审批采纳只返回自己板块', async () => {
    const r = await call('POST', '/reply/admin/pendingAdopts', { token: mod1Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const records = r.data.obj.records || [];
    assert(records.length > 0, '列表为空');
    assert(records.every(x => Number(x.articleId) === 9001), `articleIds=${records.map(x => x.articleId)}`);
    return `total=${r.data.obj.total}`;
  });

  await test('B9', 'mod1 仪表盘只统计自己板块且用户/评论为 0', async () => {
    const r = await call('POST', '/admin/dashboard/counts', { token: mod1Token, body: {} });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    const c = r.data.obj;
    assert(Number(c.userCount) === 0, `userCount=${c.userCount}`);
    assert(Number(c.commentCount) === 0, `commentCount=${c.commentCount}`);
    assert(Number(c.articleCount) === 4, `articleCount=${c.articleCount}`);
    return `articleCount=${c.articleCount}, userCount=0, commentCount=0`;
  });

  await test('B10', 'mod1 本板块设精华通过', async () => {
    const r = await call('POST', '/admin/featured/set', { token: mod1Token, body: { articleId: 9007, isFeatured: 1 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B11', 'mod1 跨板块设精华 403', async () => {
    const r = await call('POST', '/admin/featured/set', { token: mod1Token, body: { articleId: 9003, isFeatured: 1 } });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B12', 'mod1 本板块采纳建议通过', async () => {
    const r = await call('POST', '/admin/suggestion/adopt', { token: mod1Token, body: { articleId: 9002 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B13', 'mod1 跨板块采纳建议 403', async () => {
    const r = await call('POST', '/admin/suggestion/adopt', { token: mod1Token, body: { articleId: 9003 } });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B14', 'mod1 本板块举报审核通过', async () => {
    const r = await call('POST', '/admin/report/review', {
      token: mod1Token, body: { reportId: 9001, reviewerId: 9003, status: 'rejected', remark: 'E2E' },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B15', 'mod1 跨板块举报审核 403', async () => {
    const r = await call('POST', '/admin/report/review', {
      token: mod1Token, body: { reportId: 9002, reviewerId: 9003, status: 'rejected', remark: 'E2E' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  let newViolationId;
  await test('B16', 'mod1 本板块新增违规通过', async () => {
    const r = await call('POST', '/admin/violation/add', {
      token: mod1Token,
      body: { userId: 9007, violationType: 'spam', relatedType: 'article', relatedId: 9002, operatorId: 9003, remark: 'E2E新增违规' },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery('SELECT id FROM bbs_violation WHERE remark=\'E2E新增违规\' ORDER BY id DESC LIMIT 1');
    newViolationId = rows.length ? Number(rows[0][0]) : null;
    return `violationId=${newViolationId}`;
  });

  await test('B17', 'mod1 跨板块新增违规 403', async () => {
    const r = await call('POST', '/admin/violation/add', {
      token: mod1Token,
      body: { userId: 9007, violationType: 'spam', relatedType: 'article', relatedId: 9003, operatorId: 9003, remark: 'E2E跨板块' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B18', 'mod1 本板块取消违规通过', async () => {
    const r = await call('POST', '/admin/violation/cancel', {
      token: mod1Token, body: { violationId: 9003, reason: 'E2E取消' },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B19', 'mod1 跨板块取消违规 403', async () => {
    const r = await call('POST', '/admin/violation/cancel', {
      token: mod1Token, body: { violationId: 9002, reason: 'E2E跨板块' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B20', 'mod1 本板块申诉审核通过', async () => {
    const r = await call('POST', '/admin/appeal/review', {
      token: mod1Token, body: { appealId: 9001, reviewerId: 9003, status: 'rejected', remark: 'E2E' },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B21', 'mod1 跨板块申诉审核 403', async () => {
    const r = await call('POST', '/admin/appeal/review', {
      token: mod1Token, body: { appealId: 9002, reviewerId: 9003, status: 'rejected', remark: 'E2E' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B22', 'mod1 本板块采纳审批通过', async () => {
    const r = await call('POST', '/reply/admin/approveAdopt', {
      token: mod1Token, body: { replyId: 9001, articleId: 9001, action: 'reject' },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B23', 'mod1 跨板块采纳审批 403', async () => {
    const r = await call('POST', '/reply/admin/approveAdopt', {
      token: mod1Token, body: { replyId: 9002, articleId: 9003, action: 'reject' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  await test('B24', 'Q5: mod1 不能终审精华推荐（featured:review）', async () => {
    const r = await call('POST', '/admin/featured/review', {
      token: mod1Token, body: { recommendationId: 9001, status: 'rejected', remark: 'E2E' },
    });
    assert(isForbidden(r), `期望 403，实际 status=${r.status}, code=${r.data && r.data.code}, msg=${msgOf(r)}`);
    return msgOf(r);
  });

  await test('B25', 'admin1 文章列表/详情全局可见（Q1）', async () => {
    const list = await call('POST', '/admin/article/list', { token: admin1Token, body: { page: 1, size: 200 } });
    assert(Number(list.data.code) === 200, `code=${list.data.code}`);
    const labels = new Set((list.data.obj.list || []).map(a => Number(a.articleLabelId)));
    assert(labels.has(9001) && labels.has(9002) && labels.has(9003), `labels=${[...labels]}`);
    const detail = await call('GET', '/admin/getArticleByArticleId/9003', { token: admin1Token });
    assert(Number(detail.data.code) === 200, `detail code=${detail.data.code}`);
    return `total=${list.data.obj.total}, labels=${[...labels].join(',')}`;
  });

  await test('B26', 'admin1 仪表盘为全局口径', async () => {
    const [dash, reports] = await Promise.all([
      call('POST', '/admin/dashboard/counts', { token: admin1Token, body: {} }),
      call('POST', '/admin/report/list', { token: admin1Token, body: { page: 1, size: 100 } }),
    ]);
    assert(Number(dash.data.code) === 200, `dash code=${dash.data.code}`);
    assert(Number(dash.data.obj.userCount) > 0, `userCount=${dash.data.obj.userCount}`);
    assert(Number(dash.data.obj.commentCount) > 0, `commentCount=${dash.data.obj.commentCount}`);
    const ids = (reports.data.obj.records || []).map(x => Number(x.id));
    assert(ids.includes(9001) && ids.includes(9002), `reports=${ids}`);
    return `userCount=${dash.data.obj.userCount}, reports=${ids}`;
  });

  // ---------- C. 无板块态 ----------
  group('C. 无板块态');

  await test('C1', 'mod0 业务列表 200 且为空', async () => {
    const r = await call('POST', '/admin/article/list', { token: mod0Token, body: { page: 1, size: 100 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    assert(Number(r.data.obj.total) === 0 && (r.data.obj.list || []).length === 0, `total=${r.data.obj.total}`);
    return 'total=0';
  });

  for (const [id, name, path] of [
    ['C2', '举报列表', '/admin/report/list'],
    ['C3', '违规列表', '/admin/violation/list'],
    ['C4', '申诉列表', '/admin/appeal/list'],
    ['C5', '待审批采纳', '/reply/admin/pendingAdopts'],
  ]) {
    await test(id, `mod0 ${name}为空`, async () => {
      const r = await call('POST', path, { token: mod0Token, body: { page: 1, size: 100 } });
      assert(Number(r.data.code) === 200, `code=${r.data.code}`);
      assert(Number(r.data.obj.total) === 0, `total=${r.data.obj.total}`);
      return 'total=0';
    });
  }

  await test('C6', 'mod0 仪表盘全 0', async () => {
    const r = await call('POST', '/admin/dashboard/counts', { token: mod0Token, body: {} });
    assert(Number(r.data.code) === 200, `code=${r.data.code}`);
    for (const k of ['userCount', 'commentCount', 'articleCount', 'reportPending', 'violationActive', 'appealPending']) {
      assert(Number(r.data.obj[k]) === 0, `${k}=${r.data.obj[k]}`);
    }
    return '全部为 0';
  });

  const mod0Writes = [
    ['C7', '设精华', '/admin/featured/set', { articleId: 9001, isFeatured: 1 }],
    ['C8', '采纳建议', '/admin/suggestion/adopt', { articleId: 9002 }],
    ['C9', '举报审核', '/admin/report/review', { reportId: 9002, reviewerId: 9005, status: 'rejected' }],
    ['C10', '新增违规', '/admin/violation/add', { userId: 9006, violationType: 'spam', relatedType: 'article', relatedId: 9001, operatorId: 9005 }],
    ['C11', '取消违规', '/admin/violation/cancel', { violationId: 9001, reason: 'E2E' }],
    ['C12', '申诉审核', '/admin/appeal/review', { appealId: 9001, status: 'rejected' }],
    ['C13', '采纳审批', '/reply/admin/approveAdopt', { replyId: 9001, articleId: 9001, action: 'reject' }],
  ];
  for (const [id, name, path, body] of mod0Writes) {
    await test(id, `mod0 ${name} 403 且提示尚未分配板块`, async () => {
      const r = await call('POST', path, { token: mod0Token, body });
      assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
      assert(msgOf(r).includes('尚未分配板块，请联系管理员'), `message=${msgOf(r)}`);
      return msgOf(r);
    });
  }

  // ---------- D. 系统管理 403 ----------
  group('D. 系统管理 403');

  const systemCalls = [
    ['D1', 'GET', '/admin/user/search', { query: { keyword: 'test' } }],
    ['D2', 'POST', '/admin/addUser', { body: { username: 'NORMAL1', nickname: 'x', orgNo: '51404' } }],
    ['D3', 'POST', '/admin/updateUserDetail', { body: { id: 9006, nickname: 'E2E-NORMAL1' } }],
    ['D4', 'POST', '/admin/listDict', { body: {} }],
    ['D5', 'POST', '/admin/moderator/list', { body: { page: 1, size: 10 } }],
    ['D6', 'POST', '/admin/moderator/boards', { body: { userId: 9006, labelIds: [9001] } }],
    ['D7', 'POST', '/admin/moderatorComplaint/list', { body: { page: 1, size: 10 } }],
    ['D8', 'POST', '/admin/systemConfig/list', { body: {} }],
    ['D9', 'POST', '/admin/points/adjust', { body: { userId: 9006, pointsChange: 1, reason: 'E2E-D' } }],
    ['D10', 'GET', '/sensitiveWord/getList', {}],
    ['D11', 'POST', '/getAllUser', { body: {} }],
    ['D12', 'POST', '/updateUserRole', { body: { userId: 9006, roleType: '01' } }],
  ];

  for (const [id, method, path, opts] of systemCalls) {
    await test(id, `mod1 系统接口 ${method} ${path} 403`, async () => {
      const r = await call(method, path, { token: mod1Token, ...opts });
      assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
      return msgOf(r);
    });
  }
  await test('D13', 'mod0 系统接口抽样 403', async () => {
    for (const [, method, path, opts] of systemCalls.slice(0, 6)) {
      const r = await call(method, path, { token: mod0Token, ...opts });
      assert(isForbidden(r), `${path}: status=${r.status}, code=${r.data && r.data.code}`);
    }
    return '6 个系统接口均为 403';
  });
  await test('D14', 'ADMIN 可访问系统接口（user:grant:admin 除外）', async () => {
    const details = [];
    // 管理员访问验证只证明“未被 403”；写接口传空参数触发业务错误即可，避免副作用干扰后续生命周期用例
    const noMutation = { D3: { body: {} }, D6: { body: {} }, D9: { body: {} }, D12: { body: {} } };
    for (const [id, method, path, opts] of systemCalls) {
      const callOpts = noMutation[id] || opts;
      const r = await call(method, path, { token: admin1Token, ...callOpts });
      const forbidden = isForbidden(r);
      details.push(`${id}:${r.data && r.data.code}`);
      if (id === 'D12') {
        // 角色变更映射到 user:grant:admin，管理员默认不具备，仅超管可操作
        assert(forbidden, `ADMIN 不应拥有 user:grant:admin: ${path}`);
        continue;
      }
      assert(!forbidden, `${path} 被 403`);
    }
    return details.join(',');
  });
  await test('D15', 'MODERATOR 不能查看他人积分明细（全局数据范围）', async () => {
    const r = await call('GET', '/admin/points/log/9006', { token: mod1Token });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });
  await test('D16', 'MODERATOR 不能按单位统计全局文章（全局数据范围）', async () => {
    const r = await call('POST', '/admin/articleStatisticByOrg', {
      token: mod1Token, body: { orgNo: '51404' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });
  await test('D17', 'ADMIN 仍可查看积分明细与单位统计', async () => {
    const log = await call('GET', '/admin/points/log/9006', { token: admin1Token });
    assert(!isForbidden(log), `points/log 被 403: ${msgOf(log)}`);
    const stat = await call('POST', '/admin/articleStatisticByOrg', {
      token: admin1Token, body: { orgNo: '51404' },
    });
    assert(!isForbidden(stat), `articleStatisticByOrg 被 403: ${msgOf(stat)}`);
    return `pointsLog=${log.data && log.data.code}, statistic=${stat.data && stat.data.code}`;
  });
  await test('D18', 'MODERATOR 不能导出全局积分排名（全局数据范围）', async () => {
    const r = await call('GET', '/admin/points/export', {
      token: mod1Token, query: { rankType: '01' },
    });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });
  await test('D19', 'ADMIN 可导出全局积分排名', async () => {
    const r = await call('GET', '/admin/points/export', {
      token: admin1Token, query: { rankType: '01' },
    });
    assert(!isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return `status=${r.status}`;
  });
  await test('D20', 'ADMIN 拥有 config:write（配置写接口非 403）', async () => {
    const r = await call('POST', '/admin/systemConfig/update', { token: admin1Token, body: {} });
    assert(!isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });
  await test('D21', 'MODERATOR 不具备 config:write', async () => {
    const r = await call('POST', '/admin/systemConfig/update', { token: mod1Token, body: {} });
    assert(isForbidden(r), `status=${r.status}, code=${r.data && r.data.code}`);
    return msgOf(r);
  });

  // ---------- E. 生命周期 ----------
  group('E. 生命周期');

  await test('E1', 'normal1 任命版主后 user_type 1 → 4', async () => {
    const r = await call('POST', '/admin/moderator/appoint', {
      token: superToken, body: { userId: 9006, labelId: 9001 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery(`SELECT u.user_type, g.group_code FROM bbs_user u
      LEFT JOIN bbs_permission_group g ON g.id=u.primary_group_id WHERE u.id=9006`);
    assert(rows[0][0] === '4', `user_type=${rows[0][0]}`);
    assert(rows[0][1] === 'role_moderator', `primary_group=${rows[0][1]}`);
    return 'user_type=4, primary_group=role_moderator';
  });

  await test('E2', 'admin1 任命版主后 user_type 仍为 2', async () => {
    const r = await call('POST', '/admin/moderator/appoint', {
      token: superToken, body: { userId: 9002, labelId: 9002 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery(`SELECT u.user_type, g.group_code FROM bbs_user u
      LEFT JOIN bbs_permission_group g ON g.id=u.primary_group_id WHERE u.id=9002`);
    assert(rows[0][0] === '2', `user_type=${rows[0][0]}`);
    assert(rows[0][1] === 'role_admin', `primary_group=${rows[0][1]}`);
    return 'user_type=2, primary_group=role_admin';
  });

  await test('E3', '撤销 normal1 最后一个板块后仍为版主且可登录、boardScopes=[]', async () => {
    const r = await call('POST', '/admin/moderator/dismiss', {
      token: superToken, body: { userId: 9006, labelId: 9001 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery(`SELECT u.user_type, g.group_code FROM bbs_user u
      LEFT JOIN bbs_permission_group g ON g.id=u.primary_group_id WHERE u.id=9006`);
    assert(rows[0][0] === '4', `user_type=${rows[0][0]}`);
    assert(rows[0][1] === 'role_moderator', `撤销版主后 primary_group 应保留: ${rows[0][1]}`);
    const { user } = await loginOk('NORMAL1');
    assert(user.role === 'MODERATOR', `role=${user.role}`);
    assert((user.boardScopes || []).length === 0, `boardScopes=${JSON.stringify(user.boardScopes)}`);
    return 'user_type=4, primary_group=role_moderator, login ok, boardScopes=[]';
  });

  await test('E4', '/admin/moderator/boards 差异更新 + 幂等', async () => {
    const first = await call('POST', '/admin/moderator/boards', {
      token: superToken, body: { userId: 9006, labelIds: [9002, 9003] },
    });
    assert(Number(first.data.code) === 200, `code=${first.data.code} ${msgOf(first)}`);
    const f = first.data.obj;
    assert(JSON.stringify(f.added) === '[9002,9003]' && f.removed.length === 0 && f.kept.length === 0,
      `第一次 added=${f.added}, removed=${f.removed}, kept=${f.kept}`);

    const second = await call('POST', '/admin/moderator/boards', {
      token: superToken, body: { userId: 9006, labelIds: [9002, 9003] },
    });
    assert(Number(second.data.code) === 200, `code=${second.data.code}`);
    const s = second.data.obj;
    assert(s.added.length === 0 && s.removed.length === 0 && JSON.stringify(s.kept.sort()) === '[9002,9003]',
      `第二次 added=${s.added}, removed=${s.removed}, kept=${s.kept}`);

    const third = await call('POST', '/admin/moderator/boards', {
      token: superToken, body: { userId: 9006, labelIds: [9003] },
    });
    assert(Number(third.data.code) === 200, `code=${third.data.code}`);
    const t = third.data.obj;
    assert(t.added.length === 0 && JSON.stringify(t.removed) === '[9002]' && JSON.stringify(t.kept) === '[9003]',
      `第三次 added=${t.added}, removed=${t.removed}, kept=${t.kept}`);
    return 'added/removed/kept 与幂等均正确';
  });

  // ---------- F. Q9 ----------
  group('F. Q9 删除板块拦截');

  await test('F1', '仍有有效版主时删除 label 被拒绝', async () => {
    const r = await call('POST', '/admin/delArticleLabel', { token: superToken, body: { labelId: 9005 } });
    assert(Number(r.data.code) !== 200, `期望失败，实际 code=${r.data.code}`);
    assert(msgOf(r).includes('该板块仍有 1 位版主'), `message=${msgOf(r)}`);
    const rows = mysqlQuery('SELECT COUNT(*) FROM bbs_article_label WHERE label_id=9005');
    assert(Number(rows[0][0]) === 1, '标签已被删除');
    return msgOf(r);
  });

  await test('F2', '撤销全部版主后可删除 label', async () => {
    const d = await call('POST', '/admin/moderator/dismiss', { token: superToken, body: { userId: 9007, labelId: 9005 } });
    assert(Number(d.data.code) === 200, `dismiss code=${d.data.code} ${msgOf(d)}`);
    const r = await call('POST', '/admin/delArticleLabel', { token: superToken, body: { labelId: 9005 } });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery('SELECT COUNT(*) FROM bbs_article_label WHERE label_id=9005');
    assert(Number(rows[0][0]) === 0, '标签仍存在');
    return msgOf(r);
  });

  // ---------- G. 权限组即角色 ----------
  group('G. 权限组即角色');

  let moderatorGroupId;
  let adminGroupId;
  let superGroupId;
  await test('G1', '登录返回主权限组，四个系统角色组存在', async () => {
    moderatorGroupId = systemGroupId('MODERATOR');
    adminGroupId = systemGroupId('ADMIN');
    superGroupId = systemGroupId('SUPER_ADMIN');
    systemGroupId('USER');

    const { user } = await loginOk('MOD2');
    assert(Number(user.primaryGroupId) === moderatorGroupId, `primaryGroupId=${user.primaryGroupId}`);
    assert(user.primaryGroupCode === 'role_moderator', `primaryGroupCode=${user.primaryGroupCode}`);
    assert(user.primaryGroupName === '版主', `primaryGroupName=${user.primaryGroupName}`);
    return `moderator=${moderatorGroupId}, admin=${adminGroupId}, super=${superGroupId}`;
  });

  await test('G2', '版主组收回 article:read：登录权限不含且文章列表 403，恢复后 200', async () => {
    const affected = ['article:read'];
    revokeGroupCodes(moderatorGroupId, affected);
    try {
      const { token, user } = await loginOk('MOD2');
      assert(!user.permissions.includes('article:read'), '收回后仍包含 article:read');
      const forbidden = await call('POST', '/admin/article/list', {
        token, body: { page: 1, size: 10 },
      });
      assert(isForbidden(forbidden), `收回后列表未 403: code=${forbidden.data && forbidden.data.code}`);
    } finally {
      clearGroupRevokes(moderatorGroupId, affected);
    }
    const { token, user } = await loginOk('MOD2');
    assert(user.permissions.includes('article:read'), '恢复后仍缺少 article:read');
    const ok = await call('POST', '/admin/article/list', { token, body: { page: 1, size: 10 } });
    assert(Number(ok.data.code) === 200, `恢复后列表 code=${ok.data.code}`);
    return '收回 403 → 恢复 200';
  });

  await test('G3', '版主组全部收回：channel=02 登录失败且有效权限为空，恢复后成功', async () => {
    revokeGroupCodes(moderatorGroupId, ALL_PERMISSION_CODES);
    try {
      const denied = await login('MOD2');
      assert(Number(denied.data && denied.data.code) !== 200,
        `期望登录失败，实际 code=${denied.data && denied.data.code}`);
      const effective = await call('GET', `/admin/permission/effective/9004`, { token: superToken });
      assert(Number(effective.data.code) === 200, `effective code=${effective.data.code}`);
      assert((effective.data.obj.permissions || []).length === 0,
        `有效权限未清空: ${JSON.stringify(effective.data.obj.permissions)}`);
    } finally {
      clearGroupRevokes(moderatorGroupId, ALL_PERMISSION_CODES);
    }
    const restored = await login('MOD2');
    assert(Number(restored.data && restored.data.code) === 200,
      `恢复后登录失败: code=${restored.data && restored.data.code}`);
    return '全部收回登录失败 → 恢复登录成功';
  });

  await test('G4', '管理员组收回 user:read：/admin/user/search 403，恢复后 200', async () => {
    const affected = ['user:read'];
    revokeGroupCodes(adminGroupId, affected);
    try {
      const forbidden = await call('GET', '/admin/user/search', {
        token: admin1Token, query: { keyword: 'NORMAL' },
      });
      assert(isForbidden(forbidden), `收回后未 403: code=${forbidden.data && forbidden.data.code}`);
    } finally {
      clearGroupRevokes(adminGroupId, affected);
    }
    const ok = await call('GET', '/admin/user/search', {
      token: admin1Token, query: { keyword: 'NORMAL' },
    });
    assert(Number(ok.data.code) === 200, `恢复后 code=${ok.data.code}`);
    return '收回 403 → 恢复 200';
  });

  await test('G5', '管理员组收回 admin:access：后台登录失败且旧 token 访问 /admin/** 403，恢复后正常', async () => {
    const affected = ['admin:access'];
    revokeGroupCodes(adminGroupId, affected);
    try {
      const denied = await login('ADMIN1');
      assert(Number(denied.data && denied.data.code) !== 200,
        `期望后台登录失败，实际 code=${denied.data && denied.data.code}`);
      const forbidden = await call('GET', '/admin/user/search', {
        token: admin1Token, query: { keyword: 'NORMAL' },
      });
      assert(isForbidden(forbidden), `旧 token 未被拦截: status=${forbidden.status}, code=${forbidden.data && forbidden.data.code}`);
    } finally {
      clearGroupRevokes(adminGroupId, affected);
    }
    const restored = await login('ADMIN1');
    assert(Number(restored.data && restored.data.code) === 200,
      `恢复后登录失败: code=${restored.data && restored.data.code}`);
    return '登录失败 + 旧 token 403 → 恢复成功';
  });

  await test('G6', '超管 4 个关键权限不可收回', async () => {
    const protectedCodes = ['admin:access', 'user:grant:admin', 'permission:manage', 'audit:read'];
    revokeGroupCodes(superGroupId, protectedCodes);
    try {
      const { user } = await loginOk('SUPER1');
      for (const code of protectedCodes) {
        assert(user.permissions.includes(code), `超管关键权限被收回: ${code}`);
      }
    } finally {
      clearGroupRevokes(superGroupId, protectedCodes);
    }
    return protectedCodes.join(',');
  });

  await test('G7', '系统角色不可删除/改编码/改基线/手动加成员', async () => {
    const deleted = await call('DELETE', `/admin/permission/groups/${superGroupId}`, { token: superToken });
    assert(Number(deleted.data.code) !== 200, `删除系统角色未拒绝: code=${deleted.data.code}`);
    assert(msgOf(deleted).includes('不可删除'), `message=${msgOf(deleted)}`);

    const codeChanged = await call('PUT', `/admin/permission/groups/${superGroupId}`, {
      token: superToken, body: { groupCode: 'role_super_hacked' },
    });
    assert(Number(codeChanged.data.code) !== 200, `改编码未拒绝: code=${codeChanged.data.code}`);
    assert(msgOf(codeChanged).includes('编码不可修改'), `message=${msgOf(codeChanged)}`);

    const roleChanged = await call('PUT', `/admin/permission/groups/${superGroupId}`, {
      token: superToken, body: { baseRole: 'ADMIN' },
    });
    assert(Number(roleChanged.data.code) !== 200, `改基线未拒绝: code=${roleChanged.data.code}`);
    assert(msgOf(roleChanged).includes('基线不可修改'), `message=${msgOf(roleChanged)}`);

    const assigned = await call('POST', `/admin/permission/groups/${superGroupId}/users`, {
      token: superToken, body: { userIds: [9006] },
    });
    assert(Number(assigned.data.code) !== 200, `手动加成员未拒绝: code=${assigned.data.code}`);
    assert(msgOf(assigned).includes('自动应用'), `message=${msgOf(assigned)}`);
    return '4 项保护均生效';
  });

  await test('G8', '权限矩阵/权限组返回 system/autoApply/roleUserCount', async () => {
    const groups = await call('GET', '/admin/permission/groups', { token: superToken });
    assert(Number(groups.data.code) === 200, `groups code=${groups.data.code}`);
    const superRow = (groups.data.obj || []).find(g => g.groupCode === 'role_super');
    assert(superRow && superRow.system === true, `system=${superRow && superRow.system}`);
    assert(superRow.autoApply === true, `autoApply=${superRow && superRow.autoApply}`);
    assert(Number(superRow.roleUserCount) >= 1, `roleUserCount=${superRow && superRow.roleUserCount}`);

    const matrix = await call('GET', '/admin/permission/matrix', { token: superToken });
    assert(Number(matrix.data.code) === 200, `matrix code=${matrix.data.code}`);
    const matrixSuper = (matrix.data.obj.groups || []).find(g => g.groupCode === 'role_super');
    assert(matrixSuper && matrixSuper.system === true && matrixSuper.autoApply === true,
      `matrix super=${JSON.stringify(matrixSuper)}`);
    return `roleUserCount=${superRow.roleUserCount}`;
  });

  await test('G9', '主组停用后回退 user_type 基线，展示主组同步回退', async () => {
    const original = mysqlQuery('SELECT primary_group_id FROM bbs_user WHERE id=9004');
    try {
      mysqlExec(`UPDATE bbs_user SET primary_group_id=${adminGroupId} WHERE id=9004`);
      const asAdmin = await loginOk('MOD2');
      assert(asAdmin.user.permissions.includes('user:read'), '主组 ADMIN 未生效');

      mysqlExec(`UPDATE bbs_permission_group SET enabled=0 WHERE id=${adminGroupId}`);
      const fallback = await loginOk('MOD2');
      assert(!fallback.user.permissions.includes('user:read'), '停用主组后仍按 ADMIN 计算');
      assert(fallback.user.permissions.includes('article:read'), '停用主组后未回退 MODERATOR 基线');
      assert(fallback.user.primaryGroupCode === 'role_moderator',
        `展示主组未回退: ${fallback.user.primaryGroupCode}`);
    } finally {
      mysqlExec(`UPDATE bbs_permission_group SET enabled=1 WHERE id=${adminGroupId}`);
      mysqlExec(`UPDATE bbs_user SET primary_group_id=${original[0][0]} WHERE id=9004`);
    }
    return 'ADMIN 主组生效 → 停用后回退 MODERATOR';
  });

  // ---------- 汇总 ----------
  const passed = results.filter(r => r.status === 'PASS').length;
  const failed = results.filter(r => r.status === 'FAIL');
  const summary = { base: BASE, total: results.length, passed, failed: failed.length, results };
  const outPath = path.join(__dirname, '..', '..', 'tmp', 'moderator-api-results.json');
  fs.writeFileSync(outPath, JSON.stringify(summary, null, 2), 'utf8');

  console.log('\n================ SUMMARY ================');
  console.log(`TOTAL=${results.length} PASS=${passed} FAIL=${failed.length}`);
  if (failed.length) {
    console.log('--- FAILED ---');
    for (const f of failed) console.log(`${f.id} ${f.name} :: ${f.detail}`);
  }
  console.log(`RESULT_JSON=${outPath}`);
  process.exitCode = failed.length ? 1 : 0;
}

main().catch(e => {
  console.error('FATAL', e);
  process.exit(2);
});
