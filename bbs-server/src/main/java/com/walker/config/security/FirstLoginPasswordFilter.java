package com.walker.config.security;

import com.walker.pojo.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 管理端首次登录强制改密。
 *
 * <p>当管理端账号（ADMIN / SUPER_ADMIN / MODERATOR）的 {@code is_first_login=1} 时，
 * 只放行改密、登录、用户信息等少数接口；其余管理接口返回 403，防止绕过前端直接调用。</p>
 *
 * <p>用户端（USER）不走这里，沿用 bbs-ui 已有的首次登录改密流程。</p>
 */
public class FirstLoginPasswordFilter extends OncePerRequestFilter {

    private static final String MESSAGE = "请先修改初始密码";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !requiresPasswordChange(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User)) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = (User) authentication.getPrincipal();
        if (!AccountRole.fromUserType(user.getUserType()).canAccessAdminConsole()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!Integer.valueOf(1).equals(user.getIsFirstLogin())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"" + MESSAGE + "\",\"obj\":null}");
    }

    /**
     * 只拦截管理端路径；改密、登录、用户信息、文件等不在拦截范围。
     */
    private boolean requiresPasswordChange(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            path = contextPath != null && uri.startsWith(contextPath)
                    ? uri.substring(contextPath.length())
                    : uri;
        }
        if (path == null || "/admin/login".equals(path)) {
            return false;
        }
        return path.startsWith("/admin")
                || path.startsWith("/reply/admin")
                || path.startsWith("/saOrg")
                || path.startsWith("/sensitiveWord")
                || "/getAllUser".equals(path)
                || "/updateUserRole".equals(path)
                || path.startsWith("/common/saOrg/batchUpdate");
    }
}
