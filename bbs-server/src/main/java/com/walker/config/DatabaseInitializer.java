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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据库迁移执行器（带版本追踪）
 * <p>
 * 在 Spring 完成启动后执行升级 SQL：
 * 1. 确保 bbs_schema_version 表存在（兼容旧库）
 * 2. 读取已执行的迁移版本
 * 3. 只执行尚未执行的 @migration 块
 * 4. 记录执行结果
 * </p>
 *
 * <h3>upgrade-*.sql 约定</h3>
 * <pre>
 * -- @migration: v1-feedback-contact 配置使用反馈联系方式
 * INSERT INTO bbs_system_config ...;
 *
 * -- @migration: v2-add-display-selected 增加 is_display_selected 字段
 * ALTER TABLE bbs_sa_org ADD COLUMN ...;
 * </pre>
 * <p>
 * 每个迁移块以 {@code -- @migration: <id> <description>} 开头，
 * 直到下一个 {@code -- @migration:} 或文件结束。
 * 无标记的 SQL（文件开头到第一个 @migration 之间）视为"版本前遗留"，
 * 在兼容初始化时一次性标记为已执行。
 * </p>
 */
@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String MIGRATION_PREFIX = "-- @migration: ";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        String dbType = detectDbType();
        ensureSchemaVersionTable(dbType);
        baselineExistingMigrations(dbType);
        executePendingMigrations(dbType);
    }

    // ==================== 1. 确保版本追踪表存在 ====================

    private void ensureSchemaVersionTable(String dbType) {
        try {
            if ("postgresql".equals(dbType)) {
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS bbs_schema_version (" +
                    "version varchar(50) PRIMARY KEY, " +
                    "description varchar(255), " +
                    "applied_at varchar(20) NOT NULL)");
            } else {
                // MySQL: 检查表是否存在
                List<Map<String, Object>> rs = jdbcTemplate.queryForList(
                    "SELECT COUNT(*) AS cnt FROM information_schema.TABLES " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bbs_schema_version'");
                long cnt = (Long) rs.get(0).get("cnt");
                if (cnt == 0) {
                    jdbcTemplate.execute(
                        "CREATE TABLE bbs_schema_version (" +
                        "version varchar(50) NOT NULL, " +
                        "description varchar(255), " +
                        "applied_at varchar(20) NOT NULL, " +
                        "PRIMARY KEY (version)) " +
                        "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
                }
            }
            log.info("bbs_schema_version table is ready.");
        } catch (Exception e) {
            log.warn("Failed to ensure bbs_schema_version table: {}", e.getMessage());
        }
    }

    // ==================== 2. 兼容初始化：标记遗留迁移 ====================

    /**
     * 对于已有数据库（upgrade 表已存在但 bbs_schema_version 是新建的），
     * 将 upgrade SQL 中所有已有 @migration 标记一次性录入为"已执行"，
     * 避免旧迁移被重复执行。
     */
    private void baselineExistingMigrations(String dbType) {
        try {
            Set<String> applied = getAppliedVersions();
            if (!applied.isEmpty()) {
                log.info("bbs_schema_version already has {} records, skip baseline.", applied.size());
                return;
            }

            // 表是空的 → 需要做基线：把 upgrade SQL 里所有迁移标记录入
            String sqlFile = "postgresql".equals(dbType)
                    ? "db/init/upgrade-pg.sql" : "db/init/upgrade-mysql.sql";
            List<MigrationBlock> blocks = parseMigrationBlocks(sqlFile);

            String now = LocalDateTime.now().format(FMT);
            int count = 0;
            for (MigrationBlock block : blocks) {
                if (!applied.contains(block.version)) {
                    jdbcTemplate.update(
                        "INSERT INTO bbs_schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                        block.version, block.description, now);
                    count++;
                }
            }
            log.info("Baseline: marked {} existing migrations as applied.", count);
        } catch (Exception e) {
            log.warn("Baseline failed: {}", e.getMessage());
        }
    }

    // ==================== 3. 执行未完成的迁移 ====================

    private void executePendingMigrations(String dbType) {
        try {
            String sqlFile = "postgresql".equals(dbType)
                    ? "db/init/upgrade-pg.sql" : "db/init/upgrade-mysql.sql";
            ClassPathResource resource = new ClassPathResource(sqlFile);
            if (!resource.exists()) {
                log.warn("Upgrade SQL file does not exist: {}", sqlFile);
                return;
            }

            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            Set<String> applied = getAppliedVersions();
            List<MigrationBlock> blocks = parseMigrationBlocks(sql);

            String now = LocalDateTime.now().format(FMT);
            int executed = 0;

            for (MigrationBlock block : blocks) {
                if (applied.contains(block.version)) {
                    log.debug("Skip applied migration: {}", block.version);
                    continue;
                }

                log.info("Executing migration: {} - {}", block.version, block.description);
                try {
                    // 执行该块内所有 SQL 语句
                    String[] statements = block.sql.split(";");
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (trimmed.isEmpty()) continue;
                        try {
                            jdbcTemplate.execute(trimmed);
                        } catch (Exception e) {
                            String upper = trimmed.toUpperCase();
                            if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
                                log.debug("Skip non-critical SQL: {}", preview(trimmed));
                            } else {
                                log.warn("Migration SQL warning [{}]: {}", preview(trimmed), e.getMessage());
                            }
                        }
                    }
                    // 记录成功
                    jdbcTemplate.update(
                        "INSERT INTO bbs_schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                        block.version, block.description, now);
                    executed++;
                    log.info("Migration applied: {}", block.version);
                } catch (Exception e) {
                    log.error("Migration FAILED: {} - {}", block.version, e.getMessage(), e);
                    // 不记录版本，下次重启重试
                }
            }

            if (executed > 0) {
                log.info("Database migrations complete. {} new migration(s) applied.", executed);
            } else {
                log.info("Database is up to date. No pending migrations.");
            }
        } catch (Exception e) {
            log.warn("Failed to execute migrations: {}", e.getMessage());
        }
    }

    // ==================== SQL 解析 ====================

    /**
     * 解析 upgrade SQL 文件，按 @migration 标记切分为迁移块。
     * 文件开头到第一个 @migration 之间的 SQL 归入一个空版本块（兼容期用）。
     */
    private List<MigrationBlock> parseMigrationBlocks(String sqlFilePath) {
        List<MigrationBlock> blocks = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource(sqlFilePath);
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            String[] lines = sql.split("\n");

            StringBuilder currentSql = new StringBuilder();
            String currentVersion = null;
            String currentDescription = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith(MIGRATION_PREFIX)) {
                    // 遇到新的迁移标记 → 保存上一个块
                    if (currentVersion != null) {
                        blocks.add(new MigrationBlock(currentVersion, currentDescription, currentSql.toString()));
                    }
                    // 解析新标记: "-- @migration: v1-description"
                    String rest = trimmed.substring(MIGRATION_PREFIX.length()).trim();
                    int spaceIdx = rest.indexOf(' ');
                    if (spaceIdx > 0) {
                        currentVersion = rest.substring(0, spaceIdx);
                        currentDescription = rest.substring(spaceIdx + 1).trim();
                    } else {
                        currentVersion = rest;
                        currentDescription = "";
                    }
                    currentSql = new StringBuilder();
                } else {
                    currentSql.append(line).append("\n");
                }
            }
            // 保存最后一个块
            if (currentVersion != null) {
                blocks.add(new MigrationBlock(currentVersion, currentDescription, currentSql.toString()));
            }
        } catch (Exception e) {
            log.warn("Failed to parse migration blocks from {}: {}", sqlFilePath, e.getMessage());
        }
        return blocks;
    }

    // ==================== 版本查询 ====================

    private Set<String> getAppliedVersions() {
        Set<String> versions = new HashSet<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version FROM bbs_schema_version");
            for (Map<String, Object> row : rows) {
                versions.add((String) row.get("version"));
            }
        } catch (Exception e) {
            // 表可能还不存在（首次初始化），忽略
            log.debug("Could not query bbs_schema_version: {}", e.getMessage());
        }
        return versions;
    }

    // ==================== 工具方法 ====================

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

    // ==================== 数据结构 ====================

    private static class MigrationBlock {
        final String version;
        final String description;
        final String sql;

        MigrationBlock(String version, String description, String sql) {
            this.version = version;
            this.description = description;
            this.sql = sql;
        }
    }
}
