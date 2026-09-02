package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.AppealMapper;
import com.walker.pojo.Appeal;
import com.walker.service.impl.AppealServiceImpl;
import com.walker.vo.ResultBean;
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
}
