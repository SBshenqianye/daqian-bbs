package com.walker.service.impl;

import com.walker.config.security.AccountRole;
import com.walker.config.security.Permission;
import com.walker.service.PermissionGroupService;
import com.walker.service.PermissionMatrixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 权限矩阵只读实现（Phase A）。
 *
 * <p>不修改任何鉴权逻辑，只把两处“事实来源”汇总出来：
 * 1) 角色 → 权限点：{@link Permission#codesFor(AccountRole)}；
 * 2) 接口 → 注解：Spring 已注册的 {@link RequestMappingHandlerMapping} +
 *    方法级/类级 {@link PreAuthorize}。</p>
 */
@Service
public class PermissionMatrixServiceImpl implements PermissionMatrixService {

    private static final Pattern ROLE_IN_EXPRESSION = Pattern.compile("['\"]([A-Z][A-Z0-9_]*)['\"]");
    private static final String ROLE_PREFIX = "ROLE_";

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private PermissionGroupService permissionGroupService;

    @Override
    public Map<String, Object> getMatrix() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        result.put("roles", buildRoles());
        result.put("permissions", buildPermissions());
        result.put("groups", buildGroups());

        List<Map<String, Object>> endpoints = buildEndpoints();
        result.put("endpoints", endpoints);
        result.put("summary", buildSummary(endpoints));
        return result;
    }

    private List<Map<String, Object>> buildRoles() {
        AccountRole[] order = {
                AccountRole.USER, AccountRole.MODERATOR, AccountRole.ADMIN, AccountRole.SUPER_ADMIN
        };
        List<Map<String, Object>> roles = new ArrayList<>();
        for (AccountRole role : order) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", role.name());
            row.put("userType", role.getCode());
            row.put("permissions", Permission.codesFor(role));
            roles.add(row);
        }
        return roles;
    }

    private List<Map<String, Object>> buildPermissions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", permission.getCode());
            row.put("user", permission.isGrantedTo(AccountRole.USER));
            row.put("moderator", permission.isGrantedTo(AccountRole.MODERATOR));
            row.put("admin", permission.isGrantedTo(AccountRole.ADMIN));
            row.put("super", permission.isGrantedTo(AccountRole.SUPER_ADMIN));
            row.put("superProtected", permission.isSuperAdminProtected());
            list.add(row);
        }
        return list;
    }

    /**
     * 权限矩阵中“显示权限组”区域的数据：组本身、角色基线、最终权限集合、不可剥离项。
     */
    private List<Map<String, Object>> buildGroups() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> groups = permissionGroupService.listGroups();
        for (Map<String, Object> summary : groups) {
            Object idValue = summary.get("id");
            Integer id = idValue instanceof Number ? ((Number) idValue).intValue() : null;
            Map<String, Object> detail = id == null ? null : permissionGroupService.getGroup(id);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", id);
            row.put("groupCode", summary.get("groupCode"));
            row.put("groupName", summary.get("groupName"));
            row.put("description", summary.get("description"));
            row.put("baseRole", summary.get("baseRole"));
            row.put("enabled", summary.get("enabled"));
            row.put("system", summary.get("system"));
            row.put("autoApply", summary.get("autoApply"));
            row.put("roleUserCount", summary.get("roleUserCount"));
            row.put("permissions", detail == null
                    ? Collections.emptyList() : detail.get("permissionCodes"));
            row.put("protectedCodes", detail == null
                    ? Collections.emptyList() : detail.get("protectedCodes"));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildEndpoints() {
        Map<RequestMappingInfo, HandlerMethod> handlers = requestMappingHandlerMapping.getHandlerMethods();
        List<Map<String, Object>> endpoints = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlers.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod handler = entry.getValue();

            Set<String> patterns = info.getPatternsCondition() != null
                    ? info.getPatternsCondition().getPatterns()
                    : Collections.<String>emptySet();
            boolean adminPath = patterns.stream().anyMatch(this::isAdminPath);

            PreAuthorize preAuthorize = handler.getMethodAnnotation(PreAuthorize.class);
            if (preAuthorize == null) {
                preAuthorize = AnnotatedElementUtils.findMergedAnnotation(
                        handler.getBeanType(), PreAuthorize.class);
            }
            String expression = preAuthorize != null ? preAuthorize.value() : null;
            if (expression == null && !adminPath) {
                continue;
            }

            List<String> roles = parseRoles(expression);
            String method = resolveMethod(info);
            String handlerName = handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName();

            if (patterns.isEmpty()) {
                endpoints.add(endpointRow(method, "—", expression, roles, handlerName));
                continue;
            }
            for (String path : patterns) {
                endpoints.add(endpointRow(method, path, expression, roles, handlerName));
            }
        }

        endpoints.sort(Comparator
                .comparing((Map<String, Object> row) -> String.valueOf(row.get("path")))
                .thenComparing(row -> String.valueOf(row.get("method"))));
        return endpoints;
    }

    private Map<String, Object> endpointRow(String method, String path,
                                            String expression, List<String> roles, String handler) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("method", method);
        row.put("path", path);
        row.put("expression", expression);
        row.put("roles", roles);
        row.put("declared", expression != null);
        row.put("nonRoleExpression", expression != null && roles.isEmpty());
        row.put("handler", handler);
        return row;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> endpoints) {
        int undeclared = 0;
        int nonRole = 0;
        for (Map<String, Object> row : endpoints) {
            if (Boolean.FALSE.equals(row.get("declared"))) {
                undeclared++;
            }
            if (Boolean.TRUE.equals(row.get("nonRoleExpression"))) {
                nonRole++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("endpointCount", endpoints.size());
        summary.put("undeclaredAdminCount", undeclared);
        summary.put("nonRoleExpressionCount", nonRole);
        return summary;
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin")
                || path.startsWith("/reply/admin")
                || path.startsWith("/sensitiveWord")
                || path.startsWith("/saOrg")
                || "/getAllUser".equals(path)
                || "/updateUserRole".equals(path);
    }

    private String resolveMethod(RequestMappingInfo info) {
        if (info.getMethodsCondition() == null) {
            return "ANY";
        }
        Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
        if (methods.isEmpty()) {
            return "ANY";
        }
        List<String> names = new ArrayList<>();
        for (RequestMethod method : methods) {
            names.add(method.name());
        }
        Collections.sort(names);
        return String.join(",", names);
    }

    /**
     * 从 SpEL 中解析角色名，例如：
     * {@code hasAnyRole('ADMIN','SUPER_ADMIN')} → [ADMIN, SUPER_ADMIN]
     * {@code hasRole('ROLE_MODERATOR')} → [MODERATOR]
     *
     * <p>非角色表达式（如 {@code @authz.has('x')}）返回空列表，由前端标红提示。</p>
     */
    public static List<String> parseRoles(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (!expression.contains("hasRole") && !expression.contains("hasAnyRole")) {
            return Collections.emptyList();
        }
        Set<String> roles = new LinkedHashSet<>();
        Matcher matcher = ROLE_IN_EXPRESSION.matcher(expression);
        while (matcher.find()) {
            String role = matcher.group(1);
            if (role.startsWith(ROLE_PREFIX)) {
                role = role.substring(ROLE_PREFIX.length());
            }
            roles.add(role);
        }
        return new ArrayList<>(roles);
    }
}
