package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 有效权限实现（权限组即权限来源）。
 *
 * <p>用户的有效权限来自其主权限组与额外绑定的权限组：
 * <ol>
 *   <li>主权限组决定账号身份与数据范围；</li>
 *   <li>所有启用权限组的权限点合并叠加；</li>
 *   <li>历史 revoke 条目继续生效，收回优先于授予；</li>
 *   <li>没有权限组的账号没有管理端/前台动作权限。</li>
 * </ol>
 * 数据范围由权限组的 data_scope_type 决定，不在此处计算。</p>
 */
@Service
public class EffectivePermissionServiceImpl implements EffectivePermissionService {

    private static final String DEFAULT_GROUP_CODE = "role_user";

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
        if (user == null) {
            return new LinkedHashSet<>();
        }
        List<PermissionGroup> groups = loadEffectiveGroups(user);
        Set<Integer> groupIds = groups.stream()
                .map(PermissionGroup::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return computeEffectiveCodes(groupIds, loadItems(groupIds));
    }

    @Override
    public List<PermissionGroup> getEffectiveGroups(User user) {
        return loadEffectiveGroups(user);
    }

    @Override
    public List<Integer> getUserIdsWithPermission(Permission permission) {
        if (permission == null) {
            return Collections.emptyList();
        }
        // 广播只需要身份/主组字段，避免把密码等大字段一并加载；用户量增长时内存更可控。
        List<User> users = userMapper.selectList(new QueryWrapper<User>()
                .select("id", "primary_group_id"));
        List<UserPermissionGroup> allLinks = userPermissionGroupMapper.selectList(null);
        if (users == null) {
            users = Collections.emptyList();
        }
        if (allLinks == null) {
            allLinks = Collections.emptyList();
        }

        Map<Integer, List<UserPermissionGroup>> linksByUser = allLinks.stream()
                .filter(link -> link.getUserId() != null)
                .collect(Collectors.groupingBy(UserPermissionGroup::getUserId,
                        LinkedHashMap::new, Collectors.toList()));

        // 测试/历史数据中可能只有绑定关系、没有用户行；仍按绑定组参与计算
        Map<Integer, User> usersById = new LinkedHashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                usersById.put(user.getId(), user);
            }
        }
        for (Integer linkedUserId : linksByUser.keySet()) {
            if (!usersById.containsKey(linkedUserId)) {
                User placeholder = new User();
                placeholder.setId(linkedUserId);
                usersById.put(linkedUserId, placeholder);
            }
        }
        if (usersById.isEmpty()) {
            return Collections.emptyList();
        }

        List<PermissionGroup> enabledGroups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .eq(PermissionGroup::getEnabled, 1));
        Map<Integer, PermissionGroup> groupById = new LinkedHashMap<>();
        if (enabledGroups != null) {
            for (PermissionGroup group : enabledGroups) {
                if (group != null && group.getId() != null) {
                    groupById.put(group.getId(), group);
                }
            }
        }

        Integer defaultGroupId = defaultGroupId(groupById);
        Set<Integer> referencedGroupIds = new LinkedHashSet<>();
        for (User user : usersById.values()) {
            if (user.getPrimaryGroupId() != null) {
                referencedGroupIds.add(user.getPrimaryGroupId());
            }
            List<UserPermissionGroup> links = linksByUser.get(user.getId());
            if (links != null) {
                for (UserPermissionGroup link : links) {
                    if (link.getGroupId() != null) {
                        referencedGroupIds.add(link.getGroupId());
                    }
                }
            }
        }
        if (defaultGroupId != null) {
            referencedGroupIds.add(defaultGroupId);
        }
        Map<Integer, List<PermissionGroupItem>> itemsByGroup = loadItems(referencedGroupIds);

        List<Integer> result = new ArrayList<>();
        // 2000+ 用户里大多数共享同一组组合，按组集合缓存，重复的权限集合只计算一次。
        Map<String, Set<String>> codesByProfile = new LinkedHashMap<>();
        for (User user : usersById.values()) {
            Set<Integer> groupIds = collectGroupIds(user, linksByUser.get(user.getId()), groupById);
            String profileKey = groupIds.toString();
            Set<String> codes = codesByProfile.get(profileKey);
            if (codes == null) {
                codes = computeEffectiveCodes(groupIds, itemsByGroup);
                codesByProfile.put(profileKey, codes);
            }
            if (codes.contains(permission.getCode())) {
                result.add(user.getId());
            }
        }
        return result;
    }

    /** 主权限组 + 额外绑定组；只保留启用中的组，去重。 */
    private List<PermissionGroup> loadEffectiveGroups(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        Set<Integer> groupIds = new LinkedHashSet<>();
        if (user.getPrimaryGroupId() != null) {
            groupIds.add(user.getPrimaryGroupId());
        }
        if (user.getId() != null) {
            List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                    new LambdaQueryWrapper<UserPermissionGroup>()
                            .eq(UserPermissionGroup::getUserId, user.getId()));
            if (links != null) {
                for (UserPermissionGroup link : links) {
                    if (link.getGroupId() != null) {
                        groupIds.add(link.getGroupId());
                    }
                }
            }
        }
        List<PermissionGroup> groups = new ArrayList<>();
        if (!groupIds.isEmpty()) {
            List<PermissionGroup> loaded = permissionGroupMapper.selectList(
                    new LambdaQueryWrapper<PermissionGroup>()
                            .in(PermissionGroup::getId, groupIds)
                            .eq(PermissionGroup::getEnabled, 1));
            if (loaded != null) {
                groups.addAll(loaded);
            }
        }
        // 主组缺失/停用/被删除时，回退到默认普通用户组，保持会话展示与实际权限一致。
        boolean primaryValid = user.getPrimaryGroupId() != null && groups.stream()
                .anyMatch(group -> user.getPrimaryGroupId().equals(group.getId()));
        if (!primaryValid) {
            Integer defaultGroupId = defaultGroupId(groups);
            if (defaultGroupId == null) {
                PermissionGroup fallback = permissionGroupMapper.selectOne(
                        new LambdaQueryWrapper<PermissionGroup>()
                                .eq(PermissionGroup::getGroupCode, DEFAULT_GROUP_CODE)
                                .eq(PermissionGroup::getEnabled, 1)
                                .last("LIMIT 1"));
                if (fallback != null) {
                    groups.add(fallback);
                }
            } else if (groups.stream().noneMatch(group -> defaultGroupId.equals(group.getId()))) {
                PermissionGroup fallback = permissionGroupMapper.selectById(defaultGroupId);
                if (fallback != null && Integer.valueOf(1).equals(fallback.getEnabled())) {
                    groups.add(fallback);
                }
            }
        }
        return groups;
    }

    private Set<Integer> collectGroupIds(User user,
                                         List<UserPermissionGroup> links,
                                         Map<Integer, PermissionGroup> groupById) {
        Set<Integer> groupIds = new LinkedHashSet<>();
        boolean primaryValid = user.getPrimaryGroupId() != null
                && groupById.containsKey(user.getPrimaryGroupId());
        if (primaryValid) {
            groupIds.add(user.getPrimaryGroupId());
        }
        if (links != null) {
            for (UserPermissionGroup link : links) {
                if (link.getGroupId() != null && groupById.containsKey(link.getGroupId())) {
                    groupIds.add(link.getGroupId());
                }
            }
        }
        if (!primaryValid) {
            Integer defaultGroupId = defaultGroupId(groupById);
            if (defaultGroupId != null) {
                groupIds.add(defaultGroupId);
            }
        }
        return groupIds;
    }

    /**
     * 默认普通用户组：优先 group_code=role_user；
     * 其次选一个全局范围且不带 admin:access 的启用组，避免 role_user 被删后把普通用户回退到管理类组。
     */
    private Integer defaultGroupId(Map<Integer, PermissionGroup> groupById) {
        if (groupById == null || groupById.isEmpty()) {
            return null;
        }
        for (PermissionGroup group : groupById.values()) {
            if (DEFAULT_GROUP_CODE.equals(group.getGroupCode())) {
                return group.getId();
            }
        }
        List<Integer> candidateIds = new ArrayList<>();
        for (PermissionGroup group : groupById.values()) {
            if (!"BOARD_MODERATOR".equalsIgnoreCase(group.getDataScopeType())) {
                candidateIds.add(group.getId());
            }
        }
        if (candidateIds.isEmpty()) {
            return null;
        }
        Set<Integer> adminGroupIds = groupIdsGranting(Permission.ADMIN_ACCESS.getCode(), candidateIds);
        for (Integer groupId : candidateIds) {
            if (!adminGroupIds.contains(groupId)) {
                return groupId;
            }
        }
        return null;
    }

    private Integer defaultGroupId(List<PermissionGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return null;
        }
        Map<Integer, PermissionGroup> groupById = new LinkedHashMap<>();
        for (PermissionGroup group : groups) {
            if (group != null && group.getId() != null) {
                groupById.put(group.getId(), group);
            }
        }
        return defaultGroupId(groupById);
    }

    /** 给定权限组里，哪些直接授予了指定权限点（grant）。 */
    private Set<Integer> groupIdsGranting(String permissionCode, List<Integer> groupIds) {
        if (permissionCode == null || groupIds == null || groupIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .in(PermissionGroupItem::getGroupId, groupIds)
                        .eq(PermissionGroupItem::getPermissionCode, permissionCode)
                        .eq(PermissionGroupItem::getEffect, "grant"));
        if (items == null) {
            return Collections.emptySet();
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (PermissionGroupItem item : items) {
            if (item.getGroupId() != null) {
                result.add(item.getGroupId());
            }
        }
        return result;
    }

    /**
     * 单人/批量共用同一套计算：合并所有组的 grant/revoke，再统一应用收回优先。
     */
    private Set<String> computeEffectiveCodes(Set<Integer> groupIds,
                                              Map<Integer, List<PermissionGroupItem>> itemsByGroup) {
        Set<String> codes = new LinkedHashSet<>();
        Map<Integer, List<PermissionGroupItem>> effectiveItems = new LinkedHashMap<>();
        for (Integer groupId : groupIds) {
            effectiveItems.put(groupId, itemsByGroup.getOrDefault(groupId, Collections.emptyList()));
        }
        applyItems(codes, effectiveItems);
        return codes;
    }

    private Map<Integer, List<PermissionGroupItem>> loadItems(Set<Integer> groupIds) {
        Map<Integer, List<PermissionGroupItem>> result = new LinkedHashMap<>();
        if (groupIds == null || groupIds.isEmpty()) {
            return result;
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .in(PermissionGroupItem::getGroupId, groupIds));
        if (items == null) {
            return result;
        }
        for (PermissionGroupItem item : items) {
            if (item.getGroupId() == null) {
                continue;
            }
            result.computeIfAbsent(item.getGroupId(), key -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private void applyItems(Set<String> codes,
                            Map<Integer, List<PermissionGroupItem>> itemsByGroup) {
        Set<String> grants = new LinkedHashSet<>();
        Set<String> revokes = new LinkedHashSet<>();
        for (List<PermissionGroupItem> items : itemsByGroup.values()) {
            for (PermissionGroupItem item : items) {
                if (item.getPermissionCode() == null
                        || Permission.fromCode(item.getPermissionCode()) == null) {
                    continue;
                }
                if ("revoke".equalsIgnoreCase(item.getEffect())) {
                    revokes.add(item.getPermissionCode());
                } else {
                    grants.add(item.getPermissionCode());
                }
            }
        }

        codes.addAll(grants);
        codes.removeAll(revokes);
    }
}
