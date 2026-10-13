package com.walker.controller;

import com.walker.pojo.Article;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.User;
import com.walker.service.ArticleLabelService;
import com.walker.service.ArticleService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 2026-10-12 需求：采纳建议改为登录态身份校验 + 自采纳限制超管豁免。
 * 不启动 Spring，直接实例化 Controller，mock 依赖注入后调用 adoptSuggestion。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleControllerTest {

    @InjectMocks
    private ArticleController articleController;

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleLabelService articleLabelService;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // 默认当前登录管理员 id=1（超管），敏感用例可覆盖
        setCurrentUser(1);
    }

    @AfterEach
    void tearDownAuth() {
        SecurityContextHolder.clearContext();
    }

    /** 模拟 JWT 登录态：principal 为 User 实体 */
    private void setCurrentUser(int id) {
        User user = new User();
        user.setId(id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    /** 构造"建议反馈"用途的帖子 */
    private Article suggestionArticle(int articleId, int authorId) {
        Article article = new Article();
        article.setArticleId(articleId);
        article.setUserId(authorId);
        article.setArticleLabelId(10);
        article.setArticleTitle("测试建议");
        return article;
    }

    private Map<String, Object> params(Integer articleId, Integer operatorId) {
        Map<String, Object> p = new HashMap<>();
        p.put("articleId", articleId);
        p.put("operatorId", operatorId);
        return p;
    }

    @Test
    @DisplayName("采纳建议 → 无登录态 → 拒绝")
    void adoptSuggestion_noAuth_returnsError() {
        SecurityContextHolder.clearContext();
        ResultBean result = articleController.adoptSuggestion(params(1, 1));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("登录"));
        verify(pointsLogService, never()).adjustUserPoints(any(), anyInt(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("采纳建议 → 前端伪造操作人 id ≠ 登录态 → 拒绝")
    void adoptSuggestion_forgedOperator_returnsError() {
        // 登录态为 1，却传 operatorId=2（伪造他人）→ 身份校验先于业务处理拒绝
        ResultBean result = articleController.adoptSuggestion(params(1, 2));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("身份"));
        verify(articleService, never()).queryArticleById(any());
    }

    @Test
    @DisplayName("采纳建议 → 普通管理员采纳自己的建议 → 拒绝")
    void adoptSuggestion_selfAdopt_normalAdmin_returnsError() {
        setCurrentUser(5);
        when(articleService.queryArticleById(1)).thenReturn(suggestionArticle(1, 5));
        when(articleLabelService.getById(10)).thenReturn(label("suggestion"));

        ResultBean result = articleController.adoptSuggestion(params(1, 5));
        assertEquals(500, result.getCode());
        assertEquals("不能采纳自己的建议", result.getMessage());
        verify(pointsLogService, never()).adjustUserPoints(any(), anyInt(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("采纳建议 → 超管采纳自己的建议 → 放行并加分")
    void adoptSuggestion_selfAdopt_superAdmin_allowed() {
        // 登录态默认 1（超管），文章作者也是 1 → 超管豁免自采纳拦截
        when(articleService.queryArticleById(1)).thenReturn(suggestionArticle(1, 1));
        when(articleLabelService.getById(10)).thenReturn(label("suggestion"));
        when(pointsLogService.countSuggestionAdoptForArticle(1)).thenReturn(0);

        ResultBean result = articleController.adoptSuggestion(params(1, 1));
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(5), contains("建议被采纳"), eq("article"), eq(1), eq(1));
        verify(notificationService).createNotification(eq(1), eq(1), eq("suggestion_adopted"), contains("建议被采纳"), eq("article"), eq(1));
    }

    @Test
    @DisplayName("采纳建议 → 正常采纳他人建议 → 成功")
    void adoptSuggestion_valid_succeeds() {
        when(articleService.queryArticleById(1)).thenReturn(suggestionArticle(1, 2));
        when(articleLabelService.getById(10)).thenReturn(label("suggestion"));
        when(pointsLogService.countSuggestionAdoptForArticle(1)).thenReturn(0);

        ResultBean result = articleController.adoptSuggestion(params(1, 1));
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(2), eq(5), contains("建议被采纳"), eq("article"), eq(1), eq(1));
        verify(notificationService).createNotification(eq(2), eq(1), eq("suggestion_adopted"), contains("建议被采纳"), eq("article"), eq(1));
    }

    @Test
    @DisplayName("采纳建议 → 非建议反馈标签 → 拒绝")
    void adoptSuggestion_notSuggestionLabel_returnsError() {
        when(articleService.queryArticleById(1)).thenReturn(suggestionArticle(1, 2));
        when(articleLabelService.getById(10)).thenReturn(label("normal"));

        ResultBean result = articleController.adoptSuggestion(params(1, 1));
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("建议反馈"));
        verify(pointsLogService, never()).adjustUserPoints(any(), anyInt(), anyString(), any(), any(), any());
    }

    private ArticleLabel label(String type) {
        ArticleLabel label = new ArticleLabel();
        label.setLabelId(10);
        label.setLabelType(type);
        return label;
    }
}
