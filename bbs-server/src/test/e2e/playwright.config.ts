import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:9081/bbs-user',
    headless: false,
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium', channel: 'chrome' } },
  ],
});
