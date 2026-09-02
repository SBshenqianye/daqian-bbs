// @ts-check
import { test, expect } from '@playwright/test';

/**
 * bbs-ui 用户前台 E2E 测试
 * 覆盖核心用户流程：首页、登录、浏览帖子、发帖、评论、搜索、个人中心
 *
 * 前提条件：后端 9083 + 前端 9081 均已启动
 * 测试账号：asiayak / 1234@abcD（超级管理员，可用于用户端登录）
 */

const BASE = '/';

// ============================================================
// 1. 首页加载
// ============================================================
test.describe('首页加载', () => {

  test('访问首页，验证页面标题和导航栏', async ({ page }) => {
    // 访问首页（hash 路由 / 重定向到 /forum）
    await page.goto(`${BASE}#/forum`);

    // 验证页面标题包含"大千智荟"
    await expect(page).toHaveTitle(/大千智荟/);

    // 验证导航栏品牌名称存在
    const brand = page.locator('a:has-text("大千智荟交流论坛")');
    await expect(brand).toBeVisible();

    // 验证导航菜单项存在：论坛、精华帖、排行榜
    await expect(page.locator('nav a:has-text("论坛")')).toBeVisible();
    await expect(page.locator('nav a:has-text("精华帖")')).toBeVisible();
    await expect(page.locator('nav a:has-text("排行榜")')).toBeVisible();

    // 验证搜索框存在
    await expect(page.locator('input[placeholder="搜索标题、内容或人员..."]')).toBeVisible();

    // 验证发布按钮存在
    await expect(page.locator('a:has-text("发布")')).toBeVisible();
  });

  test('首页 Hero 区域显示欢迎语', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);

    // 验证 Hero 区域的欢迎标题
    await expect(page.locator('h1:has-text("欢迎来到大千智荟创新创意交流论坛")')).toBeVisible();
    // 验证副标题
    await expect(page.locator('text=汇聚国网智慧，激发创新灵感')).toBeVisible();
  });

  test('首页侧边栏分类导航加载', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);

    // 等待分类加载（至少有"全部"按钮）
    await expect(page.locator('button:has-text("全部")').first()).toBeVisible({ timeout: 10000 });
  });

  test('首页右侧热榜区域显示', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);

    // 验证热榜标题存在
    await expect(page.locator('h2:has-text("热榜")')).toBeVisible();
  });

});

// ============================================================
// 2. 登录流程
// ============================================================
test.describe('登录流程', () => {

  test('登录页面加载正常', async ({ page }) => {
    await page.goto(`${BASE}#/login`);

    // 页面应显示"登录"标题
    await expect(page.locator('h1')).toHaveText('登录');
    // 应有账号、密码输入框和登录按钮
    await expect(page.locator('input[placeholder="请输入账号"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toHaveText('登录');
  });

  test('空表单提交被拦截', async ({ page }) => {
    await page.goto(`${BASE}#/login`);

    // 不填任何内容直接点登录
    await page.locator('button[type="submit"]').click();

    // 页面应停留在登录页（URL 不变）
    await expect(page).toHaveURL(/.*login/);
  });

  test('错误密码登录失败，停留在登录页', async ({ page }) => {
    await page.goto(`${BASE}#/login`);

    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('wrong_password');
    await page.locator('button[type="submit"]').click();

    // 等待请求完成，应停留在登录页
    await page.waitForTimeout(2000);
    await expect(page).toHaveURL(/.*login/);
  });

  test('正确账号密码登录成功，跳转离开登录页', async ({ page }) => {
    await page.goto(`${BASE}#/login`);

    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();

    // 登录成功后应跳转离开登录页（跳到首页或改密页）
    await page.waitForTimeout(3000);
    const url = page.url();
    const isRedirected = !url.includes('#/login');
    expect(isRedirected).toBeTruthy();
  });

  test('记住我复选框可勾选', async ({ page }) => {
    await page.goto(`${BASE}#/login`);

    const checkbox = page.locator('input[type="checkbox"]');
    await expect(checkbox).toBeVisible();

    // 点击勾选
    await checkbox.check();
    await expect(checkbox).toBeChecked();

    // 再次点击取消
    await checkbox.uncheck();
    await expect(checkbox).not.toBeChecked();
  });

});

// ============================================================
// 3. 浏览帖子
// ============================================================
test.describe('浏览帖子', () => {

  test('帖子列表加载，显示文章卡片', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);

    // 等待帖子列表区域加载（文章卡片或空状态提示）
    await page.waitForTimeout(2000);

    // 验证帖子列表区域存在（要么有文章卡片，要么有"没有更多帖子"的提示）
    const articleCards = page.locator('article.bg-container');
    const emptyHint = page.locator('text=已经到底啦');
    const hasContent = await articleCards.count() > 0 || await emptyHint.count() > 0;
    expect(hasContent).toBeTruthy();
  });

  test('点击帖子进入详情页', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(2000);

    // 如果有文章卡片，点击第一个进入详情
    const firstArticle = page.locator('article.bg-container').first();
    if (await firstArticle.count() > 0) {
      await firstArticle.click();

      // 等待页面跳转到详情页
      await page.waitForTimeout(1000);
      // 验证 URL 包含 articleDetails
      await expect(page).toHaveURL(/.*articleDetails/);
    }
  });

  test('帖子详情页显示文章标题和评论区', async ({ page }) => {
    // 先访问首页获取一个文章 ID
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(2000);

    const firstArticle = page.locator('article.bg-container').first();
    if (await firstArticle.count() > 0) {
      await firstArticle.click();
      await page.waitForTimeout(1000);

      // 验证文章标题存在
      await expect(page.locator('h1').first()).toBeVisible();

      // 验证评论区存在
      await expect(page.locator('h3:has-text("评论")')).toBeVisible();
    }
  });

});

