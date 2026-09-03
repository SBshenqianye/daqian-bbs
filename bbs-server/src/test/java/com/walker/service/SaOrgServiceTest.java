package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.impl.SaOrgServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaOrgServiceTest {

    @InjectMocks
    private SaOrgServiceImpl saOrgService;

    @Mock
    private SaOrgMapper saOrgMapper;

    @Mock
    private UserMapper userMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = saOrgService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(saOrgService, saOrgMapper);

        // 注册实体元数据（Service 使用 LambdaQueryWrapper 需要）
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SaOrg.class
        );
    }

    @Test
    @DisplayName("删除单位 → 有子单位 → 返回错误")
    void deleteSaOrg_hasChildren_returnsError() {
        // First call: children query returns non-empty
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(new SaOrg()));

        ResultBean result = saOrgService.deleteSaOrgByOrgNo("5140401");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("删除单位 → 无子单位无用户 → 删除成功")
    void deleteSaOrg_noChildrenNoUsers_succeeds() {
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>())  // no children
                .thenReturn(new ArrayList<>()); // no users
        when(saOrgMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        ResultBean result = saOrgService.deleteSaOrgByOrgNo("5140401");
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("获取组织树 → 空数据 → 返回空列表")
    void getOrgTree_empty_returnsEmptyList() {
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        List<?> result = saOrgService.getOrgTree();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("解析显示组织名 → orgNo为空 → 返回fallback")
    void resolveDisplayOrgName_emptyOrgNo_returnsFallback() {
        assertEquals("默认名", saOrgService.resolveDisplayOrgName("", "默认名"));
        assertEquals("默认名", saOrgService.resolveDisplayOrgName(null, "默认名"));
    }

    @Test
    @DisplayName("解析组织路径 → orgNo为空 → 返回null")
    void resolveOrgPath_emptyOrgNo_returnsNull() {
        assertNull(saOrgService.resolveOrgPath(""));
        assertNull(saOrgService.resolveOrgPath(null));
    }

    @Test
    @DisplayName("删除单位 → 有人员注册 → 返回错误")
    void deleteSaOrg_hasUsers_returnsError() {
        // deleteSaOrgByOrgNo calls saOrgMapper.selectList for children, then userMapper.selectList for users
        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>()); // no children
        when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(new User())); // has users
        ResultBean result = saOrgService.deleteSaOrgByOrgNo("5140401");
        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("人员"));
    }

    @Test
    @DisplayName("获取组织树 → 有数据 → 构建树结构")
    void getOrgTree_withData_buildsTree() {
        SaOrg parent = new SaOrg();
        parent.setOrgNo("51404");
        parent.setOrgName("内江市公司");
        parent.setPOrgNo("");
        parent.setIsDelete(0);
        parent.setOrgTree("51404");

        SaOrg child = new SaOrg();
        child.setOrgNo("5140401");
        child.setOrgName("市中区分公司");
        child.setPOrgNo("51404");
        child.setIsDelete(0);
        child.setOrgTree("51404|5140401");

        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(parent, child));

        List<?> result = saOrgService.getOrgTree();
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("添加单位 → 正常添加成功")
    void addSaOrg_valid_succeeds() {
        SaOrg existing = new SaOrg();
        existing.setId(1);
        existing.setOrgNo("51404");
        existing.setPOrgNo("");
        existing.setOrgTree("51404");
        existing.setIsDelete(0);

        // this.list() passes EmptyWrapper (not null, not LambdaQueryWrapper) to baseMapper.selectList()
        when(saOrgMapper.selectList(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(existing)));
        when(saOrgMapper.insert(any(SaOrg.class))).thenReturn(1);

        ResultBean result = saOrgService.addSaOrg("51404", "新单位");
        assertEquals(200, result.getCode());
        verify(saOrgMapper).insert(any(SaOrg.class));
    }

    @Test
    @DisplayName("解析显示组织名 → 有数据 → 返回解析结果")
    void resolveDisplayOrgNames_withData_returnsResolved() {
        SaOrg org = new SaOrg();
        org.setOrgNo("5140401");
        org.setOrgName("市中区分公司");
        org.setIsDisplaySelected(1);
        org.setOrgTree("51404|5140401");

        SaOrg root = new SaOrg();
        root.setOrgNo("51404");
        root.setOrgName("内江市公司");
        root.setIsDisplaySelected(0);
        root.setOrgTree("51404");

        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(new ArrayList<>(Arrays.asList(org, root)));

        java.util.Map<String, String> result = saOrgService.resolveDisplayOrgNames(Arrays.asList("5140401"));
        assertEquals("市中区分公司", result.get("5140401"));
    }

    // ========== resolveOrgPath 补充测试 ==========

    @Test
    @DisplayName("解析组织路径 → 有效orgNo → 返回完整路径 'A > B > C'")
    void resolveOrgPath_validOrg_returnsPath() {
        SaOrg root = new SaOrg();
        root.setOrgNo("51404");
        root.setOrgName("A");
        root.setPOrgNo("");

        SaOrg mid = new SaOrg();
        mid.setOrgNo("5140401");
        mid.setOrgName("B");
        mid.setPOrgNo("51404");

        SaOrg leaf = new SaOrg();
        leaf.setOrgNo("514040101");
        leaf.setOrgName("C");
        leaf.setPOrgNo("5140401");

        when(saOrgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(root, mid, leaf));

        String path = saOrgService.resolveOrgPath("514040101");
        assertEquals("A > B > C", path);
    }
}
