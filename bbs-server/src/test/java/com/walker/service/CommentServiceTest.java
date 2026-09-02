package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.CommentMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.service.impl.CommentServiceImpl;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommentParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentServiceImpl commentService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ArticleService articleService;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private DictService dictService;

    @Mock
    private ReplyService replyService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = commentService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(commentService, commentMapper);
    }

    @Test
    @DisplayName("评论成功 → 保存记录")
    void saveUserComment_validComment_succeeds() {
        CommentParam param = new CommentParam();
        param.setCommentContent("好文章！");
        param.setCommentUserId(1);
        param.setCommentArticleId(1);

        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(2);
        when(articleService.getById(1)).thenReturn(article);
        when(dictService.getValueByKey("reply")).thenReturn("1");
        when(pointsLogService.countReplyPointsForArticle(1, 1)).thenReturn(0);
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        ResultBean result = commentService.saveUserComment(param);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("评论 → 自己帖子内评论不计积分")
    void saveUserComment_ownArticle_noPoints() {
        CommentParam param = new CommentParam();
        param.setCommentContent("自评");
        param.setCommentUserId(1);
        param.setCommentArticleId(1);

        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        when(articleService.getById(1)).thenReturn(article);
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        ResultBean result = commentService.saveUserComment(param);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), eq("评论积分"), anyString(), anyInt(), isNull());
    }

    @Test
    @DisplayName("评论 → 已达3次积分上限不再加分")
    void saveUserComment_maxPoints_noMorePoints() {
        CommentParam param = new CommentParam();
        param.setCommentContent("第4条评论");
        param.setCommentUserId(1);
        param.setCommentArticleId(1);

        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(2);
        when(articleService.getById(1)).thenReturn(article);
        when(pointsLogService.countReplyPointsForArticle(1, 1)).thenReturn(3);
        when(commentMapper.insert(any(Comment.class))).thenReturn(1);

        ResultBean result = commentService.saveUserComment(param);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), eq("评论积分"), anyString(), anyInt(), isNull());
    }

    @Test
    @DisplayName("删除评论 → 已审核通过 → 扣回积分")
    void deleteComment_enabledComment_deductsPoints() {
        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setCommentUserId(1);
        comment.setEnable(1);
        when(commentMapper.selectById(1)).thenReturn(comment);
        when(dictService.getValueByKey("reply")).thenReturn("1");
        when(commentMapper.deleteById(1)).thenReturn(1);

        ResultBean result = commentService.deleteCommentById(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-1), contains("删除评论扣回积分"), eq("comment"), eq(1), isNull());
    }

    @Test
    @DisplayName("删除评论 → 未审核通过 → 不扣积分")
    void deleteComment_disabledComment_noDeduct() {
        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setCommentUserId(1);
        comment.setEnable(0);
        when(commentMapper.selectById(1)).thenReturn(comment);
        when(commentMapper.deleteById(1)).thenReturn(1);

        ResultBean result = commentService.deleteCommentById(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("热度奖励 → 互动数未达阈值 → 不发奖励")
    void checkHotBonus_belowThreshold_noReward() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setIsHotBonus(0);
        when(articleService.queryArticleById(1)).thenReturn(article);
        when(dictService.getValueByKey("hot_threshold")).thenReturn("10");
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        commentService.checkHotBonus(1);
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), anyString(), anyString(), anyInt(), any());
    }

    @Test
    @DisplayName("热度奖励 → 已发过 → 不重复发")
    void checkHotBonus_alreadyIssued_noReward() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setIsHotBonus(1);
        when(articleService.queryArticleById(1)).thenReturn(article);

        commentService.checkHotBonus(1);
        verify(commentMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("热度奖励 → 互动数达阈值 → 发奖励并通知")
    void checkHotBonus_reachedThreshold_rewards() {
        Article article = new Article();
        article.setArticleId(1);
        article.setUserId(1);
        article.setIsHotBonus(0);
        when(articleService.queryArticleById(1)).thenReturn(article);
        when(dictService.getValueByKey("hot_threshold")).thenReturn("3");

        java.util.List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Comment c = new Comment();
            c.setCommentId(i + 1);
            comments.add(c);
        }
        when(commentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(comments);
        when(replyService.queryReplyByCommentId(anyInt())).thenReturn(new ArrayList<>());
        when(articleService.updateById(any(Article.class))).thenReturn(true);

        commentService.checkHotBonus(1);

        verify(pointsLogService).adjustUserPoints(eq(1), eq(1), contains("帖子热度奖励"), eq("hot_bonus"), eq(1), isNull());
        verify(notificationService).createNotification(eq(1), isNull(), eq("hot_bonus"), contains("热度奖励"), eq("article"), eq(1));
    }
}
