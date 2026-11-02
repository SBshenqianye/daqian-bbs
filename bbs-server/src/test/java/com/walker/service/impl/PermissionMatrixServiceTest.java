package com.walker.service.impl;

import com.walker.config.security.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 权限矩阵 Phase A 单测：只读、无副作用。
 */
@ExtendWith(MockitoExtension.class)
class PermissionMatrixServiceTest {

    @InjectMocks
    private PermissionMatrixServiceImpl service;

    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    @DisplayName("parseRoles：hasRole / hasAnyRole / ROLE_ 前缀 / 非角色表达式")
    void parseRoles_cases() {
        assertEquals(Collections.singletonList("ADMIN"), PermissionMatrixServiceImpl.parseRoles("hasRole('ADMIN')"));
        assertEquals(java.util.Arrays.asList("ADMIN", "SUPER_ADMIN"),
                PermissionMatrixServiceImpl.parseRoles("hasAnyRole('ADMIN','SUPER_ADMIN')"));
        assertEquals(Collections.singletonList("MODERATOR"),
                PermissionMatrixServiceImpl.parseRoles("hasRole('ROLE_MODERATOR')"));
        assertEquals(java.util.Arrays.asList("ADMIN", "SUPER_ADMIN"),
                PermissionMatrixServiceImpl.parseRoles("hasAnyRole(\"ADMIN\", \"SUPER_ADMIN\")"));

        assertTrue(PermissionMatrixServiceImpl.parseRoles(null).isEmpty());
        assertTrue(PermissionMatrixServiceImpl.parseRoles("  ").isEmpty());
        assertTrue(PermissionMatrixServiceImpl.parseRoles("@authz.has('article:read')").isEmpty());
    }

    @Test
    @DisplayName("角色/权限点矩阵：MODERATOR 有业务权限、没有系统权限与 permission:manage")
    @SuppressWarnings("unchecked")
    void matrix_rolesAndPermissions() {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Collections.emptyMap());

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> roles = (List<Map<String, Object>>) matrix.get("roles");
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) matrix.get("permissions");

        assertEquals(4, roles.size());
        assertEquals("USER", roles.get(0).get("code"));
        assertEquals("MODERATOR", roles.get(1).get("code"));
        assertEquals("ADMIN", roles.get(2).get("code"));
        assertEquals("SUPER_ADMIN", roles.get(3).get("code"));

        Map<String, Object> moderator = roles.get(1);
        List<String> moderatorPermissions = (List<String>) moderator.get("permissions");
        assertTrue(moderatorPermissions.contains("article:read"));
        assertFalse(moderatorPermissions.contains("user:read"));
        assertFalse(moderatorPermissions.contains("permission:manage"));

        Map<String, Object> manage = permissions.stream()
                .filter(p -> "permission:manage".equals(p.get("code")))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals(Boolean.FALSE, manage.get("user"));
        assertEquals(Boolean.FALSE, manage.get("moderator"));
        assertEquals(Boolean.FALSE, manage.get("admin"));
        assertEquals(Boolean.TRUE, manage.get("super"));
    }

    @Test
    @DisplayName("接口矩阵：读取方法级 @PreAuthorize 并解析角色")
    @SuppressWarnings("unchecked")
    void matrix_endpoints_readsPreAuthorize() throws Exception {
        RequestMappingInfo info = RequestMappingInfo
                .paths("/admin/demo")
                .methods(RequestMethod.GET)
                .build();
        HandlerMethod handler = new HandlerMethod(
                new DemoController(), DemoController.class.getMethod("demo"));
        when(requestMappingHandlerMapping.getHandlerMethods())
                .thenReturn(Collections.singletonMap(info, handler));

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) matrix.get("endpoints");

        assertEquals(1, endpoints.size());
        Map<String, Object> row = endpoints.get(0);
        assertEquals("GET", row.get("method"));
        assertEquals("/admin/demo", row.get("path"));
        assertEquals("hasAnyRole('ADMIN','SUPER_ADMIN')", row.get("expression"));
        assertEquals(java.util.Arrays.asList("ADMIN", "SUPER_ADMIN"), row.get("roles"));
        assertEquals(Boolean.TRUE, row.get("declared"));

        Map<String, Object> summary = (Map<String, Object>) matrix.get("summary");
        assertEquals(1, summary.get("endpointCount"));
        assertEquals(0, summary.get("undeclaredAdminCount"));
    }

    @Test
    @DisplayName("接口矩阵：/admin 路径没有 @PreAuthorize 时标记为未声明")
    @SuppressWarnings("unchecked")
    void matrix_endpoints_marksUndeclared() throws Exception {
        RequestMappingInfo info = RequestMappingInfo
                .paths("/admin/open")
                .methods(RequestMethod.POST)
                .build();
        HandlerMethod handler = new HandlerMethod(
                new DemoController(), DemoController.class.getMethod("open"));
        when(requestMappingHandlerMapping.getHandlerMethods())
                .thenReturn(Collections.singletonMap(info, handler));

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) matrix.get("endpoints");
        Map<String, Object> row = endpoints.get(0);

        assertEquals(Boolean.FALSE, row.get("declared"));
        assertNull(row.get("expression"));
        assertEquals(Boolean.FALSE, row.get("nonRoleExpression"));
        assertEquals(1, ((Map<String, Object>) matrix.get("summary")).get("undeclaredAdminCount"));
    }

    @Test
    @DisplayName("Permission 枚举：permission:manage 只授予超管")
    void permissionManage_onlySuper() {
        assertFalse(Permission.PERMISSION_MANAGE.isGrantedTo(com.walker.config.security.AccountRole.USER));
        assertFalse(Permission.PERMISSION_MANAGE.isGrantedTo(com.walker.config.security.AccountRole.MODERATOR));
        assertFalse(Permission.PERMISSION_MANAGE.isGrantedTo(com.walker.config.security.AccountRole.ADMIN));
        assertTrue(Permission.PERMISSION_MANAGE.isGrantedTo(com.walker.config.security.AccountRole.SUPER_ADMIN));
    }

    /** 测试用控制器：一个带注解、一个不带注解 */
    static class DemoController {

        @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
        public void demo() {
        }

        public void open() {
        }
    }
}
