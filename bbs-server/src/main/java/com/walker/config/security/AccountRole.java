package com.walker.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 账号级角色（全局维度）。
 *
 * <p>角色以 bbs_user.user_type 为唯一事实来源：
 * "1" 普通用户、 "2" 管理员、 "3" 超级管理员。
 * 版主任职属于板块级数据范围，不在此枚举中。</p>
 */
public enum AccountRole {

    USER("1"),
    ADMIN("2"),
    SUPER_ADMIN("3");

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
        log.warn("检测到非法 user_type={}，已按普通用户处理", userType);
        return USER;
    }

    /** 是否为管理员或超级管理员 */
    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }
}
