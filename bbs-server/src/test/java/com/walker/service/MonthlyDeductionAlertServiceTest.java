package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.PointsAlertMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.PointsAlert;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonthlyDeductionAlertServiceTest {

    @InjectMocks
    private MonthlyDeductionAlertService service;

    @Mock
    private PointsAlertMapper pointsAlertMapper;

    @Mock
    private ViolationMapper violationMapper;

    @Mock
    private DictService dictService;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PointsAlert.class);
    }

    @Test
    @DisplayName("达到阈值 → 写入去重记录并发送提醒")
    void reachesThreshold_notifiesOnce() {
        when(dictService.getValueByKey("monthly_deduction_alert_enabled")).thenReturn("1");
        when(dictService.getValueByKey("monthly_deduction_alert_threshold")).thenReturn("10");
        when(pointsAlertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(pointsAlertMapper.insert(any(PointsAlert.class))).thenReturn(1);

        service.checkAndNotify(1, 10, "2026-11");

        verify(pointsAlertMapper).insert(any(PointsAlert.class));
        verify(notificationService).createNotification(eq(1), isNull(), eq("monthly_deduction_alert"),
                contains("累计扣分已达 10 分"), eq("user"), eq(1));
    }

    @Test
    @DisplayName("未达阈值 → 不提醒")
    void belowThreshold_doesNothing() {
        when(dictService.getValueByKey("monthly_deduction_alert_enabled")).thenReturn("1");
        when(dictService.getValueByKey("monthly_deduction_alert_threshold")).thenReturn("10");

        service.checkAndNotify(1, 9, "2026-11");

        verify(pointsAlertMapper, never()).insert(any(PointsAlert.class));
        verify(notificationService, never()).createNotification(anyInt(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("本月已提醒 → 不重复")
    void alreadyAlerted_doesNothing() {
        when(dictService.getValueByKey("monthly_deduction_alert_enabled")).thenReturn("1");
        when(dictService.getValueByKey("monthly_deduction_alert_threshold")).thenReturn("10");
        when(pointsAlertMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service.checkAndNotify(1, 15, "2026-11");

        verify(pointsAlertMapper, never()).insert(any(PointsAlert.class));
        verify(notificationService, never()).createNotification(anyInt(), any(), anyString(), anyString(), anyString(), any());
    }
}
