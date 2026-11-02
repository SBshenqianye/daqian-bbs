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
    @DisplayName("ADMIN 默认拥有除 user:grant:admin、config:write、permission:manage 外的权限")
    void admin_defaultPermissions() {
        for (Permission permission : Permission.values()) {
            boolean expected = permission != Permission.USER_GRANT_ADMIN
                    && permission != Permission.CONFIG_WRITE
                    && permission != Permission.PERMISSION_MANAGE;
            assertEquals(expected, permission.isGrantedTo(AccountRole.ADMIN), permission.getCode());
        }
        assertFalse(Permission.codesFor(AccountRole.ADMIN).contains("user:grant:admin"));
        assertFalse(Permission.codesFor(AccountRole.ADMIN).contains("config:write"));
        assertFalse(Permission.codesFor(AccountRole.ADMIN).contains("permission:manage"));
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

    @Test
    @DisplayName("MODERATOR 仅拥有业务管理权限点，系统管理全部拒绝")
    void moderator_permissions() {
        java.util.List<String> codes = Permission.codesFor(AccountRole.MODERATOR);
        assertTrue(codes.contains("admin:access"));
        assertTrue(codes.contains("article:read"));
        assertTrue(codes.contains("article:review"));
        assertTrue(codes.contains("article:delete"));
        assertTrue(codes.contains("article:feature"));
        assertTrue(codes.contains("article:suggestion:adopt"));
        assertTrue(codes.contains("report:review"));
        assertTrue(codes.contains("appeal:review"));
        assertTrue(codes.contains("adopt:review"));
        assertTrue(codes.contains("featured:recommend"));
        assertTrue(codes.contains("points:read"));
        assertTrue(codes.contains("dashboard:read"));

        assertFalse(codes.contains("user:read"));
        assertFalse(codes.contains("user:grant:admin"));
        assertFalse(codes.contains("config:read"));
        assertFalse(codes.contains("config:write"));
        assertFalse(codes.contains("permission:manage"));
        assertFalse(codes.contains("moderator:manage"));
        assertFalse(codes.contains("moderator:complaint:review"));
        assertFalse(codes.contains("featured:review"));
        assertFalse(codes.contains("points:adjust"));
    }
}
