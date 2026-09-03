package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleFile;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.User;
import com.walker.service.impl.ArticleServiceImpl;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleUserService articleUserService;

    @Mock
    private UserService userService;

    @Mock
    private DictService dictService;

    @Mock
    private ArticleFileService articleFileService;

    @Mock
    private ArticleLabelService articleLabelService;

    @Mock
    private SaOrgMapper saOrgMapper;

    @Mock
    private SaOrgService saOrgService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private PointsLogService pointsLogService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = articleService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(articleService, articleMapper);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Article.class);
    }

    @Test
    @DisplayName("发帖 → 标题为空 → 返回错误")
    void publish_emptyTitle_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("");
        param.setArticleContent("内容");

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
        assertEquals("标题不能为空", result.getMessage());
    }

    @Test
    @DisplayName("发帖 → 内容为空 → 返回错误")
    void publish_emptyContent_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题");
        param.setArticleContent("");

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
        assertEquals("内容不能为空", result.getMessage());
    }

    @Test
    @DisplayName("发帖 → 标题含HTML标签 → 返回错误")
    void publish_htmlTitle_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题<b>粗体</b>");
        param.setArticleContent("内容");

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("发帖 → 标签已被禁用 → 返回错误")
    void publish_disabledLabel_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题");
        param.setArticleContent("内容");
        param.setArticleLabelId(1);

        ArticleLabel label = new ArticleLabel();
        label.setEnabled(0);
        when(articleLabelService.getById(1)).thenReturn(label);

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("发帖 → 账号被限制发帖 → 返回错误")
    void publish_restrictedUser_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题");
        param.setArticleContent("内容");
        param.setUserId(1);

        User user = new User();
        user.setId(1);
        user.setPostRestricted(1);
        user.setPostRestrictedUntil("2099-12-31 23:59:59");
        when(userService.getById(1)).thenReturn(user);

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("发帖 → 正常发布成功 → 保存并加分")
    void publish_validArticle_succeeds() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("正常标题");
        param.setArticleContent("正常内容");
        param.setUserId(1);
        param.setArticleAuthor("作者");

        when(userService.getById(1)).thenReturn(null);
        when(dictService.getValueByKey("post")).thenReturn("3");
        when(articleMapper.insert(any(Article.class))).thenReturn(1);

        ResultBean result = articleService.publish(param);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(3), eq("发帖积分"), eq("article"), any(), isNull());
    }

    @Test
    @DisplayName("编辑文章 → 标题为空 → 返回错误")
    void editArticle_emptyTitle_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("");
        param.setArticleContent("内容");

        ResultBean result = articleService.editArticle(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("删除文章 → 已审核通过 → 扣回发帖积分")
    void deleteArticle_enabledArticle_deductsPoints() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setEnable(1);
        article.setIsFeatured(0);
        when(articleMapper.selectById(1)).thenReturn(article);
        when(dictService.getValueByKey("post")).thenReturn("3");
        when(articleMapper.deleteById(1)).thenReturn(1);

        ResultBean result = articleService.deleteArticleByArticleId(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-3), contains("删除帖子扣回积分"), eq("article"), eq(1), isNull());
    }

    @Test
    @DisplayName("删除文章 → 精华帖 → 额外扣回精华加分")
    void deleteArticle_featuredArticle_deductsFeaturedPoints() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setEnable(1);
        article.setIsFeatured(1);
        when(articleMapper.selectById(1)).thenReturn(article);
        when(dictService.getValueByKey("post")).thenReturn("3");
        when(dictService.getValueByKey("featured")).thenReturn("10");
        when(articleMapper.deleteById(1)).thenReturn(1);

        ResultBean result = articleService.deleteArticleByArticleId(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-10), contains("删除精华帖扣回加分"), eq("article"), eq(1), isNull());
    }

    @Test
    @DisplayName("删除文章 → 未审核通过 → 不扣积分")
    void deleteArticle_disabledArticle_noDeduct() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setEnable(0);
        when(articleMapper.selectById(1)).thenReturn(article);
        when(articleMapper.deleteById(1)).thenReturn(1);

        ResultBean result = articleService.deleteArticleByArticleId(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("设置精华 → 文章不存在 → 返回错误")
    void setFeatured_articleNotFound_returnsError() {
        when(articleMapper.selectById(999)).thenReturn(null);

        ResultBean result = articleService.setFeatured(999, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("设置精华 → 从非精华设为精华 → 加分")
    void setFeatured_setFeatured_addsPoints() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setIsFeatured(0);
        when(articleMapper.selectById(1)).thenReturn(article);
        when(dictService.getValueByKey("featured")).thenReturn("10");
        when(articleMapper.updateById(any(Article.class))).thenReturn(1);

        ResultBean result = articleService.setFeatured(1, 1);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(10), eq("精华帖奖励积分"), eq("article"), eq(1), isNull());
    }

    @Test
    @DisplayName("取消精华 → 从精华取消 → 扣回")
    void setFeatured_removeFeatured_deductsPoints() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setIsFeatured(1);
        when(articleMapper.selectById(1)).thenReturn(article);
        when(dictService.getValueByKey("featured")).thenReturn("10");
        when(articleMapper.updateById(any(Article.class))).thenReturn(1);

        ResultBean result = articleService.setFeatured(1, 0);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-10), eq("取消精华帖扣回积分"), eq("article"), eq(1), isNull());
    }

    @Test
    @DisplayName("获取文章总数 → 返回计数")
    void getArticleCount_returnsCount() {
        when(articleMapper.selectCount(null)).thenReturn(100L);
        ResultBean result = articleService.getArticleCount();
        assertEquals(200, result.getCode());
        assertEquals(100L, result.getObj());
    }

    @Test
    @DisplayName("批量审核 → 审核所有未通过文章")
    void batchAudit_auditsAllPending() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setEnable(0);
        Article a2 = new Article();
        a2.setArticleId(2);
        a2.setEnable(0);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1, a2));
        when(articleMapper.updateById(any(Article.class))).thenReturn(1);

        articleService.batchAudit();
        verify(articleMapper, times(2)).updateById(any(Article.class));
    }

    // ========== 以下为新增测试 ==========

    @Test
    @DisplayName("查询顶部推荐 → 返回推荐文章列表")
    void queryHeaderRecommend_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setArticleTitle("推荐1");
        Article a2 = new Article();
        a2.setArticleId(2);
        a2.setArticleTitle("推荐2");

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1, a2));

        List<Article> result = articleService.queryHeaderRecommend();
        assertEquals(2, result.size());
        assertEquals("推荐1", result.get(0).getArticleTitle());
        verify(articleMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询推荐文章 → 返回按点赞数排序的列表")
    void queryRecommend_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(10);
        a1.setArticleTitle("热门帖");

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        List<Article> result = articleService.queryRecommend();
        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getArticleId());
    }

    @Test
    @DisplayName("查询最新文章 → 返回按时间排序的列表")
    void queryNewest_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(5);
        Article a2 = new Article();
        a2.setArticleId(6);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1, a2));

        List<Article> result = articleService.queryNewest();
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("查询热榜 → 返回按浏览量排序的列表")
    void queryHot_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setArticleTitle("热帖");

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        List<Article> result = articleService.queryHot();
        assertEquals(1, result.size());
        assertEquals("热帖", result.get(0).getArticleTitle());
    }

    @Test
    @DisplayName("查询所有文章列表 → 关键词为空 → 返回全部已审核文章")
    void queryAllArticleList_emptyKeywords_returnsAll() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setUserId(1);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>(Arrays.asList(a1)));
        when(userService.listUsersWithOrgInfo(any())).thenReturn(new ArrayList<>());
        when(saOrgService.resolveDisplayOrgNames(any())).thenReturn(new HashMap<>());
        when(commentMapper.countByArticleIds(anyList())).thenReturn(new ArrayList<>());

        List<Article> result = articleService.queryAllArticleList(null);
        assertFalse(result.isEmpty());
        assertEquals(1, result.get(0).getArticleId());
    }

    @Test
    @DisplayName("查询所有文章列表 → 有关键词 → 搜索内容和标题")
    void queryAllArticleList_withKeywords_searchesContent() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setUserId(1);

        when(articleFileService.getArticleFileByKeywords("test")).thenReturn(null);
        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>(Arrays.asList(a1)));
        when(userService.listUsersWithOrgInfo(any())).thenReturn(new ArrayList<>());
        when(saOrgService.resolveDisplayOrgNames(any())).thenReturn(new HashMap<>());
        when(commentMapper.countByArticleIds(anyList())).thenReturn(new ArrayList<>());

        List<Article> result = articleService.queryAllArticleList("test");
        assertFalse(result.isEmpty());
        verify(articleFileService).getArticleFileByKeywords("test");
    }

    @Test
    @DisplayName("按社区ID查询文章 → 返回该社区的文章")
    void queryArticleByCommunityId_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setArticleCommunityId(100);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        List<Article> result = articleService.queryArticleByCommunityId(100);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getArticleCommunityId());
    }

    @Test
    @DisplayName("按关键词搜索文章 → 返回匹配的文章")
    void getArticleByKeywords_returnsMatchingArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setArticleTitle("测试文章");

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        List<Article> result = articleService.getArticleByKeywords("测试");
        assertEquals(1, result.size());
        verify(articleMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("按ID批量查询文章 → 返回对应文章")
    void getArticlesByIds_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        Article a2 = new Article();
        a2.setArticleId(2);

        List<Integer> ids = Arrays.asList(1, 2);
        when(articleMapper.selectBatchIds(ids)).thenReturn(Arrays.asList(a1, a2));

        List<Article> result = articleService.getArticlesByIds(ids);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("管理后台删除文章 → 调用deleteById")
    void adminDeleteArticleByArticleId_deletesArticle() {
        when(articleMapper.deleteById(1)).thenReturn(1);

        ResultBean result = articleService.adminDeleteArticleByArticleId(1);
        assertEquals(200, result.getCode());
        verify(articleMapper).deleteById(1);
    }

    @Test
    @DisplayName("审核文章 → 设置enable=1")
    void auditArticleByArticleId_setsEnable() {
        when(articleMapper.updateById(any(Article.class))).thenReturn(1);

        ResultBean result = articleService.auditArticleByArticleId(5);
        assertEquals(200, result.getCode());
        verify(articleMapper).updateById(argThat(a -> a.getEnable() == 1 && a.getArticleId() == 5));
    }

    @Test
    @DisplayName("批量删除已审核文章 → 删除所有enable=1的文章")
    void handleBatchDeleteArticlesByAlive_deletesAll() {
        when(articleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(5);

        articleService.handleBatchDeleteArticlesByAlive();
        verify(articleMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询已审核文章 → 返回enable=1的文章列表")
    void getAliveArticles_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setEnable(1);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        ResultBean result = articleService.getAliveArticles();
        assertEquals(200, result.getCode());
        assertNotNull(result.getObj());
    }

    @Test
    @DisplayName("查询未审核文章 → 返回enable=0的文章列表")
    void getNotAliveArticles_returnsArticles() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setEnable(0);

        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(a1));

        ResultBean result = articleService.getNotAliveArticles();
        assertEquals(200, result.getCode());
        assertNotNull(result.getObj());
    }

    @Test
    @DisplayName("获取未通过审核文章数 → 返回计数")
    void getArticleCountWithNotPass_returnsCount() {
        when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        ResultBean result = articleService.getArticleCountWithNotPass();
        assertEquals(200, result.getCode());
        assertEquals(5L, result.getObj());
    }

    @Test
    @DisplayName("按文章ID查询 → 返回单篇文章详情")
    void getArticleByArticle_returnsArticle() {
        Article a1 = new Article();
        a1.setArticleId(1);
        a1.setArticleTitle("测试");

        when(articleMapper.selectById(1)).thenReturn(a1);

        ResultBean result = articleService.getArticleByArticle(1);
        assertEquals(200, result.getCode());
        assertNotNull(result.getObj());
    }

    @Test
    @DisplayName("编辑文章 → 标题含HTML标签 → 返回错误")
    void editArticle_htmlTitle_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题<b>test</b>");
        param.setArticleContent("内容");

        ResultBean result = articleService.editArticle(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("编辑文章 → 内容为空 → 返回错误")
    void editArticle_emptyContent_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("正常标题");
        param.setArticleContent("");

        ResultBean result = articleService.editArticle(param);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("发帖 → 永久限制发帖（postRestrictedUntil为null）→ 返回错误")
    void publish_permanentRestriction_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题");
        param.setArticleContent("内容");
        param.setUserId(1);

        User user = new User();
        user.setId(1);
        user.setPostRestricted(1);
        user.setPostRestrictedUntil(null);  // 永久限制
        when(userService.getById(1)).thenReturn(user);

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
        assertEquals("您的账号已被限制发帖，请联系管理员", result.getMessage());
    }

    @Test
    @DisplayName("发帖 → 标签不存在 → 返回错误")
    void publish_labelNotFound_returnsError() {
        ArticleParam param = new ArticleParam();
        param.setArticleTitle("标题");
        param.setArticleContent("内容");
        param.setUserId(1);
        param.setArticleLabelId(999);

        when(userService.getById(1)).thenReturn(null);
        when(articleLabelService.getById(999)).thenReturn(null);

        ResultBean result = articleService.publish(param);
        assertEquals(500, result.getCode());
        assertEquals("所选标签不存在", result.getMessage());
    }
}
