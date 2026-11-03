package com.walker.config.security;

import com.walker.config.audit.AuditDenialRecorder;
import com.walker.pojo.User;
import com.walker.service.EffectivePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * 管理路径统一入口校验：必须拥有有效的 admin:access 权限点。
 *
 * <p>JWT 认证完成后再执行，因此权限组收回 admin:access 后：
 * 已登录会话无法继续访问 /admin/**，即使旧 token 仍然有效。
 * 登录、公共接口、文件、改密路径不在此处拦截。</p>
 */
@Component
public class AdminAccessFilter extends OncePerRequestFilter {

    private static final String MESSAGE = "无权访问管理后台，请联系管理员";

    @Autowired
    private EffectivePermissionService effectivePermissionService;

    @Autowired
    private AuditDenialRecorder auditDenialRecorder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !isAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User)) {
            // 未登录/匿名交给 Spring Security 的 entry point 统一返回 401
            filterChain.doFilter(request, response);
            return;
        }

        User user = (User) authentication.getPrincipal();
        Set<String> codes = effectivePermissionService.getEffectiveCodes(user);
        if (codes.contains(Permission.ADMIN_ACCESS.getCode())) {
            filterChain.doFilter(request, response);
            return;
        }

        auditDenialRecorder.record(request, MESSAGE);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"code\":403,\"message\":\"" + MESSAGE + "\",\"obj\":null}");
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            path = contextPath != null && uri.startsWith(contextPath)
                    ? uri.substring(contextPath.length())
                    : uri;
        }
        if (path == null || path.isEmpty()) {
            return false;
        }
        // 放行登录、公共接口、文件与改密相关路径
        if ("/admin/login".equals(path)
                || path.startsWith("/common/")
                || path.startsWith("/files/")
                || path.contains("/modPwd")
                || path.contains("/changePassword")
                || path.contains("/resetPassword")) {
            return false;
        }
        return path.startsWith("/admin")
                || path.startsWith("/reply/admin")
                || path.startsWith("/saOrg")
                || path.startsWith("/sensitiveWord")
                || "/getAllUser".equals(path)
                || "/updateUserRole".equals(path);
    }
}
