package com.walker.service;

import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.User;
import com.walker.pojo.UserPermissionGroup;
import com.walker.service.impl.EffectivePermissionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EffectivePermissionServiceTest {

    @InjectMocks
    private EffectivePermissionServiceImpl service;

    @Mock
    private PermissionGroupMapper permissionGroupMapper;

    @Mock
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Mock
    private UserPermissionGroupMapper userPermissionGroupMapper;

    @Mock
    private UserMapper userMapper;

    private User user(int id, String type) {
        User user = new User();
        user.setId(id);
        user.setUserType(type);
        return user;
    }

    @Test
    @DisplayName("角色基线 + 权限组 grant 并集")
    void effectiveCodes_rolePlusGroup() {
        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        PermissionGroup group = new PermissionGroup();
        group.setId(10);
        group.setEnabled(1);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(group));

        PermissionGroupItem item = new PermissionGroupItem();
        item.setGroupId(10);
        item.setPermissionCode(Permission.PERMISSION_MANAGE.getCode());
        item.setEffect("grant");
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        Set<String> codes = service.getEffectiveCodes(user(5, "2"));
        assertTrue(codes.contains("admin:access"));
        assertTrue(codes.contains("permission:manage"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 直接返回全量，不再查权限组")
    void superAdmin_skipsGroups() {
        Set<String> codes = service.getEffectiveCodes(user(1, "3"));
        assertEquals(Permission.values().length, codes.size());
        verifyNoInteractions(userPermissionGroupMapper);
    }

    @Test
    @DisplayName("禁用权限组不生效")
    void disabledGroup_ignored() {
        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        PermissionGroup group = new PermissionGroup();
        group.setId(10);
        group.setEnabled(0);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Set<String> codes = service.getEffectiveCodes(user(5, "2"));
        assertFalse(codes.contains("permission:manage"));
    }

    @Test
    @DisplayName("按权限广播：角色基线与权限组用户都会返回")
    void userIds_roleAndGroup() {
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(user(2, "2"), user(3, "3")));

        PermissionGroupItem item = new PermissionGroupItem();
        item.setGroupId(10);
        item.setPermissionCode(Permission.MODERATOR_MANAGE.getCode());
        item.setEffect("grant");
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        PermissionGroup group = new PermissionGroup();
        group.setId(10);
        group.setEnabled(1);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(group));

        UserPermissionGroup link = new UserPermissionGroup();
        link.setUserId(99);
        link.setGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        java.util.List<Integer> ids = service.getUserIdsWithPermission(Permission.MODERATOR_MANAGE);
        assertTrue(ids.contains(2));
        assertTrue(ids.contains(3));
        assertTrue(ids.contains(99));
    }
}
