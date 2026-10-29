package com.walker.controller;

import com.walker.pojo.Article;
import com.walker.service.ArticleService;
import com.walker.service.AuthorizationService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.ReplyService;
import com.walker.service.SecurityTestSupport;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 采纳审批数据范围测试（D4）。
 */
@ExtendWith(MockitoExtension.class)
class ReplyControllerTest {

    @InjectMocks
    private ReplyController replyController;

    @Mock
    private ReplyService replyService;

    @Mock
    private ArticleService articleService;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        SecurityTestSupport.injectCurrentUser(replyController);
        SecurityTestSupport.injectSelfApprovalPolicy(replyController, authorizationService);
        SecurityTestSupport.setCurrentUser(5);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Map<String, Object> approveParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("replyId", 1);
        params.put("articleId", 1);
        params.put("action", "confirm");
        params.put("adminId", 5);
        return params;
    }

    @Test
    @DisplayName("版主跨板块审批采纳 → 403 拒绝")
    void approveAdopt_crossBoard_forbidden() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(100);
        article.setArticleLabelId(10);
        when(articleService.queryArticleById(1)).thenReturn(article);
        when(authorizationService.canModerateBoard(5, 10)).thenReturn(false);

        ResultBean result = replyController.approveAdopt(approveParams());

        assertEquals(403, result.getCode());
        assertEquals("无权审批该板块内容", result.getMessage());
        verify(replyService, never()).updateById(any());
        verify(pointsLogService, never()).adjustUserPoints(any(), anyInt(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("版主审批本板块采纳 → 放行到业务层")
    void approveAdopt_ownBoard_allowed() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(100);
        article.setArticleLabelId(10);
        when(articleService.queryArticleById(1)).thenReturn(article);
        when(authorizationService.canModerateBoard(5, 10)).thenReturn(true);
        when(replyService.getById(1)).thenReturn(null);

        ResultBean result = replyController.approveAdopt(approveParams());

        // 业务层继续执行后返回“回复不存在”，说明未被权限拦截
        assertEquals(500, result.getCode());
        assertEquals("回复不存在", result.getMessage());
    }
}
