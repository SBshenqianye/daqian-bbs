package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.PointsLogMapper;
import com.walker.pojo.LevelItem;
import com.walker.pojo.LevelProfile;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), LevelProfile.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), LevelItem.class);
    }

    @Test
    @DisplayName("启用中的方案即为生效方案")
    void activeProfile_returnsEnabledProfile() {
        LevelProfile enabled = new LevelProfile();
        enabled.setId(1);
        enabled.setStatus(1);
        when(profileMapper.selectList(any())).thenReturn(Collections.singletonList(enabled));

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
    @DisplayName("保存已有方案：按 level_index 原地更新，id 保持稳定")
    void saveProfile_keepsItemIdsStable() {
        LevelProfile profile = new LevelProfile();
        profile.setId(3);
        profile.setProfileName("方案二 · 电网匠心");
        profile.setStatus(0);

        LevelItem row1 = item(1, 1, 0);
        row1.setId(101);
        row1.setProfileId(3);
        row1.setLevelIndex(1);
        LevelItem row2 = item(1, 2, 100);
        row2.setId(102);
        row2.setProfileId(3);
        row2.setLevelIndex(2);
        LevelItem stale = item(1, 3, 200);
        stale.setId(103);
        stale.setProfileId(3);
        stale.setLevelIndex(3);
        when(itemMapper.selectList(any())).thenReturn(Arrays.asList(row1, row2, stale));

        List<LevelItem> items = Arrays.asList(item(1, 1, 0), item(1, 2, 120));

        com.walker.vo.ResultBean result = service.saveProfile(profile, items);

        assertEquals(200, result.getCode(), result.getMessage());
        assertEquals(101, items.get(0).getId());
        assertEquals(102, items.get(1).getId());
        verify(itemMapper, times(2)).updateById(any(LevelItem.class));
        verify(itemMapper, never()).insert(any(LevelItem.class));
        verify(itemMapper, times(1)).delete(any());
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

    @Test
    @DisplayName("默认 24 级体系：按产品累计门槛解析等级，年度极限停在工匠前一级")
    void getUserLevel_resolvesDefault24Ladder() {
        LevelProfile profile = new LevelProfile();
        profile.setId(1);
        profile.setProfileName("默认等级体系");
        profile.setStatus(1);
        when(profileMapper.selectList(any())).thenReturn(Collections.singletonList(profile));

        int[] thresholds = {0, 100, 250, 500, 800, 1200, 1800, 2600, 3600, 5100, 7100, 9600,
                12600, 16100, 20100, 25100, 31100, 38100, 46100, 55100, 65100, 77100, 92100, 110100};
        List<LevelItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < thresholds.length; i++) {
            LevelItem levelItem = item(i / 3 + 1, i % 3 + 1, thresholds[i]);
            levelItem.setLevelIndex(i + 1);
            items.add(levelItem);
        }
        when(itemMapper.selectList(any())).thenReturn(items);

        when(pointsLogService.getPointsAdjustment(7)).thenReturn(43890);
        Map<String, Object> yearlyLimit = service.getUserLevel(7);
        assertEquals(18, yearlyLimit.get("level"));
        assertEquals(46100, yearlyLimit.get("nextThreshold"));

        when(pointsLogService.getPointsAdjustment(7)).thenReturn(110100);
        Map<String, Object> ceiling = service.getUserLevel(7);
        assertEquals(24, ceiling.get("level"));
        assertNull(ceiling.get("nextThreshold"));
    }

    @Test
    @DisplayName("启用方案后其余方案自动停用")
    void activateProfile_switchesStatus() {
        LevelProfile target = new LevelProfile();
        target.setId(7);
        target.setProfileName("方案X");
        target.setStatus(0);
        when(profileMapper.selectById(7)).thenReturn(target);

        com.walker.vo.ResultBean result = service.activateProfile(7);

        assertEquals(200, result.getCode(), result.getMessage());
        assertEquals(1, target.getStatus());
        verify(profileMapper).updateById(target);
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
