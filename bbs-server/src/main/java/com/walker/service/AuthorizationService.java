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

    /** 是否全局管理员（ADMIN 或 SUPER_ADMIN）；全局管理员不受板块范围限制 */
    boolean isGlobalAdmin(User user);

    /** 是否 MODERATOR 账号角色 */
    boolean isModerator(User user);

    /** 用户的有效板块 ID（status=1）；无任职返回空列表 */
    List<Integer> getActiveBoardIds(Integer userId);

    /**
     * 当前用户可见的板块范围。
     *
     * @return {@code null} 表示全局管理员（不做板块过滤）；空列表表示版主但尚未分配板块（应返回空数据）
     */
    List<Integer> getVisibleBoardIds(User user);

    /**
     * 板块访问校验，返回 null 表示允许，否则返回应提示给用户的文案。
     *
     * <p>文案优先级：未登录 → 无板块 → 无法确定板块 → 跨板块。</p>
     */
    String getBoardScopeDenialMessage(User user, Integer labelId);

    /** 是否拥有权限点 */
    boolean hasPermission(User user, Permission permission);

    /** 是否可管理指定板块：ADMIN/SUPER_ADMIN 全局放行，MODERATOR 要求有效版主任职 */
    boolean canModerateBoard(Integer userId, Integer labelId);

    /** 拥有指定权限点的用户 ID（用于按权限广播系统通知） */
    List<Integer> getUserIdsWithPermission(Permission permission);

    /** 无权限时抛出 ForbiddenException */
    void requirePermission(User user, Permission permission);

    /** 非全局管理员且不是该板块版主时抛出 ForbiddenException */
    void requireBoardModerator(Integer userId, Integer labelId);
}
