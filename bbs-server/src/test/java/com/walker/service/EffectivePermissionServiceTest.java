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
import java.util.LinkedHashSet;
import java.util.List;
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

    private User user(int id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private PermissionGroup group(int id, int enabled) {
        PermissionGroup group = new PermissionGroup();
        group.setId(id);
        group.setEnabled(enabled);
        group.setGroupCode("group_" + id);
        group.setGroupName("权限组" + id);
        group.setDataScopeType("GLOBAL");
        return group;
    }

    private PermissionGroupItem item(int groupId, Permission permission, String effect) {
        PermissionGroupItem item = new PermissionGroupItem();
        item.setGroupId(groupId);
        item.setPermissionCode(permission.getCode());
        item.setEffect(effect);
        return item;
    }

    @Test
    @DisplayName("主权限组 + 额外绑定组：权限点并集")
    void effectiveCodes_groupUnion() {
        User user = user(5);
        user.setPrimaryGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Collections.singletonList(group(10, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(10, Permission.USER_READ, "grant"),
                item(10, Permission.PERMISSION_MANAGE, "grant")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.contains(Permission.USER_READ.getCode()));
        assertTrue(codes.contains(Permission.PERMISSION_MANAGE.getCode()));
        assertFalse(codes.contains(Permission.USER_DELETE.getCode()));
    }

    @Test
    @DisplayName("没有权限组：没有任何权限点")
    void noGroup_noPermissions() {
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Set<String> codes = service.getEffectiveCodes(user(5));

        assertTrue(codes.isEmpty());
    }

    @Test
    @DisplayName("主权限组自动生效，无需额外绑定")
    void primaryGroup_appliesWithoutExplicitBinding() {
        User user = user(5);
        user.setPrimaryGroupId(30);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Collections.singletonList(group(30, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(30, Permission.ARTICLE_READ, "grant"),
                item(30, Permission.CONFIG_WRITE, "grant")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.contains(Permission.ARTICLE_READ.getCode()));
        assertTrue(codes.contains(Permission.CONFIG_WRITE.getCode()));
    }

    @Test
    @DisplayName("停用的权限组不生效")
    void disabledGroup_ignored() {
        User user = user(5);
        user.setPrimaryGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.isEmpty());
    }

    @Test
    @DisplayName("收回优先：同组内 grant + revoke 同一权限时以收回为准")
    void revokeWinsOverGrant() {
        User user = user(5);
        user.setPrimaryGroupId(20);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Collections.singletonList(group(20, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(20, Permission.USER_READ, "grant"),
                item(20, Permission.USER_READ, "revoke")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertFalse(codes.contains(Permission.USER_READ.getCode()));
    }

    @Test
    @DisplayName("额外权限组可叠加任何组，不存在系统角色/临时组限制")
    void extraGroup_overlaysAnyGroup() {
        User user = user(5);
        user.setPrimaryGroupId(30);
        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(40);
        link.setUserId(5);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));
        when(permissionGroupMapper.selectList(any())).thenReturn(Arrays.asList(
                group(30, 1), group(40, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(30, Permission.ARTICLE_READ, "grant"),
                item(40, Permission.USER_READ, "grant")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.contains(Permission.ARTICLE_READ.getCode()));
        assertTrue(codes.contains(Permission.USER_READ.getCode()));
    }

    @Test
    @DisplayName("按权限广播：主组与额外组成员都会返回")
    void userIds_primaryAndExtraGroups() {
        User primary = user(2);
        primary.setPrimaryGroupId(10);
        User extra = user(3);
        when(userMapper.selectList(any())).thenReturn(Arrays.asList(primary, extra));

        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Collections.singletonList(group(10, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(10, Permission.MODERATOR_MANAGE, "grant")));

        UserPermissionGroup link = new UserPermissionGroup();
        link.setUserId(3);
        link.setGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        List<Integer> ids = service.getUserIdsWithPermission(Permission.MODERATOR_MANAGE);

        assertTrue(ids.contains(2));
        assertTrue(ids.contains(3));
    }

    @Test
    @DisplayName("批量计算与单人一致：跨组 revoke 优先")
    void userIds_revokePriorityMatchesSingleUser() {
        User user = user(5);
        user.setPrimaryGroupId(20);
        when(userMapper.selectList(any())).thenReturn(Collections.singletonList(user));

        UserPermissionGroup link = new UserPermissionGroup();
        link.setUserId(5);
        link.setGroupId(40);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        when(permissionGroupMapper.selectList(any())).thenReturn(Arrays.asList(
                group(20, 1), group(40, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(20, Permission.USER_READ, "revoke"),
                item(40, Permission.USER_READ, "grant")));

        Set<String> single = service.getEffectiveCodes(user);
        List<Integer> batch = service.getUserIdsWithPermission(Permission.USER_READ);

        assertFalse(single.contains(Permission.USER_READ.getCode()), "单人计算未按收回优先");
        assertFalse(batch.contains(5), "批量计算与单人计算不一致");
    }

    @Test
    @DisplayName("2000 个同组用户批量广播完整返回")
    void userIds_manyUsersSameProfile() {
        List<User> users = new java.util.ArrayList<>();
        for (int i = 1; i <= 2000; i++) {
            User u = user(i);
            u.setPrimaryGroupId(10);
            users.add(u);
        }
        when(userMapper.selectList(any())).thenReturn(users);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupMapper.selectList(any()))
                .thenReturn(Collections.singletonList(group(10, 1)));
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(10, Permission.POST_CREATE, "grant")));

        List<Integer> ids = service.getUserIdsWithPermission(Permission.POST_CREATE);

        assertEquals(2000, ids.size());
        assertTrue(ids.contains(1));
        assertTrue(ids.contains(2000));
    }

    @Test
    @DisplayName("getEffectiveGroups 返回启用中的主组与额外组")
    void effectiveGroups_returnsEnabledGroups() {
        User user = user(5);
        user.setPrimaryGroupId(10);
        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(20);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));
        when(permissionGroupMapper.selectList(any())).thenReturn(Arrays.asList(
                group(10, 1), group(20, 1)));

        List<PermissionGroup> groups = service.getEffectiveGroups(user);

        assertEquals(2, groups.size());
        Set<Integer> ids = new LinkedHashSet<>();
        groups.forEach(g -> ids.add(g.getId()));
        assertTrue(ids.contains(10));
        assertTrue(ids.contains(20));
    }
}
