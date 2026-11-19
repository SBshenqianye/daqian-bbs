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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
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

    private RankingCategory category;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(service, categoryMapper);

        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RankingCategory.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RankingCategoryOrg.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SaOrg.class);

        category = new RankingCategory();
        category.setId(1);
        category.setCategoryName("测试分类");
        lenient().when(categoryMapper.selectById(1)).thenReturn(category);
    }

    @Test
    @DisplayName("自定义分类 → 按人均得分排序，同时返回总分/人数")
    void customCategory_sortsByAverage() {
        when(categoryOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(member("A", "include", 1), member("B", "include", 1)));
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(org("A", "组织A"), org("B", "组织B")));

        when(rankingStatsMapper.countMembersByOrgs(argThat(list -> list != null && list.contains("A")))).thenReturn(10);
        when(rankingStatsMapper.sumPointsByOrgs(argThat(list -> list != null && list.contains("A")), anyString(), anyString())).thenReturn(100);
        when(rankingStatsMapper.countPostsByOrgs(argThat(list -> list != null && list.contains("A")), anyString(), anyString())).thenReturn(5);
        when(rankingStatsMapper.countRepliesByOrgs(argThat(list -> list != null && list.contains("A")), anyString(), anyString())).thenReturn(2);

        when(rankingStatsMapper.countMembersByOrgs(argThat(list -> list != null && list.contains("B")))).thenReturn(4);
        when(rankingStatsMapper.sumPointsByOrgs(argThat(list -> list != null && list.contains("B")), anyString(), anyString())).thenReturn(80);
        when(rankingStatsMapper.countPostsByOrgs(argThat(list -> list != null && list.contains("B")), anyString(), anyString())).thenReturn(1);
        when(rankingStatsMapper.countRepliesByOrgs(argThat(list -> list != null && list.contains("B")), anyString(), anyString())).thenReturn(1);

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
    @DisplayName("分类没有成员 → 返回空榜，不再回落旧排名")
    void emptyMembers_returnsEmptyWithoutLegacyFallback() {
        when(categoryOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        ResultBean result = service.getOrgRank(1, "2026-11-01", "2026-11-30");

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<PointsRankVO> rows = (List<PointsRankVO>) result.getObj();
        assertTrue(rows.isEmpty());
        verifyNoInteractions(rankingStatsMapper);
    }

    @Test
    @DisplayName("上级含下级 + 部分下级未勾选 → 未勾选组织从统计集合剔除")
    void excludedSubtree_isRemovedFromParentTotals() {
        when(categoryOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(
                        member("A", "include", 1),
                        member("A1", "exclude", 1)));
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(
                        org("A", "组织A"),
                        org("A1", "组织A1", "A"),
                        org("A2", "组织A2", "A")));

        when(rankingStatsMapper.countMembersByOrgs(anyList())).thenReturn(10);
        when(rankingStatsMapper.sumPointsByOrgs(anyList(), anyString(), anyString())).thenReturn(100);
        when(rankingStatsMapper.countPostsByOrgs(anyList(), anyString(), anyString())).thenReturn(3);
        when(rankingStatsMapper.countRepliesByOrgs(anyList(), anyString(), anyString())).thenReturn(2);

        ResultBean result = service.getOrgRank(1, "2026-11-01", "2026-11-30");

        assertEquals(200, result.getCode());
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(rankingStatsMapper).countMembersByOrgs(captor.capture());
        assertEquals(Arrays.asList("A", "A2"), captor.getValue());
        assertFalse(captor.getValue().contains("A1"));
    }

    @Test
    @DisplayName("三态保存：部分选中父节点 → include 父节点 + exclude 未勾选子级")
    void saveSelection_serializesPartialSelection() {
        SaOrg parent = org("P", "父组织");
        SaOrg selected = org("P1", "子组织1", "P");
        SaOrg unselected = org("P2", "子组织2", "P");
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(parent, selected, unselected));

        ResultBean result = service.saveSelection(1, Arrays.asList("P1"), Arrays.asList("P"));

        assertEquals(200, result.getCode());
        ArgumentCaptor<RankingCategoryOrg> captor = ArgumentCaptor.forClass(RankingCategoryOrg.class);
        verify(categoryOrgMapper, times(2)).insert(captor.capture());
        List<RankingCategoryOrg> rows = captor.getAllValues();
        assertEquals("P", rows.get(0).getOrgNo());
        assertEquals("include", rows.get(0).getSelectionType());
        assertEquals(1, rows.get(0).getIncludeDescendants());
        assertEquals("P2", rows.get(1).getOrgNo());
        assertEquals("exclude", rows.get(1).getSelectionType());
    }

    @Test
    @DisplayName("三态保存：仅勾选本级且未勾选下级 → 保留 include_descendants=0")
    void saveSelection_keepsSelfOnlySelection() {
        SaOrg parent = org("P", "父组织");
        SaOrg child = org("P1", "子组织1", "P");
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(parent, child));

        ResultBean result = service.saveSelection(1, Arrays.asList("P"), Collections.emptyList());

        assertEquals(200, result.getCode());
        ArgumentCaptor<RankingCategoryOrg> captor = ArgumentCaptor.forClass(RankingCategoryOrg.class);
        verify(categoryOrgMapper).insert(captor.capture());
        assertEquals("P", captor.getValue().getOrgNo());
        assertEquals("include", captor.getValue().getSelectionType());
        assertEquals(0, captor.getValue().getIncludeDescendants());
    }

    @Test
    @DisplayName("三态保存：中间层被前端省略但深层仍勾选 → 父级仍按含下级保存")
    void saveSelection_deepCheckedDescendant_keepsSubtreeInclude() {
        SaOrg parent = org("P", "父组织");
        SaOrg middle = org("C", "中间层", "P");
        SaOrg leaf = org("G", "深层", "C");
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(parent, middle, leaf));

        ResultBean result = service.saveSelection(1, Arrays.asList("P", "G"), Collections.emptyList());

        assertEquals(200, result.getCode());
        ArgumentCaptor<RankingCategoryOrg> captor = ArgumentCaptor.forClass(RankingCategoryOrg.class);
        verify(categoryOrgMapper).insert(captor.capture());
        assertEquals("P", captor.getValue().getOrgNo());
        assertEquals(1, captor.getValue().getIncludeDescendants());
    }

    @Test
    @DisplayName("恢复默认：内置分类重置名称/说明/排序/启用状态，参与单位保持不变")
    void resetCategoryToDefault_restoresFieldsOnly() {
        category.setCategoryCode("company_units");
        category.setCategoryName("被我改坏的名字");
        category.setSortOrder(99);
        category.setEnabled(0);

        ResultBean result = service.resetCategoryToDefault(1);

        assertEquals(200, result.getCode());
        ArgumentCaptor<RankingCategory> categoryCaptor = ArgumentCaptor.forClass(RankingCategory.class);
        verify(categoryMapper).updateById(categoryCaptor.capture());
        assertEquals("公司所属各单位", categoryCaptor.getValue().getCategoryName());
        assertEquals(30, categoryCaptor.getValue().getSortOrder());
        assertEquals(1, categoryCaptor.getValue().getEnabled());
        assertTrue(categoryCaptor.getValue().getDescription().contains("直属单位"));
        verify(categoryOrgMapper, never()).insert(any());
    }

    @Test
    @DisplayName("公开分类不返回说明与编码，避免用户端看到管理端配置")
    void listPublicCategories_hidesDescriptionAndCode() {
        category.setDescription("内部说明");
        category.setCategoryCode("county_units");
        when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(category));
        when(categoryOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        ResultBean result = service.listPublicCategories();

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<com.walker.vo.RankingCategoryVO> list = (List<com.walker.vo.RankingCategoryVO>) result.getObj();
        assertEquals(1, list.size());
        assertNull(list.get(0).getDescription());
        assertNull(list.get(0).getCategoryCode());
        assertEquals("测试分类", list.get(0).getCategoryName());
    }

    @Test
    @DisplayName("恢复默认成员：按内置种子口径返回参与单位")
    void getDefaultMembers_usesSeedScope() {
        category.setCategoryCode("company_units");
        SaOrg root = org("5140403", "公司所属各单位");
        SaOrg childA = org("514040301", "供电服务指挥中心", "5140403");
        SaOrg childB = org("514040302", "信息通信分公司", "5140403");
        SaOrg other = org("5140404", "内江星原公司", "5140403");
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(root, childA, childB, other));

        ResultBean result = service.getDefaultMembers(1);

        assertEquals(200, result.getCode());
        @SuppressWarnings("unchecked")
        List<String> orgNos = (List<String>) result.getObj();
        assertEquals(Arrays.asList("514040301", "514040302", "5140404"), orgNos);
    }

    @Test
    @DisplayName("拖动排序：按传入顺序批量写入 sortOrder")
    void updateSortOrders_writesEachCategory() {
        com.walker.vo.param.RankingSortItem first = new com.walker.vo.param.RankingSortItem();
        first.setId(1);
        first.setSortOrder(10);
        com.walker.vo.param.RankingSortItem second = new com.walker.vo.param.RankingSortItem();
        second.setId(2);
        second.setSortOrder(20);

        ResultBean result = service.updateSortOrders(Arrays.asList(first, second));

        assertEquals(200, result.getCode());
        ArgumentCaptor<RankingCategory> captor = ArgumentCaptor.forClass(RankingCategory.class);
        verify(categoryMapper, times(2)).updateById(captor.capture());
        assertEquals(Integer.valueOf(1), captor.getAllValues().get(0).getId());
        assertEquals(Integer.valueOf(10), captor.getAllValues().get(0).getSortOrder());
        assertEquals(Integer.valueOf(20), captor.getAllValues().get(1).getSortOrder());
    }

    @Test
    @DisplayName("分类成员重复 → 返回错误")
    void addOrg_duplicate_returnsError() {
        SaOrg org = org("A", "组织A");
        when(saOrgMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(org);
        when(categoryOrgMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        ResultBean result = service.addOrg(1, "A", true);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已在分类中"));
    }

    private RankingCategoryOrg member(String orgNo, String type, int include) {
        RankingCategoryOrg row = new RankingCategoryOrg();
        row.setCategoryId(1);
        row.setOrgNo(orgNo);
        row.setSelectionType(type);
        row.setIncludeDescendants(include);
        row.setSortOrder(0);
        return row;
    }

    private SaOrg org(String orgNo, String name) {
        return org(orgNo, name, null);
    }

    private SaOrg org(String orgNo, String name, String parent) {
        SaOrg org = new SaOrg();
        org.setOrgNo(orgNo);
        org.setOrgName(name);
        org.setPOrgNo(parent);
        org.setIsDelete(0);
        return org;
    }
}
