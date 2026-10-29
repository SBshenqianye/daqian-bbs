package com.walker.service;

import com.walker.config.security.Permission;
import com.walker.pojo.User;

import java.util.List;

/**
 * 统一授权服务。
 *
 * <p>是否允许操作（权限点）与是否允许对自己操作（SelfApprovalPolicy）分离。
 * 业务代码不再直接判断 id == 1。</p>
 */
public interface AuthorizationService {

    /** 指定用户是否超级管理员（按 AccountRole/SUPER_ADMIN 判定，不依赖 id） */
    boolean isSuperAdmin(User user);

    /** 按用户 ID 判断超级管理员；当前登录人优先使用登录态，避免额外查询 */
    boolean isSuperAdmin(Integer userId);

    /** 是否拥有权限点 */
    boolean hasPermission(User user, Permission permission);

    /** 是否可管理指定板块：超管放行，否则要求有效版主任职 */
    boolean canModerateBoard(Integer userId, Integer labelId);

    /** 拥有指定权限点的用户 ID（用于按权限广播系统通知） */
    List<Integer> getUserIdsWithPermission(Permission permission);

    /** 无权限时抛出 ForbiddenException */
    void requirePermission(User user, Permission permission);

    /** 非超管且不是该板块版主时抛出 ForbiddenException */
    void requireBoardModerator(Integer userId, Integer labelId);
}
