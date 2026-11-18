package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.PointsLogMapper;
import com.walker.pojo.LevelProfile;
import com.walker.pojo.OperationCycle;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

    @InjectMocks
    private LevelService service;

    @Mock private LevelProfileMapper profileMapper;
    @Mock private LevelItemMapper itemMapper;
    @Mock private PointsLogService pointsLogService;
    @Mock private PointsLogMapper pointsLogMapper;
    @Mock private OperationCycleService operationCycleService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), LevelProfile.class);
    }

    @Test
    @DisplayName("运营周期绑定等级方案时，优先使用周期绑定的方案")
    void activeProfile_usesCycleBinding() {
        OperationCycle cycle = new OperationCycle();
        cycle.setLevelProfileId(5);
        when(operationCycleService.getActiveCycle()).thenReturn(cycle);
        LevelProfile bound = new LevelProfile();
        bound.setId(5);
        bound.setProfileName("周期绑定方案");
        when(profileMapper.selectById(5)).thenReturn(bound);

        LevelProfile result = service.getActiveProfile();

        assertEquals(5, result.getId());
        verify(profileMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("周期未绑定等级方案时，回落到全局启用方案")
    void activeProfile_fallsBackToGlobal() {
        when(operationCycleService.getActiveCycle()).thenReturn(null);
        LevelProfile global = new LevelProfile();
        global.setId(1);
        global.setStatus(1);
        when(profileMapper.selectList(any())).thenReturn(Collections.singletonList(global));

        LevelProfile result = service.getActiveProfile();

        assertEquals(1, result.getId());
    }
}
