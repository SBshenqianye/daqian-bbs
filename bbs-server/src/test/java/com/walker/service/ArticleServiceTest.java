package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.User;
import com.walker.service.impl.ArticleServiceImpl;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

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
}
