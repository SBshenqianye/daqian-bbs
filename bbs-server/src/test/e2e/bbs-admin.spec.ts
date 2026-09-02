// @ts-check
import { test, expect } from '@playwright/test';

/**
 * bbs-admin-ui 管理后台 E2E 测试
 * 覆盖管理流程：登录、用户管理、文章管理、评论管理、组织机构管理
 *
 * 前提条件：后端 9083 + 管理前端 9082 均已启动
 * 测试账号：asiayak / 1234@abcD（超级管理员）
 *
 * 注意：管理后台使用 hash 路由，URL 格式为 http://localhost:9082/bbs-admin/#/xxx
 * 本测试文件的 baseURL 在 playwright.config.ts 中配置为 bbs-ui，
 * 管理后台测试使用绝对 URL。
 */

// 管理后台地址（独立于 playwright.config.ts 中的 baseURL）
const ADMIN_BASE = 'http://localhost:9082/bbs-admin';

// ============================================================
// 1. 管理后台登录
// ============================================================
test.describe('管理后台登录', () => {

  test('管理员登录页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/login`);

    // 验证页面标题
    await expect(page).toHaveTitle(/大千智荟/);

    // 验证登录表单元素存在
    await expect(page.locator('h1:has-text("管理员登录")')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('空表单提交被拦截，显示错误提示', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/login`);

    // 不填任何内容直接点登录
    await page.locator('button[type="submit"]').click();

    // 应显示"请输入用户名"错误提示
    await expect(page.locator('text=请输入用户名')).toBeVisible();
  });

  test('只填用户名不填密码，显示错误提示', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/login`);

    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('button[type="submit"]').click();

    // 应显示"请输入密码"错误提示
    await expect(page.locator('text=请输入密码')).toBeVisible();
  });

  test('错误密码登录失败', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/login`);

    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('wrong_password');
    await page.locator('button[type="submit"]').click();

    // 等待请求完成，应显示错误信息并停留在登录页
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/.*login/);
  });

  test('正确账号密码登录成功，跳转到仪表盘', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/login`);

    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();

    // 登录成功后应跳转离开登录页
    await page.waitForTimeout(3000);
    const url = page.url();
    const isRedirected = !url.includes('#/login');
    expect(isRedirected).toBeTruthy();
  });

});

// ============================================================
// 2. 用户管理页面
// ============================================================
test.describe('用户管理页面', () => {

  test.beforeEach(async ({ page }) => {
    // 登录管理后台
    await page.goto(`${ADMIN_BASE}/#/login`);
    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
  });

  test('用户管理页面加载正常', async ({ page }) => {
    // 跳转到用户管理页
    await page.goto(`${ADMIN_BASE}/#/user`);
    await page.waitForTimeout(2000);

    // 验证页面存在（表格或加载状态）
    // Element UI 表格
    const table = page.locator('.el-table');
    await expect(table).toBeVisible();
  });

  test('用户管理页面显示搜索功能', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/user`);
    await page.waitForTimeout(2000);

    // 验证搜索输入框存在
    const searchInput = page.locator('input[placeholder*="搜索"]').or(page.locator('input[placeholder*="请输入"]'));
    await expect(searchInput.first()).toBeVisible();
  });

});

// ============================================================
// 3. 文章管理页面
// ============================================================
test.describe('文章管理页面', () => {

  test.beforeEach(async ({ page }) => {
    // 登录管理后台
    await page.goto(`${ADMIN_BASE}/#/login`);
    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
  });

  test('文章管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/article`);
    await page.waitForTimeout(2000);

    // 验证 Element UI 表格存在
    const table = page.locator('.el-table');
    await expect(table).toBeVisible();
  });

  test('文章管理页面显示标签筛选', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/article`);
    await page.waitForTimeout(2000);

    // 验证标签筛选下拉框存在
    const labelSelect = page.locator('select').first();
    await expect(labelSelect).toBeVisible();
  });

});

// ============================================================
// 4. 评论管理页面（通过举报管理间接验证）
// ============================================================
test.describe('评论管理页面', () => {

  test.beforeEach(async ({ page }) => {
    // 登录管理后台
    await page.goto(`${ADMIN_BASE}/#/login`);
    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
  });

  test('举报管理页面加载正常', async ({ page }) => {
    // 评论管理通过举报管理页面验证（report 管理页面管理评论举报）
    await page.goto(`${ADMIN_BASE}/#/report`);
    await page.waitForTimeout(2000);

    // 验证页面存在
    const pageContent = page.locator('.el-table, .el-card, main');
    await expect(pageContent.first()).toBeVisible();
  });

  test('违规管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/violation`);
    await page.waitForTimeout(2000);

    // 验证页面存在
    const pageContent = page.locator('.el-table, .el-card, main');
    await expect(pageContent.first()).toBeVisible();
  });

});

// ============================================================
// 5. 组织机构管理页面
// ============================================================
test.describe('组织机构管理页面', () => {

  test.beforeEach(async ({ page }) => {
    // 登录管理后台
    await page.goto(`${ADMIN_BASE}/#/login`);
    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
  });

  test('单位管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/unitManage`);
    await page.waitForTimeout(2000);

    // 验证页面存在（可能有树形选择器或表格）
    const pageContent = page.locator('.el-table, .el-card, main, [class*="org"]');
    await expect(pageContent.first()).toBeVisible();
  });

});

// ============================================================
// 6. 其他管理页面加载验证
// ============================================================
test.describe('其他管理页面', () => {

  test.beforeEach(async ({ page }) => {
    // 登录管理后台
    await page.goto(`${ADMIN_BASE}/#/login`);
    await page.locator('input[placeholder="请输入用户名"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);
  });

  test('仪表盘页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/dashboard`);
    await page.waitForTimeout(2000);

    // 验证仪表盘页面存在
    const pageContent = page.locator('.el-card, main');
    await expect(pageContent.first()).toBeVisible();
  });

  test('标签管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/articleLable`);
    await page.waitForTimeout(2000);

    const table = page.locator('.el-table');
    await expect(table).toBeVisible();
  });

  test('配置管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/dict`);
    await page.waitForTimeout(2000);

    const table = page.locator('.el-table');
    await expect(table).toBeVisible();
  });

  test('敏感词管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/sensitiveWord`);
    await page.waitForTimeout(2000);

    const table = page.locator('.el-table');
    await expect(table).toBeVisible();
  });

  test('积分排名页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/points`);
    await page.waitForTimeout(2000);

    const pageContent = page.locator('.el-table, .el-card, main');
    await expect(pageContent.first()).toBeVisible();
  });

  test('版主管理页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/moderator`);
    await page.waitForTimeout(2000);

    const pageContent = page.locator('.el-table, .el-card, main');
    await expect(pageContent.first()).toBeVisible();
  });

  test('系统配置页面加载正常', async ({ page }) => {
    await page.goto(`${ADMIN_BASE}/#/systemConfig`);
    await page.waitForTimeout(2000);

    const pageContent = page.locator('.el-table, .el-card, main, form');
    await expect(pageContent.first()).toBeVisible();
  });

});
