package com.walker.service;

import com.walker.mapper.AreaMapper;
import com.walker.pojo.Area;
import com.walker.service.impl.AreaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @InjectMocks
    private AreaServiceImpl areaService;

    @Mock
    private AreaMapper areaMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = areaService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(areaService, areaMapper);
    }

    @Test
    @DisplayName("查询推荐区域 → 返回列表")
    void queryArea_returnsList() {
        Area a1 = new Area();
        a1.setAreaId(1);
        a1.setImageTitle("推荐文章");
        a1.setImageUrl("img.png");
        when(areaMapper.selectList(null)).thenReturn(Arrays.asList(a1));

        List<Area> result = areaService.queryArea();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("推荐文章", result.get(0).getImageTitle());
    }

    @Test
    @DisplayName("查询推荐区域 → 无数据返回空列表")
    void queryArea_empty() {
        when(areaMapper.selectList(null)).thenReturn(Collections.emptyList());

        List<Area> result = areaService.queryArea();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
