package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库引导初始化工具（在 Spring 启动前执行）
 * <p>
 * 负责：数据库不存在 → 创建；表不存在 → 建表导数据。
 * 使用 raw JDBC，不依赖 Spring 容器或连接池。
 * </p>
 */
public class DatabaseInitHelper {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitHelper.class);

    /**
     * 从配置属性执行数据库引导初始化。
     * 在 SpringApplication.run() 之前调用。
     */
    public static void bootstrap(String jdbcUrl, String dbUsername, String dbPassword) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            log.warn("未提供 JDBC URL，跳过数据库引导初始化");
            return;
        }

        String dbName = extractDatabaseName(jdbcUrl);
        String dbType = detectDbType(jdbcUrl);
        if (dbName == null) {
            log.info("JDBC URL 中未指定数据库名，跳过引导初始化");
            return;
        }

        try {
            // 1. 确保数据库存在
            String adminUrl = buildAdminUrl(jdbcUrl, dbType);
            ensureDatabaseExists(adminUrl, dbUsername, dbPassword, dbName, dbType);

            // 2. 检查表是否存在，不存在则建表导数据
            boolean tablesExist = checkTablesExist(jdbcUrl, dbUsername, dbPassword);
            if (!tablesExist) {
                log.info("数据库 [{}] 为空，开始执行初始化脚本...", dbName);
                executeInitSql(jdbcUrl, dbUsername, dbPassword, dbType);
                log.info("数据库 [{}] 初始化脚本执行完成", dbName);
            } else {
                log.info("数据库 [{}] 表已存在，跳过初始化", dbName);
            }
        } catch (Exception e) {
            log.error("数据库引导初始化失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 数据库创建 ====================

    private static void ensureDatabaseExists(String adminUrl, String username, String password,
                                             String dbName, String dbType) throws Exception {
        log.info("检查数据库 [{}] 是否存在 (via: {})", dbName, adminUrl);
        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            boolean exists = "postgresql".equals(dbType)
                    ? checkPgDatabaseExists(conn, dbName)
                    : checkMysqlDatabaseExists(conn, dbName);
            if (!exists) {
                createDatabase(conn, dbName, dbType);
                log.info("数据库 [{}] 创建成功", dbName);
            } else {
                log.info("数据库 [{}] 已存在", dbName);
            }
        } catch (SQLException e) {
            log.warn("创建数据库失败: {}", e.getMessage());
        }
    }

    private static boolean checkMysqlDatabaseExists(Connection conn, String dbName) throws Exception {
        try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT 1 FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + dbName + "'")) {
            return rs.next();
        }
    }

    private static boolean checkPgDatabaseExists(Connection conn, String dbName) throws Exception {
        // PostgreSQL 的 CREATE DATABASE 不能放在事务中，先关掉自动提交
        conn.setAutoCommit(true);
        try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
            return rs.next();
        }
    }

    private static void createDatabase(Connection conn, String dbName, String dbType) throws Exception {
        if ("postgresql".equals(dbType)) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
            }
        } else {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE `" + dbName + "` CHARACTER SET utf8 COLLATE utf8_general_ci");
            }
        }
    }

    // ==================== 建表 + 导数据 ====================

    private static boolean checkTablesExist(String url, String username, String password) throws Exception {
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "bbs_user", null)) {
                return rs.next();
            }
        }
    }

    private static void executeInitSql(String url, String username, String password,
                                       String dbType) throws Exception {
        String sqlFile = "postgresql".equals(dbType) ? "db/init/init-pg.sql" : "db/init/init-mysql.sql";
        log.info("执行初始化脚本：{}", sqlFile);

        ClassPathResource resource = new ClassPathResource(sqlFile);
        if (!resource.exists()) {
            log.error("初始化 SQL 文件不存在：{}", sqlFile);
            return;
        }

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    String upper = trimmed.toUpperCase();
                    if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
                        log.debug("跳过非关键语句: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                    } else {
                        log.warn("执行 SQL 时出现警告: {} —— {}", trimmed.substring(0, Math.min(80, trimmed.length())), e.getMessage());
                    }
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private static String extractDatabaseName(String url) {
        Matcher m = Pattern.compile("/([^/?]+)(\\?|$)").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private static String buildAdminUrl(String originalUrl, String dbType) {
        String url;
        if ("postgresql".equals(dbType)) {
            url = originalUrl.replaceFirst("/[^/?]+(\\?|$)", "/postgres$1");
        } else {
            url = originalUrl.replaceFirst("/[^/?]+(\\?|$)", "/information_schema$1");
        }
        if (!url.contains("connectTimeout")) {
            url += (url.contains("?") ? "&" : "?") + "connectTimeout=5000";
        }
        return url;
    }

    private static String detectDbType(String url) {
        return (url != null && url.startsWith("jdbc:postgresql")) ? "postgresql" : "mysql";
    }
}
