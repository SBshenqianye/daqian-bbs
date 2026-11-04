#!/usr/bin/env node
/**
 * 权限组 / 权限边界 API 冒烟测试。
 *
 * 覆盖真实 HTTP 入口：
 *   - channel 02 登录必须有有效 admin:access
 *   - 权限组 CRUD、预设详情/删除、删除组后的主组用户迁移
 *   - permission:manage / audit:read / config:write 的授权边界
 *   - 跨域 DELETE 预检（浏览器删除请求的真实入口）
 *
 * 前置：
 *   - bbs_e2e_test 已从 bbs_db 复制
 *   - 后端以 e2e profile 启动（默认 http://127.0.0.1:9084/bbs-server）
 *
 * 运行：
 *   node tests/api/permission-group-api-smoke.js
 *
 * 安全：
 *   - 只写 bbs_e2e_test；脚本结束会删除临时权限组/预设，并把 testuser 主组恢复为 role_user。
 *   - 禁止把 BBS_E2E_BASE 指向开发库 9083。
 */
const { execFileSync } = require('child_process');

const BASE = process.env.BBS_E2E_BASE || 'http://127.0.0.1:9084/bbs-server';
const PASSWORD = '1234@abcD';
const TEST_DB = 'bbs_e2e_test';
const STAMP = Date.now();
const TEMP_GROUP_CODE = `e2e_perm_${STAMP}`;
const TEMP_GROUP_NAME = `E2E权限组${STAMP}`;
const TEMP_GROUP_NAME_UPDATED = `${TEMP_GROUP_NAME}-更新`;
const TEMP_PRESET_NAME = `E2E预设${STAMP}`;
const ORIGIN = process.env.BBS_E2E_ORIGIN || 'http://localhost:9086';

if (/:9083\b/.test(BASE)) {
  console.error('[ABORT] BBS_E2E_BASE 指向开发后端 9083，拒绝在非隔离库上执行写操作。');
  process.exit(1);
}

const results = [];
let currentGroup = '';
let tempGroupId = null;

function group(name) {
  currentGroup = name;
}

async function call(method, urlPath, { token, body, headers } = {}) {
  const finalHeaders = { 'Content-Type': 'application/json', ...(headers || {}) };
  if (token) finalHeaders.Authorization = token;
  const res = await fetch(BASE + urlPath, {
    method,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch (e) {
    data = { raw: text };
  }
  return { status: res.status, data, headers: res.headers };
}

function msgOf(r) {
  return (r.data && (r.data.message || r.data.msg)) || '';
}

function isForbidden(r) {
  return r.status === 403 || (r.data && Number(r.data.code) === 403);
}

function assert(cond, message) {
  if (!cond) throw new Error(message);
}

function assertOk(r, message) {
  assert(r.data && Number(r.data.code) === 200,
    `${message}: HTTP ${r.status}, code=${r.data && r.data.code}, message=${msgOf(r)}`);
}

function assertForbidden(r, message) {
  assert(isForbidden(r),
    `${message}: 期望 403，实际 HTTP ${r.status}, code=${r.data && r.data.code}, message=${msgOf(r)}`);
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

async function login(username, channel = '02') {
  return call('POST', '/common/login', {
    body: { username, password: PASSWORD, channel },
  });
}

async function loginOk(username, channel = '02') {
  const r = await login(username, channel);
  assertOk(r, `${username} 登录失败`);
  const obj = r.data.obj;
  return { token: obj.tokenHead + obj.token, user: obj.user };
}

function mysqlQuery(sql) {
  const out = execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '-N', '-B', TEST_DB, '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8' });
  return out.trim() ? out.trim().split(/\r?\n/).map(line => line.split('\t')) : [];
}

function mysqlExec(sql) {
  execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '-N', '-B', TEST_DB, '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8' });
}

function findPresetByName(presets, name) {
  return (presets || []).find(p => p.presetName === name);
}

