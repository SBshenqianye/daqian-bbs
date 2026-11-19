package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.SvgAssetMapper;
import com.walker.pojo.LevelItem;
import com.walker.pojo.LevelProfile;
import com.walker.pojo.SvgAsset;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SvgAssetServiceTest {

    @InjectMocks
    private SvgAssetService service;

    @Mock private SvgAssetMapper svgAssetMapper;
    @Mock private LevelProfileMapper levelProfileMapper;
    @Mock private LevelItemMapper levelItemMapper;

    @Test
    @DisplayName("等级图表：包含方案名、等级名和累计门槛，且为合法 SVG")
    void buildLevelChart_containsSchemeAndLevels() {
        LevelProfile profile = new LevelProfile();
        profile.setProfileName("方案一 · 智荟成长");
        List<LevelItem> items = Arrays.asList(
                item(1, 1, 1, "智荟新芽·观", "智荟新芽", 0),
                item(2, 1, 2, "智荟新芽·悟", "智荟新芽", 100),
                item(3, 2, 1, "拾光新锐·观", "拾光新锐", 200)
        );

        String svg = service.buildLevelChart(profile, items);

        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("方案一 · 智荟成长"));
        assertTrue(svg.contains("智荟新芽·观"));
        assertTrue(svg.contains("累计 200 分"));
        assertTrue(svg.contains("Lv.3"));
        assertNotNull(service.sanitize(svg));
    }

    @Test
    @DisplayName("自动生成：素材库中不存在时插入两套等级图表资产")
    void generateLevelChartAssets_insertsChartAsset() {
        LevelProfile profile = new LevelProfile();
        profile.setId(1);
        profile.setProfileName("方案一 · 智荟成长");
        when(levelProfileMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(profile)   // 第一个方案
                .thenReturn(null);     // 第二个方案不存在
        List<LevelItem> items = Collections.singletonList(
                item(1, 1, 1, "智荟新芽·观", "智荟新芽", 0));
        when(levelItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(items);
        when(svgAssetMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ResultBean result = service.generateLevelChartAssets();

        assertEquals(200, result.getCode());
        assertEquals(1, result.getObj());
        ArgumentCaptor<SvgAsset> captor = ArgumentCaptor.forClass(SvgAsset.class);
        verify(svgAssetMapper).insert(captor.capture());
        assertEquals("等级图表 · 智荟成长", captor.getValue().getAssetName());
        assertEquals("chart", captor.getValue().getCategory());
    }

    private LevelItem item(int levelIndex, int majorIndex, int minorIndex,
                           String displayName, String majorName, int threshold) {
        LevelItem item = new LevelItem();
        item.setLevelIndex(levelIndex);
        item.setMajorIndex(majorIndex);
        item.setMinorIndex(minorIndex);
        item.setDisplayName(displayName);
        item.setMajorName(majorName);
        item.setPointsThreshold(threshold);
        return item;
    }
}
