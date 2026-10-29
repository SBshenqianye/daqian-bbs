package com.walker.service.impl;

import com.walker.config.security.AccountRole;
import com.walker.config.security.ForbiddenException;
import com.walker.config.security.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.User;
import com.walker.service.AuthorizationService;
import com.walker.service.BoardModeratorService;
import com.walker.service.CurrentUserService;
import com.walker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一授权服务实现。
 */
@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private BoardModeratorService boardModeratorService;

    @Override
    public boolean isSuperAdmin(User user) {
        return user != null && AccountRole.fromUserType(user.getUserType()) == AccountRole.SUPER_ADMIN;
    }

    @Override
    public boolean isSuperAdmin(Integer userId) {
        if (userId == null) {
            return false;
        }
        User current = currentUserService.getCurrentUser();
        if (current != null && userId.equals(current.getId())) {
            return isSuperAdmin(current);
        }
        return isSuperAdmin(userService.getById(userId));
    }

    @Override
    public boolean hasPermission(User user, Permission permission) {
        if (user == null || permission == null) {
            return false;
        }
        AccountRole role = AccountRole.fromUserType(user.getUserType());
        return permission.isGrantedTo(role);
    }

    @Override
    public boolean canModerateBoard(Integer userId, Integer labelId) {
        if (userId == null) {
            return false;
        }
        if (isSuperAdmin(userId)) {
            return true;
        }
        if (labelId == null) {
            return false;
        }
        return boardModeratorService.hasActiveRole(userId, labelId);
    }

    @Override
    public void requirePermission(User user, Permission permission) {
        if (!hasPermission(user, permission)) {
            log.warn("[权限拒绝] userId={}, permission={}", user != null ? user.getId() : null, permission);
            throw new ForbiddenException("权限不足，请联系管理员");
        }
    }

    @Override
    public List<Integer> getUserIdsWithPermission(Permission permission) {
        if (permission == null) {
            return Collections.emptyList();
        }
        List<String> userTypes = new ArrayList<>();
        if (permission.isGrantedTo(AccountRole.ADMIN)) {
            userTypes.add(AccountRole.ADMIN.getCode());
            userTypes.add(AccountRole.SUPER_ADMIN.getCode());
        } else if (permission.isGrantedTo(AccountRole.SUPER_ADMIN)) {
            userTypes.add(AccountRole.SUPER_ADMIN.getCode());
        }
        if (userTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return userService.list(new LambdaQueryWrapper<User>()
                        .in(User::getUserType, userTypes))
                .stream()
                .map(User::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    @Override
    public void requireBoardModerator(Integer userId, Integer labelId) {
        if (!canModerateBoard(userId, labelId)) {
            log.warn("[权限拒绝] userId={}, labelId={}, 需要版主数据范围", userId, labelId);
            throw new ForbiddenException("无权操作该板块内容");
        }
    }
}
