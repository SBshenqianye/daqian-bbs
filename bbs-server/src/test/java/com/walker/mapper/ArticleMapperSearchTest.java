package com.walker.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.walker.pojo.Article;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleMapper 搜索 SQL 行为测试（H2 内存库，PostgreSQL 兼容模式）
 *
 * 覆盖：
 * 1. 用户前台搜索可命中人员：作者昵称快照 / 当前昵称 / 账号 / 人员编号；
 * 2. 用户前台搜索排除已删除、未审核帖子；
 * 3. 管理端列表 deleted=0/1 分别查询未删除 / 已删除帖子。
 */
class ArticleMapperSearchTest {

    private static SqlSessionFactory factory;

    @BeforeAll
    static void setUp() throws Exception {
        DataSource dataSource = new UnpooledDataSource(
                "org.h2.Driver",
                "jdbc:h2:mem:article_mapper_search;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "sa",
                "");
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE bbs_article ("
                    + "article_id INT PRIMARY KEY, article_label_id INT, article_author VARCHAR(100),"
                    + "article_title VARCHAR(200), article_summary VARCHAR(500), article_type_id INT,"
                    + "article_content CLOB, article_content_html CLOB, article_image VARCHAR(255),"
                    + "user_id INT, article_good_num INT, article_view_num INT, article_community_id INT,"
                    + "create_time VARCHAR(20), recommend VARCHAR(10), enable INT, is_delete INT, is_featured INT)");
            st.execute("CREATE TABLE bbs_user ("
                    + "id INT PRIMARY KEY, username VARCHAR(50), nickname VARCHAR(50),"
                    + "personnel_id VARCHAR(50), is_delete INT)");
            st.execute("CREATE TABLE bbs_article_label ("
                    + "label_id INT PRIMARY KEY, label_name VARCHAR(50), label_type VARCHAR(20))");

            st.execute("INSERT INTO bbs_user VALUES (1, 'zhangsan', '张三', 'P001', 0)");
            st.execute("INSERT INTO bbs_user VALUES (2, 'lisi', '李四', 'P002', 0)");
            st.execute("INSERT INTO bbs_article_label VALUES (10, '经验分享', 'normal')");

            // 正常帖子（作者昵称快照为张三）
            st.execute("INSERT INTO bbs_article VALUES (1, 10, '张三', '普通标题A', '', 1, '正文A', '', '', 1, 0, 0, 0,"
                    + "'2026-10-01 10:00:00', '0', 1, 0, 0)");
            // 正常帖子（作者李四，正文/标题均不含人名关键词）
            st.execute("INSERT INTO bbs_article VALUES (2, 10, '李四', '普通标题B', '', 1, '正文B', '', '', 2, 0, 0, 0,"
                    + "'2026-10-02 10:00:00', '0', 1, 0, 0)");
            // 已删除帖子（标题关键词可命中，但不应出现在前台搜索结果）
            st.execute("INSERT INTO bbs_article VALUES (3, 10, '张三', '已删标题C', '', 1, '正文C', '', '', 1, 0, 0, 0,"
                    + "'2026-10-03 10:00:00', '0', 1, 1, 0)");
            // 未审核帖子（标题关键词可命中，但不应出现在前台搜索结果）
            st.execute("INSERT INTO bbs_article VALUES (4, 10, '李四', '待审标题D', '', 1, '正文D', '', '', 2, 0, 0, 0,"
                    + "'2026-10-04 10:00:00', '0', 0, 0, 0)");
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("test", new JdbcTransactionFactory(), dataSource));
        configuration.addMapper(ArticleMapper.class);
        try (InputStream in = Resources.getResourceAsStream("mapper/ArticleMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mapper/ArticleMapper.xml", configuration.getSqlFragments()).parse();
        }
        factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @AfterAll
    static void tearDown() {
        factory = null;
    }

    private List<Article> search(String keywords) {
        return search(keywords, new java.util.ArrayList<Integer>());
    }

    private List<Article> search(String keywords, List<Integer> fileArticleIds) {
        Map<String, Object> params = new HashMap<>();
        params.put("keywords", keywords);
        params.put("fileArticleIds", fileArticleIds);
        try (SqlSession session = factory.openSession()) {
            return session.getMapper(ArticleMapper.class).selectArticleListByKeywords(params);
        }
    }

    private List<Article> adminList(int deleted) {
        Map<String, Object> params = new HashMap<>();
        params.put("keywords", "");
        params.put("labelId", null);
        params.put("startTime", "");
        params.put("endTime", "");
        params.put("enable", deleted == 1 ? null : 1);
        params.put("deleted", deleted);
        params.put("offset", 0);
        params.put("size", 10);
        try (SqlSession session = factory.openSession()) {
            return session.getMapper(ArticleMapper.class).selectAdminArticleList(params);
        }
    }

    @Test
    @DisplayName("搜索人名 → 命中作者昵称快照")
    void searchByAuthorNickname_returnsArticle() {
        List<Article> list = search("张三");
        assertEquals(1, list.size());
        assertEquals(Integer.valueOf(1), list.get(0).getArticleId());
    }

    @Test
    @DisplayName("搜索人名 → 命中账号 / 人员编号")
    void searchByUsernameOrPersonnelId_returnsArticle() {
        assertEquals(Integer.valueOf(1), search("zhangsan").get(0).getArticleId());
        assertEquals(Integer.valueOf(2), search("P002").get(0).getArticleId());
    }

    @Test
    @DisplayName("搜索标题/正文仍可用，已删除与未审核帖子被排除")
    void searchByTitleAndContent_excludesDeletedAndPending() {
        // "正文B" 命中正常帖子 2
        assertEquals(1, search("正文B").size());
        assertEquals(Integer.valueOf(2), search("正文B").get(0).getArticleId());
        // 标题命中已删除 / 未审核帖子时不返回
        assertTrue(search("已删标题C").isEmpty());
        assertTrue(search("待审标题D").isEmpty());
    }

    @Test
    @DisplayName("搜索附件名 → 附件命中的已审核帖子参与结果")
    void searchByAttachment_returnsMatchedArticle() {
        List<Article> list = search("完全无关的词", java.util.Arrays.asList(2));
        assertEquals(1, list.size());
        assertEquals(Integer.valueOf(2), list.get(0).getArticleId());
    }

    @Test
    @DisplayName("管理端列表 → deleted=0 查未删除，deleted=1 查已删除")
    void adminList_splitsDeletedAndNotDeleted() {
        List<Article> notDeleted = adminList(0);
        assertEquals(2, notDeleted.size());
        assertTrue(notDeleted.stream().allMatch(a -> Integer.valueOf(0).equals(a.getIsDelete())));

        List<Article> deleted = adminList(1);
        assertEquals(1, deleted.size());
        assertEquals(Integer.valueOf(3), deleted.get(0).getArticleId());
        assertEquals(Integer.valueOf(1), deleted.get(0).getIsDelete());
        assertEquals("经验分享", deleted.get(0).getArticleLabelName());
    }

    @Test
    @DisplayName("管理端列表 → 已删除总数统计")
    void countDeletedArticles_returnsDeletedCount() {
        try (SqlSession session = factory.openSession()) {
            assertEquals(1L, session.getMapper(ArticleMapper.class).countDeletedArticles());
        }
    }
}
