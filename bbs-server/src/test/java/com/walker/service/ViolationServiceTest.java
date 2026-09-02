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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;

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
}
