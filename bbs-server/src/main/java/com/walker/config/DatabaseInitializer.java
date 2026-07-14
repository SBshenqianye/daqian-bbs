package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

/**
 * 数据库初始化后置组件
 * <p>
 * 在 Spring 完全启动后执行升级 SQL（安全可重复执行，失败仅打 warning）。
 * 建库建表等前置工作由 DatabaseInitHelper.bootstrap() 在 Spring 启动前完成。
 * </p>
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        String dbType = detectDbType();
        executeUpgradeSql(dbType);
    }

    /**
     * 执行升级 SQL 文件（每次启动运行，出错仅打 warning，保证幂等）
     */
    private void executeUpgradeSql(String dbType) {
        String sqlFile = "postgresql".equals(dbType) ? "db/init/upgrade-pg.sql" : "db/init/upgrade-mysql.sql";
        try {
            ClassPathResource resource = new ClassPathResource(sqlFile);
            if (!resource.exists()) {
                log.warn("Upgrade SQL file does not exist: {}", sqlFile);
                return;
            }
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    jdbcTemplate.execute(trimmed);
                    log.info("Upgrade SQL executed: {}", preview(trimmed));
                } catch (Exception e) {
                    log.warn("Upgrade SQL warning [{}]: {}", preview(trimmed), e.getMessage());
                }
            }
            log.info("Database upgrade scripts applied successfully.");
        } catch (Exception e) {
            log.warn("Failed to apply upgrade scripts: {}", e.getMessage());
        }
    }

    private String detectDbType() {
        try {
            String url = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            return (url != null && url.startsWith("jdbc:postgresql")) ? "postgresql" : "mysql";
        } catch (Exception e) {
            return "mysql";
        }
    }

    private static String preview(String sql) {
        return sql.substring(0, Math.min(100, sql.length())).replace("\n", " ").replace("\r", "");
    }
}
