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
  await test('D14', 'ADMIN 可访问系统接口（不被 403）', async () => {
    const details = [];
    // 管理员访问验证只证明“未被 403”；写接口传空参数触发业务错误即可，避免副作用干扰后续生命周期用例
    const noMutation = { D3: { body: {} }, D6: { body: {} }, D9: { body: {} }, D12: { body: {} } };
    for (const [id, method, path, opts] of systemCalls) {
      const callOpts = noMutation[id] || opts;
      const r = await call(method, path, { token: admin1Token, ...callOpts });
      const forbidden = isForbidden(r);
      details.push(`${id}:${r.data && r.data.code}`);
      assert(!forbidden, `${path} 被 403`);
    }
    return details.join(',');
  });

  // ---------- E. 生命周期 ----------
  group('E. 生命周期');

  await test('E1', 'normal1 任命版主后 user_type 1 → 4', async () => {
    const r = await call('POST', '/admin/moderator/appoint', {
      token: superToken, body: { userId: 9006, labelId: 9001 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery('SELECT user_type FROM bbs_user WHERE id=9006');
    assert(rows[0][0] === '4', `user_type=${rows[0][0]}`);
    return 'user_type=4';
  });

  await test('E2', 'admin1 任命版主后 user_type 仍为 2', async () => {
    const r = await call('POST', '/admin/moderator/appoint', {
      token: superToken, body: { userId: 9002, labelId: 9002 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery('SELECT user_type FROM bbs_user WHERE id=9002');
    assert(rows[0][0] === '2', `user_type=${rows[0][0]}`);
    return 'user_type=2';
  });

  await test('E3', '撤销 normal1 最后一个板块后仍为版主且可登录、boardScopes=[]', async () => {
    const r = await call('POST', '/admin/moderator/dismiss', {
      token: superToken, body: { userId: 9006, labelId: 9001 },
    });
    assert(Number(r.data.code) === 200, `code=${r.data.code} ${msgOf(r)}`);
    const rows = mysqlQuery('SELECT user_type FROM bbs_user WHERE id=9006');
    assert(rows[0][0] === '4', `user_type=${rows[0][0]}`);
    const { user } = await loginOk('NORMAL1');
    assert(user.role === 'MODERATOR', `role=${user.role}`);
    assert((user.boardScopes || []).length === 0, `boardScopes=${JSON.stringify(user.boardScopes)}`);
    return 'user_type=4, login ok, boardScopes=[]';
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
