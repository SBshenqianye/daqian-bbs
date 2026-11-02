package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.config.security.AccountRole;
import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.User;
import com.walker.pojo.UserPermissionGroup;
import com.walker.service.EffectivePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 有效权限实现。
 *
 * <p>Phase B 采用 grant-only：权限组只做并集，不收回角色基线；
 * 数据范围仍由账号角色和 bbs_board_moderator 决定。</p>
 */
@Service
public class EffectivePermissionServiceImpl implements EffectivePermissionService {

    @Autowired
    private PermissionGroupMapper permissionGroupMapper;

    @Autowired
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Autowired
    private UserPermissionGroupMapper userPermissionGroupMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Set<String> getEffectiveCodes(User user) {
        Set<String> codes = new LinkedHashSet<>();
        if (user == null) {
            return codes;
        }
        AccountRole role = AccountRole.fromUserType(user.getUserType());
        codes.addAll(Permission.codesFor(role));
        if (role == AccountRole.SUPER_ADMIN) {
            return codes;
        }

        List<Integer> groupIds = activeGroupIds(user.getId());
        if (groupIds.isEmpty()) {
            return codes;
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .in(PermissionGroupItem::getGroupId, groupIds)
                        .eq(PermissionGroupItem::getEffect, "grant"));
        for (PermissionGroupItem item : items) {
            if (Permission.fromCode(item.getPermissionCode()) != null) {
                codes.add(item.getPermissionCode());
            }
        }
        return codes;
    }

    @Override
    public List<Integer> getUserIdsWithPermission(Permission permission) {
        if (permission == null) {
            return Collections.emptyList();
        }
        Set<Integer> userIds = new LinkedHashSet<>();

        // 1) 角色基线
        List<String> userTypes = new ArrayList<>();
        if (permission.isGrantedTo(AccountRole.USER)) {
            userTypes.add(AccountRole.USER.getCode());
        }
        if (permission.isGrantedTo(AccountRole.MODERATOR)) {
            userTypes.add(AccountRole.MODERATOR.getCode());
        }
        if (permission.isGrantedTo(AccountRole.ADMIN)) {
            userTypes.add(AccountRole.ADMIN.getCode());
        }
        if (permission.isGrantedTo(AccountRole.SUPER_ADMIN)) {
            userTypes.add(AccountRole.SUPER_ADMIN.getCode());
        }
        if (!userTypes.isEmpty()) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .in(User::getUserType, userTypes));
            for (User user : users) {
                if (user.getId() != null) {
                    userIds.add(user.getId());
                }
            }
        }

        // 2) 权限组额外授予
        List<Integer> groupIds = groupIdsWithPermission(permission.getCode());
        if (!groupIds.isEmpty()) {
            List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                    new LambdaQueryWrapper<UserPermissionGroup>()
                            .in(UserPermissionGroup::getGroupId, groupIds));
            for (UserPermissionGroup link : links) {
                if (link.getUserId() != null) {
                    userIds.add(link.getUserId());
                }
            }
        }
        return new ArrayList<>(userIds);
    }

    private List<Integer> activeGroupIds(Integer userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                new LambdaQueryWrapper<UserPermissionGroup>()
                        .eq(UserPermissionGroup::getUserId, userId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> groupIds = links.stream()
                .map(UserPermissionGroup::getGroupId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PermissionGroup> enabledGroups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .in(PermissionGroup::getId, groupIds)
                        .eq(PermissionGroup::getEnabled, 1));
        return enabledGroups.stream()
                .map(PermissionGroup::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }

    private List<Integer> groupIdsWithPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getPermissionCode, permissionCode)
                        .eq(PermissionGroupItem::getEffect, "grant"));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> groupIds = items.stream()
                .map(PermissionGroupItem::getGroupId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<PermissionGroup> enabledGroups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .in(PermissionGroup::getId, groupIds)
                        .eq(PermissionGroup::getEnabled, 1));
        return enabledGroups.stream()
                .map(PermissionGroup::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }
}
