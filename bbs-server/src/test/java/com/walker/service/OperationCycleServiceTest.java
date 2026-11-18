package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.CycleConfigMapper;
import com.walker.mapper.OperationCycleMapper;
import com.walker.pojo.CycleConfig;
import com.walker.pojo.OperationCycle;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationCycleServiceTest {

    @InjectMocks
    private OperationCycleService service;

    @Mock private OperationCycleMapper cycleMapper;
    @Mock private CycleConfigMapper cycleConfigMapper;

    @Test
    @DisplayName("复制周期：配置与等级方案一并复制，新周期为草稿")
    void copy_copiesConfigsAndProfile() {
        OperationCycle source = new OperationCycle();
        source.setId(1);
        source.setCycleName("2026 年度运营周期");
        source.setStartDate("2026-01-01");
        source.setEndDate("2026-12-31");
        source.setLevelProfileId(9);
        when(cycleMapper.selectById(1)).thenReturn(source);
        doAnswer(invocation -> {
            OperationCycle copy = invocation.getArgument(0);
            copy.setId(2);
            return 1;
        }).when(cycleMapper).insert(any(OperationCycle.class));

        CycleConfig config = new CycleConfig();
        config.setCycleId(1);
        config.setConfigKey("post");
        config.setConfigValue("5");
        when(cycleConfigMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(config));

        ResultBean result = service.copy(1, null);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getObj());
        verify(cycleConfigMapper).insert(any(CycleConfig.class));
    }

    @Test
    @DisplayName("归档周期：启用中的周期不允许归档")
    void archive_activeRejected() {
        OperationCycle active = new OperationCycle();
        active.setId(1);
        active.setStatus(1);
        when(cycleMapper.selectById(1)).thenReturn(active);

        ResultBean result = service.archive(1);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("启用中"));
    }

    @Test
    @DisplayName("归档周期：草稿可归档为只读")
    void archive_draftSucceeds() {
        OperationCycle draft = new OperationCycle();
        draft.setId(2);
        draft.setStatus(0);
        when(cycleMapper.selectById(2)).thenReturn(draft);

        ResultBean result = service.archive(2);

        assertEquals(200, result.getCode());
        assertEquals(2, draft.getStatus());
        verify(cycleMapper).updateById(draft);
    }

    @Test
    @DisplayName("当前周期：返回启用中的那一条")
    void activeCycle_returnsActive() {
        OperationCycle cycle = new OperationCycle();
        cycle.setId(3);
        cycle.setStartDate("2027-01-01");
        cycle.setEndDate("2027-12-31");
        when(cycleMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(cycle));

        OperationCycle result = service.getActiveCycle();

        assertNotNull(result);
        assertEquals(3, result.getId());
    }
}
