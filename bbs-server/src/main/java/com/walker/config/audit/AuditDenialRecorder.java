package com.walker.config.audit;

import com.walker.pojo.AuditLog;
import com.walker.pojo.User;
import com.walker.service.AuditLogService;
import com.walker.service.CurrentUserService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 权限拒绝审计：供 GlobalExceptionHandler / RestfulAccessDeniedHandler 调用。
 *
 * <p>只记录 operator、path、reason，不记录请求体，避免敏感数据落库。</p>
 */
@Component
public class AuditDenialRecorder {

    private static final Logger log = LoggerFactory.getLogger(AuditDenialRecorder.class);

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CurrentUserService currentUserService;

    public void record(HttpServletRequest request, String reason) {
        try {
            AuditLog entity = new AuditLog();
            fillOperator(entity);
            entity.setModule("AUTH");
            entity.setAction("AUTH_DENIED");
            entity.setTargetType("ENDPOINT");
            if (request != null) {
                String method = request.getMethod();
                String path = request.getRequestURI();
                entity.setTargetId(truncate(method + " " + path, 128));
                entity.setRequestMethod(truncate(method, 10));
                entity.setRequestPath(truncate(path, 500));
                entity.setIp(truncate(resolveIp(request), 64));
                entity.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
            }
            entity.setResult("fail");
            entity.setErrorMsg(truncate(reason, 1000));
            entity.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            auditLogService.record(entity);
        } catch (Exception e) {
            log.warn("[审计日志] 权限拒绝记录失败，已忽略: {}", e.getMessage());
        }
    }

    private void fillOperator(AuditLog entity) {
        User user = currentUserService.getCurrentUser();
        if (user != null) {
            entity.setOperatorId(user.getId());
            entity.setOperatorName(truncate(user.getUsername(), 64));
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                User principalUser = (User) principal;
                entity.setOperatorId(principalUser.getId());
                entity.setOperatorName(truncate(principalUser.getUsername(), 64));
            } else if (!"anonymousUser".equals(authentication.getName())) {
                entity.setOperatorName(truncate(authentication.getName(), 64));
            }
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
