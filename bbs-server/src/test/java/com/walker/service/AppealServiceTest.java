package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.AppealMapper;
import com.walker.pojo.Appeal;
import com.walker.service.impl.AppealServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
