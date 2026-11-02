package com.walker.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账号级角色（全局维度）。
 *
 * <p>角色以 bbs_user.user_type 为唯一事实来源：
 * "1" 普通用户、 "2" 管理员、 "3" 超级管理员、 "4" 版主。
 * 版主任职（bbs_board_moderator）仍属于板块级数据范围；
 * MODERATOR 账号没有板块时依然可以登录管理后台，但只能看到空的业务列表。</p>
 */
public enum AccountRole {

    USER("1"),
    ADMIN("2"),
    SUPER_ADMIN("3"),
    MODERATOR("4");

    private static final Logger log = LoggerFactory.getLogger(AccountRole.class);

    private final String code;

    AccountRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Spring Security 角色名，固定为 ROLE_ + 枚举名 */
    public String getAuthority() {
        return "ROLE_" + name();
    }

    /**
     * user_type 转账号角色。
     * 空值按普通用户处理；未知值按普通用户处理并记录告警，避免非法数据放大权限。
     */
    public static AccountRole fromUserType(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return USER;
        }
        String code = userType.trim();
        if (USER.code.equals(code)) {
            return USER;
        }
        if (ADMIN.code.equals(code)) {
            return ADMIN;
        }
        if (SUPER_ADMIN.code.equals(code)) {
            return SUPER_ADMIN;
        }
        if (MODERATOR.code.equals(code)) {
            return MODERATOR;
        }
        log.warn("检测到非法 user_type={}，已按普通用户处理", userType);
        return USER;
    }

    /** 是否为管理员或超级管理员（全局管理角色；不含 MODERATOR） */
    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    /** 是否为版主账号角色 */
    public boolean isModerator() {
        return this == MODERATOR;
    }

    /**
     * 是否允许进入管理后台。
     *
     * <p>注意：不能把 MODERATOR 并入 {@link #isAdmin()}，否则
     * {@code User#getAuthorities()} 会给版主追加 ROLE_ADMIN，造成越权。</p>
     */
    public boolean canAccessAdminConsole() {
        return isAdmin() || this == MODERATOR;
    }
}
