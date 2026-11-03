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
    @DisplayName("SUPER_ADMIN 保留关键权限，其余权限可被权限组收回")
    void superAdmin_appliesRevokesButKeepsProtected() {
        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(10);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        PermissionGroup group = new PermissionGroup();
        group.setId(10);
        group.setEnabled(1);
        when(permissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(group));

        PermissionGroupItem revokeArticleDelete = new PermissionGroupItem();
        revokeArticleDelete.setGroupId(10);
        revokeArticleDelete.setPermissionCode(Permission.ARTICLE_DELETE.getCode());
        revokeArticleDelete.setEffect("revoke");

        PermissionGroupItem revokeProtected = new PermissionGroupItem();
        revokeProtected.setGroupId(10);
        revokeProtected.setPermissionCode(Permission.PERMISSION_MANAGE.getCode());
        revokeProtected.setEffect("revoke");

        when(permissionGroupItemMapper.selectList(any()))
                .thenReturn(Arrays.asList(revokeArticleDelete, revokeProtected));

        Set<String> codes = service.getEffectiveCodes(user(1, "3"));
        assertFalse(codes.contains("article:delete"));
        assertTrue(codes.contains("permission:manage"));
        assertEquals(Permission.values().length - 1, codes.size());
    }

    private PermissionGroup systemGroup(int id, String baseRole, int enabled) {
        PermissionGroup group = new PermissionGroup();
        group.setId(id);
        group.setBaseRole(baseRole);
        group.setEnabled(enabled);
        group.setGroupCode("role_" + baseRole.toLowerCase());
        group.setGroupName(baseRole);
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
    @DisplayName("主组自动生效：无需显式绑定，角色基线以主组 baseRole 为准")
    void primaryGroup_appliesWithoutExplicitBinding() {
        User user = user(5, "4");
        user.setPrimaryGroupId(30);
        when(permissionGroupMapper.selectById(30)).thenReturn(systemGroup(30, "ADMIN", 1));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(30, Permission.CONFIG_WRITE, "grant")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.contains(Permission.USER_READ.getCode()));
        assertTrue(codes.contains(Permission.CONFIG_WRITE.getCode()));
        assertTrue(codes.contains(Permission.FEATURED_REVIEW.getCode()), "应使用主组 ADMIN 基线而非 user_type MODERATOR");
    }

    @Test
    @DisplayName("主组为空：回退 user_type 角色基线")
    void noPrimaryGroup_fallsBackToUserType() {
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Set<String> codes = service.getEffectiveCodes(user(5, "4"));

        assertTrue(codes.contains(Permission.ARTICLE_READ.getCode()));
        assertFalse(codes.contains(Permission.USER_READ.getCode()));
        assertFalse(codes.contains(Permission.FEATURED_REVIEW.getCode()));
    }

    @Test
    @DisplayName("主组停用：回退 user_type 角色基线")
    void disabledPrimaryGroup_fallsBackToUserType() {
        User user = user(5, "4");
        user.setPrimaryGroupId(30);
        when(permissionGroupMapper.selectById(30)).thenReturn(systemGroup(30, "ADMIN", 0));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());

        Set<String> codes = service.getEffectiveCodes(user);

        assertFalse(codes.contains(Permission.USER_READ.getCode()));
        assertTrue(codes.contains(Permission.ARTICLE_READ.getCode()));
    }

    @Test
    @DisplayName("收回优先：同组内 grant + revoke 同一权限时以收回为准")
    void revokeWinsOverGrant() {
        User user = user(5, "2");
        user.setPrimaryGroupId(20);
        when(permissionGroupMapper.selectById(20)).thenReturn(systemGroup(20, "ADMIN", 1));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Arrays.asList(
                item(20, Permission.USER_READ, "grant"),
                item(20, Permission.USER_READ, "revoke")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertFalse(codes.contains(Permission.USER_READ.getCode()));
        assertTrue(codes.contains(Permission.ARTICLE_READ.getCode()));
    }

    @Test
    @DisplayName("临时自定义组叠加：系统组不参与手动绑定")
    void temporaryGroup_overlaysCustomOnly() {
        User user = user(5, "4");
        user.setPrimaryGroupId(30);
        when(permissionGroupMapper.selectById(30)).thenReturn(systemGroup(30, "MODERATOR", 1));

        UserPermissionGroup link = new UserPermissionGroup();
        link.setGroupId(40);
        link.setUserId(5);
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.singletonList(link));

        PermissionGroup temp = new PermissionGroup();
        temp.setId(40);
        temp.setEnabled(1);
        temp.setBaseRole(null);
        PermissionGroup manualSystem = new PermissionGroup();
        manualSystem.setId(31);
        manualSystem.setEnabled(1);
        manualSystem.setBaseRole("ADMIN");
        when(permissionGroupMapper.selectList(any())).thenReturn(Arrays.asList(manualSystem, temp));

        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(40, Permission.USER_READ, "grant")));

        Set<String> codes = service.getEffectiveCodes(user);

        assertTrue(codes.contains(Permission.USER_READ.getCode()));
        assertFalse(codes.contains(Permission.USER_DELETE.getCode()), "手动绑定的系统组不应参与叠加");
    }

    @Test
    @DisplayName("超管主组异常时仍保留 4 个关键权限")
    void superAdmin_mismatchedPrimaryGroup_keepsProtected() {
        User user = user(1, "3");
        user.setPrimaryGroupId(20);
        when(permissionGroupMapper.selectById(20)).thenReturn(systemGroup(20, "ADMIN", 1));
        when(userPermissionGroupMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(permissionGroupItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                item(20, Permission.ADMIN_ACCESS, "revoke")));

        Set<String> codes = service.getEffectiveCodes(user);

        for (Permission permission : Permission.values()) {
            if (permission.isSuperAdminProtected()) {
                assertTrue(codes.contains(permission.getCode()), "关键权限丢失: " + permission.getCode());
            }
        }
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
