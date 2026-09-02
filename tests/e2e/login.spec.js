// @ts-check
const { test, expect } = require('@playwright/test');

/**
 * 登录流程 E2E 测试原型
 * 覆盖场景：
 * 1. 登录页面加载正常
 * 2. 空表单提交被拦截
 * 3. 错误密码登录失败
 * 4. 正确账号密码登录成功 → 跳转首页
 */

//  baseURL 已在 playwright.config.js 配置为 http://localhost:9081
//  bbs-ui 是 hash 路由，路径格式: /bbs-user/#/login
const LOGIN_URL = '/bbs-ui/#/login';

test.describe('登录流程', () => {

  test('登录页面加载正常', async ({ page }) => {
    await page.goto(LOGIN_URL);
    // 页面应显示"登录"标题
    await expect(page.locator('h1')).toHaveText('登录');
    // 应有账号、密码输入框和登录按钮
    await expect(page.locator('input[placeholder="请输入账号"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toHaveText('登录');
  });

  test('空表单提交被拦截', async ({ page }) => {
    await page.goto(LOGIN_URL);
    // 不填任何内容直接点登录
    await page.locator('button[type="submit"]').click();
    // 页面应停留在登录页（URL 不变）
    await expect(page).toHaveURL(/.*login/);
  });

  test('错误密码登录失败', async ({ page }) => {
    await page.goto(LOGIN_URL);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('wrong_password');
    await page.locator('button[type="submit"]').click();
    // 应停留在登录页
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/.*login/);
  });

  test('正确账号密码登录成功 → 跳转首页', async ({ page }) => {
    await page.goto(LOGIN_URL);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    // 登录成功后应跳转离开登录页（跳到首页或改密页）
    await page.waitForTimeout(3000);
    const url = page.url();
    const isRedirected = !url.includes('#/login');
    expect(isRedirected).toBeTruthy();
  });

});
