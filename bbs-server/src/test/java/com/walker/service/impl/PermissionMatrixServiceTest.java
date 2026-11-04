package com.walker.service.impl;

import com.walker.service.PermissionGroupService;
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
 * 权限矩阵只读单测：只展示权限组、权限点与接口注解。
 */
@ExtendWith(MockitoExtension.class)
class PermissionMatrixServiceTest {

    @InjectMocks
    private PermissionMatrixServiceImpl service;

    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Mock
    private PermissionGroupService permissionGroupService;

    @Test
    @DisplayName("权限矩阵：只返回权限点列表，不再有角色基线列")
    @SuppressWarnings("unchecked")
    void matrix_permissionsOnly() {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Collections.emptyMap());
        when(permissionGroupService.listGroups()).thenReturn(Collections.emptyList());

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> permissions = (List<Map<String, Object>>) matrix.get("permissions");

        assertFalse(matrix.containsKey("roles"));
        Map<String, Object> manage = permissions.stream()
                .filter(p -> "permission:manage".equals(p.get("code")))
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertEquals(Collections.singleton("code"), manage.keySet());
    }

    @Test
    @DisplayName("权限组：返回组自身权限点")
    @SuppressWarnings("unchecked")
    void matrix_groups_readsDirectPermissions() {
        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(Collections.emptyMap());
        when(permissionGroupService.listGroups()).thenReturn(Collections.singletonList(
                new java.util.LinkedHashMap<String, Object>() {{
                    put("id", 10);
                    put("groupCode", "role_admin");
                    put("groupName", "管理员");
                    put("enabled", 1);
                }}));
        when(permissionGroupService.getGroup(10)).thenReturn(new java.util.LinkedHashMap<String, Object>() {{
            put("permissionCodes", Collections.singletonList("article:read"));
        }});

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> groups = (List<Map<String, Object>>) matrix.get("groups");

        assertEquals(1, groups.size());
        assertEquals("role_admin", groups.get(0).get("groupCode"));
        assertEquals(Collections.singletonList("article:read"), groups.get(0).get("permissions"));
    }

    @Test
    @DisplayName("接口矩阵：读取方法级 @PreAuthorize")
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
        when(permissionGroupService.listGroups()).thenReturn(Collections.emptyList());

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) matrix.get("endpoints");

        assertEquals(1, endpoints.size());
        Map<String, Object> row = endpoints.get(0);
        assertEquals("GET", row.get("method"));
        assertEquals("/admin/demo", row.get("path"));
        assertEquals("@authz.hasPermission(principal, 'article:read')", row.get("expression"));
        assertEquals(Boolean.TRUE, row.get("declared"));

        Map<String, Object> summary = (Map<String, Object>) matrix.get("summary");
        assertEquals(1, summary.get("endpointCount"));
        assertFalse(summary.containsKey("undeclaredAdminCount"));
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
        when(permissionGroupService.listGroups()).thenReturn(Collections.emptyList());

        Map<String, Object> matrix = service.getMatrix();
        List<Map<String, Object>> endpoints = (List<Map<String, Object>>) matrix.get("endpoints");
        Map<String, Object> row = endpoints.get(0);

        assertEquals(Boolean.FALSE, row.get("declared"));
        assertNull(row.get("expression"));
        assertFalse(((Map<String, Object>) matrix.get("summary"))
                .containsKey("undeclaredAdminCount"));
    }

    /** 测试用控制器：一个带注解、一个不带注解 */
    static class DemoController {

        @PreAuthorize("@authz.hasPermission(principal, 'article:read')")
        public void demo() {
        }

        public void open() {
        }
    }
}
