package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.RankingCategoryMapper;
import com.walker.mapper.RankingCategoryOrgMapper;
import com.walker.mapper.RankingStatsMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.RankingCategory;
import com.walker.pojo.RankingCategoryOrg;
import com.walker.pojo.SaOrg;
import com.walker.service.impl.RankingCategoryServiceImpl;
import com.walker.vo.PointsRankVO;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingCategoryServiceTest {

    @InjectMocks
    private RankingCategoryServiceImpl service;

    @Mock
    private RankingCategoryMapper categoryMapper;

    @Mock
    private RankingCategoryOrgMapper categoryOrgMapper;

    @Mock
    private RankingStatsMapper rankingStatsMapper;

    @Mock
    private SaOrgMapper saOrgMapper;

    @Mock
    private ArticleService articleService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, categoryMapper);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RankingCategory.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RankingCategoryOrg.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SaOrg.class);
    }

    @Test
    @DisplayName("自定义分类 → 按人均得分排序，同时返回总分/人数")
    void customCategory_sortsByAverage() {
        RankingCategory category = new RankingCategory();
        category.setId(1);
        category.setCategoryName("测试分类");
        when(categoryMapper.selectById(1)).thenReturn(category);

        when(categoryOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(member("A", 1), member("B", 1)));

        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(org("A", "组织A"), org("B", "组织B")));

        when(rankingStatsMapper.countMembers(eq("A"), eq(true))).thenReturn(10);
        when(rankingStatsMapper.sumPoints(eq("A"), eq(true), anyString(), anyString())).thenReturn(100);
        when(rankingStatsMapper.countPosts(eq("A"), eq(true), anyString(), anyString())).thenReturn(5);
        when(rankingStatsMapper.countReplies(eq("A"), eq(true), anyString(), anyString())).thenReturn(2);

        when(rankingStatsMapper.countMembers(eq("B"), eq(true))).thenReturn(4);
        when(rankingStatsMapper.sumPoints(eq("B"), eq(true), anyString(), anyString())).thenReturn(80);
        when(rankingStatsMapper.countPosts(eq("B"), eq(true), anyString(), anyString())).thenReturn(1);
        when(rankingStatsMapper.countReplies(eq("B"), eq(true), anyString(), anyString())).thenReturn(1);

        ResultBean result = service.getOrgRank(1, "2026-11-01", "2026-11-30");

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<PointsRankVO> rows = (List<PointsRankVO>) result.getObj();
        assertEquals(2, rows.size());
        assertEquals("B", rows.get(0).getOrgNo());
        assertEquals(20.0, rows.get(0).getAvgPoints(), 0.001);
        assertEquals(80, rows.get(0).getPoints());
        assertEquals(4, rows.get(0).getMemberCount());
        assertEquals(1, rows.get(0).getRankNum());
        assertEquals("A", rows.get(1).getOrgNo());
        assertEquals(10.0, rows.get(1).getAvgPoints(), 0.001);
        assertEquals(2, rows.get(1).getRankNum());
    }

    @Test
    @DisplayName("本部 LEGACY → 沿用旧排名结果，并补充人均")
    void legacyCategory_delegatesAndEnrichesAverage() {
        PointsRankVO row = new PointsRankVO();
        row.setOrgNo("5140400");
        row.setOrgName("市区供电中心");
        row.setPoints(50);
        row.setPosts(3);
        row.setReplies(4);
        when(articleService.pointsRank(any())).thenReturn(ResultBean.success("查询成功", Arrays.asList(row)));
        when(rankingStatsMapper.countMembers(eq("5140400"), eq(true))).thenReturn(5);

        ResultBean result = service.getOrgRank(0, "2026-11-01", "2026-11-30");

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<PointsRankVO> rows = (List<PointsRankVO>) result.getObj();
        assertEquals(1, rows.size());
        assertEquals(10.0, rows.get(0).getAvgPoints(), 0.001);
        assertEquals(5, rows.get(0).getMemberCount());
        verify(articleService).pointsRank(any());
    }

    @Test
    @DisplayName("分类成员重复 → 返回错误")
    void addOrg_duplicate_returnsError() {
        RankingCategory category = new RankingCategory();
        category.setId(1);
        when(categoryMapper.selectById(1)).thenReturn(category);
        SaOrg org = org("A", "组织A");
        when(saOrgMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(org);
        when(categoryOrgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ResultBean result = service.addOrg(1, "A", true);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已在分类中"));
    }

    private RankingCategoryOrg member(String orgNo, int include) {
        RankingCategoryOrg row = new RankingCategoryOrg();
        row.setCategoryId(1);
        row.setOrgNo(orgNo);
        row.setIncludeDescendants(include);
        row.setSortOrder(0);
        return row;
    }

    private SaOrg org(String orgNo, String name) {
        SaOrg org = new SaOrg();
        org.setOrgNo(orgNo);
        org.setOrgName(name);
        org.setIsDelete(0);
        return org;
    }
}
