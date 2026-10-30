// @ts-check
const { test, expect } = require('@playwright/test');
const { execFileSync } = require('child_process');

/**
 * 版主角色化（MODERATOR / user_type=4）管理端 E2E
 *
 * 前置：
 *   - 后端 e2e profile（http://127.0.0.1:9084/bbs-server）
 *   - 管理端 dev server（http://localhost:9082/bbs-admin/，DEV_BACKEND_URL 指向 9084）
 *   - MySQL bbs_e2e_test 已执行 tests/fixtures/moderator-seed.sql
 *
 * 运行：
 *   npx playwright test tests/e2e/moderator-role.spec.js
 */
test.use({
  baseURL: process.env.E2E_ADMIN_BASE || 'http://localhost:9086',
  // 1280 宽会自动收起侧栏并隐藏分组标题，这里用宽屏保持菜单文案可见
  viewport: { width: 1600, height: 900 },
});

const LOGIN_PATH = '/bbs-admin/#/login';

function mysqlQuery(sql) {
  const out = execFileSync('mysql', [
    '-h', 'localhost', '-P', '3306', '-u', 'root', '-N', '-B', 'bbs_e2e_test', '-e', sql,
  ], { env: { ...process.env, MYSQL_PWD: 'root' }, encoding: 'utf8' });
  return out.trim() ? out.trim().split(/\r?\n/).map(line => line.split('\t')) : [];
}

async function login(page, username) {
  await page.goto(LOGIN_PATH);
  await page.locator('input[placeholder="请输入用户名"]').fill(username);
  await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/#\/dashboard/, { timeout: 15000 });
}

async function currentAdmin(page) {
  return page.evaluate(() => {
    const raw = window.sessionStorage.getItem('admin');
    return raw ? JSON.parse(raw) : null;
  });
}

test.describe('版主角色化 E2E', () => {

  test('MODERATOR 无板块：业务菜单可见、系统菜单不可见、顶部横幅、不退出登录', async ({ page }) => {
    await login(page, 'MOD0');

    const sidebar = page.locator('aside');
    await expect(sidebar.getByText('业务管理', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('系统管理', { exact: true })).toHaveCount(0);
    await expect(sidebar.getByText('帖子管理', { exact: true })).toBeVisible();

    // 无板块横幅
    await expect(page.getByText('尚未分配板块，请联系管理员', { exact: true })).toBeVisible();

    // 直达系统页：后端 403 → 前端跳 /403，但登录态保留
    await page.goto('/bbs-admin/#/user');
    await expect(page).toHaveURL(/#\/403/, { timeout: 15000 });
    const admin = await currentAdmin(page);
    expect(admin && admin.id).toBe(9005);
    expect(await page.evaluate(() => !!window.sessionStorage.getItem('tokenStr'))).toBeTruthy();
    await expect(page).not.toHaveURL(/#\/login/);
  });

  test('MODERATOR 有板块：侧栏只有业务管理；刷新后菜单/权限不丢失', async ({ page }) => {
    await login(page, 'MOD1');

    const sidebar = page.locator('aside');
    await expect(sidebar.getByText('业务管理', { exact: true })).toBeVisible();
    // 系统管理组不应出现（标签管理也属于系统管理）
    await expect(sidebar.getByText('系统管理', { exact: true })).toHaveCount(0);
    await expect(page.getByText('尚未分配板块，请联系管理员', { exact: true })).toHaveCount(0);

    // 刷新页面：/common/user/info 必须同步返回 role/permissions/boardScopes
    await page.reload();
    await expect(page).toHaveURL(/#\/dashboard/, { timeout: 15000 });
    await expect(page.locator('aside').getByText('业务管理', { exact: true })).toBeVisible();
    await expect(page.locator('aside').getByText('系统管理', { exact: true })).toHaveCount(0);
    await expect(page.locator('aside').getByText('帖子管理', { exact: true })).toBeVisible();
    await expect(page.getByText('尚未分配板块，请联系管理员', { exact: true })).toHaveCount(0);

    const admin = await currentAdmin(page);
    expect(admin.role).toBe('MODERATOR');
    expect(Array.isArray(admin.permissions) && admin.permissions.includes('article:read')).toBeTruthy();
    expect(admin.permissions.includes('featured:review')).toBeFalsy();
    expect((admin.boardScopes || []).some(b => Number(b.labelId) === 9001)).toBeTruthy();
  });

  test('MODERATOR 跨板块操作 403 且不退出登录', async ({ page }) => {
    await login(page, 'MOD1');

    // 1) 直达其他板块文章详情 → 403 页面
    await page.goto('/bbs-admin/#/article/id?articleId=9003');
    await expect(page).toHaveURL(/#\/403/, { timeout: 15000 });
    expect((await currentAdmin(page)).id).toBe(9003);
    expect(await page.evaluate(() => !!window.sessionStorage.getItem('tokenStr'))).toBeTruthy();

    // 2) 跨板块写接口（设精华）→ body.code=403
    await page.goto('/bbs-admin/#/dashboard');
    await expect(page).toHaveURL(/#\/dashboard/, { timeout: 15000 });
    const result = await page.evaluate(async () => {
      const token = window.sessionStorage.getItem('tokenStr');
      const res = await fetch('/admin-api/admin/featured/set', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: token },
        body: JSON.stringify({ articleId: 9003, isFeatured: 1 }),
      });
      return { status: res.status, body: await res.json() };
    });
    expect(result.body.code).toBe(403);
    expect(result.body.message).toContain('无权操作该板块内容');
  });

  test('版主管理页“编辑板块”保存后列表与数据库一致', async ({ page }) => {
    await login(page, 'SUPER1');
    await page.goto('/bbs-admin/#/moderator');
    await expect(page.getByText('当前版主', { exact: true })).toBeVisible({ timeout: 15000 });

    const row = page.locator('tr', { hasText: 'E2E-MOD2' });
    await expect(row).toBeVisible({ timeout: 15000 });
    await expect(row).toContainText('E2E板块B');
    await expect(row).toContainText('E2E板块C');

    // 取消勾选“E2E板块C”并保存
    await row.getByRole('button', { name: '编辑板块' }).click();
    const cCheckbox = page.locator('label:has-text("E2E板块C") input[type="checkbox"]');
    await expect(cCheckbox).toBeVisible();
    await expect(cCheckbox).toBeChecked();
    await cCheckbox.uncheck();
    await page.getByRole('button', { name: '保存', exact: true }).click();

    await expect(row).not.toContainText('E2E板块C', { timeout: 15000 });
    await expect(row).toContainText('E2E板块B');
    let rows = mysqlQuery("SELECT COUNT(*) FROM bbs_board_moderator WHERE user_id=9004 AND label_id=9003 AND status=1");
    expect(Number(rows[0][0])).toBe(0);
    rows = mysqlQuery("SELECT COUNT(*) FROM bbs_board_moderator WHERE user_id=9004 AND label_id=9002 AND status=1");
    expect(Number(rows[0][0])).toBe(1);

    // 还原板块，避免影响其他用例
    await row.getByRole('button', { name: '编辑板块' }).click();
    const cCheckbox2 = page.locator('label:has-text("E2E板块C") input[type="checkbox"]');
    await expect(cCheckbox2).toBeVisible();
    await cCheckbox2.check();
    await page.getByRole('button', { name: '保存', exact: true }).click();
    await expect(row).toContainText('E2E板块C', { timeout: 15000 });
    rows = mysqlQuery("SELECT COUNT(*) FROM bbs_board_moderator WHERE user_id=9004 AND label_id=9003 AND status=1");
    expect(Number(rows[0][0])).toBe(1);
  });

});
