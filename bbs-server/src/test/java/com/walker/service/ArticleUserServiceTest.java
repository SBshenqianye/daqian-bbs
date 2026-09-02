package com.walker.service;

import com.walker.mapper.ArticleUserMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleUser;
import com.walker.pojo.User;
import com.walker.service.impl.ArticleUserServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleUserServiceTest {

    @InjectMocks
    private ArticleUserServiceImpl articleUserService;

    @Mock
    private ArticleUserMapper articleUserMapper;

    @Mock
    private ArticleService articleService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = articleUserService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(articleUserService, articleUserMapper);
    }

    // ==================== saveUserCollection ====================

    @Test
    @DisplayName("收藏文章 → 首次收藏 → 成功")
    void saveUserCollection_firstTime_succeeds() {
        when(articleUserMapper.selectOne(any())).thenReturn(null);
        when(articleUserMapper.insert(any(ArticleUser.class))).thenReturn(1);
        doNothing().when(articleService).articleGoodNumPlusOne(100);

        User user = new User();
        user.setGood(0);
        when(userService.getById(1)).thenReturn(user);
        when(userService.updateById(any())).thenReturn(true);

        ResultBean result = articleUserService.saveUserCollection(1, 100);

        assertEquals(200, result.getCode());
        assertEquals("收藏成功！", result.getMessage());
        verify(articleService).articleGoodNumPlusOne(100);
        assertEquals(1, user.getGood());
    }

    @Test
    @DisplayName("收藏文章 → 重复收藏 → 返回错误")
    void saveUserCollection_duplicate_returnsError() {
        ArticleUser existing = new ArticleUser();
        existing.setId(1);
        when(articleUserMapper.selectOne(any())).thenReturn(existing);

        ResultBean result = articleUserService.saveUserCollection(1, 100);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已经收藏"));
    }

    // ==================== getMyCollection ====================

    @Test
    @DisplayName("获取我的收藏 → 返回文章列表")
    void getMyCollection_returnsArticles() {
        ArticleUser au1 = new ArticleUser();
        au1.setArticleId(100);
        ArticleUser au2 = new ArticleUser();
        au2.setArticleId(200);
        when(articleUserMapper.selectList(any())).thenReturn(Arrays.asList(au1, au2));

        Article a1 = new Article();
        a1.setArticleId(100);
        when(articleService.getArticlesByIds(Arrays.asList(100, 200)))
                .thenReturn(Arrays.asList(a1));

        List<Article> result = articleUserService.getMyCollection(1);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取我的收藏 → 无收藏 → 返回空列表")
    void getMyCollection_empty() {
        when(articleUserMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(articleService.getArticlesByIds(anyList())).thenReturn(Collections.emptyList());

        List<Article> result = articleUserService.getMyCollection(1);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getArticleUserByArticleIds ====================

    @Test
    @DisplayName("按文章ID列表查询收藏关系 → 返回结果")
    void getArticleUserByArticleIds_returnsResults() {
        ArticleUser au = new ArticleUser();
        when(articleUserMapper.selectList(any())).thenReturn(Arrays.asList(au));

        List<ArticleUser> result = articleUserService.getArticleUserByArticleIds(Arrays.asList(100, 200));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按文章ID列表查询 → null输入 → 返回null")
    void getArticleUserByArticleIds_nullInput_returnsNull() {
        List<ArticleUser> result = articleUserService.getArticleUserByArticleIds(null);

        assertNull(result);
    }

    @Test
    @DisplayName("按文章ID列表查询 → 空列表 → 返回空列表")
    void getArticleUserByArticleIds_emptyInput() {
        when(articleUserMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ArticleUser> result = articleUserService.getArticleUserByArticleIds(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
