package com.walker.service;

import com.walker.config.security.Permission;
import com.walker.pojo.User;
import com.walker.service.impl.CurrentUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 当前登录用户上下文测试。
 */
@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @InjectMocks
    private CurrentUserServiceImpl currentUserService;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    private void loginAs(int id, boolean superAdmin) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        Set<String> codes = new LinkedHashSet<>();
        if (superAdmin) {
            codes.add(Permission.USER_GRANT_ADMIN.getCode());
        }
        when(effectivePermissionService.getEffectiveCodes(any(User.class))).thenReturn(codes);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无登录态 → 用户与ID为空，非超管")
    void noAuthentication_returnsNull() {
        assertNull(currentUserService.getCurrentUser());
        assertNull(currentUserService.getCurrentUserId());
        assertFalse(currentUserService.isSuperAdmin());
    }

    @Test
    @DisplayName("普通管理员 → 非超管")
    void admin_isNotSuperAdmin() {
        loginAs(2, false);
        assertEquals(2, currentUserService.getCurrentUserId());
        assertFalse(currentUserService.isSuperAdmin());
    }

    @Test
    @DisplayName("超管 → 按权限组权限判定，不依赖 id")
    void superAdmin_byPermissionNotId() {
        loginAs(99, true);
        assertEquals(99, currentUserService.getCurrentUserId());
        assertTrue(currentUserService.isSuperAdmin());
    }
}
