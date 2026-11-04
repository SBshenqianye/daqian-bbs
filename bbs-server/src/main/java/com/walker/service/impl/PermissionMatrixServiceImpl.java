package com.walker.service.impl;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限矩阵只读实现。
 *
 * <p>只汇总两处事实来源：
 * 1) 权限组 → 权限点：来自权限组自身的权限点配置；
 * 2) 接口 → 注解：Spring 已注册的 {@link RequestMappingHandlerMapping} +
 *    方法级/类级 {@link PreAuthorize}。</p>
 */
@Service
public class PermissionMatrixServiceImpl implements PermissionMatrixService {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private PermissionGroupService permissionGroupService;

    @Override
    public Map<String, Object> getMatrix() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("generatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        result.put("permissions", buildPermissions());
        result.put("groups", buildGroups());

        List<Map<String, Object>> endpoints = buildEndpoints();
        result.put("endpoints", endpoints);
        result.put("summary", buildSummary(endpoints));
        return result;
    }

    private List<Map<String, Object>> buildPermissions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Permission permission : Permission.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", permission.getCode());
            list.add(row);
        }
        return list;
    }

    /**
     * 权限矩阵中的权限组数据：组本身与最终权限集合。
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
            row.put("dataScopeType", summary.get("dataScopeType"));
            row.put("enabled", summary.get("enabled"));
            row.put("permissions", detail == null
                    ? Collections.emptyList() : detail.get("permissionCodes"));
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

            String method = resolveMethod(info);
            String handlerName = handler.getBeanType().getSimpleName() + "#" + handler.getMethod().getName();

            if (patterns.isEmpty()) {
                endpoints.add(endpointRow(method, "—", expression, handlerName));
                continue;
            }
            for (String path : patterns) {
                endpoints.add(endpointRow(method, path, expression, handlerName));
            }
        }

        endpoints.sort(Comparator
                .comparing((Map<String, Object> row) -> String.valueOf(row.get("path")))
                .thenComparing(row -> String.valueOf(row.get("method"))));
        return endpoints;
    }

    private Map<String, Object> endpointRow(String method, String path,
                                            String expression, String handler) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("method", method);
        row.put("path", path);
        row.put("expression", expression);
        row.put("declared", expression != null);
        row.put("handler", handler);
        return row;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> endpoints) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("endpointCount", endpoints.size());
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
}
