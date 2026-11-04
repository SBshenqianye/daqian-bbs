package com.walker.config.security;

import com.walker.pojo.User;
import com.walker.service.EffectivePermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * 管理端首次登录强制改密过滤器测试（C3）。
 */
@ExtendWith(MockitoExtension.class)
class FirstLoginPasswordFilterTest {

    @InjectMocks
    private FirstLoginPasswordFilter filter;

    @Mock
    private EffectivePermissionService effectivePermissionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String userType, int firstLogin) {
        User user = new User();
        user.setId(1);
        user.setUsername("tester");
        user.setUserType(userType);
        user.setIsFirstLogin(firstLogin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        Set<String> codes = new LinkedHashSet<>();
        if (!"1".equals(userType)) {
            codes.add(Permission.ADMIN_ACCESS.getCode());
        }
        lenient().when(effectivePermissionService.getEffectiveCodes(any(User.class))).thenReturn(codes);
    }

    private MockHttpServletResponse run(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response;
    }

    private boolean passed(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null;
    }

    @Test
    @DisplayName("首次登录管理员访问管理接口 → 403")
    void firstLoginAdmin_adminPath_forbidden() throws Exception {
        loginAs("2", 1);
        MockHttpServletResponse response = run("/admin/user/search");
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("请先修改初始密码"));
    }

    @Test
    @DisplayName("首次登录管理员访问改密/登录/用户信息 → 放行")
    void firstLoginAdmin_allowedPaths_pass() throws Exception {
        loginAs("2", 1);
        assertTrue(passed("/user/modPwd"));
        assertTrue(passed("/admin/login"));
        assertTrue(passed("/common/user/info"));
        assertTrue(passed("/common/login"));
    }

    @Test
    @DisplayName("已改密管理员访问管理接口 → 放行")
    void changedPasswordAdmin_pass() throws Exception {
        loginAs("2", 0);
        assertTrue(passed("/admin/user/search"));
    }

    @Test
    @DisplayName("普通用户首次登录不在此过滤器拦截")
    void firstLoginUser_pass() throws Exception {
        loginAs("1", 1);
        assertTrue(passed("/admin/user/search"));
    }

    @Test
    @DisplayName("首次登录版主访问 /reply/admin → 403")
    void firstLoginModerator_replyAdmin_forbidden() throws Exception {
        loginAs("4", 1);
        assertEquals(403, run("/reply/admin/pendingAdopts").getStatus());
    }

    @Test
    @DisplayName("未登录不在此过滤器拦截（交给 Security 返回 401）")
    void anonymous_pass() throws Exception {
        assertTrue(passed("/admin/user/search"));
    }
}
