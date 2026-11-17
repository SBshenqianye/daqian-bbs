package com.daqian.e2e;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 大千智荟论坛 · 界面级 E2E（Selenium）
 * <p>
 * 特点：
 * - 单 jar + chromedriver，离线内网可用（不依赖 Maven / 外网）；
 * - 默认只读模式（readonly）：只做登录、浏览、断言、截图，不点会写数据的按钮；
 * - write 模式才执行点赞/评论等写操作，用于测试库/预发环境；
 * - 结果输出到 reports/ 下的 HTML 报告与截图。
 * </p>
 */
public class Main {

    private static final Properties CFG = new Properties();
    private static final List<Result> RESULTS = new ArrayList<Result>();
    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static Path baseDir;
    private static Path reportDir;
    private static Path shotDir;

    private static String baseUrl;
    private static String username;
    private static String password;
    private static boolean headless;
    private static boolean writeMode;
    private static boolean detailEnabled;
    private static boolean screenshotEnabled;
    private static String detailUrl;
    private static String runStamp;

    private static boolean selfCheckMode;

    public static void main(String[] args) {
        selfCheckMode = args.length > 0 && "--selfcheck".equalsIgnoreCase(args[0]);
        runStamp = TS.format(new Date());
        try {
            baseDir = Paths.get("").toAbsolutePath();
            loadConfig();
            prepareDirs();
            startDriver();
            if (selfCheckMode) {
                runSelfCheck();
            } else {
                runAll();
            }
        } catch (Throwable t) {
            System.out.println("[FATAL] " + t.getMessage());
            t.printStackTrace();
            RESULTS.add(new Result("启动", "FAIL", 0, String.valueOf(t.getMessage()), null));
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignore) { }
            }
            writeReport();
        }
        int failed = 0;
        for (Result r : RESULTS) {
            if ("FAIL".equals(r.status)) failed++;
        }
        System.out.println("汇总：通过 " + count("PASS") + " / 失败 " + failed + " / 跳过 " + count("SKIP")
                + " / 共 " + RESULTS.size());
        System.out.println("报告：" + (reportDir == null ? "-" : reportDir.resolve("report-" + runStamp + ".html")));
        System.exit(failed > 0 ? 1 : 0);
    }

    /* ==================== 配置与初始化 ==================== */

    private static void loadConfig() throws Exception {
        File f = new File(baseDir.toFile(), "config.properties");
        if (!f.exists()) {
            throw new IllegalStateException("缺少 config.properties（应与 e2e-ui.jar 同目录）");
        }
        InputStreamReader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        try { CFG.load(reader); } finally { reader.close(); }

        baseUrl = trim(get("baseUrl", "http://127.0.0.1:8080/bbs-user/"));
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        username = trim(get("username", ""));
        password = get("password", "");
        headless = Boolean.parseBoolean(get("headless", "true"));
        writeMode = "write".equalsIgnoreCase(trim(get("mode", "readonly")));
        detailEnabled = Boolean.parseBoolean(get("detail.enabled", "false"));
        detailUrl = trim(get("detail.url", ""));
        screenshotEnabled = Boolean.parseBoolean(get("screenshot", "true"));
    }

    private static void prepareDirs() throws Exception {
        reportDir = baseDir.resolve("reports");
        shotDir = reportDir.resolve("shots");
        Files.createDirectories(shotDir);
    }

    private static void startDriver() throws Exception {
        String driverPath = trim(get("driver.path", "driver/chromedriver.exe"));
        if (driverPath.length() > 0) {
            File df = new File(baseDir.toFile(), driverPath);
            if (!df.exists()) df = new File(driverPath);
            if (df.exists()) {
                System.setProperty("webdriver.chrome.driver", df.getAbsolutePath());
            } else if (isWindows()) {
                throw new IllegalStateException("找不到 chromedriver：" + driverPath);
            }
        }

        ChromeOptions options = new ChromeOptions();
        String chromeBinary = trim(get("chrome.binary", ""));
        if (chromeBinary.length() > 0) options.setBinary(chromeBinary);
        if (headless) {
            options.addArguments("--headless", "--disable-gpu", "--hide-scrollbars",
                    "--window-size=1440,1600", "--no-first-run", "--no-default-browser-check");
        } else {
            options.addArguments("--start-maximized");
        }
        options.addArguments("--disable-dev-shm-usage");
        options.setAcceptInsecureCerts(true);

        driver = new ChromeDriver(options);
        int timeout = Integer.parseInt(trim(get("timeout", "20")));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(Math.max(timeout, 30)));
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    /* ==================== 用例 ==================== */

    private static void runAll() {
        step("打开登录页", new Step() {
            public void run() {
                open("#/login");
                waitText("登录");
                shot("01-login");
            }
        });

        step("登录", new Step() {
            public void run() {
                if (username.length() == 0) throw new SkipException("未配置账号（config.properties username/password）");
                open("#/login");
                WebElement u = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input.ui-input")));
                u.clear();
                u.sendKeys(username);
                WebElement p = driver.findElement(By.cssSelector("input[type=password]"));
                p.clear();
                p.sendKeys(password);
                driver.findElement(By.xpath("//button[contains(normalize-space(.),'登录')]")).click();
                waitTextAny(new String[]{"论坛", "精华帖"}, "登录后未进入首页");
                shot("02-after-login");
            }
        });

        step("论坛列表渲染", new Step() {
            public void run() {
                open("#/forum");
                waitText("论坛");
                List<WebElement> cards = driver.findElements(By.cssSelector("article.ui-card"));
                if (cards.isEmpty()) throw new IllegalStateException("帖子列表没有渲染出卡片");
                shot("03-forum");
            }
        });

        step("打开帖子详情", new Step() {
            public void run() {
                if (!detailEnabled) throw new SkipException("detail.enabled=false（生产只读默认关闭，避免浏览量自增）");
                if (detailUrl.length() > 0) {
                    driver.get(detailUrl);
                } else {
                    List<WebElement> cards = driver.findElements(By.cssSelector("article.ui-card"));
                    if (cards.isEmpty()) throw new IllegalStateException("没有可点击的帖子卡片");
                    cards.get(0).click();
                }
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h1")));
                String text = bodyText();
                if (text.contains("已删除或不可见") || text.contains("帖子不存在")) {
                    throw new IllegalStateException("详情页显示不可见/不存在");
                }
                shot("04-detail");
            }
        });

        step("点赞（写操作）", new Step() {
            public void run() {
                requireWrite();
                List<WebElement> likes = driver.findElements(By.cssSelector(".int-btn"));
                if (likes.isEmpty()) throw new IllegalStateException("未找到互动按钮");
                WebElement like = likes.get(0);
                like.click();
                sleep(600);
                like.click();
                sleep(400);
                shot("05-like");
            }
        });

        step("发表评论并删除（写操作）", new Step() {
            public void run() {
                requireWrite();
                String content = "自动验收-" + runStamp;
                WebElement area = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("textarea")));
                area.clear();
                area.sendKeys(content);
                driver.findElement(By.xpath("//button[contains(normalize-space(.),'发表评论')]")).click();
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.xpath("//*[contains(normalize-space(text()),'" + content + "')]")));
                shot("06-comment");
                // 删除自己刚发的评论（找不到删除按钮时保留，由人工清理）
                List<WebElement> deletes = driver.findElements(By.cssSelector(".comment-action[title='删除']"));
                if (!deletes.isEmpty()) {
                    deletes.get(deletes.size() - 1).click();
                    sleep(800);
                }
            }
        });

        step("精华页渲染", new Step() {
            public void run() {
                open("#/featured");
                waitText("精华");
                shot("07-featured");
            }
        });

        step("排行榜渲染", new Step() {
            public void run() {
                open("#/points");
                waitText("排行榜");
                shot("08-points");
            }
        });

        step("个人中心", new Step() {
            public void run() {
                if (username.length() == 0) throw new SkipException("未配置账号");
                open("#/userinfo");
                waitText("个人中心");
                shot("09-userinfo");
            }
        });

        step("头像跳转消息/个人中心", new Step() {
            public void run() {
                if (username.length() == 0) throw new SkipException("未配置账号");
                open("#/forum");
                WebElement avatar = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("img[alt='Avatar']")));
                avatar.click();
                sleep(1200);
                String url = driver.getCurrentUrl();
                if (!url.contains("userinfo")) {
                    throw new IllegalStateException("点击头像后未跳转个人中心/消息页：" + url);
                }
                shot("10-avatar-jump");
            }
        });
    }

    private static void requireWrite() {
        if (!writeMode) throw new SkipException("只读模式：写操作已跳过（config.properties mode=write 可开启）");
    }

    /* ==================== 负向自检（只读） ==================== */

    private static void runSelfCheck() {
        step("[负向] 基线：登录页可渲染", new Step() {
            public void run() {
                open("#/login");
                waitText("登录");
            }
        });

        step("[负向] 错误 baseUrl 必须失败", new Step() {
            public void run() {
                try {
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));
                    boolean failed = false;
                    try {
                        driver.get("http://127.0.0.1:1/bbs-user/#/login");
                    } catch (Exception e) {
                        failed = true;
                    }
                    if (!failed) {
                        String text = "";
                        try { text = bodyText(); } catch (Exception ignore) { }
                        if (text.contains("欢迎回来")) {
                            throw new IllegalStateException("错误地址竟然渲染出业务页面，判据不可靠");
                        }
                    }
                } finally {
                    driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
                }
            }
        });

        step("[负向] 错误断言必须被捕获", new Step() {
            public void run() {
                boolean caught = false;
                try {
                    assertContains("__MUST_NOT_EXIST__", "错误断言");
                } catch (Exception e) {
                    caught = true;
                }
                if (!caught) throw new IllegalStateException("断言没有报错，判据可能是装饰品");
            }
        });

        step("[负向] 未登录访问业务接口被拒绝", new Step() {
            public void run() {
                String url = apiBase() + "/notification/unreadSummary?userId=1";
                String body = httpGet(url);
                if (body == null || body.indexOf("\"code\":401") < 0) {
                    throw new IllegalStateException("未登录竟然可以访问：" + clip(body));
                }
            }
        });
    }

    private static void assertContains(String expected, String label) {
        String body = bodyText();
        if (!body.contains(expected)) {
            throw new IllegalStateException(label + " 未命中预期文本：" + expected);
        }
    }

    private static String apiBase() {
        String cfg = trim(get("apiBase", ""));
        if (cfg.length() > 0) return cfg.replaceAll("/+$", "");
        try {
            java.net.URI u = java.net.URI.create(baseUrl);
            String port = u.getPort() > 0 ? ":" + u.getPort() : "";
            return u.getScheme() + "://" + u.getHost() + port + "/bbs-server";
        } catch (Exception e) {
            return baseUrl + "bbs-server";
        }
    }

    private static String httpGet(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            conn.setRequestMethod("GET");
            InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while (in != null && (n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /* ==================== 步骤执行器 ==================== */

    private interface Step {
        void run() throws Exception;
    }

    private static void step(String name, Step step) {
        long start = System.currentTimeMillis();
        String status = "PASS";
        String message = "";
        Path shotPath = null;
        try {
            step.run();
        } catch (SkipException e) {
            status = "SKIP";
            message = e.getMessage();
        } catch (Throwable t) {
            status = "FAIL";
            message = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
            shotPath = shot("fail-" + slug(name));
        }
        long cost = System.currentTimeMillis() - start;
        RESULTS.add(new Result(name, status, cost, message, shotPath));
        System.out.println("[" + status + "] " + name + " (" + cost + "ms)" + (message.length() > 0 ? " - " + message : ""));
    }

    private static void open(String hash) {
        driver.get(baseUrl + (hash.startsWith("#") ? hash : "#" + hash));
    }

    private static String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    private static void waitText(String text) {
        waitTextAny(new String[]{text}, "页面未出现文本：" + text);
    }

    private static void waitTextAny(String[] texts, String errMsg) {
        final String[] arr = texts;
        try {
            wait.until(new org.openqa.selenium.support.ui.ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver d) {
                    String body = d.findElement(By.tagName("body")).getText();
                    for (String t : arr) {
                        if (body.contains(t)) return true;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException(errMsg);
        }
    }

    private static Path shot(String name) {
        if (!screenshotEnabled || driver == null) return null;
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path target = shotDir.resolve(runStamp + "-" + slug(name) + ".png");
            Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (Exception e) {
            return null;
        }
    }

    private static String slug(String s) {
        return s.replaceAll("[^\\p{L}\\p{N}]+", "-");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignore) { }
    }

    /* ==================== 报告 ==================== */

    private static void writeReport() {
        if (reportDir == null) return;
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>大千智荟论坛 · UI E2E 报告</title>");
        html.append("<style>body{font-family:'Microsoft YaHei',Arial,sans-serif;background:#f5f7fb;color:#181c23;margin:24px}")
                .append("h1{font-size:20px}table{border-collapse:collapse;width:100%;background:#fff}")
                .append("th,td{border-bottom:1px solid #eef1f6;padding:6px 9px;text-align:left;font-size:13px}")
                .append("th{background:#f8fafd;color:#5b6475;font-weight:normal}")
                .append(".pass{color:#0a7d33}.fail{color:#c62828}.skip{color:#8a6d1d}")
                .append(".meta{color:#7b8194;font-size:12px;margin:8px 0 16px}")
                .append("a{color:#0052d9}</style></head><body>");
        html.append("<h1>大千智荟论坛 · 界面级 E2E 报告</h1>");
        html.append("<div class='meta'>时间：").append(new Date()).append("<br>")
                .append("地址：").append(esc(baseUrl)).append("<br>")
                .append("模式：").append(selfCheckMode ? "负向自检（selfcheck，不计入业务失败）" : (writeMode ? "write（含写操作）" : "readonly（只读）")).append("<br>")
                .append("账号：").append(username.length() > 0 ? esc(username) : "(未配置)").append("<br>")
                .append("汇总：通过 ").append(count("PASS")).append(" / 失败 ").append(count("FAIL"))
                .append(" / 跳过 ").append(count("SKIP")).append("</div>");
        html.append("<table><tr><th>#</th><th>用例</th><th>结果</th><th>耗时</th><th>说明</th><th>截图</th></tr>");
        int i = 1;
        for (Result r : RESULTS) {
            String cls = "PASS".equals(r.status) ? "pass" : ("FAIL".equals(r.status) ? "fail" : "skip");
            html.append("<tr><td>").append(i++).append("</td><td>").append(esc(r.name)).append("</td><td class='")
                    .append(cls).append("'>").append(r.status).append("</td><td>").append(r.cost).append("ms</td><td>")
                    .append(esc(r.message)).append("</td><td>");
            if (r.shot != null) {
                String rel = reportDir.relativize(r.shot).toString().replace('\\', '/');
                html.append("<a href='").append(rel).append("'>查看</a>");
            }
            html.append("</td></tr>");
        }
        html.append("</table></body></html>");
        try {
            Files.write(reportDir.resolve("report-" + runStamp + ".html"),
                    html.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("[WARN] 报告写入失败：" + e.getMessage());
        }
    }

    private static int count(String status) {
        int n = 0;
        for (Result r : RESULTS) if (status.equals(r.status)) n++;
        return n;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /* ==================== 小工具 ==================== */

    private static String get(String key, String def) {
        String v = CFG.getProperty(key);
        return v == null ? def : v;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String clip(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) + " ..." : s;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static class SkipException extends RuntimeException {
        SkipException(String msg) { super(msg); }
    }

    private static class Result {
        final String name;
        final String status;
        final long cost;
        final String message;
        final Path shot;

        Result(String name, String status, long cost, String message, Path shot) {
            this.name = name;
            this.status = status;
            this.cost = cost;
            this.message = message;
            this.shot = shot;
        }
    }
}
