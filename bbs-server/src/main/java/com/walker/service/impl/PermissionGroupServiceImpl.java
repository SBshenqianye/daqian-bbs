package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.config.security.Permission;
import com.walker.mapper.PermissionGroupItemMapper;
import com.walker.mapper.PermissionGroupMapper;
import com.walker.mapper.PermissionPresetMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.UserPermissionGroupMapper;
import com.walker.pojo.PermissionGroup;
import com.walker.pojo.PermissionGroupItem;
import com.walker.pojo.PermissionPreset;
import com.walker.pojo.User;
import com.walker.pojo.UserPermissionGroup;
import com.walker.service.CurrentUserService;
import com.walker.service.EffectivePermissionService;
import com.walker.service.PermissionGroupService;
import com.walker.vo.ResultBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限组实现。
 *
 * <p>权限组是账号权限的唯一来源：每个组直接保存自己授予的权限点；
 * 用户通过主权限组获得身份与数据范围，并可通过额外绑定权限组叠加权限。
 * 组之间没有“系统角色/临时权限组”之分，默认内置的四个组也可以像普通组一样编辑和删除。</p>
 */
@Service
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private static final Set<String> SCOPE_TYPES = new HashSet<>(Arrays.asList("ADMIN", "FRONT", "ALL"));
    private static final Set<String> DATA_SCOPE_TYPES = new HashSet<>(Arrays.asList(
            "BOARD_MODERATOR", "GLOBAL"));
    /**
     * 一个组要真正能“救回系统”，必须同时包含这三个关键权限：
     * 进入管理后台、权限管理、授予/调整管理员。
     * 只看 user:grant:admin 不够——曾出现过去掉 admin:access 后无人能登录管理端的事故。
     */
    private static final List<String> SUPER_GROUP_REQUIRED_CODES = Arrays.asList(
            Permission.ADMIN_ACCESS.getCode(),
            Permission.PERMISSION_MANAGE.getCode(),
            Permission.USER_GRANT_ADMIN.getCode());

    @Autowired
    private PermissionGroupMapper permissionGroupMapper;

    @Autowired
    private PermissionGroupItemMapper permissionGroupItemMapper;

    @Autowired
    private UserPermissionGroupMapper userPermissionGroupMapper;

    @Autowired
    private PermissionPresetMapper permissionPresetMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private EffectivePermissionService effectivePermissionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listGroups() {
        List<PermissionGroup> groups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .orderByAsc(PermissionGroup::getSortOrder)
                        .orderByAsc(PermissionGroup::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PermissionGroup group : groups) {
            result.add(groupSummary(group));
        }
        return result;
    }

    @Override
    public Map<String, Object> getGroup(Integer id) {
        PermissionGroup group = id == null ? null : permissionGroupMapper.selectById(id);
        if (group == null) {
            return null;
        }
        Map<String, Object> data = groupSummary(group);
        data.put("permissionCodes", new ArrayList<>(effectiveCodes(group)));
        data.put("users", groupUsers(id));
        return data;
    }

    @Override
    @Transactional
    public ResultBean createGroup(PermissionGroup group, List<String> permissionCodes) {
        if (group != null) {
            group.setDataScopeType(normalizeDataScope(group.getDataScopeType()));
        }
        ResultBean validation = validateGroup(group, null);
        if (validation != null) {
            return validation;
        }
        ResultBean codeValidation = validatePermissionCodes(permissionCodes);
        if (codeValidation != null) {
            return codeValidation;
        }
        if (permissionCodes != null) {
            group.setScopeType(deriveScopeType(permissionCodes));
        }
        String now = now();
        group.setGroupCode(group.getGroupCode().trim());
        group.setGroupName(group.getGroupName().trim());
        normalizeGroup(group, now, true);
        permissionGroupMapper.insert(group);
        replaceEffectiveItems(group, permissionCodes, now);
        return ResultBean.success("创建成功", group.getId());
    }

    @Override
    @Transactional
    public ResultBean updateGroup(Integer id, PermissionGroup group, List<String> permissionCodes) {
        PermissionGroup existing = id == null ? null : permissionGroupMapper.selectById(id);
        if (existing == null) {
            return ResultBean.error("权限组不存在");
        }
        if (group == null) {
            group = new PermissionGroup();
        }
        group.setId(id);
        if (StringUtils.isBlank(group.getGroupCode())) {
            group.setGroupCode(existing.getGroupCode());
        }
        if (StringUtils.isBlank(group.getGroupName())) {
            group.setGroupName(existing.getGroupName());
        }
        group.setDataScopeType(normalizeDataScope(group.getDataScopeType()));
        ResultBean validation = validateGroup(group, id);
        if (validation != null) {
            return validation;
        }
        if (permissionCodes != null) {
            ResultBean codeValidation = validatePermissionCodes(permissionCodes);
            if (codeValidation != null) {
                return codeValidation;
            }
        }
        existing.setGroupCode(group.getGroupCode().trim());
        existing.setGroupName(group.getGroupName().trim());
        if (group.getDescription() != null) {
            existing.setDescription(group.getDescription());
        }
        if (StringUtils.isNotBlank(group.getScopeType())) {
            existing.setScopeType(group.getScopeType());
        }
        if (StringUtils.isNotBlank(group.getDataScopeType())) {
            existing.setDataScopeType(group.getDataScopeType());
        }
        if (group.getEnabled() != null) {
            existing.setEnabled(group.getEnabled());
        }
        if (group.getSortOrder() != null) {
            existing.setSortOrder(group.getSortOrder());
        }
        if (permissionCodes != null) {
            existing.setScopeType(deriveScopeType(permissionCodes));
        }
        existing.setUpdateBy(currentUsername());
        existing.setUpdateTime(now());
        // 风险保护：不能让系统失去最后一个启用且拥有“授予/调整管理员”的权限组。
        boolean targetEnabled = group.getEnabled() != null
                ? Integer.valueOf(1).equals(group.getEnabled())
                : Integer.valueOf(1).equals(existing.getEnabled());
        boolean targetSuper = permissionCodes != null
                ? containsAllCritical(permissionCodes)
                : groupHasAllCritical(id);
        ResultBean selfGuard = ensureCurrentUserKeepsCriticalPermissions(id, permissionCodes, targetEnabled);
        if (selfGuard != null) {
            return selfGuard;
        }
        ResultBean superGuard = ensureSuperAdminGroupRemains(id, targetEnabled && targetSuper);
        if (superGuard != null) {
            return superGuard;
        }
        permissionGroupMapper.updateById(existing);
        if (permissionCodes != null) {
            replaceEffectiveItems(existing, permissionCodes, now());
        }
        return ResultBean.success("修改成功");
    }

    @Override
    @Transactional
    public ResultBean deleteGroup(Integer id) {
        PermissionGroup group = id == null ? null : permissionGroupMapper.selectById(id);
        if (group == null) {
            return ResultBean.error("权限组不存在");
        }
        ResultBean superGuard = ensureSuperAdminGroupRemains(id, false);
        if (superGuard != null) {
            return superGuard;
        }
        ResultBean selfGuard = ensureCurrentUserKeepsCriticalPermissions(id, Collections.emptyList(), false);
        if (selfGuard != null) {
            return selfGuard;
        }
        permissionGroupItemMapper.delete(new LambdaQueryWrapper<PermissionGroupItem>()
                .eq(PermissionGroupItem::getGroupId, id));
        userPermissionGroupMapper.delete(new LambdaQueryWrapper<UserPermissionGroup>()
                .eq(UserPermissionGroup::getGroupId, id));
        // 主组用户优先迁移到普通用户组，避免留下空主组导致身份/数据范围悬空；
        // 找不到普通组时宁可置空，也不能把普通用户误升级到管理类组。
        Integer fallbackGroupId = resolveFallbackGroupId(id);
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                .set(User::getPrimaryGroupId, fallbackGroupId)
                .eq(User::getPrimaryGroupId, id));
        permissionGroupMapper.deleteById(id);
        return ResultBean.success("删除成功");
    }

    @Override
    @Transactional
    public ResultBean replaceItems(Integer id, List<String> permissionCodes) {
        PermissionGroup group = id == null ? null : permissionGroupMapper.selectById(id);
        if (group == null) {
            return ResultBean.error("权限组不存在");
        }
        ResultBean validation = validatePermissionCodes(permissionCodes);
        if (validation != null) {
            return validation;
        }
        boolean targetSuper = permissionCodes != null
                && containsAllCritical(permissionCodes);
        ResultBean superGuard = ensureSuperAdminGroupRemains(id,
                Integer.valueOf(1).equals(group.getEnabled()) && targetSuper);
        if (superGuard != null) {
            return superGuard;
        }
        ResultBean selfGuard = ensureCurrentUserKeepsCriticalPermissions(id, permissionCodes,
                Integer.valueOf(1).equals(group.getEnabled()));
        if (selfGuard != null) {
            return selfGuard;
        }
        group.setScopeType(deriveScopeType(permissionCodes));
        group.setUpdateBy(currentUsername());
        group.setUpdateTime(now());
        permissionGroupMapper.updateById(group);
        replaceEffectiveItems(group, permissionCodes, now());
        return ResultBean.success("权限点已更新");
    }

    @Override
    @Transactional
    public ResultBean assignUsers(Integer id, List<Integer> userIds) {
        PermissionGroup group = id == null ? null : permissionGroupMapper.selectById(id);
        if (group == null) {
            return ResultBean.error("权限组不存在");
        }
        if (userIds == null || userIds.isEmpty()) {
            return ResultBean.error("请选择用户");
        }
        Set<Integer> unique = new LinkedHashSet<>(userIds);
        List<User> users = userMapper.selectBatchIds(unique);
        Set<Integer> existingUserIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        List<Integer> missing = unique.stream()
                .filter(uid -> !existingUserIds.contains(uid))
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            return ResultBean.error("存在无效用户: " + missing);
        }
        String now = now();
        String operator = currentUsername();
        for (Integer userId : unique) {
            long count = userPermissionGroupMapper.selectCount(
                    new LambdaQueryWrapper<UserPermissionGroup>()
                            .eq(UserPermissionGroup::getUserId, userId)
                            .eq(UserPermissionGroup::getGroupId, id));
            if (count > 0) {
                continue;
            }
            UserPermissionGroup link = new UserPermissionGroup();
            link.setUserId(userId);
            link.setGroupId(id);
            link.setPriority(0);
            link.setCreateBy(operator);
            link.setCreateTime(now);
            userPermissionGroupMapper.insert(link);
        }
        return ResultBean.success("用户已加入权限组");
    }

    @Override
    @Transactional
    public ResultBean removeUser(Integer id, Integer userId) {
        if (id == null || userId == null) {
            return ResultBean.error("参数不完整");
        }
        userPermissionGroupMapper.delete(new LambdaQueryWrapper<UserPermissionGroup>()
                .eq(UserPermissionGroup::getGroupId, id)
                .eq(UserPermissionGroup::getUserId, userId));
        return ResultBean.success("已移除");
    }

    @Override
    public List<Map<String, Object>> listPresets() {
        List<PermissionPreset> presets = permissionPresetMapper.selectList(
                new LambdaQueryWrapper<PermissionPreset>()
                        .orderByDesc(PermissionPreset::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PermissionPreset preset : presets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", preset.getId());
            row.put("presetCode", preset.getPresetCode());
            row.put("presetName", preset.getPresetName());
            row.put("createBy", preset.getCreateBy());
            row.put("createTime", preset.getCreateTime());
            // 预设详情：把快照里的来源组、数据范围和权限点一并返回，前端可直接展开查看。
            if (StringUtils.isNotBlank(preset.getSnapshotJson())) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snapshot = objectMapper.readValue(
                            preset.getSnapshotJson(), LinkedHashMap.class);
                    row.put("sourceGroupName", snapshot.get("groupName"));
                    row.put("sourceGroupCode", snapshot.get("groupCode"));
                    row.put("scopeType", snapshot.get("scopeType"));
                    row.put("dataScopeType", snapshot.get("dataScopeType"));
                    Object codes = snapshot.get("permissionCodes");
                    row.put("permissionCodes", codes instanceof List ? codes : Collections.emptyList());
                } catch (Exception ignored) {
                    // 历史快照格式异常时只返回基础字段，不影响列表展示
                }
            }
            result.add(row);
        }
        return result;
    }

    @Override
    @Transactional
    public ResultBean createPreset(String presetCode, String presetName, Integer groupId) {
        PermissionGroup group = groupId == null ? null : permissionGroupMapper.selectById(groupId);
        if (group == null) {
            return ResultBean.error("权限组不存在");
        }
        String code = StringUtils.isBlank(presetCode)
                ? "preset-" + System.currentTimeMillis() : presetCode.trim();
        String name = StringUtils.isBlank(presetName) ? group.getGroupName() : presetName.trim();
        if (permissionPresetMapper.selectCount(new LambdaQueryWrapper<PermissionPreset>()
                .eq(PermissionPreset::getPresetCode, code)) > 0) {
            return ResultBean.error("预设编码已存在");
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("groupCode", group.getGroupCode());
        snapshot.put("groupName", group.getGroupName());
        snapshot.put("description", group.getDescription());
        snapshot.put("scopeType", group.getScopeType());
        snapshot.put("dataScopeType", group.getDataScopeType());
        snapshot.put("permissionCodes", new ArrayList<>(effectiveCodes(group)));

        PermissionPreset preset = new PermissionPreset();
        preset.setPresetCode(code);
        preset.setPresetName(name);
        try {
            preset.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            return ResultBean.error("预设快照生成失败");
        }
        preset.setCreateBy(currentUsername());
        preset.setCreateTime(now());
        permissionPresetMapper.insert(preset);
        return ResultBean.success("预设已保存", preset.getId());
    }

    @Override
    @Transactional
    public ResultBean deletePreset(Integer presetId) {
        PermissionPreset preset = presetId == null ? null : permissionPresetMapper.selectById(presetId);
        if (preset == null) {
            return ResultBean.error("预设不存在");
        }
        permissionPresetMapper.deleteById(presetId);
        return ResultBean.success("删除成功");
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public ResultBean applyPreset(Integer presetId, Integer groupId) {
        PermissionPreset preset = presetId == null ? null : permissionPresetMapper.selectById(presetId);
        if (preset == null) {
            return ResultBean.error("预设不存在");
        }
        PermissionGroup group = groupId == null ? null : permissionGroupMapper.selectById(groupId);
        if (group == null) {
            return ResultBean.error("权限组不存在");
        }
        Map<String, Object> snapshot;
        try {
            snapshot = objectMapper.readValue(preset.getSnapshotJson(), LinkedHashMap.class);
        } catch (Exception e) {
            return ResultBean.error("预设解析失败");
        }
        Object codesObj = snapshot.get("permissionCodes");
        List<String> codes = codesObj instanceof List
                ? ((List<Object>) codesObj).stream().map(String::valueOf).collect(Collectors.toList())
                : Collections.emptyList();
        ResultBean codeValidation = validatePermissionCodes(codes);
        if (codeValidation != null) {
            return codeValidation;
        }

        if (snapshot.get("groupCode") != null) {
            group.setGroupCode(String.valueOf(snapshot.get("groupCode")));
        }
        if (snapshot.get("groupName") != null) {
            group.setGroupName(String.valueOf(snapshot.get("groupName")));
        }
        if (snapshot.get("description") != null) {
            group.setDescription(String.valueOf(snapshot.get("description")));
        }
        if (snapshot.get("scopeType") != null) {
            group.setScopeType(String.valueOf(snapshot.get("scopeType")));
        }
        if (snapshot.get("dataScopeType") != null) {
            group.setDataScopeType(normalizeDataScope(String.valueOf(snapshot.get("dataScopeType"))));
        }
        // 适用端由权限点决定，不信任快照中的旧标记。
        group.setScopeType(deriveScopeType(codes));
        ResultBean validation = validateGroup(group, groupId);
        if (validation != null) {
            return validation;
        }
        boolean targetSuper = containsAllCritical(codes);
        ResultBean superGuard = ensureSuperAdminGroupRemains(groupId,
                Integer.valueOf(1).equals(group.getEnabled()) && targetSuper);
        if (superGuard != null) {
            return superGuard;
        }
        ResultBean selfGuard = ensureCurrentUserKeepsCriticalPermissions(groupId, codes,
                Integer.valueOf(1).equals(group.getEnabled()));
        if (selfGuard != null) {
            return selfGuard;
        }
        group.setUpdateBy(currentUsername());
        group.setUpdateTime(now());
        permissionGroupMapper.updateById(group);
        replaceEffectiveItems(group, codes, now());
        return ResultBean.success("预设已应用");
    }

    @Override
    public Map<String, Object> effective(Integer userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("primaryGroupId", user.getPrimaryGroupId());
        data.put("permissions", new ArrayList<>(effectivePermissionService.getEffectiveCodes(user)));
        List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                new LambdaQueryWrapper<UserPermissionGroup>()
                        .eq(UserPermissionGroup::getUserId, userId));
        List<Integer> groupIds = links.stream()
                .map(UserPermissionGroup::getGroupId)
                .filter(gid -> gid != null)
                .collect(Collectors.toList());
        data.put("groupIds", groupIds);
        return data;
    }

    /** 组是否同时具备三个管理端关键权限。 */
    private boolean groupHasAllCritical(Integer groupId) {
        for (String code : SUPER_GROUP_REQUIRED_CODES) {
            if (!groupGrants(groupId, code)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsAllCritical(List<String> codes) {
        return codes != null && codes.containsAll(SUPER_GROUP_REQUIRED_CODES);
    }

    /** 权限组是否直接授予某个权限点（只看 grant）。 */
    private boolean groupGrants(Integer groupId, String permissionCode) {
        if (groupId == null || StringUtils.isBlank(permissionCode)) {
            return false;
        }
        Long count = permissionGroupItemMapper.selectCount(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getGroupId, groupId)
                        .eq(PermissionGroupItem::getPermissionCode, permissionCode)
                        .eq(PermissionGroupItem::getEffect, "grant"));
        return count != null && count > 0;
    }

    /** 当前所有“启用且同时具备管理端关键权限”的权限组。 */
    private Set<Integer> enabledSuperGroupIds() {
        List<PermissionGroup> enabledGroups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .eq(PermissionGroup::getEnabled, 1));
        if (enabledGroups == null || enabledGroups.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> groupIds = enabledGroups.stream()
                .map(PermissionGroup::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .in(PermissionGroupItem::getGroupId, groupIds)
                        .in(PermissionGroupItem::getPermissionCode, SUPER_GROUP_REQUIRED_CODES)
                        .eq(PermissionGroupItem::getEffect, "grant"));
        if (items == null) {
            return Collections.emptySet();
        }
        Map<Integer, Set<String>> codesByGroup = new LinkedHashMap<>();
        for (PermissionGroupItem item : items) {
            if (item.getGroupId() == null || item.getPermissionCode() == null) {
                continue;
            }
            codesByGroup.computeIfAbsent(item.getGroupId(), key -> new LinkedHashSet<>())
                    .add(item.getPermissionCode());
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (Map.Entry<Integer, Set<String>> entry : codesByGroup.entrySet()) {
            if (entry.getValue().containsAll(SUPER_GROUP_REQUIRED_CODES)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * 防止当前登录的超管把自己锁在系统外：
     * 不能从“自己的主权限组”里移除 admin:access / permission:manage / user:grant:admin，
     * 也不能停用或删除这个组。
     */
    private ResultBean ensureCurrentUserKeepsCriticalPermissions(Integer targetGroupId,
                                                                 List<String> newCodes,
                                                                 boolean targetEnabled) {
        User current = currentUserService == null ? null : currentUserService.getCurrentUser();
        if (current == null || current.getPrimaryGroupId() == null
                || !current.getPrimaryGroupId().equals(targetGroupId)) {
            return null;
        }
        // 没改权限点且组仍启用：不会丢失关键权限
        if (newCodes == null && targetEnabled) {
            return null;
        }
        for (String code : SUPER_GROUP_REQUIRED_CODES) {
            if (!groupGrants(targetGroupId, code)) {
                continue;
            }
            boolean retained = targetEnabled && newCodes != null && newCodes.contains(code);
            if (!retained) {
                return ResultBean.error("不能移除当前登录账号主权限组的关键权限（进入管理后台 / 权限管理 / 授予管理员），否则你将无法继续管理系统");
            }
        }
        return null;
    }

    /**
     * 防止把系统改到“没有任何启用超管组”的状态。
     * 系统本来就没有超管组时放行，避免阻止修复操作。
     */
    private ResultBean ensureSuperAdminGroupRemains(Integer targetGroupId, boolean targetWillBeSuperEnabled) {
        Set<Integer> superGroupIds = enabledSuperGroupIds();
        if (superGroupIds.isEmpty() || targetWillBeSuperEnabled) {
            return null;
        }
        boolean hasOther = superGroupIds.stream()
                .anyMatch(groupId -> !groupId.equals(targetGroupId));
        if (hasOther) {
            return null;
        }
        return ResultBean.error("至少保留一个启用且拥有「授予/调整管理员」权限的权限组，否则将无人能管理权限");
    }

    /**
     * 删除组后给原主组用户找一个安全的落点：
     * 优先普通用户组，其次全局且没有 admin:access 的组；找不到就返回 null。
     */
    private Integer resolveFallbackGroupId(Integer excludeGroupId) {
        List<PermissionGroup> enabledGroups = permissionGroupMapper.selectList(
                new LambdaQueryWrapper<PermissionGroup>()
                        .eq(PermissionGroup::getEnabled, 1)
                        .orderByAsc(PermissionGroup::getSortOrder)
                        .orderByAsc(PermissionGroup::getId));
        if (enabledGroups == null || enabledGroups.isEmpty()) {
            return null;
        }
        List<PermissionGroup> candidates = enabledGroups.stream()
                .filter(group -> group.getId() != null && !group.getId().equals(excludeGroupId))
                .collect(Collectors.toList());
        for (PermissionGroup group : candidates) {
            if ("role_user".equals(group.getGroupCode())) {
                return group.getId();
            }
        }
        Set<Integer> adminGroupIds = adminGrantGroupIds(candidates);
        for (PermissionGroup group : candidates) {
            if ("GLOBAL".equalsIgnoreCase(group.getDataScopeType())
                    && !adminGroupIds.contains(group.getId())) {
                return group.getId();
            }
        }
        for (PermissionGroup group : candidates) {
            if (!adminGroupIds.contains(group.getId())) {
                return group.getId();
            }
        }
        return null;
    }

    /** 给定候选组里，哪些组授予了 admin:access。 */
    private Set<Integer> adminGrantGroupIds(List<PermissionGroup> groups) {
        List<Integer> groupIds = groups.stream()
                .map(PermissionGroup::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (groupIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .in(PermissionGroupItem::getGroupId, groupIds)
                        .eq(PermissionGroupItem::getPermissionCode, Permission.ADMIN_ACCESS.getCode())
                        .eq(PermissionGroupItem::getEffect, "grant"));
        if (items == null) {
            return Collections.emptySet();
        }
        return items.stream()
                .map(PermissionGroupItem::getGroupId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    /** 适用端自动标记：勾选权限点里同时有管理端/前台动作时为 ALL。 */
    private String deriveScopeType(List<String> permissionCodes) {
        boolean hasAdmin = false;
        boolean hasFront = false;
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                Permission permission = Permission.fromCode(code);
                if (permission == null) {
                    continue;
                }
                if (permission.isFrontAction()) {
                    hasFront = true;
                } else {
                    hasAdmin = true;
                }
            }
        }
        if (hasAdmin && hasFront) {
            return "ALL";
        }
        return hasFront ? "FRONT" : "ADMIN";
    }

    /** 旧枚举名 ROLE_DEFAULT 与 GLOBAL 等价；统一转成 GLOBAL 后再校验。 */
    private String normalizeDataScope(String dataScopeType) {
        if (StringUtils.isBlank(dataScopeType)) {
            return dataScopeType;
        }
        return "ROLE_DEFAULT".equalsIgnoreCase(dataScopeType.trim()) ? "GLOBAL" : dataScopeType.trim();
    }

    private ResultBean validateGroup(PermissionGroup group, Integer excludeId) {
        if (group == null || StringUtils.isBlank(group.getGroupCode())
                || StringUtils.isBlank(group.getGroupName())) {
            return ResultBean.error("权限组编码和名称不能为空");
        }
        LambdaQueryWrapper<PermissionGroup> wrapper = new LambdaQueryWrapper<PermissionGroup>()
                .eq(PermissionGroup::getGroupCode, group.getGroupCode().trim());
        if (excludeId != null) {
            wrapper.ne(PermissionGroup::getId, excludeId);
        }
        if (permissionGroupMapper.selectCount(wrapper) > 0) {
            return ResultBean.error("权限组编码已存在");
        }
        if (StringUtils.isNotBlank(group.getScopeType())
                && !SCOPE_TYPES.contains(group.getScopeType())) {
            return ResultBean.error("范围只能是 ADMIN/FRONT/ALL");
        }
        if (StringUtils.isNotBlank(group.getDataScopeType())
                && !DATA_SCOPE_TYPES.contains(group.getDataScopeType())) {
            return ResultBean.error("数据范围只能是 GLOBAL/BOARD_MODERATOR");
        }
        return null;
    }

    private ResultBean validatePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return null;
        }
        for (String code : permissionCodes) {
            if (Permission.fromCode(code) == null) {
                return ResultBean.error("未知权限点: " + code);
            }
        }
        return null;
    }

    private void normalizeGroup(PermissionGroup group, String now, boolean create) {
        if (StringUtils.isBlank(group.getScopeType())) {
            group.setScopeType("ADMIN");
        }
        if (StringUtils.isBlank(group.getDataScopeType())) {
            group.setDataScopeType("GLOBAL");
        }
        if (group.getEnabled() == null) {
            group.setEnabled(1);
        }
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        if (create) {
            group.setCreateBy(currentUsername());
            group.setCreateTime(now);
        }
        group.setUpdateBy(currentUsername());
        group.setUpdateTime(now);
    }

    /** 权限组直接保存勾选的权限点，不再区分角色基线与 grant/revoke 差异。 */
    private void replaceEffectiveItems(PermissionGroup group, List<String> permissionCodes, String now) {
        if (group == null || group.getId() == null) {
            return;
        }
        Integer groupId = group.getId();
        permissionGroupItemMapper.delete(new LambdaQueryWrapper<PermissionGroupItem>()
                .eq(PermissionGroupItem::getGroupId, groupId));
        if (permissionCodes == null) {
            return;
        }
        Set<String> selected = new LinkedHashSet<>(permissionCodes);
        for (String code : selected) {
            if (Permission.fromCode(code) == null) {
                continue;
            }
            insertItem(groupId, code, "grant", now);
        }
    }

    private void insertItem(Integer groupId, String code, String effect, String now) {
        PermissionGroupItem item = new PermissionGroupItem();
        item.setGroupId(groupId);
        item.setPermissionCode(code);
        item.setEffect(effect);
        item.setCreateTime(now);
        permissionGroupItemMapper.insert(item);
    }

    /** 权限组自身最终权限集合：grant 叠加，历史 revoke 兼容收回。 */
    private Set<String> effectiveCodes(PermissionGroup group) {
        Set<String> codes = new LinkedHashSet<>();
        if (group == null || group.getId() == null) {
            return codes;
        }
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getGroupId, group.getId()));
        Set<String> grants = new LinkedHashSet<>();
        Set<String> revokes = new LinkedHashSet<>();
        for (PermissionGroupItem item : items) {
            if (item.getPermissionCode() == null || Permission.fromCode(item.getPermissionCode()) == null) {
                continue;
            }
            if ("revoke".equalsIgnoreCase(item.getEffect())) {
                revokes.add(item.getPermissionCode());
            } else {
                grants.add(item.getPermissionCode());
            }
        }
        codes.addAll(grants);
        codes.removeAll(revokes);
        return codes;
    }

    private List<Map<String, Object>> groupUsers(Integer groupId) {
        List<UserPermissionGroup> links = userPermissionGroupMapper.selectList(
                new LambdaQueryWrapper<UserPermissionGroup>()
                        .eq(UserPermissionGroup::getGroupId, groupId));
        if (links.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> userIds = links.stream()
                .map(UserPermissionGroup::getUserId)
                .filter(uid -> uid != null)
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.getId());
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            result.add(row);
        }
        return result;
    }

    private Map<String, Object> groupSummary(PermissionGroup group) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", group.getId());
        row.put("groupCode", group.getGroupCode());
        row.put("groupName", group.getGroupName());
        row.put("description", group.getDescription());
        row.put("scopeType", group.getScopeType());
        row.put("dataScopeType", group.getDataScopeType());
        row.put("enabled", group.getEnabled());
        row.put("sortOrder", group.getSortOrder());
        row.put("createBy", group.getCreateBy());
        row.put("createTime", group.getCreateTime());
        row.put("updateTime", group.getUpdateTime());
        row.put("permissionCount", effectiveCodes(group).size());
        long primaryCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPrimaryGroupId, group.getId()));
        long memberCount = userPermissionGroupMapper.selectCount(
                new LambdaQueryWrapper<UserPermissionGroup>()
                        .eq(UserPermissionGroup::getGroupId, group.getId()));
        row.put("primaryUserCount", primaryCount);
        row.put("memberCount", memberCount);
        row.put("userCount", primaryCount + memberCount);
        return row;
    }

    private String currentUsername() {
        User user = currentUserService.getCurrentUser();
        return user == null ? "system" : user.getUsername();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