async function cleanup(superToken) {
  try {
    const presetList = await call('GET', '/admin/permission/presets', { token: superToken });
    const preset = presetList.data && presetList.data.code === 200
      ? findPresetByName(presetList.data.obj, TEMP_PRESET_NAME)
      : null;
    if (preset) {
      await call('DELETE', `/admin/permission/presets/${preset.id}`, { token: superToken });
    }
  } catch (e) {
    console.warn('[cleanup] 预设清理失败:', e.message);
  }

  try {
    if (tempGroupId) {
      await call('DELETE', `/admin/permission/groups/${tempGroupId}`, { token: superToken });
    }
  } catch (e) {
    console.warn('[cleanup] 权限组清理失败:', e.message);
  }

  try {
    // testuser 的原始主组固定为 role_user；无论用例是否走到删除步骤都恢复
    const roleUser = mysqlQuery("SELECT id FROM bbs_permission_group WHERE group_code='role_user'");
    if (roleUser.length) {
      mysqlExec(`UPDATE bbs_user SET primary_group_id=${Number(roleUser[0][0])} WHERE username='testuser'`);
    }
  } catch (e) {
    console.warn('[cleanup] testuser 主组恢复失败:', e.message);
  }
}

async function main() {
  const superUser = await loginOk('asiayak');
  const admin = await loginOk('testadmin');
  const normalUser = await loginOk('testuser', '01');

  group('登录');

  await test('AUTH-01', 'channel 02 缺少 admin:access：密码正确也明确拒绝', async () => {
    const r = await login('testuser', '02');
    assert(r.data && Number(r.data.code) === 500,
      `期望登录失败，实际 code=${r.data && r.data.code}`);
    assert(msgOf(r).includes('没有管理端访问权限'),
      `提示应指向权限缺失而不是密码错误，实际: ${msgOf(r)}`);
    return msgOf(r);
  });

  await test('AUTH-02', '同一账号 channel 01 仍可正常登录', async () => {
    assertOk(await login('testuser', '01'), '普通用户前台登录失败');
    return 'channel 01 ok';
  });

  group('权限组读取边界');

  await test('GROUP-01', '超管可读取权限组列表与详情', async () => {
    const list = await call('GET', '/admin/permission/groups', { token: superUser.token });
    assertOk(list, '权限组列表失败');
    assert(Array.isArray(list.data.obj) && list.data.obj.length >= 4,
      '权限组列表应包含内置组');
    const superGroup = list.data.obj.find(g => g.groupCode === 'role_super');
    assert(superGroup, '缺少 role_super');
    const detail = await call('GET', `/admin/permission/groups/${superGroup.id}`,
      { token: superUser.token });
    assertOk(detail, '权限组详情失败');
    assert(Array.isArray(detail.data.obj.permissionCodes), '详情缺少 permissionCodes');
    assert(detail.data.obj.permissionCodes.includes('permission:manage'),
      '超管组详情应包含 permission:manage');
    return `${list.data.obj.length} 个组`;
  });

  await test('GROUP-02', '普通管理员/普通用户/未登录访问权限管理 → 403/401', async () => {
    assertForbidden(await call('GET', '/admin/permission/groups', { token: admin.token }),
      '普通管理员不应拥有 permission:manage');
    assertForbidden(await call('GET', '/admin/permission/groups', { token: normalUser.token }),
      '普通用户不应访问权限管理');
    const anonymous = await call('GET', '/admin/permission/groups');
    assert(anonymous.data && Number(anonymous.data.code) === 401,
      `未登录应返回 401，实际 HTTP ${anonymous.status} code=${anonymous.data && anonymous.data.code}`);
    return 'admin=403 user=403 anonymous=401';
  });

  group('跨域');

  await test('CORS-01', '浏览器 DELETE 预检在 /admin/** 放行且返回 CORS 头', async () => {
    const r = await call('OPTIONS', '/admin/permission/groups/1', {
      headers: {
        Origin: ORIGIN,
        'Access-Control-Request-Method': 'DELETE',
        'Access-Control-Request-Headers': 'authorization',
      },
    });
    assert(r.status === 200 || r.status === 204,
      `预检应成功，实际 HTTP ${r.status}, body=${JSON.stringify(r.data)}`);
    const allowMethods = r.headers.get('access-control-allow-methods') || '';
    assert(allowMethods.toUpperCase().includes('DELETE'),
      `预检响应缺少 DELETE: ${allowMethods}`);
    assert(r.headers.get('access-control-allow-origin') === ORIGIN,
      `预检响应缺少允许源: ${r.headers.get('access-control-allow-origin')}`);
    return allowMethods;
  });

  group('权限组写入');

  await test('GROUP-03', '创建/修改权限组：权限点与适用端同步更新', async () => {
    const create = await call('POST', '/admin/permission/groups', {
      token: superUser.token,
      body: {
        groupCode: TEMP_GROUP_CODE,
        groupName: TEMP_GROUP_NAME,
        description: 'API 冒烟临时组',
        dataScopeType: 'GLOBAL',
        enabled: 1,
        sortOrder: 999,
        permissionCodes: ['article:read', 'post:create'],
      },
    });
    assertOk(create, '创建权限组失败');
    tempGroupId = Number(create.data.obj);
    assert(tempGroupId > 0, '创建后未返回有效 id');

    const detail = await call('GET', `/admin/permission/groups/${tempGroupId}`,
      { token: superUser.token });
    assertOk(detail, '读取新建组失败');
    assert(detail.data.obj.scopeType === 'ALL',
      `管理端+前台动作应标记 ALL，实际 ${detail.data.obj.scopeType}`);
    assert(detail.data.obj.permissionCodes.includes('post:create'), '缺少 post:create');

    const update = await call('PUT', `/admin/permission/groups/${tempGroupId}`, {
      token: superUser.token,
      body: {
        groupCode: TEMP_GROUP_CODE,
        groupName: TEMP_GROUP_NAME_UPDATED,
        description: 'API 冒烟临时组',
        dataScopeType: 'GLOBAL',
        enabled: 1,
        sortOrder: 999,
        permissionCodes: ['post:create'],
      },
    });
    assertOk(update, '修改权限组失败');

    const updated = await call('GET', `/admin/permission/groups/${tempGroupId}`,
      { token: superUser.token });
    assertOk(updated, '读取修改后的组失败');
    assert(updated.data.obj.groupName === TEMP_GROUP_NAME_UPDATED, '名称未更新');
    assert(updated.data.obj.scopeType === 'FRONT',
      `仅前台动作应标记 FRONT，实际 ${updated.data.obj.scopeType}`);
    assert(updated.data.obj.permissionCodes.length === 1
      && updated.data.obj.permissionCodes[0] === 'post:create', '权限点未按提交覆盖');
    return `group=${tempGroupId}`;
  });

  await test('GROUP-04', '普通管理员删除/修改权限组 → 403（方法级权限）', async () => {
    assertForbidden(await call('PUT', `/admin/permission/groups/${tempGroupId}`, {
      token: admin.token,
      body: { groupName: 'hijack' },
    }), '普通管理员不应能修改权限组');
    assertForbidden(await call('DELETE', `/admin/permission/groups/${tempGroupId}`,
      { token: admin.token }), '普通管理员不应能删除权限组');
    return 'method-level 403';
  });

  group('预设');

  await test('PRESET-01', '保存预设后列表返回来源组/权限点详情，可删除', async () => {
    const create = await call('POST', '/admin/permission/presets', {
      token: superUser.token,
      body: { presetName: TEMP_PRESET_NAME, groupId: tempGroupId },
    });
    assertOk(create, '保存预设失败');

    const list = await call('GET', '/admin/permission/presets', { token: superUser.token });
    assertOk(list, '读取预设列表失败');
    const preset = findPresetByName(list.data.obj, TEMP_PRESET_NAME);
    assert(preset, '列表中找不到刚保存的预设');
    assert(preset.sourceGroupName === TEMP_GROUP_NAME_UPDATED,
      `预设详情来源组错误: ${preset.sourceGroupName}`);
    assert(preset.sourceGroupCode === TEMP_GROUP_CODE,
      `预设详情来源编码错误: ${preset.sourceGroupCode}`);
    assert(preset.dataScopeType === 'GLOBAL', '预设详情缺少数据范围');
    assert(Array.isArray(preset.permissionCodes)
      && preset.permissionCodes.includes('post:create'), '预设详情缺少权限点');

    assertOk(await call('DELETE', `/admin/permission/presets/${preset.id}`,
      { token: superUser.token }), '删除预设失败');
    const after = await call('GET', '/admin/permission/presets', { token: superUser.token });
    assertOk(after, '删除后读取预设失败');
    assert(!findPresetByName(after.data.obj, TEMP_PRESET_NAME), '预设删除后仍存在');
    return `preset=${preset.id}`;
  });

  group('删除组迁移');

  await test('GROUP-05', '删除权限组：原主组用户迁移到 role_user，不留在空主组', async () => {
    mysqlExec(`UPDATE bbs_user SET primary_group_id=${tempGroupId} WHERE username='testuser'`);
    const before = mysqlQuery("SELECT primary_group_id FROM bbs_user WHERE username='testuser'");
    assert(Number(before[0][0]) === tempGroupId, '测试数据准备失败：testuser 未指向临时组');

    assertOk(await call('DELETE', `/admin/permission/groups/${tempGroupId}`,
      { token: superUser.token }), '删除权限组失败');
    tempGroupId = null;

    const after = mysqlQuery("SELECT primary_group_id FROM bbs_user WHERE username='testuser'");
    const roleUser = mysqlQuery("SELECT id FROM bbs_permission_group WHERE group_code='role_user'");
    assert(roleUser.length, '缺少 role_user 权限组，无法验证迁移目标');
    assert(Number(after[0][0]) === Number(roleUser[0][0]),
      `删除组后 testuser 主组应回退到 role_user(${roleUser[0][0]})，实际 ${after[0][0]}`);
    return `testuser -> role_user(${roleUser[0][0]})`;
  });

  group('其他授权边界');

  await test('AUDIT-01', '审计日志仅超管可读', async () => {
    assertForbidden(await call('GET', '/admin/audit/list', { token: admin.token }),
      '普通管理员不应读取审计日志');
    assertOk(await call('GET', '/admin/audit/list?page=1&size=5', { token: superUser.token }),
      '超管读取审计日志失败');
    return 'admin=403 super=200';
  });

  await test('CONFIG-01', 'config:write 已开放给管理员（不再返回 403）', async () => {
    const r = await call('POST', '/admin/deleteDict', {
      token: admin.token,
      body: { id: 999999 },
    });
    assert(!isForbidden(r),
      `管理员应具备 config:write，实际仍被拒绝: ${msgOf(r)}`);
    assert(!msgOf(r).includes('权限不足'), `不应返回权限不足: ${msgOf(r)}`);
    return `code=${r.data && r.data.code}`;
  });

  await cleanup(superUser.token);

  const failed = results.filter(r => r.status === 'FAIL');
  console.log('\n========================================');
  console.log(`权限组 API 冒烟：${results.length - failed.length}/${results.length} 通过`);
  if (failed.length) {
    console.log('失败用例：');
    failed.forEach(f => console.log(`  - ${f.id} ${f.name}: ${f.detail}`));
  }
  console.log('========================================');
  process.exit(failed.length ? 1 : 0);
}

main().catch(async e => {
  console.error('[FATAL]', e);
  try {
    const superUser = await loginOk('asiayak');
    await cleanup(superUser.token);
  } catch (cleanupError) {
    console.warn('[cleanup] 失败:', cleanupError.message);
  }
  process.exit(1);
});
