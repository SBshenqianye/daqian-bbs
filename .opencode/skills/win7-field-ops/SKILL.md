---
name: win7-field-ops
description: 在 Win7 / 内网离线机器上做部署验收、连数据库、跑 GUI 与 E2E 工具时使用；包含 HeidiSQL/libpq 兼容、mshta HTA 验收、chromedriver 版本约束。
whenToUse: 目标机是 Win7（或 Win8 以下）且不能联外网；需要现场查库、发离线工具、跑界面验收 E2E，或遇到"DLL 报内部错误 127 / 找不到程序输入点"时加载。
---

# Win7 现场运维（离线 / 内网）

## 1. 数据库客户端

- 用 `dist/HeidiSQL-12.22-Win7-中文便携版.zip`：免费、解压即用、自带简体中文语言包。
- Navicat Premium Lite 官方要求 Win8+，**Win7 装不了**。
- **libpq 必须是老构建**：HeidiSQL 自带的 `libpq-15/17.dll` 由 OpenSSL 3 构建，静态引用 Win8+ 的 `GetSystemTimePreciseAsFileTime`，加载即报「无法定位程序输入点 / 内部错误 127」。
  - 判据：`strings libpq*.dll | grep GetSystemTimePreciseAsFileTime`，有命中就是不能用。
  - 可用替代：PostgreSQL 13 及更早的官方 MSVC 构建 `libpq.dll`（配 `libssl-1_1-x64.dll` / `libcrypto-1_1-x64.dll`，可复用 HeidiSQL 自带 `libintl-9.dll`）。
  - PG 13 服务端密码哈希是 `md5…`，老客户端可直接认证。
  - 同批中招：`libmariadb.dll`、`libmysql-8.4/9.4.dll`、`sqlite3.dll`；Win7 改用 `libmysql-6.1.dll` / `libmysql.dll`。

## 2. 界面级验收 E2E

- 首选单文件 HTA：`scripts/test/e2e/daqian-bbs-e2e.hta`，mshta + WinHttp，**不需要安装任何运行库**；负向组 3 项单独统计。
- Selenium 用 `scripts/test/e2e-selenium/`：单 jar + **chromedriver 109**（Chrome 109 是 Win7 能装的最后一个大版本；更高版本要求 Win10+）。Chrome 与 driver 大版本必须一致，否则报 `session not created: This version of ChromeDriver only supports Chrome version 109`。
- 浏览器自动探测覆盖 Chrome / Edge / 360 极速 / 360 安全（注册表 App Paths + 常见安装目录）；UI 冒烟走 `--headless`，失败回退 `--headless=new`，兼容含空格路径。
- 生产安全模式默认开启：不跑写入用例；内置负向自检 4 项，负向失败单独统计但整体退出码为 1。

## 3. 排障顺序

1. HTA 打不开 → 先看组策略是否禁用 `mshta`；被禁则改用 Selenium 包。
2. 连库失败 → 先看 DLL 加载错误（127 / 找不到入口点），再怀疑密码。
3. 控制台中文乱码、脚本 CRLF、含空格路径传参 → 见全局 skill `windows-powershell`。
4. GUI 工具/自绘界面必须截图验收，不要只看退出码 → 见全局 skill `win-desktop-shell`。
