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
import org.apache.commons.lang3.StringUtils;
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
 * 有效权限实现（权限组即角色）。
 *
 * <p>权限来源优先级：
 * <ol>
 *   <li>用户的主权限组（{@code bbs_user.primary_group_id}）必须指向启用的系统角色组，
 *       以该组 baseRole 的角色基线为准，再叠加该组的 grant / revoke；</li>
 *   <li>主组为空、失效或不是系统组时，回退到 user_type 对应的角色基线（兼容旧数据/旧 token）；</li>
 *   <li>显式绑定的临时自定义权限组继续叠加；系统组不参与手动绑定；</li>
 *   <li>收回优先；超级管理员的关键权限不允许收回。</li>
 * </ol>
 * 数据范围仍由账号角色和 bbs_board_moderator 决定，不在此处计算。</p>
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

        PermissionGroup primaryGroup = resolveEnabledSystemGroup(user.getPrimaryGroupId());
        AccountRole baseRole = primaryGroup == null
                ? AccountRole.fromUserType(user.getUserType())
                : parseBaseRole(primaryGroup.getBaseRole());
        if (baseRole == null) {
            baseRole = AccountRole.USER;
        }
        boolean protectSuper = baseRole == AccountRole.SUPER_ADMIN
                || AccountRole.fromUserType(user.getUserType()) == AccountRole.SUPER_ADMIN;

        codes.addAll(Permission.codesFor(baseRole));

        Map<Integer, List<PermissionGroupItem>> itemsByGroup = loadItemsByGroup(
                user.getId(), primaryGroup, true);
        applyItems(codes, itemsByGroup, protectSuper);
        addSuperProtected(codes, user);
        return codes;
    }

    @Override
    public List<Integer> getUserIdsWithPermission(Permission permission) {
        if (permission == null) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(null);
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

        // 测试/历史数据中可能只有绑定关系、没有用户行；仍按 USER 基线参与计算
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
        Map<Integer, List<PermissionGroupItem>> itemsByGroup = loadItems(referencedGroupIds);

        List<Integer> result = new ArrayList<>();
        for (User user : usersById.values()) {
            Set<String> codes = effectiveCodes(user, groupById, linksByUser, itemsByGroup);
            if (codes.contains(permission.getCode())) {
                result.add(user.getId());
            }
        }
        return result;
    }

    private Set<String> effectiveCodes(User user,
                                       Map<Integer, PermissionGroup> groupById,
                                       Map<Integer, List<UserPermissionGroup>> linksByUser,
                                       Map<Integer, List<PermissionGroupItem>> itemsByGroup) {
        Set<String> codes = new LinkedHashSet<>();
        PermissionGroup primaryGroup = systemGroup(groupById.get(user.getPrimaryGroupId()));
        AccountRole baseRole = primaryGroup == null
                ? AccountRole.fromUserType(user.getUserType())
                : parseBaseRole(primaryGroup.getBaseRole());
        if (baseRole == null) {
            baseRole = AccountRole.USER;
        }
        boolean protectSuper = baseRole == AccountRole.SUPER_ADMIN
                || AccountRole.fromUserType(user.getUserType()) == AccountRole.SUPER_ADMIN;

        codes.addAll(Permission.codesFor(baseRole));
        if (primaryGroup != null && primaryGroup.getId() != null) {
            applyItems(codes, Collections.singletonMap(primaryGroup.getId(),
                    itemsByGroup.getOrDefault(primaryGroup.getId(), Collections.emptyList())), protectSuper);
        }

        List<UserPermissionGroup> links = linksByUser.get(user.getId());
        if (links != null) {
            for (UserPermissionGroup link : links) {
                PermissionGroup group = groupById.get(link.getGroupId());
                if (!isTemporaryGroup(group) || group.getId().equals(
                        primaryGroup == null ? null : primaryGroup.getId())) {
                    continue;
                }
                applyItems(codes, Collections.singletonMap(group.getId(),
                        itemsByGroup.getOrDefault(group.getId(), Collections.emptyList())), protectSuper);
            }
        }
        addSuperProtected(codes, user);
        return codes;
    }

    /** 超管关键权限在任何主组/临时组组合下都必须保留。 */
    private void addSuperProtected(Set<String> codes, User user) {
        if (AccountRole.fromUserType(user.getUserType()) != AccountRole.SUPER_ADMIN) {
            return;
        }
        for (Permission permission : Permission.values()) {
            if (permission.isSuperAdminProtected()) {
                codes.add(permission.getCode());
            }
        }
    }

    /**
     * 加载主组 + 显式绑定的临时组下所有权限条目。
     */
    private Map<Integer, List<PermissionGroupItem>> loadItemsByGroup(Integer userId,
                                                                     PermissionGroup primaryGroup,
                                                                     boolean includeTemporary) {
        Set<Integer> groupIds = new LinkedHashSet<>();
        if (primaryGroup != null && primaryGroup.getId() != null) {
            groupIds.add(primaryGroup.getId());
        }
        if (includeTemporary && userId != null) {
            List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                    new LambdaQueryWrapper<UserPermissionGroup>()
                            .eq(UserPermissionGroup::getUserId, userId));
            if (links != null && !links.isEmpty()) {
                List<Integer> linkedGroupIds = links.stream()
                        .map(UserPermissionGroup::getGroupId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList());
                if (!linkedGroupIds.isEmpty()) {
                    List<PermissionGroup> linkedGroups = permissionGroupMapper.selectList(
                            new LambdaQueryWrapper<PermissionGroup>()
                                    .in(PermissionGroup::getId, linkedGroupIds)
                                    .eq(PermissionGroup::getEnabled, 1));
                    if (linkedGroups != null) {
                        for (PermissionGroup group : linkedGroups) {
                            if (isTemporaryGroup(group)) {
                                groupIds.add(group.getId());
                            }
                        }
                    }
                }
            }
        }
        return loadItems(groupIds);
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
                            Map<Integer, List<PermissionGroupItem>> itemsByGroup,
                            boolean protectSuper) {
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
        for (String code : revokes) {
            Permission permission = Permission.fromCode(code);
            if (permission == null) {
                continue;
            }
            if (protectSuper && permission.isSuperAdminProtected()) {
                continue;
            }
            codes.remove(code);
        }
    }

    /** 主组必须是启用的系统角色组（base_role 非空）。 */
    private PermissionGroup resolveEnabledSystemGroup(Integer groupId) {
        if (groupId == null) {
            return null;
        }
        PermissionGroup group = permissionGroupMapper.selectById(groupId);
        return systemGroup(group);
    }

    private PermissionGroup systemGroup(PermissionGroup group) {
        if (group == null || !Integer.valueOf(1).equals(group.getEnabled())) {
            return null;
        }
        if (StringUtils.isBlank(group.getBaseRole())) {
            return null;
        }
        return group;
    }

    private boolean isTemporaryGroup(PermissionGroup group) {
        return group != null
                && Integer.valueOf(1).equals(group.getEnabled())
                && StringUtils.isBlank(group.getBaseRole());
    }

    private AccountRole parseBaseRole(String baseRole) {
        if (StringUtils.isBlank(baseRole)) {
            return null;
        }
        try {
            return AccountRole.valueOf(baseRole.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
