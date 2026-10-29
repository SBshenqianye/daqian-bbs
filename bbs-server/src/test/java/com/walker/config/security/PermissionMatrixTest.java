package com.walker.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色-权限点矩阵测试（设计规划 3.2）。
 */
class PermissionMatrixTest {

    @Test
    @DisplayName("USER 不具备任何管理端权限点")
    void user_hasNoAdminPermission() {
        for (Permission permission : Permission.values()) {
            assertFalse(permission.isGrantedTo(AccountRole.USER), permission.getCode());
        }
        assertTrue(Permission.codesFor(AccountRole.USER).isEmpty());
    }

    @Test
    @DisplayName("ADMIN 默认拥有除 user:grant:admin、config:write 外的权限")
    void admin_defaultPermissions() {
        for (Permission permission : Permission.values()) {
            boolean expected = permission != Permission.USER_GRANT_ADMIN
                    && permission != Permission.CONFIG_WRITE;
            assertEquals(expected, permission.isGrantedTo(AccountRole.ADMIN), permission.getCode());
        }
        assertFalse(Permission.codesFor(AccountRole.ADMIN).contains("user:grant:admin"));
        assertFalse(Permission.codesFor(AccountRole.ADMIN).contains("config:write"));
        assertTrue(Permission.codesFor(AccountRole.ADMIN).contains("admin:access"));
    }

    @Test
    @DisplayName("SUPER_ADMIN 拥有全部权限点")
    void superAdmin_hasAllPermissions() {
        assertEquals(Permission.values().length, Permission.codesFor(AccountRole.SUPER_ADMIN).size());
        for (Permission permission : Permission.values()) {
            assertTrue(permission.isGrantedTo(AccountRole.SUPER_ADMIN), permission.getCode());
        }
    }
}
