package com.walker.config.security;

/**
 * 权限点（固定角色 + 静态权限映射）。
 *
 * <p>ADMIN 默认拥有除 user:grant:admin、config:write 外的管理权限；
 * SUPER_ADMIN 拥有全部权限。USER 不具备管理端权限点。</p>
 */
public enum Permission {

    ADMIN_ACCESS("admin:access", true),

    USER_READ("user:read", true),
    USER_CREATE("user:create", true),
    USER_UPDATE("user:update", true),
    USER_DELETE("user:delete", true),
    USER_GRANT_ADMIN("user:grant:admin", false),

    CONFIG_READ("config:read", true),
    CONFIG_WRITE("config:write", false),

    ARTICLE_READ("article:read", true),
    ARTICLE_REVIEW("article:review", true),
    ARTICLE_DELETE("article:delete", true),
    ARTICLE_FEATURE("article:feature", true),
    ARTICLE_SUGGESTION_ADOPT("article:suggestion:adopt", true),

    REPORT_REVIEW("report:review", true),
    APPEAL_REVIEW("appeal:review", true),

    MODERATOR_MANAGE("moderator:manage", true),
    MODERATOR_COMPLAINT_REVIEW("moderator:complaint:review", true),
    ADOPT_REVIEW("adopt:review", true),

    FEATURED_RECOMMEND("featured:recommend", true),
    FEATURED_REVIEW("featured:review", true),

    POINTS_READ("points:read", true),
    POINTS_ADJUST("points:adjust", true),

    DASHBOARD_READ("dashboard:read", true);

    private final String code;
    /** ADMIN 是否默认拥有；SUPER_ADMIN 恒为 true */
    private final boolean adminDefault;

    Permission(String code, boolean adminDefault) {
        this.code = code;
        this.adminDefault = adminDefault;
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
