package com.walker.service;

import com.walker.mapper.ReplyMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.service.impl.ReplyServiceImpl;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ReplyParam;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplyServiceTest {

    @InjectMocks
    private ReplyServiceImpl replyService;

    @Mock
    private ReplyMapper replyMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CommentService commentService;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private ArticleService articleService;

    @Mock
    private DictService dictService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = replyService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(replyService, replyMapper);
    }

    @Test
    @DisplayName("回复成功 → 保存记录")
    void saveUserReply_validReply_succeeds() {
        ReplyParam param = new ReplyParam();
        param.setReplyContent("说得对！");
        param.setReplyUserId(1);
        param.setReplyToUserId(2);
        param.setCommentId(1);

        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setCommentArticleId(10);
        when(commentService.getById(1)).thenReturn(comment);

        Article article = new Article();
        article.setArticleId(10);
        article.setUserId(3);
        when(articleService.getById(10)).thenReturn(article);
        when(dictService.getValueByKey("reply")).thenReturn("1");
        when(pointsLogService.countReplyPointsForArticle(1, 10)).thenReturn(0);
        when(replyMapper.insert(any(Reply.class))).thenReturn(1);

        ResultBean result = replyService.saveUserReply(param);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("回复 → 自己帖子内回复不计积分")
    void saveUserReply_ownArticle_noPoints() {
        ReplyParam param = new ReplyParam();
        param.setReplyContent("自回复");
        param.setReplyUserId(1);
        param.setReplyToUserId(2);
        param.setCommentId(1);

        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setCommentArticleId(10);
        when(commentService.getById(1)).thenReturn(comment);

        Article article = new Article();
        article.setArticleId(10);
        article.setUserId(1);
        when(articleService.getById(10)).thenReturn(article);
        when(replyMapper.insert(any(Reply.class))).thenReturn(1);

        ResultBean result = replyService.saveUserReply(param);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), eq("回复积分"), anyString(), anyInt(), isNull());
    }

    @Test
    @DisplayName("回复 → 回复自己 → 不通知")
    void saveUserReply_replyToSelf_noNotification() {
        ReplyParam param = new ReplyParam();
        param.setReplyContent("自言自语");
        param.setReplyUserId(1);
        param.setReplyToUserId(1);
        param.setCommentId(1);

        Comment comment = new Comment();
        comment.setCommentId(1);
        comment.setCommentArticleId(10);
        when(commentService.getById(1)).thenReturn(comment);

        Article article = new Article();
        article.setArticleId(10);
        article.setUserId(2);
        when(articleService.getById(10)).thenReturn(article);
        when(dictService.getValueByKey("reply")).thenReturn("1");
        when(pointsLogService.countReplyPointsForArticle(1, 10)).thenReturn(0);
        when(replyMapper.insert(any(Reply.class))).thenReturn(1);

        ResultBean result = replyService.saveUserReply(param);
        assertEquals(200, result.getCode());
        verify(notificationService, never()).createNotification(anyInt(), anyInt(), eq("reply"), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("删除回复 → 已审核通过 → 扣回积分")
    void deleteReply_enabledReply_deductsPoints() {
        Reply reply = new Reply();
        reply.setReplyId(1);
        reply.setReplyUserId(1);
        reply.setEnable(1);
        when(replyMapper.selectById(1)).thenReturn(reply);
        when(dictService.getValueByKey("reply")).thenReturn("1");
        when(replyMapper.deleteById(1)).thenReturn(1);

        ResultBean result = replyService.deleteReplyById(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-1), contains("删除回复扣回积分"), eq("reply"), eq(1), isNull());
    }

    @Test
    @DisplayName("删除回复 → 未审核通过 → 不扣积分")
    void deleteReply_disabledReply_noDeduct() {
        Reply reply = new Reply();
        reply.setReplyId(1);
        reply.setReplyUserId(1);
        reply.setEnable(0);
        when(replyMapper.selectById(1)).thenReturn(reply);
        when(replyMapper.deleteById(1)).thenReturn(1);

        ResultBean result = replyService.deleteReplyById(1);
        assertEquals(200, result.getCode());
        verify(pointsLogService, never()).adjustUserPoints(anyInt(), anyInt(), anyString(), anyString(), anyInt(), any());
    }
}
