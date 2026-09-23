package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.AppealMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.User;
import com.walker.service.impl.AppealServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppealServiceTest {

    @InjectMocks
    private AppealServiceImpl appealService;

    @Mock
    private AppealMapper appealMapper;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private ViolationService violationService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = appealService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(appealService, appealMapper);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Appeal.class);
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
    @DisplayName("提交申诉 → 参数不完整 → 返回错误")
    void submitAppeal_missingParams_returnsError() {
        ResultBean result = appealService.submitAppeal(null, "violation", 1, "理由");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交申诉 → 违规申诉无关联ID → 返回错误")
    void submitAppeal_violationWithoutRelatedId_returnsError() {
        ResultBean result = appealService.submitAppeal(1, "violation", null, "理由");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交申诉 → 已有待审申诉 → 返回错误")
    void submitAppeal_existingPending_returnsError() {
        when(appealMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ResultBean result = appealService.submitAppeal(1, "violation", 1, "理由");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交申诉 → 正常提交成功")
    void submitAppeal_valid_succeeds() {
        when(appealMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(appealMapper.insert(any(Appeal.class))).thenReturn(1);

        ResultBean result = appealService.submitAppeal(1, "violation", 1, "申诉理由");
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("审核申诉 → 参数不完整 → 返回错误")
    void reviewAppeal_missingParams_returnsError() {
        ResultBean result = appealService.reviewAppeal(null, 1, "accepted", "同意");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核申诉 → 申诉不存在 → 返回错误")
    void reviewAppeal_notFound_returnsError() {
        when(appealMapper.selectById(999)).thenReturn(null);

        ResultBean result = appealService.reviewAppeal(999, 1, "accepted", "同意");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核申诉 → 已处理 → 返回错误")
    void reviewAppeal_alreadyProcessed_returnsError() {
        Appeal appeal = new Appeal();
        appeal.setId(1);
        appeal.setStatus("accepted");
        when(appealMapper.selectById(1)).thenReturn(appeal);

        ResultBean result = appealService.reviewAppeal(1, 1, "accepted", "同意");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核申诉 → 通过 → 通知申诉人")
    void reviewAppeal_accept_notifies() {
        Appeal appeal = new Appeal();
        appeal.setId(1);
        appeal.setUserId(2);
        appeal.setStatus("pending");
        when(appealMapper.selectById(1)).thenReturn(appeal);
        when(appealMapper.updateById(any(Appeal.class))).thenReturn(1);

        ResultBean result = appealService.reviewAppeal(1, 1, "accepted", "同意");
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(2), eq(1), eq("appeal_review"), contains("通过"), eq("appeal"), eq(1));
    }

    @Test
    @DisplayName("审核申诉 → 驳回 → 通知申诉人")
    void reviewAppeal_reject_notifies() {
        Appeal appeal = new Appeal();
        appeal.setId(1);
        appeal.setUserId(2);
        appeal.setStatus("pending");
        when(appealMapper.selectById(1)).thenReturn(appeal);
        when(appealMapper.updateById(any(Appeal.class))).thenReturn(1);

        ResultBean result = appealService.reviewAppeal(1, 1, "rejected", "理由不充分");
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(2), eq(1), eq("appeal_review"), contains("驳回"), eq("appeal"), eq(1));
    }

    @Test
    @DisplayName("审核申诉 → 违规申诉通过 → 自动取消关联违规（回滚扣分+恢复内容）")
    void reviewAppeal_acceptViolation_autoCancelsViolation() {
        Appeal appeal = new Appeal();
        appeal.setId(1);
        appeal.setUserId(2);
        appeal.setAppealType("violation");
        appeal.setRelatedId(100);
        appeal.setStatus("pending");
        when(appealMapper.selectById(1)).thenReturn(appeal);
        when(appealMapper.updateById(any(Appeal.class))).thenReturn(1);

        ResultBean result = appealService.reviewAppeal(1, 1, "accepted", "确实误判");
        assertEquals(200, result.getCode());
        verify(violationService).autoCancelByAppeal(eq(100), eq(1), eq("确实误判"));
    }

    @Test
    @DisplayName("审核申诉 → 违规申诉驳回 → 不触发取消违规")
    void reviewAppeal_rejectViolation_noCancel() {
        Appeal appeal = new Appeal();
        appeal.setId(1);
        appeal.setUserId(2);
        appeal.setAppealType("violation");
        appeal.setRelatedId(100);
        appeal.setStatus("pending");
        when(appealMapper.selectById(1)).thenReturn(appeal);
        when(appealMapper.updateById(any(Appeal.class))).thenReturn(1);

        ResultBean result = appealService.reviewAppeal(1, 1, "rejected", "理由不充分");
        assertEquals(200, result.getCode());
        verify(violationService, never()).autoCancelByAppeal(anyInt(), anyInt(), any());
    }

    // ==================== submitAppeal — 空内容 ====================

    @Test
    @DisplayName("提交申诉 → 内容为空 → 返回错误")
    void submitAppeal_emptyContent_returnsError() {
        ResultBean result = appealService.submitAppeal(1, "violation", 1, "");
        assertEquals(500, result.getCode());
    }

    // ==================== listMyAppeals ====================

    @Test
    @DisplayName("查询我的申诉 → 返回分页结果")
    void listMyAppeals_returnsResults() {
        Appeal a1 = new Appeal();
        a1.setId(1);
        a1.setUserId(10);
        a1.setContent("申诉理由");
        a1.setStatus("pending");

        Page<Appeal> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(a1));
        page.setTotal(1);
        when(appealMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        ResultBean result = appealService.listMyAppeals(10, 1, 10);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.getObj();
        assertNotNull(data.get("records"));
        assertEquals(1L, data.get("total"));
    }

    // ==================== listAppeals with status filter ====================

    @Test
    @DisplayName("查询申诉列表 → 按状态过滤 → 返回匹配结果")
    void listAppeals_withStatus_filtersCorrectly() {
        Appeal a1 = new Appeal();
        a1.setId(1);
        a1.setStatus("pending");
        a1.setAppealType("violation");
        a1.setRelatedId(10);

        Page<Appeal> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(a1));
        page.setTotal(1);
        when(appealMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(userService.listUsersWithOrgInfo(any())).thenReturn(Collections.emptyList());
        when(violationService.listByIds(any())).thenReturn(Collections.emptyList());

        ResultBean result = appealService.listAppeals("pending", 1, 10);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.getObj();
        assertNotNull(data.get("records"));
    }

    // ==================== 操作人身份校验（JWT） ====================

    @Test
    @DisplayName("审核申诉 → 无登录态 → 拒绝")
    void reviewAppeal_noAuth_returnsError() {
        SecurityContextHolder.clearContext();
        ResultBean result = appealService.reviewAppeal(1, 1, "accepted", "同意");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("登录"));
    }

    @Test
    @DisplayName("审核申诉 → 前端伪造审核人 id ≠ 登录态 → 拒绝")
    void reviewAppeal_forgedReviewer_returnsError() {
        // 登录态为 1，却传 reviewerId=2（伪造他人）→ 拒绝
        ResultBean result = appealService.reviewAppeal(1, 2, "accepted", "同意");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("身份"));
        verify(appealMapper, never()).updateById(any(Appeal.class));
    }
}
