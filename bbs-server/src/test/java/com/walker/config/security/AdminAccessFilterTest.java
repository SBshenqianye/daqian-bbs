package com.walker.config.security;

import com.walker.config.audit.AuditDenialRecorder;
import com.walker.pojo.User;
import com.walker.service.EffectivePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 管理路径入口校验：旧 token / 权限组收回后，/admin 与历史管理接口都必须重新判断 admin:access。
 */
@ExtendWith(MockitoExtension.class)
class AdminAccessFilterTest {

    @InjectMocks
    private AdminAccessFilter filter;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    @Mock
    private AuditDenialRecorder auditDenialRecorder;

    @Mock
    private FilterChain chain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(boolean adminAccess) {
        User user = new User();
        user.setId(5);
        user.setUsername("tester");
        user.setUserType("2");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        Set<String> codes = new LinkedHashSet<>();
        if (adminAccess) {
            codes.add(Permission.ADMIN_ACCESS.getCode());
        }
        lenient().when(effectivePermissionService.getEffectiveCodes(any(User.class))).thenReturn(codes);
    }

    private MockHttpServletResponse call(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, chain);
        return response;
    }

    @Test
    @DisplayName("已登录但无 admin:access：/admin/** 返回 HTTP 200 + body.code=403，并记审计")
    void adminPathWithoutAccess_denied() throws Exception {
        login(false);

        MockHttpServletResponse response = call("/admin/user/search");

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
        verify(chain, never()).doFilter(any(), any());
        verify(auditDenialRecorder).record(any(), any());
    }

    @Test
    @DisplayName("历史管理接口 /common/saOrg/batchUpdate* 也必须校验 admin:access")
    void commonSaOrgBatchUpdate_withoutAccess_denied() throws Exception {
        login(false);

        MockHttpServletResponse response = call("/common/saOrg/batchUpdateRanking");

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("根路径管理接口 /getAllUser 也必须校验 admin:access")
    void rootAdminPath_withoutAccess_denied() throws Exception {
        login(false);

        MockHttpServletResponse response = call("/getAllUser");

        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":403"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("有 admin:access：放行")
    void adminPathWithAccess_allowed() throws Exception {
        login(true);

        MockHttpServletResponse response = call("/admin/user/search");

        assertEquals(200, response.getStatus());
        verify(chain).doFilter(any(), any());
    }

    @Test
    @DisplayName("公共/自身接口与登录路径不要求 admin:access")
    void publicPaths_allowed() throws Exception {
        login(false);

        call("/common/user/info");
        call("/user/modPwd");
        call("/common/article/getNewest");
        call("/admin/login");

        verify(chain, org.mockito.Mockito.times(4)).doFilter(any(), any());
    }

    @Test
    @DisplayName("未登录：交给 Spring Security 返回 401")
    void anonymous_passesThrough() throws Exception {
        call("/admin/user/search");

        verify(chain).doFilter(any(), any());
        verify(auditDenialRecorder, never()).record(any(), any());
    }
}
