package com.walker.service;

import com.walker.pojo.User;

/**
 * 当前登录用户上下文服务。
 *
 * <p>统一从 SecurityContextHolder 获取可信身份，禁止业务代码各自解析
 * Authentication，也禁止信任前端传入的 operatorId/reviewerId/adminId。</p>
 */
public interface CurrentUserService {

    /** 获取当前登录用户，未登录返回 null */
    User getCurrentUser();

    /** 获取当前登录用户 ID，未登录返回 null */
    Integer getCurrentUserId();

    /** 当前登录用户是否超级管理员 */
    boolean isSuperAdmin();
}
