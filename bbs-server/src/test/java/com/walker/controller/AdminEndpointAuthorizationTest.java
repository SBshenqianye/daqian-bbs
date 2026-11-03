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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    @Test
    @DisplayName("标签/社区/组织写接口仍允许 ADMIN（未被 config:write 误伤）")
    void adminBusinessWritesRemainAvailableToAdmin() {
        assertPermissionGrantedToAdmin(ArticleLabelController.class, "addArticleLabel");
        assertPermissionGrantedToAdmin(ArticleLabelController.class, "updateArticleLabel");
        assertPermissionGrantedToAdmin(ArticleLabelController.class, "deleteArticleLabel");
        assertPermissionGrantedToAdmin(CommunityController.class, "updateCommunityStatus");
        assertPermissionGrantedToAdmin(CommunityController.class, "deleteCommunityByCommunityId");
        assertPermissionGrantedToAdmin(SaOrgController.class, "deleteSaOrgByOrgNo");
        assertPermissionGrantedToAdmin(SaOrgController.class, "addSaOrg");
        assertPermissionGrantedToAdmin(SaOrgController.class, "batchUpdateRanking");
        assertPermissionGrantedToAdmin(SaOrgController.class, "batchUpdateDisplay");
    }

    private static void assertPermissionGrantedToAdmin(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            PreAuthorize auth = AnnotatedElementUtils.findMergedAnnotation(
                    method, PreAuthorize.class);
            assertNotNull(auth, controller.getSimpleName() + "#" + methodName + " 缺少 @PreAuthorize");
            Matcher matcher = Pattern.compile("Permission\\)\\.([A-Z_]+)").matcher(auth.value());
            assertTrue(matcher.find(), controller.getSimpleName() + "#" + methodName
                    + " 未使用权限点: " + auth.value());
            com.walker.config.security.Permission permission =
                    com.walker.config.security.Permission.valueOf(matcher.group(1));
            assertTrue(permission.isGrantedTo(com.walker.config.security.AccountRole.ADMIN),
                    controller.getSimpleName() + "#" + methodName
                            + " 使用的权限点 ADMIN 不具备: " + permission.getCode());
            return;
        }
        fail("未找到方法 " + controller.getSimpleName() + "#" + methodName);
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
