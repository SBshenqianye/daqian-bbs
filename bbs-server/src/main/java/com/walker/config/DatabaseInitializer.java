package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * 数据库自动初始化组件
 * <p>
 * 首次部署（数据库为空表时）自动执行建表和导入基础数据。
 * 每次启动都会确保超级管理员密码与配置一致。
 * 支持 MySQL 和 PostgreSQL 两种方言。
 * </p>
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${bbs.init.super-admin.username:asiayak}")
    private String superAdminUsername;

    @Value("${bbs.init.super-admin.password:1234@abcD}")
    private String superAdminPassword;

    @PostConstruct
    public void init() {
        try {
            boolean tablesExist = checkTablesExist();
            if (!tablesExist) {
                log.info("数据库为空表，开始执行初始化脚本...");
                executeInitSql();
                log.info("数据库初始化脚本执行完成");
            } else {
                log.info("数据库表已存在，跳过建表初始化");
            }
            // 每次启动都确保超级管理员密码正确
            updateSuperAdminPassword();
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检查数据库中是否已有表
     */
    private boolean checkTablesExist() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            // 检查 bbs_user 表是否存在
            try (ResultSet rs = meta.getTables(null, null, "bbs_user", null)) {
                return rs.next();
            }
        }
    }

    /**
     * 检测数据库类型并执行对应方言的初始化 SQL
     */
    private void executeInitSql() throws Exception {
        String dbType = detectDatabaseType();
        String sqlFile;

        if ("postgresql".equals(dbType)) {
            sqlFile = "db/init/init-pg.sql";
        } else {
            sqlFile = "db/init/init-mysql.sql";
        }

        log.info("检测到数据库类型：{}，执行初始化脚本：{}", dbType, sqlFile);

        ClassPathResource resource = new ClassPathResource(sqlFile);
        if (!resource.exists()) {
            log.error("初始化 SQL 文件不存在：{}", sqlFile);
            return;
        }

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

        // 按分号分割 SQL 语句逐条执行
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                jdbcTemplate.execute(trimmed);
            } catch (Exception e) {
                // 跳过 SET 类和序列重置类的非关键错误
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
                    log.debug("跳过非关键语句: {}", trimmed.substring(0, Math.min(50, trimmed.length())));
                } else {
                    log.warn("执行 SQL 语句时出现警告: {} —— {}", trimmed.substring(0, Math.min(80, trimmed.length())), e.getMessage());
                }
            }
        }
    }

    /**
     * 检测数据库类型
     */
    private String detectDatabaseType() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String productName = meta.getDatabaseProductName().toLowerCase();
            if (productName.contains("postgresql") || productName.contains("postgres")) {
                return "postgresql";
            }
            return "mysql";
        }
    }

    /**
     * 更新超级管理员密码
     */
    private void updateSuperAdminPassword() {
        try {
            String encodedPassword = passwordEncoder.encode(superAdminPassword);
            int updated = jdbcTemplate.update(
                    "UPDATE bbs_user SET password = ?, is_first_login = 0 WHERE username = ? AND is_delete = 0",
                    encodedPassword, superAdminUsername
            );
            if (updated > 0) {
                log.info("超级管理员 [{}] 密码已更新", superAdminUsername);
            } else {
                log.warn("超级管理员 [{}] 不存在或已删除，跳过密码更新", superAdminUsername);
            }
        } catch (Exception e) {
            log.warn("更新超级管理员密码失败（可能是表不存在或尚未初始化）: {}", e.getMessage());
        }
    }
}
