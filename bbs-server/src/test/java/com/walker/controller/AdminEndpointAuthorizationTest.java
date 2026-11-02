package com.walker.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3 回归：扫描 Controller 上的映射，确保管理端接口都有方法级/类级 @PreAuthorize。
 *
 * <p>唯一例外是历史遗留的 {@code /admin/login}（旧 bbs_admin 登录），
 * 它在 SecurityConfig 中显式 permitAll，不属于当前账号体系。</p>
 */
class AdminEndpointAuthorizationTest {

    private static final List<String> ALLOWED_UNDECLARED = Arrays.asList("/admin/login");

    @Test
    @DisplayName("所有管理端接口都必须声明 @PreAuthorize（/admin/login 除外）")
    void allAdminEndpointsDeclarePreAuthorize() throws Exception {
        List<String> violations = new ArrayList<>();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        for (BeanDefinition definition : scanner.findCandidateComponents("com.walker.controller")) {
            Class<?> controllerClass = Class.forName(definition.getBeanClassName());
            String classPrefix = firstPath(AnnotatedElementUtils.findMergedAnnotation(
                    controllerClass, RequestMapping.class));
            PreAuthorize classAuth = AnnotatedElementUtils.findMergedAnnotation(
                    controllerClass, PreAuthorize.class);

            for (Method method : controllerClass.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                        method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String methodPath = firstPath(mapping);
                String fullPath = joinPath(classPrefix, methodPath);
                if (!isAdminPath(fullPath) || ALLOWED_UNDECLARED.contains(fullPath)) {
                    continue;
                }
                PreAuthorize methodAuth = AnnotatedElementUtils.findMergedAnnotation(
                        method, PreAuthorize.class);
                if (methodAuth == null && classAuth == null) {
                    violations.add(controllerClass.getSimpleName() + "#" + method.getName()
                            + " -> " + fullPath);
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "以下管理端接口缺少 @PreAuthorize：\n" + String.join("\n", violations));
    }

    private static String firstPath(RequestMapping mapping) {
        if (mapping == null) {
            return "";
        }
        String[] paths = mapping.path();
        if (paths.length == 0) {
            paths = mapping.value();
        }
        return paths.length == 0 ? "" : paths[0];
    }

    private static String joinPath(String prefix, String path) {
        String left = prefix == null ? "" : prefix.trim();
        String right = path == null ? "" : path.trim();
        if (left.isEmpty()) {
            return right.isEmpty() ? "" : right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return (left.endsWith("/") ? left.substring(0, left.length() - 1) : left)
                + (right.startsWith("/") ? right : "/" + right);
    }

    private static boolean isAdminPath(String path) {
        return path.startsWith("/admin")
                || path.startsWith("/reply/admin")
                || path.startsWith("/sensitiveWord")
                || path.startsWith("/saOrg")
                || path.startsWith("/common/saOrg/batchUpdate")
                || "/getAllUser".equals(path)
                || "/updateUserRole".equals(path);
    }
}
