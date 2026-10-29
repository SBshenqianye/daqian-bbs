package com.walker.service;

import com.walker.pojo.User;
import com.walker.service.impl.CurrentUserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 当前登录用户上下文测试。
 */
class CurrentUserServiceTest {

    private final CurrentUserServiceImpl currentUserService = new CurrentUserServiceImpl();

    private void loginAs(int id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
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
        loginAs(2, "2");
        assertEquals(2, currentUserService.getCurrentUserId());
        assertFalse(currentUserService.isSuperAdmin());
    }

    @Test
    @DisplayName("超管 → 按 user_type 判定，不依赖 id")
    void superAdmin_byUserTypeNotId() {
        loginAs(99, "3");
        assertEquals(99, currentUserService.getCurrentUserId());
        assertTrue(currentUserService.isSuperAdmin());
    }
}
