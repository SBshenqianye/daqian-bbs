package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.impl.SaOrgServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
}
