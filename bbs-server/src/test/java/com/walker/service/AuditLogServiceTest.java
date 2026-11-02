package com.walker.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.mapper.AuditLogMapper;
import com.walker.pojo.AuditLog;
import com.walker.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 审计日志服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Mock
    private AuditLogMapper auditLogMapper;

    @Test
    @DisplayName("写入成功：补齐创建时间并调用 mapper")
    void record_success_insertsRow() {
        AuditLog log = new AuditLog();
        log.setModule("ARTICLE");
        log.setAction("ARTICLE_AUDIT");
        log.setResult("success");

        auditLogService.record(log);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("ARTICLE", saved.getModule());
        assertEquals("ARTICLE_AUDIT", saved.getAction());
        assertNotNull(saved.getCreateTime());
    }

    @Test
    @DisplayName("审计写入失败：只打 warn，不向业务抛异常")
    void record_whenMapperThrows_doesNotThrow() {
        when(auditLogMapper.insert(any(AuditLog.class)))
                .thenThrow(new RuntimeException("database down"));

        AuditLog log = new AuditLog();
        log.setModule("USER");
        log.setAction("USER_UPDATE");
        log.setResult("fail");

        assertDoesNotThrow(() -> auditLogService.record(log));
        verify(auditLogMapper).insert(any(AuditLog.class));
    }

    @Test
    @DisplayName("按截止时间清理：返回删除条数")
    void purgeBefore_returnsDeletedCount() {
        when(auditLogMapper.delete(any())).thenReturn(3);

        int deleted = auditLogService.purgeBefore("2026-01-01 00:00:00");

        assertEquals(3, deleted);
        verify(auditLogMapper).delete(any());
    }

    @Test
    @DisplayName("分页查询：返回 records/total/page/size")
    @SuppressWarnings("unchecked")
    void list_returnsPagedMap() {
        AuditLog row = new AuditLog();
        row.setId(1L);
        row.setModule("ARTICLE");
        Page<AuditLog> page = new Page<>(1, 20);
        page.setRecords(Arrays.asList(row));
        page.setTotal(1L);
        when(auditLogMapper.selectPage(any(Page.class), any())).thenReturn(page);

        Map<String, Object> data = auditLogService.list(
                1, 20, "ARTICLE", "ARTICLE_AUDIT", 1, "admin",
                "success", "2026-11-01 00:00:00", "2026-11-02 23:59:59",
                "ARTICLE", "1");

        assertEquals(1L, data.get("total"));
        assertEquals(1, data.get("page"));
        assertEquals(20, data.get("size"));
        assertNotNull(data.get("records"));
        verify(auditLogMapper).selectPage(any(Page.class), any());
    }
}
