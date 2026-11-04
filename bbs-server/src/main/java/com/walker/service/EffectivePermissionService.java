package com.walker.service;

import com.walker.config.security.Permission;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.User;

import java.util.List;
import java.util.Set;

/**
 * 有效权限计算：主权限组 + 额外绑定权限组的权限点合并。
 */
public interface EffectivePermissionService {

    /**
     * 用户的有效权限点编码（所有启用权限组的授予，revoke 优先）。
     */
    Set<String> getEffectiveCodes(User user);

    /**
     * 用户当前生效的权限组（主组 + 额外绑定组，仅启用中的组）。
     */
    List<PermissionGroup> getEffectiveGroups(User user);

    /**
     * 拥有指定权限点的用户 ID（按当前权限组计算）。
     */
    List<Integer> getUserIdsWithPermission(Permission permission);
}
