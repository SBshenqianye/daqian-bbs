package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.DictMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.PointsLog;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.service.impl.ViolationServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ViolationServiceTest {

    @InjectMocks
    private ViolationServiceImpl violationService;

    @Mock
    private ViolationMapper violationMapper;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private DictMapper dictMapper;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentService commentService;

    @Mock
    private ReplyService replyService;

    @Mock
    private AppealService appealService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = violationService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(violationService, violationMapper);
        // 默认当前登录管理员 id=1，敏感用例可覆盖
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

    @Test
    @DisplayName("添加违规 → 参数不完整 → 返回错误")
    void addViolation_missingParams_returnsError() {
        ResultBean result = violationService.addViolation(null, "spam", "article", 1, 1, "备注");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("添加违规 → 未知违规类型 → 返回错误")
    void addViolation_unknownType_returnsError() {
        when(dictMapper.selectValueByKey("unknown")).thenReturn(null);
        ResultBean result = violationService.addViolation(1, "unknown", "article", 1, 1, "备注");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("添加违规 → 恶意灌水 → 扣4分")
    void addViolation_spam_deducts4Points() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(4);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 1, "灌水");
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-4), contains("恶意灌水"), eq("violation"), any(), eq(1));
    }

    @Test
    @Disabled("LambdaUpdateWrapper<User> triggers MyBatis-Plus lambda cache lookup")
    @DisplayName("添加违规 → 泄密 → 立即限制发帖")
    void addViolation_leak_restrictsPosting() {
        when(dictMapper.selectValueByKey("leak")).thenReturn("20");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        User user = new User();
        user.setId(1);
        user.setPostRestricted(0);
        when(userService.getById(1)).thenReturn(user);
        when(userService.update(any(User.class), any())).thenReturn(true);

        ResultBean result = violationService.addViolation(1, "leak", "article", 1, 1, "泄密");
        assertEquals(200, result.getCode());
        verify(userService).update(any(User.class), any());
    }

    @Test
    @DisplayName("添加违规 → 月度累计超20分 → 自动限制发帖7天")
    void addViolation_monthlyExceed20_restrictsPosting() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(eq(1), anyString(), anyString())).thenReturn(21);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        User user = new User();
        user.setId(1);
        user.setPostRestricted(0);
        when(userService.getById(1)).thenReturn(user);
        when(userService.updateById(any(User.class))).thenReturn(true);

        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 1, "灌水");
        assertEquals(200, result.getCode());
        verify(userService).updateById(argThat(u -> ((User) u).getPostRestricted() == 1));
    }

    @Test
    @DisplayName("添加违规 → 已被限制 → 不重复限制")
    void addViolation_alreadyRestricted_noDuplicate() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(eq(1), anyString(), anyString())).thenReturn(21);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        User user = new User();
        user.setId(1);
        user.setPostRestricted(1);
        when(userService.getById(1)).thenReturn(user);

        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 1, "灌水");
        assertEquals(200, result.getCode());
        verify(userService, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("添加违规 → 通知用户")
    void addViolation_notifiesUser() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(4);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 1, "灌水");
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(1), eq(1), eq("violation"), contains("恶意灌水"), eq("violation"), any());
    }

    @Test
    @DisplayName("添加违规 → 违法违规 → 从字典扣15分")
    void addViolation_illegal_deducts15Points() {
        when(dictMapper.selectValueByKey("illegal")).thenReturn("15");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(15);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = violationService.addViolation(1, "illegal", "article", 1, 1, "违法内容");
        assertEquals(200, result.getCode());
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-15), contains("违法违规"), eq("violation"), any(), eq(1));
    }

    @Test
    @DisplayName("添加违规 → 关联评论 → 删除评论")
    void addViolation_withComment_deletesComment() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(4);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = violationService.addViolation(1, "spam", "comment", 100, 1, "违规评论");
        assertEquals(200, result.getCode());
        verify(commentService).deleteCommentById(100);
    }

    @Test
    @DisplayName("添加违规 → 关联回复 → 删除回复")
    void addViolation_withReply_deletesReply() {
        when(dictMapper.selectValueByKey("spam")).thenReturn("4");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(4);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

        ResultBean result = violationService.addViolation(1, "spam", "reply", 200, 1, "违规回复");
        assertEquals(200, result.getCode());
        verify(replyService).deleteReplyById(200);
    }

    // ========== 抄袭/未知类型 补充测试 ==========

    @Test
    @DisplayName("添加违规 → 抄袭 → 按帖子所得积分扣分而非固定12分")
    void addViolation_plagiarism_calculatesEarnedPoints() {
        // 模拟：字典中有 plagiarism 值，但抄袭走 calculateArticleEarnedPoints 分支
        when(dictMapper.selectValueByKey("plagiarism")).thenReturn("12");
        when(violationMapper.insert(any(Violation.class))).thenReturn(1);
        when(violationMapper.sumMonthlyDeductions(anyInt(), anyString(), anyString())).thenReturn(13);

        // 模拟帖子获得的积分：3 + 10 = 13 分
        PointsLog log1 = new PointsLog();
        log1.setPointsChange(3);
        PointsLog log2 = new PointsLog();
        log2.setPointsChange(10);
        when(pointsLogService.list(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(log1, log2));

        ResultBean result = violationService.addViolation(1, "plagiarism", "article", 42, 1, "抄袭帖子");
        assertEquals(200, result.getCode());
        // 验证扣分为帖子所得积分 13 分（而非字典/默认的 12 分）
        verify(pointsLogService).adjustUserPoints(eq(1), eq(-13), contains("抄袭剽窃"), eq("violation"), any(), eq(1));
    }

    @Test
    @DisplayName("添加违规 → 字典无值且不在默认映射 → 返回未知违规类型错误")
    void addViolation_unknownType_noFallback_returnsError() {
        // 字典无值
        when(dictMapper.selectValueByKey("nonexistent_foo")).thenReturn(null);
        // 该类型不在 DEFAULT_VIOLATION_POINTS 映射中 → points=0 → 报错
        ResultBean result = violationService.addViolation(1, "nonexistent_foo", "article", 1, 1, "未知类型");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("未知的违规类型"));
    }

    @Test
    @DisplayName("添加违规 → 无登录态 → 拒绝")
    void addViolation_noAuth_returnsError() {
        SecurityContextHolder.clearContext();
        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 1, "灌水");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("登录"));
    }

    @Test
    @DisplayName("添加违规 → 前端伪造操作人 id ≠ 登录态 → 拒绝")
    void addViolation_forgedOperator_returnsError() {
        // 登录态为 1，却传 operatorId=2 → 拒绝，且不落库
        ResultBean result = violationService.addViolation(1, "spam", "article", 1, 2, "灌水");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("身份"));
        verify(violationMapper, never()).insert(any(Violation.class));
    }

    // ========== #14 取消违规 测试 ==========

    /** 构造一条生效中的违规记录 */
    private Violation activeViolation(int id, String relatedType, int relatedId) {
        Violation v = new Violation();
        v.setId(id);
        v.setUserId(2);
        v.setViolationType("spam");
        v.setPointsDeducted(4);
        v.setRelatedType(relatedType);
        v.setRelatedId(relatedId);
        v.setOperatorId(1);
        v.setStatus("active");
        return v;
    }

    @Test
    @DisplayName("取消违规 → 成功：回滚扣分+恢复内容+状态置已取消")
    void cancelViolation_success_rollbackAndRestore() {
        Violation v = activeViolation(10, "article", 88);
        when(violationMapper.selectById(10)).thenReturn(v);
        when(appealService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        PointsLog mainLog = new PointsLog();
        mainLog.setId(500);
        mainLog.setPointsChange(-4);
        // 第一次 list 查主扣分日志，第二次 list 查内容删除扣回日志（空）
        when(pointsLogService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(mainLog)))
                .thenReturn(new ArrayList<>());
        when(violationMapper.restoreArticleById(88)).thenReturn(1);

        ResultBean result = violationService.cancelViolation(10, "误判");
        assertEquals(200, result.getCode());
        // 主扣分日志被撤销（加回 4 分）
        verify(pointsLogService).undoPointsLog(eq(500), eq(1));
        // 内容恢复可见
        verify(violationMapper).restoreArticleById(88);
        // 状态已置已取消
        verify(violationMapper).updateById(argThat((Violation saved) ->
                "cancelled".equals(saved.getStatus()) && "误判".equals(saved.getCancelReason())));
        // 通知用户
        verify(notificationService).createNotification(eq(2), eq(1), eq("violation"), anyString(), eq("violation"), eq(10));
    }

    @Test
    @DisplayName("取消违规 → 已取消 → 拒绝重复操作")
    void cancelViolation_alreadyCancelled_returnsError() {
        Violation v = activeViolation(10, "article", 88);
        v.setStatus("cancelled");
        when(violationMapper.selectById(10)).thenReturn(v);
        ResultBean result = violationService.cancelViolation(10, "误判");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已取消"));
        verify(pointsLogService, never()).undoPointsLog(anyInt(), anyInt());
    }

    @Test
    @DisplayName("取消违规 → 存在审核中申诉 → 拒绝")
    void cancelViolation_pendingAppeal_returnsError() {
        Violation v = activeViolation(10, "article", 88);
        when(violationMapper.selectById(10)).thenReturn(v);
        when(appealService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);
        ResultBean result = violationService.cancelViolation(10, "误判");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("申诉"));
        verify(pointsLogService, never()).undoPointsLog(anyInt(), anyInt());
    }

    @Test
    @DisplayName("取消违规 → 无登录态 → 拒绝")
    void cancelViolation_noAuth_returnsError() {
        SecurityContextHolder.clearContext();
        ResultBean result = violationService.cancelViolation(10, "误判");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("登录"));
    }

    @Test
    @DisplayName("取消违规 → 未填原因 → 拒绝")
    void cancelViolation_emptyReason_returnsError() {
        Violation v = activeViolation(10, "article", 88);
        when(violationMapper.selectById(10)).thenReturn(v);
        ResultBean result = violationService.cancelViolation(10, "  ");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("原因"));
    }
}
