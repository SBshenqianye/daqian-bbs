package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.mapper.NotificationMapper;
import com.walker.pojo.Notification;
import com.walker.service.impl.NotificationServiceImpl;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = notificationService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(notificationService, notificationMapper);
    }

    // ========== getUnreadCount ==========

    @Test
    @DisplayName("获取未读数 → userId为空 → 返回0")
    void getUnreadCount_nullUserId_returnsZero() {
        assertEquals(0, notificationService.getUnreadCount(null));
    }

    @Test
    @DisplayName("获取未读数 → 正常返回")
    void getUnreadCount_valid_returnsCount() {
        when(notificationMapper.countUnreadByUserId(1)).thenReturn(5);
        assertEquals(5, notificationService.getUnreadCount(1));
    }

    // ========== getUnreadSummary ==========

    @Test
    @DisplayName("获取未读汇总 → userId为空 → 返回全0")
    void getUnreadSummary_nullUserId_returnsZero() {
        Map<String, Object> result = notificationService.getUnreadSummary(null);
        assertEquals(0, result.get("total"));
    }

    @Test
    @DisplayName("获取未读汇总 → 正常返回分类和类型计数")
    void getUnreadSummary_valid_returnsGroupedCounts() {
        when(notificationMapper.countUnreadByUserId(1)).thenReturn(5);

        List<Map<String, Object>> catRows = new ArrayList<>();
        Map<String, Object> cat1 = new HashMap<>();
        cat1.put("category", "interaction");
        cat1.put("cnt", 3);
        catRows.add(cat1);
        Map<String, Object> cat2 = new HashMap<>();
        cat2.put("category", "system");
        cat2.put("cnt", 2);
        catRows.add(cat2);
        when(notificationMapper.countUnreadGroupByCategory(1)).thenReturn(catRows);

        List<Map<String, Object>> typeRows = new ArrayList<>();
        Map<String, Object> type1 = new HashMap<>();
        type1.put("type", "reply");
        type1.put("cnt", 3);
        typeRows.add(type1);
        when(notificationMapper.countUnreadGroupByType(1)).thenReturn(typeRows);

        Map<String, Object> result = notificationService.getUnreadSummary(1);
        assertEquals(5, result.get("total"));
    }

    // ========== markRead ==========

    @Test
    @DisplayName("标记已读 → userId为空 → 返回错误")
    void markRead_nullUserId_returnsError() {
        ResultBean result = notificationService.markRead(null, null, null);
        assertEquals(500, result.getCode());
    }

    @Test
    @Disabled("LambdaUpdateWrapper requires MyBatis-Plus entity metadata (Spring context)")
    @DisplayName("标记已读 → 按分类标记")
    void markRead_byCategory_succeeds() {
        when(notificationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ResultBean result = notificationService.markRead(1, null, "interaction");
        assertEquals(200, result.getCode());
    }

    @Test
    @Disabled("LambdaUpdateWrapper requires MyBatis-Plus entity metadata (Spring context)")
    @DisplayName("标记已读 → 按类型标记")
    void markRead_byType_succeeds() {
        when(notificationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ResultBean result = notificationService.markRead(1, "reply", null);
        assertEquals(200, result.getCode());
    }

    @Test
    @Disabled("LambdaUpdateWrapper requires MyBatis-Plus entity metadata (Spring context)")
    @DisplayName("标记全部已读")
    void markAllRead_succeeds() {
        when(notificationMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        ResultBean result = notificationService.markAllRead(1);
        assertEquals(200, result.getCode());
    }

    // ========== createNotification ==========

    @Test
    @DisplayName("创建通知 → 参数不完整 → 返回错误")
    void createNotification_missingParams_returnsError() {
        ResultBean result = notificationService.createNotification(null, 1, "reply", "title", "article", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("创建通知 → 自己通知自己 → 跳过")
    void createNotification_selfNotification_skips() {
        ResultBean result = notificationService.createNotification(1, 1, "reply", "title", "article", 1);
        assertEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("跳过自身通知"));
    }

    @Test
    @DisplayName("创建通知 → 正常创建")
    void createNotification_valid_succeeds() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        ResultBean result = notificationService.createNotification(2, 1, "reply", "有人回复了你", "reply", 1);
        assertEquals(200, result.getCode());
        verify(notificationMapper).insert(any(Notification.class));
    }
}
