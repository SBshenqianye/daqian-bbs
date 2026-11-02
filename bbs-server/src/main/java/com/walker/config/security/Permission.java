package com.walker.config.security;

/**
 * 权限点（固定角色 + 静态权限映射）。
 *
 * <p>ADMIN 默认拥有除 user:grant:admin、config:write、permission:manage、audit:read 外的管理权限；
 * SUPER_ADMIN 拥有全部权限；MODERATOR 仅拥有业务管理权限（板块范围在服务层校验）；
 * USER 不具备管理端权限点。</p>
 */
public enum Permission {

    ADMIN_ACCESS("admin:access", true, true),

    USER_READ("user:read", true, false),
    USER_CREATE("user:create", true, false),
    USER_UPDATE("user:update", true, false),
    USER_DELETE("user:delete", true, false),
    USER_GRANT_ADMIN("user:grant:admin", false, false),

    CONFIG_READ("config:read", true, false),
    CONFIG_WRITE("config:write", false, false),
    /** 权限矩阵只读页：仅超级管理员 */
    PERMISSION_MANAGE("permission:manage", false, false),
    /** 审计日志查询：仅超级管理员 */
    AUDIT_READ("audit:read", false, false),

    ARTICLE_READ("article:read", true, true),
    ARTICLE_REVIEW("article:review", true, true),
    ARTICLE_DELETE("article:delete", true, true),
    ARTICLE_FEATURE("article:feature", true, true),
    ARTICLE_SUGGESTION_ADOPT("article:suggestion:adopt", true, true),

    REPORT_REVIEW("report:review", true, true),
    APPEAL_REVIEW("appeal:review", true, true),

    MODERATOR_MANAGE("moderator:manage", true, false),
    MODERATOR_COMPLAINT_REVIEW("moderator:complaint:review", true, false),
    ADOPT_REVIEW("adopt:review", true, true),

    FEATURED_RECOMMEND("featured:recommend", true, true),
    FEATURED_REVIEW("featured:review", true, false),

    POINTS_READ("points:read", true, true),
    POINTS_ADJUST("points:adjust", true, false),

    DASHBOARD_READ("dashboard:read", true, true);

    private final String code;
    /** ADMIN 是否默认拥有；SUPER_ADMIN 恒为 true */
    private final boolean adminDefault;
    /** MODERATOR 是否默认拥有；板块范围由服务层另行校验 */
    private final boolean moderatorDefault;

    Permission(String code, boolean adminDefault) {
        this(code, adminDefault, false);
    }

    Permission(String code, boolean adminDefault, boolean moderatorDefault) {
        this.code = code;
        this.adminDefault = adminDefault;
        this.moderatorDefault = moderatorDefault;
    }

    public String getCode() {
        return code;
    }

    /** 指定账号角色是否拥有该权限点 */
    public boolean isGrantedTo(AccountRole role) {
        if (role == AccountRole.SUPER_ADMIN) {
            return true;
        }
        if (role == AccountRole.ADMIN) {
            return adminDefault;
        }
        if (role == AccountRole.MODERATOR) {
            return moderatorDefault;
        }
        return false;
    }

    /** 指定账号角色的全部权限点编码，用于登录响应/前端渲染 */
    public static java.util.List<String> codesFor(AccountRole role) {
        java.util.List<String> codes = new java.util.ArrayList<>();
        for (Permission permission : values()) {
            if (permission.isGrantedTo(role)) {
                codes.add(permission.getCode());
            }
        }
        return codes;
    }
}
