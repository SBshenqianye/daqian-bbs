package com.walker.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link DatabaseInitializer#splitSqlStatements(String)} 的单元测试。
 * <p>
 * 重点回归：敏感词词条可能含分号（如 HTML 实体 "&#8226;"），
 * 拆分必须忽略单引号字符串字面量内的分号，避免 INSERT 被拆断。
 * </p>
 */
class DatabaseInitializerTest {

    @Test
    void splitNormalStatements() {
        String sql = "SELECT 1;\n" +
                "INSERT INTO t (k) VALUES ('a');\n" +
                "UPDATE t SET k = 'b' WHERE k = 'a'";
        assertEquals(
                Arrays.asList("SELECT 1", "INSERT INTO t (k) VALUES ('a')", "UPDATE t SET k = 'b' WHERE k = 'a'"),
                DatabaseInitializer.splitSqlStatements(sql));
    }

    @Test
    void ignoreSemicolonInsideStringLiteral() {
        // 回归：词条 "突破封&#8226;锁" 内的分号不得拆断 INSERT
        String sql = "INSERT IGNORE INTO `bbs_sensitive_word` (`keyword`) VALUES\n" +
                "('凸点装'),\n" +
                "('突破封&#8226;锁'),\n" +
                "('突破网络封锁');";
        List<String> statements = DatabaseInitializer.splitSqlStatements(sql);
        assertEquals(1, statements.size(), "字符串内的分号不应拆分语句");
        String stmt = statements.get(0);
        assertEquals("INSERT IGNORE INTO `bbs_sensitive_word` (`keyword`) VALUES ('凸点装'), ('突破封&#8226;锁'), ('突破网络封锁')",
                stmt.replace("\n", " "));
    }

    @Test
    void multipleSemicolonsInsideStringLiteral() {
        String sql = "INSERT INTO t (k) VALUES ('a;b;c'), ('x;y');\nSELECT 1;";
        List<String> statements = DatabaseInitializer.splitSqlStatements(sql);
        assertEquals(2, statements.size());
        assertEquals("INSERT INTO t (k) VALUES ('a;b;c'), ('x;y')", statements.get(0));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void handleDoubledQuoteEscape() {
        // '' 是 SQL 标准转义的单引号，不影响字符串闭合
        String sql = "INSERT INTO t (k) VALUES ('450''C 的阀门');";
        List<String> statements = DatabaseInitializer.splitSqlStatements(sql);
        assertEquals(1, statements.size());
        assertEquals("INSERT INTO t (k) VALUES ('450''C 的阀门')", statements.get(0));
    }

    @Test
    void handleBackslashQuoteEscape() {
        // \' 是 MySQL 默认转义，其后字符不应影响字符串状态
        String sql = "INSERT INTO t (k) VALUES ('it\\'s ok;done');\nSELECT 2;";
        List<String> statements = DatabaseInitializer.splitSqlStatements(sql);
        assertEquals(2, statements.size());
        assertEquals("INSERT INTO t (k) VALUES ('it\\'s ok;done')", statements.get(0));
        assertEquals("SELECT 2", statements.get(1));
    }

    @Test
    void singleStatementWithoutSemicolon() {
        String sql = "SELECT 1";
        assertEquals(Collections.singletonList("SELECT 1"), DatabaseInitializer.splitSqlStatements(sql));
    }

    @Test
    void emptyAndBlankInput() {
        assertEquals(Collections.emptyList(), DatabaseInitializer.splitSqlStatements(""));
        assertEquals(Collections.emptyList(), DatabaseInitializer.splitSqlStatements("   \n  \t "));
        assertEquals(Collections.emptyList(), DatabaseInitializer.splitSqlStatements(";;;"));
    }

    @Test
    void semicolonAtStatementEndInsideString() {
        // 字符串结尾的分号属于字符串内容；语句末尾的分号才算分隔符
        String sql = "INSERT INTO t (k) VALUES ('a;');";
        List<String> statements = DatabaseInitializer.splitSqlStatements(sql);
        assertEquals(1, statements.size());
        assertEquals("INSERT INTO t (k) VALUES ('a;')", statements.get(0));
    }
}
