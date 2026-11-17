package com.walker.config;

import com.walker.config.security.AccountRole;
import com.walker.config.security.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
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

    /** 最多保留的备份 schema 数量（PostgreSQL）/ 备份数量（MySQL） */
    private static final int MAX_BACKUP_COUNT = 3;

    /** 自动备份名的后缀：yyyyMMdd_HHmmss（用于区分手工创建的其它库/schema） */
    private static final Pattern BACKUP_SUFFIX_PATTERN = Pattern.compile("^\\d{8}_\\d{6}$");

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
        // 注意：不再调用 baselineExistingMigrations。
        // 旧逻辑假设"表存在 = 所有旧迁移已执行"，但 init SQL 用 CREATE TABLE IF NOT EXISTS
        // 不会为旧表补列，导致 upgrade 迁移被错误跳过、新列缺失。
        // 所有迁移均设计为幂等（PG: IF NOT EXISTS / ON CONFLICT DO NOTHING;
        // MySQL: information_schema 检查 + 捕获 Duplicate column），可安全重复执行。
        executePendingMigrations(dbType);
        // 权限组改为“直接保存权限点”后，把旧的角色基线一次性物化成权限点行。
        materializeLegacyRoleGroups(dbType);
    }

    /**
     * 兼容 v042/v043 的旧数据：把 base_role 的角色基线物化为权限组自身的权限点，
     * 然后清空 base_role。执行后权限组不再区分“系统角色/临时权限组”。
     * 幂等：没有 base_role 的库直接跳过。
     */
    private void materializeLegacyRoleGroups(String dbType) {
        try {
            List<Map<String, Object>> groups = jdbcTemplate.queryForList(
                    "SELECT id, base_role FROM bbs_permission_group "
                            + "WHERE base_role IS NOT NULL AND base_role <> ''");
            String now = LocalDateTime.now().format(FMT);
            for (Map<String, Object> row : groups) {
                Integer groupId = ((Number) row.get("id")).intValue();
                String baseRole = String.valueOf(row.get("base_role")).trim().toUpperCase();
                AccountRole role;
                try {
                    role = AccountRole.valueOf(baseRole);
                } catch (IllegalArgumentException e) {
                    log.warn("Skip unknown base_role={} for permission group {}", baseRole, groupId);
                    continue;
                }

                Set<String> revokes = new HashSet<>();
                List<Map<String, Object>> revokeRows = jdbcTemplate.queryForList(
                        "SELECT permission_code FROM bbs_permission_group_item "
                                + "WHERE group_id = ? AND effect = 'revoke'", groupId);
                for (Map<String, Object> revoke : revokeRows) {
                    revokes.add(String.valueOf(revoke.get("permission_code")));
                }

                for (String code : Permission.codesFor(role)) {
                    if (revokes.contains(code)) {
                        continue;
                    }
                    if ("postgresql".equals(dbType)) {
                        jdbcTemplate.update(
                                "INSERT INTO bbs_permission_group_item "
                                        + "(group_id, permission_code, effect, create_time) "
                                        + "VALUES (?, ?, 'grant', ?) "
                                        + "ON CONFLICT (group_id, permission_code) DO NOTHING",
                                groupId, code, now);
                    } else {
                        jdbcTemplate.update(
                                "INSERT IGNORE INTO bbs_permission_group_item "
                                        + "(group_id, permission_code, effect, create_time) "
                                        + "VALUES (?, ?, 'grant', ?)",
                                groupId, code, now);
                    }
                }
                // 收回行已经体现在“未授予”里，清掉后可避免后续误用。
                jdbcTemplate.update(
                        "DELETE FROM bbs_permission_group_item "
                                + "WHERE group_id = ? AND effect = 'revoke'", groupId);

                String dataScope = role == AccountRole.MODERATOR ? "BOARD_MODERATOR" : "GLOBAL";
                jdbcTemplate.update(
                        "UPDATE bbs_permission_group SET base_role = NULL, data_scope_type = ? "
                                + "WHERE id = ?", dataScope, groupId);
                log.info("Materialized legacy role group {} ({}) into direct permissions", groupId, baseRole);
            }

            // 旧枚举名 ROLE_DEFAULT 统一改为 GLOBAL，前端只展示“全局/按版主任职”。
            jdbcTemplate.update(
                    "UPDATE bbs_permission_group SET data_scope_type = 'GLOBAL' "
                            + "WHERE data_scope_type = 'ROLE_DEFAULT'");

            // 没有主组的用户回填默认普通用户组。
            List<Map<String, Object>> defaultGroups = jdbcTemplate.queryForList(
                    "SELECT id FROM bbs_permission_group WHERE group_code = 'role_user' LIMIT 1");
            if (!defaultGroups.isEmpty()) {
                Integer defaultGroupId = ((Number) defaultGroups.get(0).get("id")).intValue();
                jdbcTemplate.update(
                        "UPDATE bbs_user SET primary_group_id = ? WHERE primary_group_id IS NULL",
                        defaultGroupId);
            }
        } catch (Exception e) {
            log.warn("Failed to materialize legacy role groups: {}", e.getMessage());
        }
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
            int failedBlocks = 0;

            for (MigrationBlock block : blocks) {
                if (applied.contains(block.version)) {
                    log.debug("Skip applied migration: {}", block.version);
                    continue;
                }

                log.info("Executing migration: {} - {}", block.version, block.description);
                try {
                    // 一个迁移块内的所有语句固定走同一条连接执行：
                    // MySQL 的条件 DDL 依赖会话变量（SET @x / PREPARE / EXECUTE），
                    // 若语句分散到连接池的不同连接上，@x 会是 NULL，导致列加不上。
                    List<String> statements = splitSqlStatements(block.sql);
                    boolean hasDdlError = executeStatementsOnOneConnection(statements, block.version);
                    if (hasDdlError) {
                        failedBlocks++;
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
            } else if (failedBlocks == 0) {
                log.info("Database is up to date. No pending migrations.");
            }
            if (failedBlocks > 0) {
                log.error("!!! {} migration block(s) FAILED and were NOT recorded. "
                        + "Schema may be incomplete — fix the SQL and restart before serving traffic.", failedBlocks);
            }
        } catch (Exception e) {
            log.warn("Failed to execute migrations: {}", e.getMessage());
        }
    }

    /**
     * 在一个连接内执行迁移块的所有语句。
     * <p>
     * MySQL 的条件 DDL 依赖会话变量（SET @x := ...; PREPARE s FROM @x; EXECUTE s;），
     * 会话变量是连接级的；若逐条走 JdbcTemplate，连接池可能换连接导致 @x 为 NULL。
     * 这里显式拿一条连接，保证整块语句共享同一会话。
     * </p>
     *
     * @return true 表示块内存在不可容忍的错误，该块不应记录为已执行
     */
    private boolean executeStatementsOnOneConnection(List<String> statements, String version) {
        Boolean failed = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            boolean hasDdlError = false;
            try (Statement statement = connection.createStatement()) {
                for (String raw : statements) {
                    String trimmed = raw.trim();
                    if (trimmed.isEmpty()) continue;
                    try {
                        statement.execute(trimmed);
                    } catch (Exception e) {
                        if (isTolerableFailure(trimmed, e)) {
                            log.info("Migration [{}] statement skipped (idempotent): {} — {}",
                                    version, preview(trimmed), e.getMessage());
                        } else {
                            log.error("Migration DDL/DML FAILED [{}]: {} — {}",
                                    version, preview(trimmed), e.getMessage());
                            hasDdlError = true;
                        }
                    }
                }
            }
            return hasDdlError;
        });
        return Boolean.TRUE.equals(failed);
    }

    /**
     * 去掉语句开头的注释（行注释或块注释），用于识别语句类型。
     */
    static String stripLeadingComments(String sql) {
        String s = sql == null ? "" : sql;
        while (true) {
            s = s.replaceFirst("^\\s+", "");
            if (s.startsWith("--")) {
                int nl = s.indexOf('\n');
                if (nl < 0) return "";
                s = s.substring(nl + 1);
                continue;
            }
            if (s.startsWith("/*")) {
                int end = s.indexOf("*/");
                if (end < 0) return "";
                s = s.substring(end + 2);
                continue;
            }
            return s;
        }
    }

    /**
     * 幂等迁移里可以安全忽略的失败：
     * 重复列/重复索引、DROP 不存在的对象、MySQL 会话变量与 PREPARE 占位语句等。
     */
    private boolean isTolerableFailure(String statement, Exception e) {
        // 迁移块里常用 "-- 说明" 注释打头；startsWith 判断前必须先剥掉前导注释，
        // 否则 "ALTER TABLE ... Duplicate column" 会被误判为致命错误（v012/v016 就踩过）。
        String upper = stripLeadingComments(statement).toUpperCase();
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (upper.startsWith("SET ") || upper.startsWith("SELECT SETVAL")) {
            return true;
        }
        if (upper.startsWith("SELECT 1") || upper.startsWith("PREPARE ") || upper.startsWith("DEALLOCATE ")) {
            return true;
        }
        if (upper.startsWith("DROP ") && (message.contains("doesn't exist") || message.contains("does not exist"))) {
            return true;
        }
        if (upper.startsWith("ALTER TABLE") && (message.contains("Duplicate column")
                || message.contains("already exists")
                || message.contains("Unknown column")
                || message.contains("doesn't exist")
                || message.contains("does not exist"))) {
            return true;
        }
        return false;
    }

    // ==================== SQL 解析 ====================

    /**
     * 按分号拆分 SQL 语句，但忽略单引号字符串字面量内的分号。
     * <p>
     * 敏感词等数据可能包含分号字符（如 HTML 实体 {@code &#8226;}），
     * 若用朴素 {@code split(";")} 会把字符串内的分号误当语句分隔符，
     * 导致 INSERT 语句被拆断、字符串未闭合而执行失败。
     * 本方法支持 {@code ''} 与 {@code \'} 两种单引号转义。
     * </p>
     *
     * @param sql 一个迁移块的完整 SQL（可含多语句）
     * @return 拆分后的非空语句列表（含原顺序）
     */
    static List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inSingleQuote) {
                current.append(c);
                if (c == '\\' && i + 1 < sql.length()) {
                    // 反斜杠转义：跳过下一个字符
                    current.append(sql.charAt(++i));
                } else if (c == '\'') {
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                        // '' 转义：跳过下一个单引号
                        current.append(sql.charAt(++i));
                    } else {
                        inSingleQuote = false;
                    }
                }
            } else if (c == '\'') {
                inSingleQuote = true;
                current.append(c);
            } else if (c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String tail = current.toString().trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

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
     * 备份受影响的表到备份位置。
     * <p>
     * MySQL：创建新数据库 "原库名_yyyyMMdd_HHmmss"，跨库复制表。<br>
     * PostgreSQL：创建同库内新 schema "bbs_backup_yyyyMMdd_HHmmss"，跨 schema 复制表
     * （PostgreSQL 不支持跨数据库引用）。
     * </p>
     * <p>
     * 流程：
     * 1. 从 DataSource 获取原始 JDBC URL
     * 2. 构建备份标识（MySQL=数据库名, PG=schema名）
     * 3. 创建备份数据库/schema
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

            String backupName;
            if ("postgresql".equals(dbType)) {
                // PostgreSQL：在同一数据库内创建 schema，避免跨库引用
                backupName = "bbs_backup_" + LocalDateTime.now().format(BACKUP_FMT);
            } else {
                // MySQL：创建新数据库（MySQL 支持跨库引用）
                backupName = dbName + "_" + LocalDateTime.now().format(BACKUP_FMT);
            }
            log.info("=== 开始数据库备份 ===");
            log.info("原数据库: {}, 备份目标: {}, 受影响表数: {}", dbName, backupName, affectedTables.size());
            log.info("受影响的表: {}", affectedTables);

            // 0. 清理旧备份，避免容量无限增长
            cleanupOldBackups(dbType);

            // 1. 创建备份数据库/schema
            createBackupDatabase(dbType, backupName);

            // 2. 逐表复制到备份位置
            int successCount = 0;
            for (String tableName : affectedTables) {
                try {
                    copyTable(dbType, dbName, backupName, tableName);
                    successCount++;
                } catch (Exception e) {
                    log.warn("备份表 [{}] 失败（该表可能尚不存在）: {}", tableName, e.getMessage());
                }
            }

            log.info("=== 数据库备份完成 === 备份目标: [{}], 成功备份: {}/{} 张表",
                    backupName, successCount, affectedTables.size());

        } catch (Exception e) {
            log.warn("数据库备份失败，将继续执行迁移（建议手动备份）: {}", e.getMessage());
        }
    }

    /**
     * 清理旧备份，保留最新的 {@link #MAX_BACKUP_COUNT} 个。
     * <p>
     * PostgreSQL：列出所有 bbs_backup_* schema 并 DROP 掉最旧的。<br>
     * MySQL：列出所有 原库名_* 数据库并 DROP 掉最旧的。
     * </p>
     */
    private void cleanupOldBackups(String dbType) {
        try {
            DataSource ds = jdbcTemplate.getDataSource();
            if (ds == null) return;

            Connection metaConn = ds.getConnection();
            String jdbcUrl = metaConn.getMetaData().getURL();
            metaConn.close();

            String dbName = extractDatabaseName(jdbcUrl);
            if (dbName == null) return;

            List<String> backupNames = new ArrayList<>();
            if ("postgresql".equals(dbType)) {
                // 查询所有 bbs_backup_* schema，只保留严格时间戳命名的（避免误删同名前缀的业务 schema）
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT schema_name FROM information_schema.schemata " +
                    "WHERE schema_name LIKE 'bbs_backup_%' ORDER BY schema_name");
                for (Map<String, Object> row : rows) {
                    String name = (String) row.get("schema_name");
                    if (isBackupName(name, "bbs_backup_")) {
                        backupNames.add(name);
                    }
                }
            } else {
                // 查询所有 原库名_* 数据库，只保留 "<库名>_yyyyMMdd_HHmmss" 这种由本程序创建的备份
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA " +
                    "WHERE SCHEMA_NAME LIKE '" + dbName + "_%' ORDER BY SCHEMA_NAME");
                for (Map<String, Object> row : rows) {
                    String name = (String) row.get("SCHEMA_NAME");
                    if (isBackupName(name, dbName + "_")) {
                        backupNames.add(name);
                    }
                }
            }

            if (backupNames.size() <= MAX_BACKUP_COUNT) {
                if (!backupNames.isEmpty()) {
                    log.info("当前备份数: {}，保留上限: {}，无需清理。", backupNames.size(), MAX_BACKUP_COUNT);
                }
                return;
            }

            // 删除超出限制的旧备份（schema 名含时间戳，字母排序即时间排序）
            int toRemove = backupNames.size() - MAX_BACKUP_COUNT;
            for (int i = 0; i < toRemove; i++) {
                String oldBackup = backupNames.get(i);
                try {
                    if ("postgresql".equals(dbType)) {
                        // PostgreSQL：先删除 schema 内所有表，再删除 schema
                        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                            "SELECT tablename FROM pg_tables WHERE schemaname = ?", oldBackup);
                        for (Map<String, Object> t : tables) {
                            String tbl = (String) t.get("tablename");
                            jdbcTemplate.execute("DROP TABLE IF EXISTS \"" + oldBackup + "\".\"" + tbl + "\"");
                        }
                        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + oldBackup + "\"");
                    } else {
                        // MySQL：删除整个数据库
                        jdbcTemplate.execute("DROP DATABASE `" + oldBackup + "`");
                    }
                    log.info("已清理旧备份: {}", oldBackup);
                } catch (Exception e) {
                    log.warn("清理旧备份 [{}] 失败: {}", oldBackup, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("清理旧备份时出错（不影响迁移）: {}", e.getMessage());
        }
    }

    /**
     * 是否为程序自动创建的备份名：前缀 + yyyyMMdd_HHmmss。
     * 例如 bbs_db_20261117_114359 / bbs_backup_20261117_114359。
     */
    private boolean isBackupName(String name, String prefix) {
        if (name == null || prefix == null || !name.startsWith(prefix)) return false;
        String suffix = name.substring(prefix.length());
        return BACKUP_SUFFIX_PATTERN.matcher(suffix).matches();
    }

    /**
     * 创建备份目标。
     * <p>
     * MySQL：创建新数据库（使用 information_schema 连接）。<br>
     * PostgreSQL：在当前数据库内创建新 schema（无需跨库，直接用 JdbcTemplate 即可）。
     * </p>
     */
    private void createBackupDatabase(String dbType, String backupName) throws Exception {
        if ("postgresql".equals(dbType)) {
            // PostgreSQL：在同一数据库内创建 schema（不需要跨库）
            // CREATE SCHEMA 可以在事务中执行，直接用 JdbcTemplate
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + backupName + "\"");
        } else {
            // MySQL：创建新数据库
            DataSource ds = jdbcTemplate.getDataSource();
            Connection metaConn = ds.getConnection();
            DatabaseMetaData metaData = metaConn.getMetaData();
            String originalUrl = metaData.getURL();
            metaConn.close();

            String username = dbUsername;
            String password = dbPassword;

            String adminUrl = originalUrl.replaceFirst("/[^/?]+(\\?|$)", "/information_schema$1");
            try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE DATABASE `" + backupName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
                }
            }
        }
        log.info("备份目标 [{}] 创建成功。", backupName);
    }

    /**
     * 将指定表从原库复制到备份位置（结构 + 数据）。
     * <p>
     * MySQL:    CREATE TABLE `backup_db`.`table` AS SELECT * FROM `original_db`.`table`<br>
     * PostgreSQL: CREATE TABLE "backup_schema"."table" AS SELECT * FROM "public"."table"
     * </p>
     * <p>
     * PostgreSQL 使用同一数据库内的 schema（而非跨库），避免
     * "cross-database references are not implemented" 错误。
     * </p>
     */
    private void copyTable(String dbType, String originalDb, String backupName, String tableName) {
        String sql;
        if ("postgresql".equals(dbType)) {
            // PostgreSQL：同库跨 schema（backupName 是 schema 名，不是数据库名）
            sql = "CREATE TABLE \"" + backupName + "\".\"" + tableName + "\"" +
                    " AS SELECT * FROM \"public\".\"" + tableName + "\"";
        } else {
            // MySQL：跨数据库
            sql = "CREATE TABLE `" + backupName + "`.`" + tableName + "`" +
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
