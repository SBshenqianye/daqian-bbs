// @ts-check
import { test, expect } from '@playwright/test';

/**
 * 权限架构重构 E2E：验证后端边界与前端配置写按钮。
 * 依赖 bbs_db 中已有的 asiayak(超管)、testadmin(管理员)、testuser(普通用户) 账号。
 */

const ADMIN_PORT = process.env.E2E_ADMIN_PORT || '9082';
const BACKEND_PORT = process.env.E2E_BACKEND_PORT || '9084';
const ADMIN_BASE = `http://localhost:${ADMIN_PORT}/bbs-admin`;
const API_BASE = `http://localhost:${BACKEND_PORT}/bbs-server`;

test.describe.configure({ timeout: 60000 });

async function loginByApiFull(request, username, channel) {
  const resp = await request.post(`${API_BASE}/common/login`, {
    data: { username, password: '1234@abcD', channel },
  });
  const body = await resp.json();
  expect(body.code).toBe(200);
  return body.obj;
}

async function loginByApi(request, username, channel) {
  const obj = await loginByApiFull(request, username, channel);
  return obj.tokenHead + obj.token;
}

async function loginAdminUi(page, request, username) {
  const resp = await request.post(`${API_BASE}/common/login`, {
    data: { username, password: '1234@abcD', channel: '02' },
  });
  const body = await resp.json();
  expect(body.code).toBe(200);
  const token = body.obj.tokenHead + body.obj.token;
  await page.addInitScript(([tokenStr, admin]) => {
    sessionStorage.setItem('tokenStr', tokenStr);
    sessionStorage.setItem('admin', JSON.stringify(admin));
  }, [token, body.obj.user]);
}

async function postAs(request, token, path, data = {}) {
  const resp = await request.post(`${API_BASE}${path}`, {
    headers: { Authorization: token },
    data,
  });
  return resp.json();
}

test.describe('权限边界', () => {
  test('普通用户 Token 调管理接口 → 403', async ({ request }) => {
    const token = await loginByApi(request, 'testuser', '01');
    const body = await postAs(request, token, '/admin/dashboard/counts');
    expect(body.code).toBe(403);
  });

  test('普通用户调用户管理/违规列表/改角色 → 403', async ({ request }) => {
    const token = await loginByApi(request, 'testuser', '01');
    expect((await postAs(request, token, '/getAllUser', { pageIndex: 1, pageSize: 10, searchInfo: '' })).code).toBe(403);
    expect((await postAs(request, token, '/admin/violation/list', { page: 1, size: 10 })).code).toBe(403);
    expect((await postAs(request, token, '/updateUserRole', { userId: 2, roleType: '01' })).code).toBe(403);
  });

  test('普通管理员可读用户/违规列表，但不能改角色/建管理员', async ({ request }) => {
    const token = await loginByApi(request, 'testadmin', '02');
    expect((await postAs(request, token, '/getAllUser', { pageIndex: 1, pageSize: 10, searchInfo: '' })).code).toBe(200);
    expect((await postAs(request, token, '/admin/violation/list', { page: 1, size: 10 })).code).toBe(200);
    expect((await postAs(request, token, '/admin/listDict')).code).toBe(200);
    expect((await postAs(request, token, '/updateUserRole', { userId: 2, roleType: '02' })).code).toBe(403);
    expect((await postAs(request, token, '/admin/addUser', {
      username: 'e2e_should_not_create',
      nickname: 'E2E',
      personnelId: 'E2E_DENY_001',
      userType: '2',
      isAlive: 0,
    })).code).toBe(403);
  });

  test('普通管理员可写配置（config:write 已开放给管理员）', async ({ request }) => {
    const token = await loginByApi(request, 'testadmin', '02');
    const resp = await request.post(`${API_BASE}/admin/deleteDict`, {
      headers: { Authorization: token },
      data: { id: 999999 },
    });
    const body = await resp.json();
    // 行为变更：role_admin 现在拥有 config:write，不再返回 403；
    // 不存在的字典由业务层返回删除失败，这里只锁定授权边界。
    expect(body.code).not.toBe(403);
    expect(body.message).not.toContain('权限不足');
  });

  test('超管访问管理接口 → 200', async ({ request }) => {
    const token = await loginByApi(request, 'asiayak', '02');
    expect((await postAs(request, token, '/admin/dashboard/counts')).code).toBe(200);
    expect((await postAs(request, token, '/getAllUser', { pageIndex: 1, pageSize: 10, searchInfo: '' })).code).toBe(200);
    expect((await postAs(request, token, '/admin/violation/list', { page: 1, size: 10 })).code).toBe(200);
  });

  test('全局管理员跨板块记录违规：不再被板块范围拦截', async ({ request }) => {
    const obj = await loginByApiFull(request, 'testadmin', '02');
    const token = obj.tokenHead + obj.token;
    // 行为变更：testadmin 的主组 role_admin 数据范围是 GLOBAL，跨板块不应返回 403。
    // 使用无效 userId，让请求在授权通过后被业务层拒绝，避免真的写入违规记录。
    const resp = await request.post(`${API_BASE}/admin/violation/add`, {
      headers: { Authorization: token },
      data: {
        userId: -1,
        violationType: 'spam',
        relatedType: 'article',
        relatedId: 13,
        operatorId: obj.user.id,
        remark: 'e2e-scope-check',
      },
    });
    const body = await resp.json();
    expect(body.code).not.toBe(403);
    expect(body.message).not.toContain('无权操作该板块内容');
  });

  test('普通管理员配置写按钮可用（config:write 已开放）', async ({ page, request }) => {
    await loginAdminUi(page, request, 'testadmin');
    await page.goto(`${ADMIN_BASE}/#/dict`);
    await page.waitForTimeout(2500);
    const addBtn = page.locator('button:has-text("新增字典")');
    await expect(addBtn).toBeVisible();
    await expect(addBtn).toBeEnabled();
    const editBtn = page.locator('button:has-text("编辑")').first();
    if (await editBtn.count() > 0) {
      await expect(editBtn).toBeEnabled();
    }
  });

  test('超管配置写按钮可用', async ({ page, request }) => {
    await loginAdminUi(page, request, 'asiayak');
    await page.goto(`${ADMIN_BASE}/#/dict`);
    await page.waitForTimeout(2500);
    const addBtn = page.locator('button:has-text("新增字典")');
    await expect(addBtn).toBeVisible();
    await expect(addBtn).toBeEnabled();
  });
});
