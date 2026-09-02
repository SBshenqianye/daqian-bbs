package com.walker.service;

import com.walker.mapper.ArticleFileMapper;
import com.walker.pojo.ArticleFile;
import com.walker.service.impl.ArticleFileServiceImpl;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleFileServiceTest {

    @InjectMocks
    private ArticleFileServiceImpl articleFileService;

    @Mock
    private ArticleFileMapper articleFileMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = articleFileService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(articleFileService, articleFileMapper);
    }

    // ==================== addArticleFile ====================

    @Test
    @DisplayName("添加文章附件 → 成功")
    void addArticleFile_succeeds() {
        ArticleFile file = new ArticleFile();
        file.setFileName("test.pdf");
        when(articleFileMapper.insert(any(ArticleFile.class))).thenReturn(1);

        int result = articleFileService.addArticleFile(file);

        assertEquals(1, result);
    }

    // ==================== updateArticleFile ====================

    @Test
    @DisplayName("更新文章附件绑定 → 成功")
    void updateArticleFile_succeeds() {
        Integer[] fileIds = {1, 2};
        when(articleFileMapper.updateArticleFile(fileIds, 100)).thenReturn(2);

        int result = articleFileService.updateArticleFile(fileIds, 100);

        assertEquals(2, result);
    }

    // ==================== getArticleFileByArticleId ====================

    @Test
    @DisplayName("按文章ID查询附件 → 返回列表")
    void getArticleFileByArticleId_returnsList() {
        ArticleFile f1 = new ArticleFile();
        f1.setFileId(1);
        f1.setArticleId(100);
        when(articleFileMapper.selectList(any())).thenReturn(Arrays.asList(f1));

        List<ArticleFile> result = articleFileService.getArticleFileByArticleId(100);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按文章ID查询附件 → 无附件返回空列表")
    void getArticleFileByArticleId_empty() {
        when(articleFileMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ArticleFile> result = articleFileService.getArticleFileByArticleId(999);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== unBindArticleFile ====================

    @Test
    @DisplayName("解除文章附件绑定 → 成功")
    void unBindArticleFile_succeeds() {
        when(articleFileMapper.unBindArticleFile(100)).thenReturn(2);

        int result = articleFileService.unBindArticleFile(100);

        assertEquals(2, result);
    }

    // ==================== getArticleFileByKeywords ====================

    @Test
    @DisplayName("按关键词搜索附件 → 返回匹配结果")
    void getArticleFileByKeywords_returnsResults() {
        ArticleFile f1 = new ArticleFile();
        f1.setFileName("测试文档.pdf");
        when(articleFileMapper.selectList(any())).thenReturn(Arrays.asList(f1));

        List<ArticleFile> result = articleFileService.getArticleFileByKeywords("测试");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按关键词搜索附件 → 无匹配返回空列表")
    void getArticleFileByKeywords_noMatch() {
        when(articleFileMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ArticleFile> result = articleFileService.getArticleFileByKeywords("不存在");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
