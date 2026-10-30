package com.walker.service;

import com.walker.config.security.AccountRole;
import com.walker.config.security.Permission;
import com.walker.pojo.User;
import com.walker.service.impl.CurrentUserServiceImpl;
import com.walker.service.impl.SelfApprovalPolicyImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;

/**
 * 权限相关单元测试辅助：把真实 CurrentUserService / SelfApprovalPolicy 注入被测对象。
 * 测试中仍通过 SecurityContextHolder 模拟 JWT 登录态。
 */
public final class SecurityTestSupport {

    private SecurityTestSupport() {
    }

    /** 注入真实 CurrentUserServiceImpl（字段名 currentUserService） */
    public static void injectCurrentUser(Object target) {
        setField(target, "currentUserService", new CurrentUserServiceImpl());
    }

    /** 注入内容板块解析器（Report/Violation 等按内容解析板块的服务） */
    public static void injectContentBoardResolver(Object target, ContentBoardResolver resolver) {
        setField(target, "contentBoardResolver", resolver);
    }

    /**
     * 注入授权服务并给出宽松默认：当前登录人可管理其请求的板块。
     * 跨板块用例可在测试内覆盖 canModerateBoard 返回值。
     */
    public static void injectScopeServices(Object target, ContentBoardResolver resolver,
                                           AuthorizationService authorizationService) {
        setField(target, "contentBoardResolver", resolver);
        setField(target, "authorizationService", authorizationService);
        lenient().when(authorizationService.isSuperAdmin(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && "3".equals(user.getUserType());
        });
        lenient().when(authorizationService.canModerateBoard(any(Integer.class), nullable(Integer.class)))
                .thenReturn(true);
        lenient().when(authorizationService.isGlobalAdmin(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && ("2".equals(user.getUserType()) || "3".equals(user.getUserType()));
        });
        lenient().when(authorizationService.isModerator(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && "4".equals(user.getUserType());
        });
        // 默认按全局管理员处理（不做板块过滤）；板块用例可在测试内覆盖
        lenient().when(authorizationService.getVisibleBoardIds(any(User.class))).thenReturn(null);
        lenient().when(authorizationService.getBoardScopeDenialMessage(any(User.class), nullable(Integer.class)))
                .thenReturn(null);
    }

    /** 注入真实 SelfApprovalPolicyImpl，并让超管判定跟随 User.userType */
    public static void injectSelfApprovalPolicy(Object target, AuthorizationService authorizationService) {
        CurrentUserServiceImpl currentUserService = new CurrentUserServiceImpl();
        setField(target, "currentUserService", currentUserService);

        SelfApprovalPolicyImpl policy = new SelfApprovalPolicyImpl();
        setField(policy, "authorizationService", authorizationService);
        setField(target, "selfApprovalPolicy", policy);

        lenient().when(authorizationService.isSuperAdmin(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && "3".equals(user.getUserType());
        });
        lenient().when(authorizationService.hasPermission(any(User.class), any(Permission.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    Permission permission = invocation.getArgument(1);
                    return user != null && permission != null
                            && permission.isGrantedTo(AccountRole.fromUserType(user.getUserType()));
                });
        lenient().when(authorizationService.canModerateBoard(any(Integer.class), nullable(Integer.class)))
                .thenAnswer(invocation -> {
                    User current = currentUserService.getCurrentUser();
                    Integer userId = invocation.getArgument(0);
                    return current != null && current.getId() != null
                            && current.getId().equals(userId);
                });
        lenient().when(authorizationService.isGlobalAdmin(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && ("2".equals(user.getUserType()) || "3".equals(user.getUserType()));
        });
        lenient().when(authorizationService.isModerator(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user != null && "4".equals(user.getUserType());
        });
        lenient().when(authorizationService.getVisibleBoardIds(any(User.class))).thenReturn(null);
        lenient().when(authorizationService.getBoardScopeDenialMessage(any(User.class), nullable(Integer.class)))
                .thenReturn(null);
        lenient().when(authorizationService.getUserIdsWithPermission(any(Permission.class)))
                .thenReturn(java.util.Collections.singletonList(1));
    }

    /**
     * 模拟登录态：id=1 视为超管（user_type=3），其余视为管理员（user_type=2）。
     * 与初始化 SQL 的种子超管语义一致，但权限判定已改为依据 user_type。
     */
    public static void setCurrentUser(int id) {
        setCurrentUser(id, id == 1 ? "3" : "2");
    }

    /** 指定账号角色模拟登录态（如 user_type=4 为 MODERATOR） */
    public static void setCurrentUser(int id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("注入测试依赖失败: " + target.getClass().getSimpleName()
                    + "." + fieldName, e);
        }
    }
}