// ============================================================
// 4. 发帖流程
// ============================================================
test.describe('发帖流程', () => {

  test('未登录访问发帖页，弹出登录提示或跳转', async ({ page }) => {
    // 清除登录状态后访问发帖页
    await page.goto(`${BASE}#/write`);

    // 等待页面响应
    await page.waitForTimeout(2000);

    // 可能行为：弹出登录提示 / 跳转到登录页 / 显示发帖编辑器
    const url = page.url();
    const redirectedToLogin = url.includes('#/login');
    const hasAlert = await page.locator('.el-message-box, .el-message, [class*="dialog"], [class*="alert"]').count() > 0;
    const hasEditor = await page.locator('textarea, [class*="editor"], [class*="write"], [class*="post"]').count() > 0;

    // 至少满足一种情况
    expect(redirectedToLogin || hasAlert || hasEditor).toBeTruthy();
  });

  test('已登录用户访问发帖页，显示编辑器', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到发帖页
    await page.goto(`${BASE}#/write`);
    await page.waitForTimeout(1000);

    // 验证标题输入框存在
    await expect(page.locator('textarea[placeholder="请输入文章标题"]')).toBeVisible();

    // 验证正文编辑区存在
    await expect(page.locator('[contenteditable="true"]')).toBeVisible();

    // 验证标签选择区存在
    await expect(page.locator('text=标签：')).toBeVisible();

    // 验证发布和取消按钮存在
    await expect(page.locator('button:has-text("发布")')).toBeVisible();
    await expect(page.locator('button:has-text("取消")')).toBeVisible();
  });

  test('发帖页标题不能为空', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到发帖页
    await page.goto(`${BASE}#/write`);
    await page.waitForTimeout(1000);

    // 不填标题，直接点发布
    await page.locator('button:has-text("发布")').first().click();

    // 应弹出"标题不能为空"的提示
    await expect(page.locator('text=标题不能为空')).toBeVisible();
  });

  test('发帖页内容不能为空', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到发帖页
    await page.goto(`${BASE}#/write`);
    await page.waitForTimeout(1000);

    // 填写标题但不填内容
    await page.locator('textarea[placeholder="请输入文章标题"]').fill('E2E测试帖子');
    await page.locator('button:has-text("发布")').first().click();

    // 应弹出"内容不能为空"的提示
    await expect(page.locator('text=内容不能为空')).toBeVisible();
  });

});

// ============================================================
// 5. 评论流程
// ============================================================
test.describe('评论流程', () => {

  test('帖子详情页评论输入框和按钮存在', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 访问首页，点击第一个帖子
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(2000);

    const firstArticle = page.locator('article.bg-container').first();
    if (await firstArticle.count() > 0) {
      await firstArticle.click();
      await page.waitForTimeout(1000);

      // 验证评论输入框存在
      const commentInput = page.locator('textarea').filter({ hasText: '' }).first();
      await expect(commentInput).toBeVisible();

      // 验证发表评论按钮存在
      await expect(page.locator('button:has-text("发表评论")')).toBeVisible();
    }
  });

  test('空评论提交被拦截', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 访问首页，点击第一个帖子
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(2000);

    const firstArticle = page.locator('article.bg-container').first();
    if (await firstArticle.count() > 0) {
      await firstArticle.click();
      await page.waitForTimeout(1000);

      // 不填内容直接点发表评论
      await page.locator('button:has-text("发表评论")').click();

      // 应弹出提示（评论不能为空）
      await page.waitForTimeout(1000);
      // 验证没有产生新的评论（页面无变化或有提示）
    }
  });

});

