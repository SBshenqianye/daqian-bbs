package com.walker.service;

import com.walker.mapper.CommunityUserMapper;
import com.walker.pojo.CommunityUser;
import com.walker.service.impl.CommunityUserServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityUserServiceTest {

    @InjectMocks
    private CommunityUserServiceImpl communityUserService;

    @Mock
    private CommunityUserMapper communityUserMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = communityUserService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(communityUserService, communityUserMapper);
    }

    // ==================== saveCommunityUser ====================

    @Test
    @DisplayName("加入社区 → 首次加入 → 成功")
    void saveCommunityUser_firstTime_succeeds() {
        when(communityUserMapper.selectOne(any())).thenReturn(null);
        when(communityUserMapper.insert(any(CommunityUser.class))).thenReturn(1);

        ResultBean result = communityUserService.saveCommunityUser(1, 100);

        assertEquals(200, result.getCode());
        assertEquals("成功加入社区！", result.getMessage());
    }

    @Test
    @DisplayName("加入社区 → 已在社区中 → 返回错误")
    void saveCommunityUser_alreadyJoined_returnsError() {
        CommunityUser existing = new CommunityUser();
        existing.setId(1);
        when(communityUserMapper.selectOne(any())).thenReturn(existing);

        ResultBean result = communityUserService.saveCommunityUser(1, 100);

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已在社区内"));
    }

    // ==================== join ====================

    @Test
    @DisplayName("检查是否已加入社区 → 已加入 → 返回true")
    void join_alreadyJoined_returnsTrue() {
        CommunityUser cu = new CommunityUser();
        when(communityUserMapper.selectOne(any())).thenReturn(cu);

        boolean result = communityUserService.join(1, 100);

        assertTrue(result);
    }

    @Test
    @DisplayName("检查是否已加入社区 → 未加入 → 返回false")
    void join_notJoined_returnsFalse() {
        when(communityUserMapper.selectOne(any())).thenReturn(null);

        boolean result = communityUserService.join(1, 100);

        assertFalse(result);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("退出社区 → 成功删除 → 返回true")
    void delete_succeeds() {
        when(communityUserMapper.delete(any())).thenReturn(1);

        boolean result = communityUserService.delete(1, 100);

        assertTrue(result);
    }

    @Test
    @DisplayName("退出社区 → 无记录可删 → 返回false")
    void delete_noRecord_returnsFalse() {
        when(communityUserMapper.delete(any())).thenReturn(0);

        boolean result = communityUserService.delete(1, 100);

        assertFalse(result);
    }
}
