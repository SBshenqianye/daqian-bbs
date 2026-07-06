package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
            log.warn("JDBC URL is empty, skip database bootstrap.");
            return;
        }

        String dbName = extractDatabaseName(jdbcUrl);
        String dbType = detectDbType(jdbcUrl);
        if (dbName == null) {
            log.info("No database name found in JDBC URL, skip database bootstrap.");
            return;
        }

        try {
            // 1. 确保数据库存在
            String adminUrl = buildAdminUrl(jdbcUrl, dbType);
            ensureDatabaseExists(adminUrl, dbUsername, dbPassword, dbName, dbType);

            // 2. 检查是否已有表（防止重复初始化覆盖数据）
            boolean isFirstInit = false;
            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUsername, dbPassword)) {
                if (hasExistingTables(conn, dbType)) {
                    log.info("Tables already exist in database [{}], skip init SQL.", dbName);
                } else {
                    isFirstInit = true;
                }
            } catch (Exception e) {
                log.warn("Failed to check existing tables, will run init SQL anyway: {}", e.getMessage());
                isFirstInit = true;
            }

            if (isFirstInit) {
                // 3. 首次初始化：建表 + 基础数据
                log.info("First-time init for database [{}], executing init SQL.", dbName);
                executeInitSql(jdbcUrl, dbUsername, dbPassword, dbType);
            }

            // 4. 升级 SQL 由 DatabaseInitializer（@PostConstruct 在 Spring 容器内执行）负责
            //    因为 bootstrap 阶段的配置加载不支持多 profile（如 dev,local），
            //    在 Spring 容器内执行更可靠
        } catch (Exception e) {
            log.error("Database bootstrap failed: {}", e.getMessage(), e);
        }
    }

    // ==================== 数据库创建 ====================

    /**
     * 确保数据库存在。不存在则创建。
     */
    private static void ensureDatabaseExists(String adminUrl, String username, String password,
                                             String dbName, String dbType) {
        log.info("Checking database [{}] via [{}]", dbName, adminUrl);
        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            boolean exists = "postgresql".equals(dbType)
                    ? checkPgDatabaseExists(conn, dbName)
                    : checkMysqlDatabaseExists(conn, dbName);
            if (!exists) {
                createDatabase(conn, dbName, dbType);
                log.info("Database [{}] created.", dbName);
            } else {
                log.info("Database [{}] already exists.", dbName);
            }
        } catch (SQLException e) {
            log.warn("Create/check database failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Create/check database failed: {}", e.getMessage(), e);
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

    /**
     * 检查指定数据库中是否已存在 bbs_user 表。
     * 用于判断是首次初始化还是重复启动。
     */
    private static boolean hasExistingTables(Connection conn, String dbType) throws Exception {
        String sql;
        if ("postgresql".equals(dbType)) {
            sql = "SELECT 1 FROM information_schema.tables WHERE table_name = 'bbs_user' AND table_schema = 'public'";
        } else {
            sql = "SELECT 1 FROM information_schema.tables WHERE table_name = 'bbs_user' AND table_schema = DATABASE()";
        }
        try (ResultSet rs = conn.createStatement().executeQuery(sql)) {
            return rs.next();
        }
    }

    // ==================== 建表 + 导数据 ====================

    private static void executeInitSql(String url, String username, String password,
                                       String dbType) throws Exception {
        String sqlFile = "postgresql".equals(dbType) ? "db/init/init-pg.sql" : "db/init/init-mysql.sql";
        log.info("Executing init SQL: {}", sqlFile);
        executeSqlFile(url, username, password, sqlFile);
    }

    private static void executeUpgradeSql(String url, String username, String password,
                                          String dbType) throws Exception {
        String sqlFile = "postgresql".equals(dbType) ? "db/init/upgrade-pg.sql" : "db/init/upgrade-mysql.sql";
        log.info("Executing upgrade SQL: {}", sqlFile);
        executeSqlFile(url, username, password, sqlFile);
    }

    private static void executeSqlFile(String url, String username, String password,
                                       String sqlFile) throws Exception {
        ClassPathResource resource = new ClassPathResource(sqlFile);
        if (!resource.exists()) {
            log.warn("SQL file does not exist: {}", sqlFile);
            return;
        }

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    String upper = trimmed.toUpperCase();
                    if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
                        log.debug("Skip non-critical SQL: {}", preview(trimmed));
                    } else {
                        log.warn("SQL warning while executing [{}]: {}", preview(trimmed), e.getMessage());
                    }
                }
            }
        }
    }

    private static String preview(String sql) {
        return sql.substring(0, Math.min(100, sql.length()));
    }

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
