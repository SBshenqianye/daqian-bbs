package com.walker.service;

import com.walker.config.security.AccountRole;
import com.walker.config.security.ForbiddenException;
import com.walker.config.security.Permission;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.User;
import com.walker.service.impl.AuthorizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private EffectivePermissionService effectivePermissionService;

    @BeforeEach
    void setUpPermissionBaseline() {
        // 默认按角色基线返回；权限组用例可覆盖
        lenient().when(effectivePermissionService.getEffectiveCodes(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    if (u == null) {
                        return Collections.emptySet();
                    }
                    return new LinkedHashSet<>(Permission.codesFor(
                            AccountRole.fromUserType(u.getUserType())));
                });
    }

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
    @DisplayName("hasPermission: 超管非关键权限也可被权限组收回")
    void hasPermission_superAdmin_respectsRevokes() {
        User superUser = user(1, "3");
        java.util.Set<String> codes = new LinkedHashSet<>(Permission.codesFor(AccountRole.SUPER_ADMIN));
        codes.remove(Permission.ARTICLE_DELETE.getCode());
        when(effectivePermissionService.getEffectiveCodes(superUser)).thenReturn(codes);

        assertFalse(authorizationService.hasPermission(superUser, Permission.ARTICLE_DELETE));
        assertTrue(authorizationService.hasPermission(superUser, Permission.PERMISSION_MANAGE));
    }

    @Test
    @DisplayName("canModerateBoard: 全局管理员放行；MODERATOR 仅本板块")
    void canModerateBoard_scope() {
        when(currentUserService.getCurrentUser()).thenReturn(user(1, "3"));
        assertTrue(authorizationService.canModerateBoard(1, 10));

        // Q1：ADMIN 为全局管理员，不再按板块限制
        when(currentUserService.getCurrentUser()).thenReturn(user(5, "2"));
        assertTrue(authorizationService.canModerateBoard(5, 10));
        assertTrue(authorizationService.canModerateBoard(5, 20));
        assertTrue(authorizationService.canModerateBoard(5, null));

        // MODERATOR：仅本板块
        when(currentUserService.getCurrentUser()).thenReturn(user(6, "4"));
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
                .thenReturn(java.util.Arrays.asList(2, 1));
        assertEquals(java.util.Arrays.asList(2, 1),
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
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requirePermission(user(2, "1"), Permission.ADMIN_ACCESS));

        when(currentUserService.getCurrentUser()).thenReturn(user(6, "4"));
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        assertThrows(ForbiddenException.class,
                () -> authorizationService.requireBoardModerator(6, 10));
    }

    @Test
    @DisplayName("getVisibleBoardIds：全局管理员 null，MODERATOR 返回板块，无板块返回空")
    void getVisibleBoardIds_byRole() {
        assertNull(authorizationService.getVisibleBoardIds(user(5, "2")));
        assertNull(authorizationService.getVisibleBoardIds(user(1, "3")));

        BoardModerator row = new BoardModerator();
        row.setUserId(6);
        row.setLabelId(10);
        row.setStatus(1);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(row));
        assertEquals(java.util.Collections.singletonList(10),
                authorizationService.getVisibleBoardIds(user(6, "4")));

        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        assertTrue(authorizationService.getVisibleBoardIds(user(7, "4")).isEmpty());
    }

    @Test
    @DisplayName("getBoardScopeDenialMessage：无板块/跨板块/全局管理员的文案")
    void boardScopeDenialMessage_byRole() {
        assertNull(authorizationService.getBoardScopeDenialMessage(user(5, "2"), 10));

        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());
        assertEquals("尚未分配板块，请联系管理员",
                authorizationService.getBoardScopeDenialMessage(user(6, "4"), 10));

        BoardModerator row = new BoardModerator();
        row.setUserId(6);
        row.setLabelId(10);
        row.setStatus(1);
        when(boardModeratorService.list(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(row));
        assertNull(authorizationService.getBoardScopeDenialMessage(user(6, "4"), 10));
        assertEquals("无权操作该板块内容",
                authorizationService.getBoardScopeDenialMessage(user(6, "4"), 20));
    }
}
