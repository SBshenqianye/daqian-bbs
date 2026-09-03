import { defineConfig } from '@playwright/test';

// 支持环境变量覆盖端口（run-e2e.ps1 会设置这些变量）
const USER_PORT = process.env.E2E_USER_PORT || '9081';
const ADMIN_PORT = process.env.E2E_ADMIN_PORT || '9082';

export default defineConfig({
  testDir: './',
  timeout: 30000,
  use: {
    baseURL: `http://localhost:${USER_PORT}/bbs-user`,
    headless: false,
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium', channel: 'chrome' } },
  ],
});

// 导出管理端 URL 供测试文件使用
export const ADMIN_BASE = `http://localhost:${ADMIN_PORT}/bbs-admin`;
