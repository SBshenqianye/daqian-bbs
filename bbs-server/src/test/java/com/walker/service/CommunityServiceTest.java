package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.CommunityMapper;
import com.walker.pojo.Community;
import com.walker.service.impl.CommunityServiceImpl;
import com.walker.vo.CommunityVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommunityParam;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
class CommunityServiceTest {

    @InjectMocks
    private CommunityServiceImpl communityService;

    @Mock
    private CommunityMapper communityMapper;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = communityService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(communityService, communityMapper);
        // 注册实体元数据以支持 LambdaQueryWrapper
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Community.class);
    }

    // ==================== queryAllCommunity ====================

    @Test
    @DisplayName("查询所有启用社区 → 返回社区列表")
    void queryAllCommunity_returnsList() {
        Community c1 = new Community();
        c1.setCommunityId(1);
        c1.setCommunityName("测试社区");
        when(communityMapper.selectList(any())).thenReturn(Arrays.asList(c1));

        List<Community> result = communityService.queryAllCommunity();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试社区", result.get(0).getCommunityName());
    }

    @Test
    @DisplayName("查询所有启用社区 → 无数据返回空列表")
    void queryAllCommunity_empty() {
        when(communityMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Community> result = communityService.queryAllCommunity();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== queryAllCommunityList ====================

    @Test
    @DisplayName("查询社区列表VO → 有创建用户")
    void queryAllCommunityList_withUser() {
        Community c = new Community();
        c.setCommunityId(1);
        c.setCommunityName("测试");
        c.setCommunityImage("img.png");
        c.setCommunityIntroduce("介绍");
        c.setCreateUserId(100);
        when(communityMapper.selectList(any())).thenReturn(Arrays.asList(c));

        com.walker.pojo.User user = new com.walker.pojo.User();
        user.setNickname("张三");
        when(userService.queryUserinfoById(100)).thenReturn(user);

        List<CommunityVO> result = communityService.queryAllCommunityList();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getCreateUserNickname());
    }

    @Test
    @DisplayName("查询社区列表VO → 创建用户不存在")
    void queryAllCommunityList_noUser() {
        Community c = new Community();
        c.setCommunityId(1);
        c.setCreateUserId(999);
        when(communityMapper.selectList(any())).thenReturn(Arrays.asList(c));
        when(userService.queryUserinfoById(999)).thenReturn(null);

        List<CommunityVO> result = communityService.queryAllCommunityList();

        assertNotNull(result);
        assertEquals("未知用户", result.get(0).getCreateUserNickname());
    }

    @Test
    @DisplayName("查询社区列表VO → 无社区数据")
    void queryAllCommunityList_empty() {
        when(communityMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<CommunityVO> result = communityService.queryAllCommunityList();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== queryCommunityById ====================

    @Test
    @DisplayName("按ID查询社区 → 返回社区")
    void queryCommunityById_found() {
        Community c = new Community();
        c.setCommunityId(1);
        when(communityMapper.selectById(1)).thenReturn(c);

        Community result = communityService.queryCommunityById(1);

        assertNotNull(result);
        assertEquals(1, result.getCommunityId());
    }

    @Test
    @DisplayName("按ID查询社区 → 不存在返回null")
    void queryCommunityById_notFound() {
        when(communityMapper.selectById(999)).thenReturn(null);

        Community result = communityService.queryCommunityById(999);

        assertNull(result);
    }

    // ==================== createCommunity ====================

    @Test
    @DisplayName("创建社区 → 成功")
    void createCommunity_succeeds() {
        when(communityMapper.insert(any(Community.class))).thenReturn(1);

        CommunityParam param = new CommunityParam();
        param.setCommunityName("新社区");
        param.setCommunityDesc("描述");
        param.setCreateUserId(1);
        param.setImage("img.png");

        ResultBean result = communityService.createCommunity(param);

        assertEquals(200, result.getCode());
        verify(communityMapper).insert(any(Community.class));
    }

    // ==================== getAllCommunity ====================

    @Test
    @DisplayName("获取所有社区 → 返回列表")
    void getAllCommunity_returnsList() {
        Community c = new Community();
        c.setCommunityId(1);
        when(communityMapper.selectList(null)).thenReturn(Arrays.asList(c));

        ResultBean result = communityService.getAllCommunity();

        assertEquals(200, result.getCode());
        assertNotNull(result.getObj());
    }

    // ==================== getCommunityByKeywords ====================

    @Test
    @DisplayName("关键词搜索社区 → 返回匹配结果")
    void getCommunityByKeywords_returnsResults() {
        Community c = new Community();
        c.setCommunityName("测试");
        when(communityMapper.selectList(any())).thenReturn(Arrays.asList(c));

        ResultBean result = communityService.getCommunityByKeywords("测试");

        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("关键词搜索社区 → 无匹配")
    void getCommunityByKeywords_noMatch() {
        when(communityMapper.selectList(any())).thenReturn(Collections.emptyList());

        ResultBean result = communityService.getCommunityByKeywords("不存在");

        assertEquals(200, result.getCode());
    }

    // ==================== updateCommunityStatus ====================

    @Test
    @DisplayName("更新社区状态 → 启用→禁用")
    void updateCommunityStatus_enableToDisable() {
        Community c = new Community();
        c.setCommunityId(1);
        c.setEnable(1);
        when(communityMapper.selectById(1)).thenReturn(c);
        when(communityMapper.updateById(any())).thenReturn(1);

        ResultBean result = communityService.updateCommunityStatus(1);

        assertEquals(200, result.getCode());
        assertEquals(0, c.getEnable());
    }

    @Test
    @DisplayName("更新社区状态 → 禁用→启用")
    void updateCommunityStatus_disableToEnable() {
        Community c = new Community();
        c.setCommunityId(1);
        c.setEnable(0);
        when(communityMapper.selectById(1)).thenReturn(c);
        when(communityMapper.updateById(any())).thenReturn(1);

        ResultBean result = communityService.updateCommunityStatus(1);

        assertEquals(200, result.getCode());
        assertEquals(1, c.getEnable());
    }

    // ==================== deleteCommunityByCommunityId ====================

    @Test
    @DisplayName("删除社区 → 成功")
    void deleteCommunityByCommunityId_succeeds() {
        when(communityMapper.deleteById(1)).thenReturn(1);

        ResultBean result = communityService.deleteCommunityByCommunityId(1);

        assertEquals(200, result.getCode());
        verify(communityMapper).deleteById(1);
    }

    // ==================== getCommunityCount ====================

    @Test
    @DisplayName("获取社区总数 → 返回数量")
    void getCommunityCount_returnsCount() {
        when(communityMapper.selectCount(null)).thenReturn(5L);

        ResultBean result = communityService.getCommunityCount();

        assertEquals(200, result.getCode());
        assertEquals(5L, result.getObj());
    }
}
