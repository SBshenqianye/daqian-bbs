package com.walker.config.audit;

import com.walker.service.AuditLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionJobTest {

    @InjectMocks
    private AuditLogRetentionJob job;

    @Mock
    private AuditLogService auditLogService;

    @Test
    @DisplayName("开启保留策略：按天数清理")
    void purge_enabled_callsPurgeBefore() throws Exception {
        setField("enabled", true);
        setField("retentionDays", 180);
        when(auditLogService.purgeBefore(anyString())).thenReturn(2);

        job.purge();

        verify(auditLogService).purgeBefore(anyString());
    }

    @Test
    @DisplayName("关闭保留策略：不清理")
    void purge_disabled_doesNothing() throws Exception {
        setField("enabled", false);
        setField("retentionDays", 180);

        job.purge();

        verify(auditLogService, never()).purgeBefore(anyString());
    }

    private void setField(String name, Object value) throws Exception {
        Field field = AuditLogRetentionJob.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(job, value);
    }
}
