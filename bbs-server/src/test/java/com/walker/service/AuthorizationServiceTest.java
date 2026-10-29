package com.walker.service;

import com.walker.config.security.ForbiddenException;
import com.walker.config.security.Permission;
import com.walker.pojo.User;
import com.walker.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 统一授权服务测试。
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

    private User user(int id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        return user;
    }

    @Test
    @DisplayName("isSuperAdmin 依据 user_type，不依赖 id=1")
    void isSuperAdmin_byUserTypeNotId() {
        assertTrue(authorizationService.isSuperAdmin(user(99, "3")));
        assertFalse(authorizationService.isSuperAdmin(user(1, "1")));
    }

    @Test
    @DisplayName("isSuperAdmin(userId) 当前登录人优先使用登录态")
    void isSuperAdmin_byUserId_usesCurrentUser() {
        when(currentUserService.getCurrentUser()).thenReturn(user(7, "3"));
        assertTrue(authorizationService.isSuperAdmin(7));
        verify(userService, never()).getById(anyInt());
    }

    @Test
    @DisplayName("isSuperAdmin(userId) 非当前用户时查询用户表")
    void isSuperAdmin_byUserId_loadsUser() {
        when(currentUserService.getCurrentUser()).thenReturn(user(7, "2"));
        when(userService.getById(8)).thenReturn(user(8, "3"));
        assertTrue(authorizationService.isSuperAdmin(8));
    }

    @Test
    @DisplayName("hasPermission: USER 拒绝，ADMIN 按矩阵，SUPER 全放行")
    void hasPermission_matrix() {
        assertFalse(authorizationService.hasPermission(user(2, "1"), Permission.ADMIN_ACCESS));
        assertTrue(authorizationService.hasPermission(user(2, "2"), Permission.ADMIN_ACCESS));
        assertFalse(authorizationService.hasPermission(user(2, "2"), Permission.USER_GRANT_ADMIN));
        assertTrue(authorizationService.hasPermission(user(1, "3"), Permission.USER_GRANT_ADMIN));
        assertFalse(authorizationService.hasPermission(null, Permission.ADMIN_ACCESS));
    }

    @Test
    @DisplayName("canModerateBoard: 超管放行，本板块版主放行，跨板块拒绝")
    void canModerateBoard_scope() {
        when(currentUserService.getCurrentUser()).thenReturn(user(1, "3"));
        assertTrue(authorizationService.canModerateBoard(1, 10));

        when(currentUserService.getCurrentUser()).thenReturn(user(5, "2"));
        when(boardModeratorService.hasActiveRole(5, 10)).thenReturn(true);
        when(boardModeratorService.hasActiveRole(5, 20)).thenReturn(false);
        assertTrue(authorizationService.canModerateBoard(5, 10));
        assertFalse(authorizationService.canModerateBoard(5, 20));
        assertFalse(authorizationService.canModerateBoard(5, null));
    }

    @Test
    @DisplayName("按权限广播 → 返回拥有该权限点的用户 ID")
    void getUserIdsWithPermission_returnsIds() {
        when(userService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Arrays.asList(user(2, "2"), user(1, "3")));
        assertEquals(java.util.Arrays.asList(2, 1),
                authorizationService.getUserIdsWithPermission(Permission.REPORT_REVIEW));
    }

    @Test
    @DisplayName("按权限广播 → 空权限点 → 返回空列表")
    void getUserIdsWithPermission_null_returnsEmpty() {
        assertTrue(authorizationService.getUserIdsWithPermission(null).isEmpty());
        verify(userService, never()).list(any());
    }

    @Test
    @DisplayName("requirePermission / requireBoardModerator 无权限时抛 ForbiddenException")
    void require_denied_throwsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requirePermission(user(2, "1"), Permission.ADMIN_ACCESS));

        when(currentUserService.getCurrentUser()).thenReturn(user(5, "2"));
        when(boardModeratorService.hasActiveRole(5, 10)).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requireBoardModerator(5, 10));
    }
}
