package com.walker.service.impl;

import com.walker.config.security.AccountRole;
import com.walker.config.security.ForbiddenException;
import com.walker.config.security.Permission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.User;
import com.walker.service.AuthorizationService;
import com.walker.service.BoardModeratorService;
import com.walker.service.CurrentUserService;
import com.walker.service.EffectivePermissionService;
import com.walker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 统一授权服务实现。
 */
@Service("authz")
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private BoardModeratorService boardModeratorService;

    @Autowired
    private EffectivePermissionService effectivePermissionService;

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
        if (isSuperAdmin(user)) {
            return true;
        }
        return effectivePermissionService.getEffectiveCodes(user).contains(permission.getCode());
    }

    @Override
    public boolean isGlobalAdmin(User user) {
        return user != null && AccountRole.fromUserType(user.getUserType()).isAdmin();
    }

    @Override
    public boolean isModerator(User user) {
        return user != null && AccountRole.fromUserType(user.getUserType()).isModerator();
    }

    @Override
    public List<Integer> getActiveBoardIds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<BoardModerator> rows = boardModeratorService.list(
                new LambdaQueryWrapper<BoardModerator>()
                        .eq(BoardModerator::getUserId, userId)
                        .eq(BoardModerator::getStatus, 1));
        return rows.stream()
                .map(BoardModerator::getLabelId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getVisibleBoardIds(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        if (isGlobalAdmin(user)) {
            // null 表示“不做板块过滤”，与“空列表”严格区分
            return null;
        }
        if (isModerator(user)) {
            return getActiveBoardIds(user.getId());
        }
        return Collections.emptyList();
    }

    @Override
    public String getBoardScopeDenialMessage(User user, Integer labelId) {
        if (user == null) {
            return "未获取到登录用户信息，请重新登录";
        }
        if (isGlobalAdmin(user)) {
            return null;
        }
        if (!isModerator(user)) {
            return "权限不足，请联系管理员";
        }
        List<Integer> boards = getActiveBoardIds(user.getId());
        if (boards.isEmpty()) {
            return "尚未分配板块，请联系管理员";
        }
        if (labelId == null) {
            return "无法确定内容所属板块，请联系管理员";
        }
        if (!boards.contains(labelId)) {
            return "无权操作该板块内容";
        }
        return null;
    }

    @Override
    public boolean canModerateBoard(Integer userId, Integer labelId) {
        if (userId == null) {
            return false;
        }
        User user = currentUserService.getCurrentUser();
        if (user == null || !userId.equals(user.getId())) {
            user = userService.getById(userId);
        }
        if (user == null) {
            return false;
        }
        // ADMIN / SUPER_ADMIN 为全局管理员，不受板块范围限制（Q1 已确认）
        if (isGlobalAdmin(user)) {
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
        return effectivePermissionService.getUserIdsWithPermission(permission);
    }

    @Override
    public void requireBoardModerator(Integer userId, Integer labelId) {
        User user = currentUserService.getCurrentUser();
        if (user == null || !userId.equals(user.getId())) {
            user = userService.getById(userId);
        }
        String denial = getBoardScopeDenialMessage(user, labelId);
        if (denial != null) {
            log.warn("[权限拒绝] userId={}, labelId={}, reason={}", userId, labelId, denial);
            throw new ForbiddenException(denial);
        }
    }
}
