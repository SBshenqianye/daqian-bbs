package com.walker.config.audit;

import com.walker.config.security.ForbiddenException;
import com.walker.service.AuditLogService;
import com.walker.service.AuditSnapshotService;
import com.walker.service.CurrentUserService;
import com.walker.vo.ResultBean;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审计切面测试。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @InjectMocks
    private AuditLogAspect aspect;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AuditSnapshotService auditSnapshotService;

    @Mock
    private AuditDataMasker auditDataMasker;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private AuditLog auditLog;

    @AfterEach
    void tearDown() {
        AuditContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void stubAnnotation() {
        lenient().when(auditLog.module()).thenReturn("ARTICLE");
        lenient().when(auditLog.action()).thenReturn("ARTICLE_AUDIT");
        lenient().when(auditLog.targetType()).thenReturn("ARTICLE");
        lenient().when(auditLog.targetId()).thenReturn("");
        lenient().when(auditLog.snapshot()).thenReturn(false);
    }

    @Test
    @DisplayName("业务成功 → 记录 success")
    void businessSuccess_recordsSuccess() throws Throwable {
        stubAnnotation();
        when(joinPoint.proceed()).thenReturn(ResultBean.success("操作成功"));

        Object result = aspect.around(joinPoint, auditLog);

        assertNotNull(result);
        ArgumentCaptor<com.walker.pojo.AuditLog> captor =
                ArgumentCaptor.forClass(com.walker.pojo.AuditLog.class);
        verify(auditLogService).record(captor.capture());
        assertEquals("success", captor.getValue().getResult());
        assertNull(captor.getValue().getErrorMsg());
    }

    @Test
    @DisplayName("业务返回失败 ResultBean → 仍记录 fail 与错误信息")
    void businessReturnsError_recordsFail() throws Throwable {
        stubAnnotation();
        when(joinPoint.proceed()).thenReturn(ResultBean.error("文章不存在"));

        Object result = aspect.around(joinPoint, auditLog);

        assertNotNull(result);
        ArgumentCaptor<com.walker.pojo.AuditLog> captor =
                ArgumentCaptor.forClass(com.walker.pojo.AuditLog.class);
        verify(auditLogService).record(captor.capture());
        assertEquals("fail", captor.getValue().getResult());
        assertEquals("文章不存在", captor.getValue().getErrorMsg());
    }

    @Test
    @DisplayName("审计写入抛异常 → 不影响业务返回")
    void auditFailure_doesNotBreakBusiness() throws Throwable {
        stubAnnotation();
        when(joinPoint.proceed()).thenReturn(ResultBean.success("操作成功"));
        doThrow(new RuntimeException("database down"))
                .when(auditLogService).record(any(com.walker.pojo.AuditLog.class));

        assertDoesNotThrow(() -> aspect.around(joinPoint, auditLog));
    }

    @Test
    @DisplayName("权限拒绝异常 → 切面不记录，交给 403 处理器")
    void forbiddenException_notRecordedByAspect() throws Throwable {
        stubAnnotation();
        when(joinPoint.proceed()).thenThrow(new ForbiddenException("无权操作"));

        assertThrows(ForbiddenException.class, () -> aspect.around(joinPoint, auditLog));
        verify(auditLogService, never()).record(any(com.walker.pojo.AuditLog.class));
    }
}
