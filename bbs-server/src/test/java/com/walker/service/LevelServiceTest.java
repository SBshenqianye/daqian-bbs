package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.PointsLogMapper;
import com.walker.pojo.LevelItem;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), LevelItem.class);
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

    @Test
    @DisplayName("保存方案：允许每个大阶段的小阶段数量不同，并自动推导数量")
    void saveProfile_allowsDifferentMinorCounts() {
        LevelProfile profile = new LevelProfile();
        profile.setProfileName("不等长方案");
        profile.setStatus(0);
        doAnswer(invocation -> {
            LevelProfile p = invocation.getArgument(0);
            p.setId(11);
            return 1;
        }).when(profileMapper).insert(any(LevelProfile.class));

        List<LevelItem> items = Arrays.asList(
                item(1, 1, 0), item(1, 2, 100),
                item(2, 1, 200), item(2, 2, 300), item(2, 3, 400)
        );

        com.walker.vo.ResultBean result = service.saveProfile(profile, items);

        assertEquals(200, result.getCode(), result.getMessage());
        assertEquals(11, result.getObj());
        assertEquals(2, profile.getMajorCount());
        assertEquals(2, profile.getMinorPerMajor());
        assertEquals(Arrays.asList(2, 3), profile.getMinorCounts());
        verify(itemMapper, times(5)).insert(any(LevelItem.class));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5),
                Arrays.asList(items.get(0).getLevelIndex(), items.get(1).getLevelIndex(),
                        items.get(2).getLevelIndex(), items.get(3).getLevelIndex(),
                        items.get(4).getLevelIndex()));
    }

    @Test
    @DisplayName("存量方案兼容：从已有等级项推导每个大阶段的小阶段数量")
    void listAdminProfiles_derivesCountsForLegacyScheme() {
        LevelProfile profile = new LevelProfile();
        profile.setId(1);
        profile.setMajorCount(7);
        profile.setMinorPerMajor(3);
        when(profileMapper.selectList(any())).thenReturn(Collections.singletonList(profile));

        List<LevelItem> items = new java.util.ArrayList<>();
        for (int m = 1; m <= 7; m++) {
            for (int n = 1; n <= 3; n++) {
                items.add(item(m, n, (m - 1) * 300 + (n - 1) * 100));
            }
        }
        when(itemMapper.selectList(any())).thenReturn(items);

        com.walker.vo.ResultBean result = service.listAdminProfiles();

        assertEquals(200, result.getCode());
        assertEquals(Arrays.asList(3, 3, 3, 3, 3, 3, 3), profile.getMinorCounts());
    }

    @Test
    @DisplayName("保存方案：小阶段序号不连续时拒绝，避免生成错乱等级")
    void saveProfile_rejectsBrokenMinorSequence() {
        LevelProfile profile = new LevelProfile();
        profile.setProfileName("错误方案");
        profile.setStatus(0);
        List<LevelItem> items = Arrays.asList(item(1, 1, 0), item(1, 3, 100));

        com.walker.vo.ResultBean result = service.saveProfile(profile, items);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("小阶段序号"));
        verify(itemMapper, never()).insert(any(LevelItem.class));
    }

    private LevelItem item(int majorIndex, int minorIndex, int threshold) {
        LevelItem item = new LevelItem();
        item.setMajorIndex(majorIndex);
        item.setMinorIndex(minorIndex);
        item.setMajorName("阶段" + majorIndex);
        item.setDisplayName("阶段" + majorIndex + "-" + minorIndex);
        item.setPointsThreshold(threshold);
        return item;
    }
}
