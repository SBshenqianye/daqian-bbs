package com.walker.service;

import com.walker.mapper.ArticleLabelMapper;
import com.walker.pojo.ArticleLabel;
import com.walker.service.impl.ArticleLabelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleLabelServiceTest {

    @InjectMocks
    private ArticleLabelServiceImpl articleLabelService;

    @Mock
    private ArticleLabelMapper articleLabelMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = articleLabelService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(articleLabelService, articleLabelMapper);
    }

    @Test
    @DisplayName("查询所有启用标签 → 返回列表")
    void queryAllArticleLabel_returnsList() {
        ArticleLabel label = new ArticleLabel();
        label.setLabelId(1);
        label.setLabelName("技术分享");
        when(articleLabelMapper.selectList(any())).thenReturn(Arrays.asList(label));

        List<ArticleLabel> result = articleLabelService.queryAllArticleLabel();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("技术分享", result.get(0).getLabelName());
    }

    @Test
    @DisplayName("查询所有启用标签 → 无数据返回空列表")
    void queryAllArticleLabel_empty() {
        when(articleLabelMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ArticleLabel> result = articleLabelService.queryAllArticleLabel();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Disabled("existsByLabelName 使用 lambdaQuery()，需要 Spring 上下文")
    @Test
    @DisplayName("检查标签名是否存在 → 需要lambdaQuery")
    void existsByLabelName_needsSpring() {
    }

    @Disabled("existsByLabelNameExcludeId 使用 lambdaQuery()，需要 Spring 上下文")
    @Test
    @DisplayName("检查标签名是否存在(排除自身) → 需要lambdaQuery")
    void existsByLabelNameExcludeId_needsSpring() {
    }

    @Test
    @DisplayName("分页查询标签 → 返回PageInfo")
    void getAllArticleLabelByPageAndSearch_returnsPageInfo() {
        ArticleLabel label = new ArticleLabel();
        label.setLabelId(1);
        label.setLabelName("技术");
        when(articleLabelMapper.selectList(any())).thenReturn(Arrays.asList(label));

        com.github.pagehelper.PageInfo<ArticleLabel> result = articleLabelService.getAllArticleLabelByPageAndSearch(1, 10, "技术");

        assertNotNull(result);
        assertEquals(1, result.getList().size());
    }
}
