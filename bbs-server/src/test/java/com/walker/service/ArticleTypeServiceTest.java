package com.walker.service;

import com.walker.mapper.ArticleTypeMapper;
import com.walker.pojo.ArticleType;
import com.walker.service.impl.ArticleTypeServiceImpl;
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
class ArticleTypeServiceTest {

    @InjectMocks
    private ArticleTypeServiceImpl articleTypeService;

    @Mock
    private ArticleTypeMapper articleTypeMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = articleTypeService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(articleTypeService, articleTypeMapper);
    }

    @Test
    @DisplayName("查询所有文章类型 → 返回列表")
    void queryAllArticleType_returnsList() {
        ArticleType t1 = new ArticleType();
        t1.setTypeId(1);
        t1.setTypeName("技术");
        when(articleTypeMapper.selectList(null)).thenReturn(Arrays.asList(t1));

        List<ArticleType> result = articleTypeService.queryAllArticleType();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("技术", result.get(0).getTypeName());
    }

    @Test
    @DisplayName("查询所有文章类型 → 无数据返回空列表")
    void queryAllArticleType_empty() {
        when(articleTypeMapper.selectList(null)).thenReturn(Collections.emptyList());

        List<ArticleType> result = articleTypeService.queryAllArticleType();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
