package com.walker.service.impl;

import com.walker.config.security.AccountRole;
import com.walker.pojo.User;
import com.walker.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 当前登录用户上下文服务实现。
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    @Override
    public Integer getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    @Override
    public boolean isSuperAdmin() {
        User user = getCurrentUser();
        return user != null && AccountRole.fromUserType(user.getUserType()) == AccountRole.SUPER_ADMIN;
    }
}
