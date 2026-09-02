package com.walker.service;

import com.walker.mapper.FansMapper;
import com.walker.pojo.Fans;
import com.walker.pojo.User;
import com.walker.service.impl.FansServiceImpl;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FansServiceTest {

    @InjectMocks
    private FansServiceImpl fansService;

    @Mock
    private FansMapper fansMapper;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = fansService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(fansService, fansMapper);
    }

    // ==================== cancelFans ====================

    @Test
    @DisplayName("取消关注 → 关注关系存在 → 成功")
    void cancelFans_exists_succeeds() {
        Fans fans = new Fans();
        fans.setId(1);
        fans.setUserId(10);
        fans.setAttentionId(20);
        when(fansMapper.selectOne(any())).thenReturn(fans);
        when(fansMapper.deleteById(1)).thenReturn(1);

        User user10 = new User();
        user10.setAttention(5);
        User user20 = new User();
        user20.setFans(3);
        when(userService.getById(10)).thenReturn(user10);
        when(userService.getById(20)).thenReturn(user20);
        when(userService.updateById(any())).thenReturn(true);

        boolean result = fansService.cancelFans(10, 20);

        assertTrue(result);
        assertEquals(4, user10.getAttention());
        assertEquals(2, user20.getFans());
    }

    @Test
    @DisplayName("取消关注 → 关注关系不存在 → 返回false")
    void cancelFans_notExists_returnsFalse() {
        when(fansMapper.selectOne(any())).thenReturn(null);

        boolean result = fansService.cancelFans(10, 20);

        assertFalse(result);
    }

    // ==================== saveForm ====================

    @Test
    @DisplayName("保存关注 → 成功")
    void saveForm_succeeds() {
        Fans form = new Fans();
        form.setUserId(10);
        form.setAttentionId(20);
        when(fansMapper.insert(any(Fans.class))).thenReturn(1);

        User user10 = new User();
        user10.setAttention(5);
        User user20 = new User();
        user20.setFans(3);
        when(userService.getById(10)).thenReturn(user10);
        when(userService.getById(20)).thenReturn(user20);
        when(userService.updateById(any())).thenReturn(true);

        boolean result = fansService.saveForm(form);

        assertTrue(result);
        assertNotNull(form.getCreateTime());
        assertEquals(6, user10.getAttention());
        assertEquals(4, user20.getFans());
    }

    // ==================== getFansInfo ====================

    @Test
    @DisplayName("获取关注信息 → 关注存在 → 返回true")
    void getFansInfo_exists_returnsTrue() {
        Fans fans = new Fans();
        fans.setId(1);
        when(fansMapper.selectOne(any())).thenReturn(fans);

        boolean result = fansService.getFansInfo(10, 20);

        assertTrue(result);
    }

    @Test
    @DisplayName("获取关注信息 → 关注不存在 → 返回false")
    void getFansInfo_notExists_returnsFalse() {
        when(fansMapper.selectOne(any())).thenReturn(null);

        boolean result = fansService.getFansInfo(10, 20);

        assertFalse(result);
    }

    // ==================== getAttentionList ====================

    @Test
    @DisplayName("获取关注列表 → 返回用户信息")
    void getAttentionList_returnsUsers() {
        Fans fans = new Fans();
        fans.setUserId(10);
        fans.setAttentionId(20);
        fans.setCreateTime("2024-01-01");
        when(fansMapper.selectList(any())).thenReturn(Arrays.asList(fans));

        User user = new User();
        user.setNickname("张三");
        user.setPortrait("avatar.png");
        when(userService.getById(20)).thenReturn(user);

        List<Map<String, String>> result = fansService.getAttentionList(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).get("name"));
    }

    @Test
    @DisplayName("获取关注列表 → 无关注 → 返回空列表")
    void getAttentionList_empty() {
        when(fansMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, String>> result = fansService.getAttentionList(10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getFansList ====================

    @Test
    @DisplayName("获取粉丝列表 → 返回用户信息")
    void getFansList_returnsUsers() {
        Fans fans = new Fans();
        fans.setUserId(30);
        fans.setAttentionId(10);
        fans.setCreateTime("2024-01-01");
        when(fansMapper.selectList(any())).thenReturn(Arrays.asList(fans));

        User user = new User();
        user.setNickname("李四");
        user.setPortrait("avatar2.png");
        when(userService.getById(30)).thenReturn(user);

        List<Map<String, String>> result = fansService.getFansList(10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("李四", result.get(0).get("name"));
    }

    @Test
    @DisplayName("获取粉丝列表 → 无粉丝 → 返回空列表")
    void getFansList_empty() {
        when(fansMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, String>> result = fansService.getFansList(10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
