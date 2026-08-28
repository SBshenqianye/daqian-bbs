package com.walker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库迁移执行器（带版本追踪 + 自动备份）
 * <p>
 * 在 Spring 完成启动后执行升级 SQL：
 * 1. 确保 bbs_schema_version 表存在（兼容旧库）
 * 2. 读取已执行的迁移版本
 * 3. 如果有待执行迁移，先自动备份受影响的表到新数据库
 * 4. 只执行尚未执行的 @migration 块
 * 5. 记录执行结果
 * </p>
 * <p>
 * 备份策略：每次有新迁移时，自动创建备份数据库 "原库名_yyyyMMdd_HHmmss"，
 * 将迁移 SQL 中涉及的表（结构+数据）复制到备份库中，确保数据安全可回滚。
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
    private static final DateTimeFormatter BACKUP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** SQL 语句中提取表名的正则：匹配 ALTER TABLE / INSERT INTO / UPDATE / DELETE FROM / CREATE TABLE / DROP TABLE */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
        "(?:ALTER\\s+TABLE|INSERT\\s+INTO|UPDATE|DELETE\\s+FROM|CREATE\\s+(?:TABLE|INDEX)|DROP\\s+TABLE)\\s+(?:IF\\s+(?:NOT\\s+EXISTS|EXISTS)\\s+)?[`\"']?([a-zA-Z_][a-zA-Z0-9_]*)[`\"']?",
        Pattern.CASE_INSENSITIVE);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.username:}")
    private String dbUsername;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

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

            Set<String> applied = getAppliedVersions();
            List<MigrationBlock> blocks = parseMigrationBlocks(sqlFile);

            // 收集待执行的迁移块
            List<MigrationBlock> pendingBlocks = new ArrayList<>();
            for (MigrationBlock block : blocks) {
                if (!applied.contains(block.version)) {
                    pendingBlocks.add(block);
                }
            }

            // ===== 自动备份：有待执行迁移时，先备份受影响的表 =====
            if (!pendingBlocks.isEmpty()) {
                Set<String> affectedTables = extractAffectedTables(pendingBlocks);
                if (!affectedTables.isEmpty()) {
                    backupAffectedTables(dbType, affectedTables);
                }
            }

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
                    boolean hasDdlError = false;
                    String[] statements = block.sql.split(";");
                    for (String statement : statements) {
                        String trimmed = statement.trim();
                        if (trimmed.isEmpty()) continue;
                        try {
                            jdbcTemplate.execute(trimmed);
                        } catch (Exception e) {
                            String upper = trimmed.toUpperCase();
                            if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
                                // MySQL 会话变量赋值 / PG 序列操作，JdbcTemplate 不支持，跳过
                                log.debug("Skip session-variable SQL: {}", preview(trimmed));
                            } else if (upper.startsWith("DROP ") && e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                                // DROP 不存在的对象，正常跳过
                                log.debug("Skip drop-if-not-exists: {}", preview(trimmed));
                            } else if (upper.startsWith("SELECT 1") || upper.startsWith("PREPARE ") || upper.startsWith("DEALLOCATE ")) {
                                // 纯占位语句 / MySQL PREPARE/DEALLOCATE，跳过
                                log.debug("Skip placeholder/prepared-stmt: {}", preview(trimmed));
                            } else if (upper.startsWith("ALTER TABLE") && e.getMessage() != null
                                    && (e.getMessage().contains("Duplicate column") || e.getMessage().contains("already exists"))) {
                                // 列已存在，视为成功
                                log.debug("Column already exists, skip: {}", preview(trimmed));
                            } else {
                                log.error("Migration DDL/DML FAILED [{}]: {} — {}", block.version, preview(trimmed), e.getMessage());
                                hasDdlError = true;
                            }
                        }
                    }
                    if (hasDdlError) {
                        log.error("Migration [{}] has errors, NOT recorded. Fix SQL and restart to retry.", block.version);
                        // 不记录版本，下次重启重试
                    } else {
                        // 全部成功，记录版本
                        jdbcTemplate.update(
                            "INSERT INTO bbs_schema_version (version, description, applied_at) VALUES (?, ?, ?)",
                            block.version, block.description, now);
                        executed++;
                        log.info("Migration applied: {}", block.version);
                    }
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

    /**
     * 从 JDBC URL 中提取数据库名称。
     */
    private String extractDatabaseName(String jdbcUrl) {
        try {
            Matcher m = Pattern.compile("/([^/?]+)(\\?|$)").matcher(jdbcUrl);
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String preview(String sql) {
        return sql.substring(0, Math.min(100, sql.length())).replace("\n", " ").replace("\r", "");
    }

    // ==================== 备份相关方法 ====================

    /**
     * 从待执行的迁移块中提取所有涉及的表名（去重，排除系统表）。
     */
    private Set<String> extractAffectedTables(List<MigrationBlock> pendingBlocks) {
        Set<String> tables = new LinkedHashSet<>();
        for (MigrationBlock block : pendingBlocks) {
            String upper = block.sql.toUpperCase();
            Matcher matcher = TABLE_NAME_PATTERN.matcher(upper);
            while (matcher.find()) {
                String tableName = matcher.group(1).toLowerCase();
                // 排除系统表/版本表
                if (!tableName.startsWith("bbs_schema_version")
                        && !tableName.startsWith("pg_")
                        && !tableName.startsWith("information_")) {
                    tables.add(tableName);
                }
            }
        }
        return tables;
    }

    /**
     * 备份受影响的表到新数据库 "原库名_yyyyMMdd_HHmmss"。
     * <p>
     * 流程：
     * 1. 从 DataSource 获取原始 JDBC URL
     * 2. 构建备份数据库名 = 原库名_yyyyMMdd_HHmmss
     * 3. 通过管理连接创建备份数据库
     * 4. 逐表复制（CREATE TABLE ... AS SELECT * FROM ...）
     * </p>
     */
    private void backupAffectedTables(String dbType, Set<String> affectedTables) {
        try {
            DataSource ds = jdbcTemplate.getDataSource();
            if (ds == null) {
                log.warn("DataSource is null, skip backup.");
                return;
            }

            Connection metaConn = ds.getConnection();
            String jdbcUrl = metaConn.getMetaData().getURL();
            metaConn.close();

            String dbName = extractDatabaseName(jdbcUrl);
            if (dbName == null) {
                log.warn("Cannot extract database name from URL, skip backup.");
                return;
            }

            String backupDbName = dbName + "_" + LocalDateTime.now().format(BACKUP_FMT);
            log.info("=== 开始数据库备份 ===");
            log.info("原数据库: {}, 备份数据库: {}, 受影响表数: {}", dbName, backupDbName, affectedTables.size());
            log.info("受影响的表: {}", affectedTables);

            // 1. 创建备份数据库
            createBackupDatabase(dbType, backupDbName);

            // 2. 逐表复制到备份库
            int successCount = 0;
            for (String tableName : affectedTables) {
                try {
                    copyTable(dbType, dbName, backupDbName, tableName);
                    successCount++;
                } catch (Exception e) {
                    log.warn("备份表 [{}] 失败（该表可能尚不存在）: {}", tableName, e.getMessage());
                }
            }

            log.info("=== 数据库备份完成 === 备份库: [{}], 成功备份: {}/{} 张表",
                    backupDbName, successCount, affectedTables.size());

        } catch (Exception e) {
            log.warn("数据库备份失败，将继续执行迁移（建议手动备份）: {}", e.getMessage());
        }
    }

    /**
     * 创建备份数据库。使用原始 JDBC 连接执行 CREATE DATABASE，
     * 因为 PostgreSQL 的 CREATE DATABASE 不能在事务中执行。
     */
    private void createBackupDatabase(String dbType, String backupDbName) throws Exception {
        DataSource ds = jdbcTemplate.getDataSource();
        Connection metaConn = ds.getConnection();
        DatabaseMetaData metaData = metaConn.getMetaData();
        String originalUrl = metaData.getURL();
        metaConn.close();

        String username = dbUsername;
        String password = dbPassword;

        String adminUrl;
        if ("postgresql".equals(dbType)) {
            adminUrl = originalUrl.replaceFirst("/[^/?]+(\\?|$)", "/postgres$1");
            // PostgreSQL: CREATE DATABASE 不能在事务中，必须用独立连接且 autoCommit=true
            try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
                conn.setAutoCommit(true);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE DATABASE \"" + backupDbName + "\"");
                }
            }
        } else {
            adminUrl = originalUrl.replaceFirst("/[^/?]+(\\?|$)", "/information_schema$1");
            try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE DATABASE `" + backupDbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
                }
            }
        }
        log.info("备份数据库 [{}] 创建成功。", backupDbName);
    }

    /**
     * 将指定表从原库复制到备份库（结构 + 数据）。
     * <p>
     * MySQL:    CREATE TABLE `backup_db`.`table` AS SELECT * FROM `original_db`.`table`<br>
     * PostgreSQL: CREATE TABLE "backup_db"."public"."table" AS SELECT * FROM "original_db"."public"."table"
     * </p>
     */
    private void copyTable(String dbType, String originalDb, String backupDb, String tableName) {
        String sql;
        if ("postgresql".equals(dbType)) {
            sql = "CREATE TABLE \"" + backupDb + "\".public.\"" + tableName + "\"" +
                    " AS SELECT * FROM \"" + originalDb + "\".public.\"" + tableName + "\"";
        } else {
            sql = "CREATE TABLE `" + backupDb + "`.`" + tableName + "`" +
                    " AS SELECT * FROM `" + originalDb + "`.`" + tableName + "`";
        }
        jdbcTemplate.execute(sql);
        log.info("  备份表 [{}] 完成。", tableName);
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
