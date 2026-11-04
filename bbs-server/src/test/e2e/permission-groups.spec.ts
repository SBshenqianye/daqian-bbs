// @ts-check
import { test, expect, request as playwrightRequest } from '@playwright/test';

/**
 * 权限组中心 / 首页权限概览 / 弹层视口约束 E2E。
 *
 * 依赖 e2e 隔离后端（默认 9084）中的 asiayak / testadmin / testuser。
 * 只写 bbs_e2e_test；临时权限组与预设会在用例结束时通过 API 清理。
 */

const ADMIN_PORT = process.env.E2E_ADMIN_PORT || '9082';
const BACKEND_PORT = process.env.E2E_BACKEND_PORT || '9084';
const ADMIN_BASE = `http://localhost:${ADMIN_PORT}/bbs-admin`;
const API_BASE = `http://localhost:${BACKEND_PORT}/bbs-server`;
const PASSWORD = '1234@abcD';
const RUN_ID = Date.now();
const TEMP_GROUP_PREFIX = `e2e_perm_${RUN_ID}`;
const TEMP_GROUP_CODE = TEMP_GROUP_PREFIX;
const TEMP_GROUP_NAME = `E2E临时权限组${RUN_ID}`;
const TEMP_PRESET_NAME = `E2E临时预设${RUN_ID}`;

test.describe.configure({ timeout: 90000 });

// 宽屏避免侧栏自动收起，保证菜单文字可点击
test.use({ viewport: { width: 1600, height: 900 } });

async function apiLogin(request, username, channel = '02') {
  const resp = await request.post(`${API_BASE}/common/login`, {
    data: { username, password: PASSWORD, channel },
  });
  const body = await resp.json();
  expect(body.code, `${username} 登录失败`).toBe(200);
  return { token: body.obj.tokenHead + body.obj.token, user: body.obj.user };
}

async function loginAdminUi(page, request, username) {
  const { token, user } = await apiLogin(request, username, '02');
  await page.addInitScript(([tokenStr, admin]) => {
    sessionStorage.setItem('tokenStr', tokenStr);
    sessionStorage.setItem('admin', JSON.stringify(admin));
  }, [token, user]);
}

async function cleanupTempData() {
  // 使用独立 request context：即使测试超时导致 page/context 已关闭，也能完成清理
  const ctx = await playwrightRequest.newContext();
  try {
    const { token } = await apiLogin(ctx, 'asiayak');
    const headers = { Authorization: token };

    const presetResp = await ctx.get(`${API_BASE}/admin/permission/presets`, { headers });
    const presets = (await presetResp.json()).obj || [];
    for (const preset of presets) {
      if (String(preset.presetName || '').startsWith('E2E临时预设')) {
        await ctx.delete(`${API_BASE}/admin/permission/presets/${preset.id}`, { headers });
      }
    }

    const groupResp = await ctx.get(`${API_BASE}/admin/permission/groups`, { headers });
    const groups = (await groupResp.json()).obj || [];
    for (const group of groups) {
      if (String(group.groupCode || '').startsWith('e2e_perm_')) {
        await ctx.delete(`${API_BASE}/admin/permission/groups/${group.id}`, { headers });
      }
    }
  } catch (e) {
    console.warn('[cleanup] 临时权限数据清理失败:', e);
  } finally {
    await ctx.dispose();
  }
}

test.describe('首页权限概览', () => {
  test('超管首页展示主权限组、数据范围、权限安全入口', async ({ page, request }) => {
    await loginAdminUi(page, request, 'asiayak');
    await page.goto(`${ADMIN_BASE}/#/dashboard`);

    await expect(page.getByRole('heading', { name: '我的权限' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '权限安全' })).toBeVisible();
    await expect(page.getByText('role_super').first()).toBeVisible();
    await expect(page.getByText('超级管理员').first()).toBeVisible();
    await expect(page.getByText('全局').first()).toBeVisible();

    await expect(page.getByRole('link', { name: '权限组管理', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: '权限矩阵', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: '审计日志', exact: true })).toBeVisible();

    const dock = page.locator('.fixed.bottom-6.right-6');
    await expect(dock.getByRole('link', { name: '权限管理' })).toBeVisible();
    await expect(dock.getByRole('link', { name: '权限矩阵' })).toBeVisible();
    await expect(dock.getByRole('link', { name: '审计日志' })).toBeVisible();
  });

  test('普通管理员首页隐藏权限管理入口，提示联系超管', async ({ page, request }) => {
    await loginAdminUi(page, request, 'testadmin');
    await page.goto(`${ADMIN_BASE}/#/dashboard`);

    await expect(page.getByText('权限由管理员统一配置')).toBeVisible();
    await expect(page.getByText('如需调整账号权限，请联系超级管理员')).toBeVisible();

    const dock = page.locator('.fixed.bottom-6.right-6');
    await expect(dock.getByRole('link', { name: '权限管理' })).toHaveCount(0);
    await expect(dock.getByRole('link', { name: '权限矩阵' })).toHaveCount(0);
    await expect(dock.getByRole('link', { name: '审计日志' })).toHaveCount(0);
    await expect(dock.getByRole('link', { name: '用户管理' })).toBeVisible();
  });
});

