package com.walker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.PermissionPresetMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.User;
import com.walker.service.impl.PermissionGroupServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionGroupServiceTest {

    @InjectMocks
    private PermissionGroupServiceImpl service;

    @Mock
    private PermissionGroupMapper permissionGroupMapper;

    @Mock
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Mock
    private UserPermissionGroupMapper userPermissionGroupMapper;

    @Mock
    private PermissionPresetMapper permissionPresetMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private PermissionGroup group() {
        PermissionGroup group = new PermissionGroup();
        group.setGroupCode("content_operator");
        group.setGroupName("内容运营");
        group.setScopeType("ADMIN");
        group.setEnabled(1);
        return group;
    }

    @Test
    @DisplayName("创建权限组：写入组和权限点")
    void createGroup_success() {
        PermissionGroup group = group();
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);
        when(permissionGroupMapper.insert(any(PermissionGroup.class))).thenAnswer(invocation -> {
            PermissionGroup g = invocation.getArgument(0);
            g.setId(1);
            return 1;
        });
        when(currentUserService.getCurrentUser()).thenReturn(null);

        ResultBean result = service.createGroup(group,
                Arrays.asList("article:read", "article:review"));

        assertEquals(200, result.getCode());
        assertEquals(1, result.getObj());
        verify(permissionGroupMapper).insert(any(PermissionGroup.class));
        verify(permissionGroupItemMapper, times(2)).insert(any(PermissionGroupItem.class));
    }

    @Test
    @DisplayName("创建权限组：禁止授予 user:grant:admin")
    void createGroup_forbiddenGrant() {
        PermissionGroup group = group();
        when(permissionGroupMapper.selectCount(any())).thenReturn(0L);

        ResultBean result = service.createGroup(group,
                Collections.singletonList(Permission.USER_GRANT_ADMIN.getCode()));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("不能授予"));
        verify(permissionGroupMapper, never()).insert(any(PermissionGroup.class));
    }

    @Test
    @DisplayName("创建权限组：编码重复拒绝")
    void createGroup_duplicateCode() {
        when(permissionGroupMapper.selectCount(any())).thenReturn(1L);

        ResultBean result = service.createGroup(group(),
                Collections.singletonList("article:read"));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("已存在"));
    }

    @Test
    @DisplayName("替换权限点：先删后插")
    void replaceItems_success() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);

        ResultBean result = service.replaceItems(1,
                Arrays.asList("article:read", "report:review"));

        assertEquals(200, result.getCode());
        verify(permissionGroupItemMapper).delete(any());
        verify(permissionGroupItemMapper, times(2)).insert(any(PermissionGroupItem.class));
    }

    @Test
    @DisplayName("用户加入权限组：无效用户拒绝")
    void assignUsers_invalidUser() {
        PermissionGroup group = group();
        group.setId(1);
        when(permissionGroupMapper.selectById(1)).thenReturn(group);
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        ResultBean result = service.assignUsers(1, Collections.singletonList(999));

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("无效用户"));
    }

    @Test
    @DisplayName("有效权限：返回用户与权限点")
    void effective_returnsCodes() {
        User user = new User();
        user.setId(5);
        user.setUsername("tester");
        user.setUserType("2");
        when(userMapper.selectById(5)).thenReturn(user);
        when(effectivePermissionService.getEffectiveCodes(user))
                .thenReturn(new java.util.LinkedHashSet<>(Arrays.asList("admin:access", "article:read")));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        java.util.Map<String, Object> data = service.effective(5);

        assertNotNull(data);
        assertEquals(5, data.get("userId"));
        assertTrue(((java.util.List<?>) data.get("permissions")).contains("article:read"));
    }
}
