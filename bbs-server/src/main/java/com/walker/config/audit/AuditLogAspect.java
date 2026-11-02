package com.walker.config.audit;

import com.walker.config.security.ForbiddenException;
import com.walker.pojo.User;
import com.walker.service.AuditLogService;
import com.walker.service.AuditSnapshotService;
import com.walker.service.CurrentUserService;
import com.walker.vo.ResultBean;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志切面：负责记录操作人、请求上下文、成功/失败结果，以及白名单快照。
 *
 * <p>权限拒绝（ForbiddenException / AccessDeniedException）不在这里记录，
 * 由 {@code GlobalExceptionHandler} 和 {@code RestfulAccessDeniedHandler} 统一记录，
 * 避免同一次请求产生两条审计。</p>
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
            new DefaultParameterNameDiscoverer();

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditSnapshotService auditSnapshotService;

    @Autowired
    private AuditDataMasker auditDataMasker;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        String targetId = resolveTargetId(joinPoint, auditLog.targetId());
        Map<String, Object> before = safeSnapshot(auditLog, targetId);

        Object result = null;
        Throwable error = null;
        boolean denied = false;
        try {
            try {
                result = joinPoint.proceed();
            } catch (ForbiddenException | AccessDeniedException e) {
                // 权限拒绝统一交给 403 处理器记录，避免重复
                denied = true;
                throw e;
            } catch (Throwable t) {
                error = t;
                throw t;
            }
        } finally {
            try {
                if (!denied) {
                    writeAuditLog(joinPoint, auditLog, targetId, before, result, error);
                }
            } catch (Throwable t) {
                // 审计失败绝不影响业务主流程
                log.warn("[审计日志] 记录失败，已忽略: module={}, action={}, err={}",
                        safe(auditLog.module()), safe(auditLog.action()), t.getMessage());
            } finally {
                AuditContext.clear();
            }
        }
        return result;
    }

    private void writeAuditLog(ProceedingJoinPoint joinPoint, AuditLog auditLog,
                               String targetId, Map<String, Object> before,
                               Object result, Throwable error) {
        boolean success = error == null && isSuccessResult(result);
        String errorMsg = error != null
                ? error.getMessage()
                : (success ? null : messageOf(result));

        String effectiveTargetId = firstNonBlank(AuditContext.getTargetId(), targetId);
        if (StringUtils.isBlank(effectiveTargetId)) {
            // 新增类操作：方法执行后实体 ID 才被回填，再解析一次
            effectiveTargetId = resolveTargetId(joinPoint, auditLog.targetId());
        }
        String effectiveAction = firstNonBlank(AuditContext.getAction(), auditLog.action());
        Map<String, Object> effectiveBefore = AuditContext.getBefore() != null
                ? AuditContext.getBefore() : before;

        Map<String, Object> after = AuditContext.getAfter();
        if (after == null && auditLog.snapshot() && error == null) {
            after = safeSnapshot(auditLog, effectiveTargetId);
            if (after == null && effectiveBefore != null && isDeleteAction(effectiveAction)) {
                after = new LinkedHashMap<>();
                after.put("deleted", true);
            }
        }

        com.walker.pojo.AuditLog entity = new com.walker.pojo.AuditLog();
        fillOperator(entity);
        entity.setModule(truncate(auditLog.module(), 32));
        entity.setAction(truncate(effectiveAction, 64));
        entity.setTargetType(truncate(emptyToNull(auditLog.targetType()), 32));
        entity.setTargetId(truncate(effectiveTargetId, 128));
        entity.setBeforeJson(auditDataMasker.toSafeJson(effectiveBefore));
        entity.setAfterJson(auditDataMasker.toSafeJson(after));
        fillRequest(entity);
        entity.setResult(success ? "success" : "fail");
        entity.setErrorMsg(truncate(errorMsg, 1000));
        entity.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        auditLogService.record(entity);
    }

    private Map<String, Object> safeSnapshot(AuditLog auditLog, String targetId) {
        if (!auditLog.snapshot() || StringUtils.isBlank(auditLog.targetType())
                || StringUtils.isBlank(targetId)) {
            return null;
        }
        try {
            return auditSnapshotService.snapshot(auditLog.targetType(), targetId);
        } catch (Exception e) {
            log.debug("[审计日志] 快照获取失败: targetType={}, targetId={}, err={}",
                    auditLog.targetType(), targetId, e.getMessage());
            return null;
        }
    }

    private String resolveTargetId(ProceedingJoinPoint joinPoint, String expression) {
        if (StringUtils.isBlank(expression)) {
            return null;
        }
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            EvaluationContext context = new MethodBasedEvaluationContext(
                    null, method, joinPoint.getArgs(), parameterNameDiscoverer);
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.debug("[审计日志] targetId 解析失败: expression={}, err={}",
                    expression, e.getMessage());
            return null;
        }
    }

    private boolean isSuccessResult(Object result) {
        if (result instanceof ResultBean) {
            return ((ResultBean<?>) result).getCode() == 200L;
        }
        return true;
    }

    private String messageOf(Object result) {
        if (result instanceof ResultBean) {
            return ((ResultBean<?>) result).getMessage();
        }
        return null;
    }

    private boolean isDeleteAction(String action) {
        return action != null && action.toUpperCase().contains("DELETE");
    }

    private void fillOperator(com.walker.pojo.AuditLog entity) {
        try {
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
                } else {
                    entity.setOperatorName(truncate(authentication.getName(), 64));
                }
            }
        } catch (Exception e) {
            log.debug("[审计日志] 操作人解析失败: {}", e.getMessage());
        }
    }

    private void fillRequest(com.walker.pojo.AuditLog entity) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return;
            }
            entity.setRequestMethod(truncate(request.getMethod(), 10));
            entity.setRequestPath(truncate(request.getRequestURI(), 500));
            entity.setIp(truncate(resolveIp(request), 64));
            entity.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        } catch (Exception e) {
            log.debug("[审计日志] 请求上下文解析失败: {}", e.getMessage());
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

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String emptyToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
