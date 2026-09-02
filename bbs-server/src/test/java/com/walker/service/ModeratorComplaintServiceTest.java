package com.walker.service;

import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.User;
import com.walker.service.impl.ModeratorComplaintServiceImpl;
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
class ModeratorComplaintServiceTest {

    @InjectMocks
    private ModeratorComplaintServiceImpl complaintService;

    @Mock
    private ModeratorComplaintMapper complaintMapper;

    @Mock
    private BoardModeratorService boardModeratorService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = complaintService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(complaintService, complaintMapper);
    }

    @Test
    @DisplayName("提交投诉 → 内容为空 → 返回错误")
    void submit_emptyContent_returnsError() {
        ResultBean result = complaintService.submit(1, 1, 1, "");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交投诉 → 投诉自己 → 返回错误")
    void submit_selfComplaint_returnsError() {
        ResultBean result = complaintService.submit(1, 1, 1, "投诉内容");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交投诉 → 已有待审投诉 → 返回错误")
    void submit_existingPending_returnsError() {
        when(complaintMapper.countPendingByReporterAndModerator(1, 2)).thenReturn(1);
        ResultBean result = complaintService.submit(1, 2, 1, "投诉内容");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("提交投诉 → 正常提交 → 通知超管")
    void submit_valid_succeeds() {
        when(complaintMapper.countPendingByReporterAndModerator(1, 2)).thenReturn(0);
        when(complaintMapper.insert(any(ModeratorComplaint.class))).thenReturn(1);

        User reporter = new User();
        reporter.setNickname("投诉人");
        when(userService.getById(1)).thenReturn(reporter);

        ResultBean result = complaintService.submit(1, 2, 1, "投诉内容");
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(1), eq(1), eq("moderator_complaint"), contains("投诉"), eq("moderator_complaint"), any());
    }

    @Test
    @DisplayName("审核投诉 → 参数不完整 → 返回错误")
    void review_missingParams_returnsError() {
        ResultBean result = complaintService.review(null, "accepted", "备注", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核投诉 → 无效状态 → 返回错误")
    void review_invalidStatus_returnsError() {
        ResultBean result = complaintService.review(1, "invalid", "备注", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核投诉 → 记录不存在 → 返回错误")
    void review_notFound_returnsError() {
        when(complaintMapper.selectById(999)).thenReturn(null);
        ResultBean result = complaintService.review(999, "accepted", "备注", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核投诉 → 已处理 → 返回错误")
    void review_alreadyProcessed_returnsError() {
        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setId(1);
        complaint.setStatus("accepted");
        when(complaintMapper.selectById(1)).thenReturn(complaint);
        ResultBean result = complaintService.review(1, "accepted", "备注", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核投诉 → 采纳 → 撤销版主")
    void review_accept_dismissesModerator() {
        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setId(1);
        complaint.setReporterId(1);
        complaint.setModeratorId(2);
        complaint.setLabelId(10);
        complaint.setStatus("pending");
        when(complaintMapper.selectById(1)).thenReturn(complaint);
        when(complaintMapper.updateById(any(ModeratorComplaint.class))).thenReturn(1);

        ResultBean result = complaintService.review(1, "accepted", "属实", 1);
        assertEquals(200, result.getCode());
        verify(boardModeratorService).dismiss(2, 10);
        verify(notificationService).createNotification(eq(1), eq(1), eq("complaint_review"), contains("已采纳"), eq("moderator_complaint"), eq(1));
    }

    @Test
    @DisplayName("审核投诉 → 驳回 → 通知投诉人")
    void review_reject_notifiesReporter() {
        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setId(1);
        complaint.setReporterId(1);
        complaint.setStatus("pending");
        when(complaintMapper.selectById(1)).thenReturn(complaint);
        when(complaintMapper.updateById(any(ModeratorComplaint.class))).thenReturn(1);

        ResultBean result = complaintService.review(1, "rejected", "证据不足", 1);
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(1), eq(1), eq("complaint_review"), contains("已驳回"), eq("moderator_complaint"), eq(1));
    }
}