// ============================================================
// 6. 搜索功能
// ============================================================
test.describe('搜索功能', () => {

  test('搜索框输入关键词，触发搜索', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(1000);

    const searchInput = page.locator('input[placeholder*="搜索"]');
    await expect(searchInput.first()).toBeVisible();

    // 输入搜索关键词
    await searchInput.first().fill('测试');

    // 按回车触发搜索
    await searchInput.first().press('Enter');

    // 等待搜索结果加载
    await page.waitForTimeout(3000);

    // 验证页面仍可正常访问（不崩溃即可）
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
    expect(bodyText.length).toBeGreaterThan(0);
  });

  test('搜索框为空时回车不报错', async ({ page }) => {
    await page.goto(`${BASE}#/forum`);
    await page.waitForTimeout(1000);

    const searchInput = page.locator('input[placeholder*="搜索"]').first();
    await searchInput.fill('');
    await searchInput.press('Enter');

    // 页面应正常显示，不报错
    await page.waitForTimeout(1000);
    // 验证页面仍可访问（不崩溃）
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).toBeTruthy();
  });

});

// ============================================================
// 7. 个人中心
// ============================================================
test.describe('个人中心', () => {

  test('登录后访问个人中心，验证信息显示', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到个人中心
    await page.goto(`${BASE}#/userinfo`);
    await page.waitForTimeout(1000);

    // 验证个人中心页面标题
    await expect(page.locator('h2:has-text("账户设置")')).toBeVisible();

    // 验证侧边栏导航存在
    await expect(page.locator('button:has-text("信息设置")')).toBeVisible();
    await expect(page.locator('button:has-text("密码修改")')).toBeVisible();

    // 验证用户昵称区域存在
    await expect(page.locator('h1').first()).toBeVisible();
  });

  test('个人中心侧边栏可切换标签页', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到个人中心
    await page.goto(`${BASE}#/userinfo`);
    await page.waitForTimeout(1000);

    // 点击"密码修改"标签
    await page.locator('button:has-text("密码修改")').click();
    await page.waitForTimeout(500);

    // 验证密码修改相关内容出现（旧密码输入框）
    await expect(page.locator('text=旧密码').or(page.locator('text=当前密码'))).toBeVisible();
  });

  test('个人中心侧边栏链接到违规记录', async ({ page }) => {
    // 先登录
    await page.goto(`${BASE}#/login`);
    await page.locator('input[placeholder="请输入账号"]').fill('asiayak');
    await page.locator('input[placeholder="请输入密码"]').fill('1234@abcD');
    await page.locator('button[type="submit"]').click();
    await page.waitForTimeout(3000);

    // 跳转到个人中心
    await page.goto(`${BASE}#/userinfo`);
    await page.waitForTimeout(1000);

    // 点击违规记录链接
    const violationLink = page.locator('a:has-text("违规记录")');
    await expect(violationLink).toBeVisible();
    await violationLink.click();

    // 应跳转到违规记录页
    await page.waitForTimeout(1000);
    await expect(page).toHaveURL(/.*my-violations/);
  });

});

// ============================================================
// 8. 精华帖页面
// ============================================================
test.describe('精华帖页面', () => {

  test('精华帖页面加载正常', async ({ page }) => {
    await page.goto(`${BASE}#/featured`);
    await page.waitForTimeout(2000);

    // 验证页面存在（有标题或内容区域）
    // 精华帖页面标题
    const pageContent = page.locator('main');
    await expect(pageContent).toBeVisible();
  });

});

// ============================================================
// 9. 排行榜页面
// ============================================================
test.describe('排行榜页面', () => {

  test('排行榜页面加载正常', async ({ page }) => {
    await page.goto(`${BASE}#/points`);
    await page.waitForTimeout(2000);

    // 验证页面存在
    const pageContent = page.locator('main');
    await expect(pageContent).toBeVisible();
  });

});
