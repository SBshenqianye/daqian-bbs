package com.walker.config.security;

import com.walker.pojo.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账号角色与 Spring Security 权限映射测试。
 */
class AccountRoleTest {

    private Set<String> authoritiesOf(String userType) {
        User user = new User();
        user.setUserType(userType);
        return user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());
    }

    private Set<String> roles(String... roles) {
        return new HashSet<>(Arrays.asList(roles));
    }

    @Test
    @DisplayName("user_type=1 → ROLE_USER")
    void userType1_mapsToRoleUser() {
        assertEquals(AccountRole.USER, AccountRole.fromUserType("1"));
        assertEquals(roles("ROLE_USER"), authoritiesOf("1"));
    }

    @Test
    @DisplayName("user_type=2 → ROLE_USER + ROLE_ADMIN")
    void userType2_mapsToAdmin() {
        assertEquals(AccountRole.ADMIN, AccountRole.fromUserType("2"));
        assertEquals(roles("ROLE_USER", "ROLE_ADMIN"), authoritiesOf("2"));
    }

    @Test
    @DisplayName("user_type=3 → ROLE_USER + ROLE_ADMIN + ROLE_SUPER_ADMIN")
    void userType3_mapsToSuperAdmin() {
        assertEquals(AccountRole.SUPER_ADMIN, AccountRole.fromUserType("3"));
        assertEquals(roles("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"), authoritiesOf("3"));
    }

    @Test
    @DisplayName("user_type=4 → ROLE_USER + ROLE_MODERATOR（不含 ROLE_ADMIN）")
    void userType4_mapsToModerator() {
        assertEquals(AccountRole.MODERATOR, AccountRole.fromUserType("4"));
        assertEquals(roles("ROLE_USER", "ROLE_MODERATOR"), authoritiesOf("4"));
        assertFalse(authoritiesOf("4").contains("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("MODERATOR 可进管理后台，但不是全局管理员")
    void moderator_canAccessConsole_butNotAdmin() {
        assertTrue(AccountRole.MODERATOR.canAccessAdminConsole());
        assertFalse(AccountRole.MODERATOR.isAdmin());
        assertTrue(AccountRole.MODERATOR.isModerator());
        assertFalse(AccountRole.ADMIN.isModerator());
    }

    @Test
    @DisplayName("空值与非法值 → 按普通用户处理，不放大权限")
    void blankOrUnknown_mapsToUser() {
        assertEquals(AccountRole.USER, AccountRole.fromUserType(null));
        assertEquals(AccountRole.USER, AccountRole.fromUserType(""));
        assertEquals(AccountRole.USER, AccountRole.fromUserType("9"));
        assertEquals(roles("ROLE_USER"), authoritiesOf("9"));
    }
}
