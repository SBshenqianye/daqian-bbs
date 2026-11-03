package com.walker.service;

import com.walker.config.security.Permission;
import com.walker.pojo.User;

import java.util.List;
import java.util.Set;

/**
 * 有效权限计算：主权限组角色基线 + grant/revoke，再叠加临时权限组。
 */
public interface EffectivePermissionService {

    /**
     * 用户的有效权限点编码（角色基线 + 启用权限组的额外授予）。
     */
    Set<String> getEffectiveCodes(User user);

    /**
     * 拥有指定权限点的用户 ID（角色基线 + 权限组）。
     */
    List<Integer> getUserIdsWithPermission(Permission permission);
}
