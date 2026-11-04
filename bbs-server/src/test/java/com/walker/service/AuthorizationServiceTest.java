package com.walker.service;

import com.walker.config.security.ForbiddenException;
import com.walker.config.security.Permission;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.User;
import com.walker.service.impl.AuthorizationServiceImpl;
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

/**
 * 统一授权服务测试：身份与数据范围均来自权限组。
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @Mock
    private BoardModeratorService boardModeratorService;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    private User user(int id, Integer primaryGroupId) {
        User user = new User();
        user.setId(id);
        user.setPrimaryGroupId(primaryGroupId);
        return user;
    }

    private PermissionGroup group(Integer id, String dataScopeType) {
        PermissionGroup group = new PermissionGroup();
        group.setId(id);
        group.setGroupCode("group_" + id);
        group.setGroupName("权限组" + id);
        group.setDataScopeType(dataScopeType);
        group.setEnabled(1);
        return group;
    }

    private void grant(User user, PermissionGroup group, Permission... permissions) {
        Set<String> codes = new LinkedHashSet<>();
        for (Permission permission : permissions) {
            codes.add(permission.getCode());
        }
        lenient().when(effectivePermissionService.getEffectiveCodes(user)).thenReturn(codes);
        lenient().when(effectivePermissionService.getEffectiveGroups(user)).thenReturn(
                group == null ? Collections.emptyList() : Collections.singletonList(group));
    }

    @Test
    @DisplayName("isSuperAdmin 依据权限组授予的 user:grant:admin")
    void isSuperAdmin_byPermissionNotId() {
        User superUser = user(99, 10);
        grant(superUser, group(10, "GLOBAL"), Permission.USER_GRANT_ADMIN);
        assertTrue(authorizationService.isSuperAdmin(superUser));

        User normal = user(1, 40);
        grant(normal, group(40, "GLOBAL"), Permission.ADMIN_ACCESS);
        assertFalse(authorizationService.isSuperAdmin(normal));
    }

    @Test
    @DisplayName("isSuperAdmin(userId) 当前登录人优先使用登录态")
    void isSuperAdmin_byUserId_usesCurrentUser() {
        User current = user(7, 10);
        grant(current, group(10, "GLOBAL"), Permission.USER_GRANT_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(current);

        assertTrue(authorizationService.isSuperAdmin(7));
        verify(userService, never()).getById(anyInt());
    }

    @Test
    @DisplayName("isSuperAdmin(userId) 非当前用户时查询用户表")
    void isSuperAdmin_byUserId_loadsUser() {
        User current = user(7, 40);
        User target = user(8, 10);
        grant(current, group(40, "GLOBAL"), Permission.ADMIN_ACCESS);
        grant(target, group(10, "GLOBAL"), Permission.USER_GRANT_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(current);
        when(userService.getById(8)).thenReturn(target);

        assertTrue(authorizationService.isSuperAdmin(8));
    }

    @Test
    @DisplayName("hasPermission：完全由有效权限点决定")
    void hasPermission_matrix() {
        User admin = user(2, 20);
        grant(admin, group(20, "GLOBAL"),
                Permission.ADMIN_ACCESS, Permission.ARTICLE_READ);
        assertTrue(authorizationService.hasPermission(admin, Permission.ADMIN_ACCESS));
        assertFalse(authorizationService.hasPermission(admin, Permission.USER_GRANT_ADMIN));
        assertFalse(authorizationService.hasPermission(null, Permission.ADMIN_ACCESS));
    }

    @Test
    @DisplayName("全局管理员与版主：由主权限组的数据范围决定")
    void globalAdminAndModerator_byDataScope() {
        User admin = user(2, 20);
        grant(admin, group(20, "GLOBAL"), Permission.ADMIN_ACCESS);
        assertTrue(authorizationService.isGlobalAdmin(admin));
        assertFalse(authorizationService.isModerator(admin));

        User moderator = user(6, 30);
        grant(moderator, group(30, "BOARD_MODERATOR"), Permission.ADMIN_ACCESS);
        assertFalse(authorizationService.isGlobalAdmin(moderator));
        assertTrue(authorizationService.isModerator(moderator));
    }

    @Test
    @DisplayName("canModerateBoard：全局管理员放行；版主仅本板块")
    void canModerateBoard_scope() {
        User superUser = user(1, 10);
        grant(superUser, group(10, "GLOBAL"), Permission.ADMIN_ACCESS);
        when(currentUserService.getCurrentUser()).thenReturn(superUser);
        assertTrue(authorizationService.canModerateBoard(1, 10));
        assertTrue(authorizationService.canModerateBoard(1, null));

        User moderator = user(6, 30);
        grant(moderator, group(30, "BOARD_MODERATOR"), Permission.ADMIN_ACCESS);
        when(currentUserService.getCurrentUser()).thenReturn(moderator);
        when(boardModeratorService.hasActiveRole(6, 10)).thenReturn(true);
        when(boardModeratorService.hasActiveRole(6, 20)).thenReturn(false);
        assertTrue(authorizationService.canModerateBoard(6, 10));
        assertFalse(authorizationService.canModerateBoard(6, 20));
        assertFalse(authorizationService.canModerateBoard(6, null));
    }

    @Test
    @DisplayName("按权限广播 → 返回拥有该权限点的用户 ID")
    void getUserIdsWithPermission_returnsIds() {
        when(effectivePermissionService.getUserIdsWithPermission(Permission.REPORT_REVIEW))
                .thenReturn(Arrays.asList(2, 1));
        assertEquals(Arrays.asList(2, 1),
                authorizationService.getUserIdsWithPermission(Permission.REPORT_REVIEW));
    }

    @Test
    @DisplayName("按权限广播 → 空权限点 → 返回空列表")
    void getUserIdsWithPermission_null_returnsEmpty() {
        assertTrue(authorizationService.getUserIdsWithPermission(null).isEmpty());
        verify(effectivePermissionService, never()).getUserIdsWithPermission(any());
    }

    @Test
    @DisplayName("requirePermission / requireBoardModerator 无权限时抛 ForbiddenException")
    void require_denied_throwsForbidden() {
        User normal = user(2, 40);
        grant(normal, group(40, "GLOBAL"), Permission.ARTICLE_READ);
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requirePermission(normal, Permission.ADMIN_ACCESS));

        User moderator = user(6, 30);
        grant(moderator, group(30, "BOARD_MODERATOR"), Permission.ADMIN_ACCESS);
        when(currentUserService.getCurrentUser()).thenReturn(moderator);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requireBoardModerator(6, 10));
    }

    @Test
    @DisplayName("getVisibleBoardIds：全局管理员 null，版主返回板块，无板块返回空")
    void getVisibleBoardIds_byDataScope() {
        User admin = user(5, 20);
        grant(admin, group(20, "GLOBAL"), Permission.ADMIN_ACCESS);
        assertNull(authorizationService.getVisibleBoardIds(admin));

        BoardModerator row = new BoardModerator();
        row.setUserId(6);
        row.setLabelId(10);
        row.setStatus(1);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(row));

        User moderator = user(6, 30);
        grant(moderator, group(30, "BOARD_MODERATOR"), Permission.ADMIN_ACCESS);
        assertEquals(Collections.singletonList(10),
                authorizationService.getVisibleBoardIds(moderator));

        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertTrue(authorizationService.getVisibleBoardIds(moderator).isEmpty());
    }

    @Test
    @DisplayName("getBoardScopeDenialMessage：无板块/跨板块/全局管理员的文案")
    void boardScopeDenialMessage_byDataScope() {
        User admin = user(5, 20);
        grant(admin, group(20, "GLOBAL"), Permission.ADMIN_ACCESS);
        assertNull(authorizationService.getBoardScopeDenialMessage(admin, 10));

        User moderator = user(6, 30);
        grant(moderator, group(30, "BOARD_MODERATOR"), Permission.ADMIN_ACCESS);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        assertEquals("尚未分配板块，请联系管理员",
                authorizationService.getBoardScopeDenialMessage(moderator, 10));

        BoardModerator row = new BoardModerator();
        row.setUserId(6);
        row.setLabelId(10);
        row.setStatus(1);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(row));
        assertNull(authorizationService.getBoardScopeDenialMessage(moderator, 10));
        assertEquals("无权操作该板块内容",
                authorizationService.getBoardScopeDenialMessage(moderator, 20));
    }
}