test.describe('权限管理页', () => {
  test('主组关键权限锁定，未保存离开时弹出确认框', async ({ page, request }) => {
    await loginAdminUi(page, request, 'asiayak');
    await page.goto(`${ADMIN_BASE}/#/permission-groups`);

    await expect(page.getByRole('heading', { name: '权限管理' })).toBeVisible();
    await expect(page.getByText('当前编辑的是你账号的主权限组')).toBeVisible({ timeout: 20000 });

    const adminAccess = page.locator('label[title="admin:access"] input[type="checkbox"]');
    await expect(adminAccess).toBeDisabled();
    const articleRead = page.locator('label[title="article:read"] input[type="checkbox"]');
    await expect(articleRead).toBeEnabled();

    await articleRead.click();
    await expect(page.getByText('有未保存修改')).toBeVisible();

    // 侧栏路由跳转必须被未保存保护拦住
    await page.getByRole('link', { name: '系统首页' }).click();
    await expect(page.locator('.el-message-box__header')).toContainText('未保存的修改');

    // MessageBox 不能超出视口，否则底部按钮不可达（等入场动画结束后再测量）
    const viewport = page.viewportSize();
    await expect.poll(async () => {
      const box = await page.locator('.el-message-box').boundingBox();
      if (!box || !viewport) return false;
      return box.y >= 0 && box.y + box.height <= viewport.height;
    }, { timeout: 5000 }).toBe(true);

    await page.getByRole('button', { name: '留在此页' }).click();
    await expect(page).toHaveURL(/permission-groups/);

    // 刷新丢弃未保存修改，避免污染后续用例
    await page.reload();
    await expect(page.getByText('当前编辑的是你账号的主权限组')).toBeVisible({ timeout: 20000 });
  });

  test('创建权限组、保存预设并查看详情、删除预设和权限组', async ({ page, request }) => {
    await loginAdminUi(page, request, 'asiayak');
    await page.goto(`${ADMIN_BASE}/#/permission-groups`);
    await expect(page.getByRole('heading', { name: '权限管理' })).toBeVisible();

    try {
      await page.getByRole('button', { name: '新建权限组' }).first().click();
      await page.locator('#permission-group-code').fill(TEMP_GROUP_CODE);
      await page.locator('#permission-group-name').fill(TEMP_GROUP_NAME);

      // 用 tab 的图标前缀区分“权限点”页签与左侧组的“41 权限点”文案
      await page.getByRole('button', { name: /^rule 权限点/ }).click();
      await page.locator('label[title="article:read"] input[type="checkbox"]').check();
      await page.getByRole('button', { name: '创建权限组' }).click();
      await expect(page.getByText('创建成功')).toBeVisible({ timeout: 15000 });
      await expect(page.locator('aside button').filter({ hasText: TEMP_GROUP_CODE })).toBeVisible();
      await expect(page.getByRole('heading', { name: TEMP_GROUP_NAME })).toBeVisible();

      // 预设：保存 -> 展开详情 -> 删除
      await page.getByRole('button', { name: /^bookmark 预设方案/ }).click();
      const presetInput = page.locator('input[placeholder^="预设名称"]');
      await expect(presetInput).toBeVisible();
      await presetInput.fill(TEMP_PRESET_NAME);
      await page.getByRole('button', { name: '保存当前配置' }).click();
      await expect(page.getByText('预设已保存')).toBeVisible({ timeout: 15000 });

      const presetName = page.getByText(TEMP_PRESET_NAME, { exact: true });
      await expect(presetName).toBeVisible();
      const presetRow = presetName.locator('xpath=ancestor::div[contains(@class,"px-3 py-3")]');
      await presetRow.getByRole('button', { name: '详情' }).click();
      await expect(presetRow.getByText('来源组')).toBeVisible();
      await expect(presetRow.getByText('权限点（1）')).toBeVisible();
      await expect(presetRow.locator('span[title="article:read"]')).toBeVisible();

      await presetRow.getByRole('button', { name: '删除' }).click();
      await expect(page.getByText(`确定删除预设「${TEMP_PRESET_NAME}」？`)).toBeVisible();
      await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();
      await expect(page.getByText('预设已删除')).toBeVisible({ timeout: 15000 });
      await expect(page.getByText(TEMP_PRESET_NAME, { exact: true })).toHaveCount(0);

      // 删除权限组
      await page.getByRole('button', { name: '删除' }).first().click();
      await expect(page.getByText('确定删除该权限组？主组用户会自动迁移到普通用户组，叠加成员绑定会一并解除。')).toBeVisible();
      await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click();
      await expect(page.getByText('已删除', { exact: true })).toBeVisible({ timeout: 15000 });
      await expect(page.locator('aside button').filter({ hasText: TEMP_GROUP_CODE })).toHaveCount(0);
    } finally {
      await cleanupTempData();
    }
  });
});

test.describe('弹层视口约束', () => {
  test('审计详情不超出视口，可通过按钮和 Esc 关闭', async ({ page, request }) => {
    await loginAdminUi(page, request, 'asiayak');
    await page.goto(`${ADMIN_BASE}/#/audit`);
    await expect(page.getByRole('heading', { name: '审计日志' })).toBeVisible();

    const firstView = page.getByRole('button', { name: '查看' }).first();
    await expect(firstView).toBeVisible({ timeout: 20000 });
    await firstView.click();

    const detailTitle = page.locator('h3:has-text("审计详情")');
    await expect(detailTitle).toBeVisible();

    const panel = page.locator('div.fixed.inset-0.z-50 > div.relative.bg-container');
    const box = await panel.boundingBox();
    const viewport = page.viewportSize();
    expect(box).not.toBeNull();
    // 真正的约束是“不超出视口”：overlay.css 允许 calc(100vh - 2rem)，
    // 不能用一个比产品规则更窄的 90vh 硬编码阈值。
    expect(box!.y).toBeGreaterThanOrEqual(0);
    expect(box!.y + box!.height).toBeLessThanOrEqual(viewport!.height);

    await page.getByRole('button', { name: '关闭审计详情' }).click();
    await expect(detailTitle).toHaveCount(0);

    // Esc 兜底关闭
    await firstView.click();
    await expect(detailTitle).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(detailTitle).toHaveCount(0);
  });
});
