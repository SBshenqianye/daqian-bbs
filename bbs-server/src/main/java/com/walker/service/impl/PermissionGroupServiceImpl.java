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
 * 权限组实现（Phase B/C，grant-only）。
 */
@Service
public class PermissionGroupServiceImpl implements PermissionGroupService {

    /** 权限组不能授予的越权能力。 */
    private static final Set<String> FORBIDDEN_GRANTS = new HashSet<>(Arrays.asList(
            Permission.USER_GRANT_ADMIN.getCode(),
            Permission.CONFIG_WRITE.getCode(),
            Permission.PERMISSION_MANAGE.getCode(),
            Permission.AUDIT_READ.getCode()
    ));

    private static final Set<String> SCOPE_TYPES = new HashSet<>(Arrays.asList("ADMIN", "FRONT", "ALL"));
    private static final Set<String> DATA_SCOPE_TYPES = new HashSet<>(Arrays.asList(
            "ROLE_DEFAULT", "BOARD_MODERATOR"));

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
        data.put("permissionCodes", itemCodes(id));
        data.put("users", groupUsers(id));
        return data;
    }

    @Override
    @Transactional
    public ResultBean createGroup(PermissionGroup group, List<String> permissionCodes) {
        ResultBean validation = validateGroup(group, null);
        if (validation != null) {
            return validation;
        }
        ResultBean codeValidation = validateCodes(permissionCodes);
        if (codeValidation != null) {
            return codeValidation;
        }
        String now = now();
        group.setGroupCode(group.getGroupCode().trim());
        group.setGroupName(group.getGroupName().trim());
        normalizeGroup(group, now, true);
        permissionGroupMapper.insert(group);
        replaceItemsInternal(group.getId(), permissionCodes, now);
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
        ResultBean validation = validateGroup(group, id);
        if (validation != null) {
            return validation;
        }
        if (permissionCodes != null) {
            ResultBean codeValidation = validateCodes(permissionCodes);
            if (codeValidation != null) {
                return codeValidation;
            }
        }
        if (StringUtils.isNotBlank(group.getGroupCode())) {
            existing.setGroupCode(group.getGroupCode().trim());
        }
        if (StringUtils.isNotBlank(group.getGroupName())) {
            existing.setGroupName(group.getGroupName().trim());
        }
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
        existing.setUpdateBy(currentUsername());
        existing.setUpdateTime(now());
        permissionGroupMapper.updateById(existing);
        if (permissionCodes != null) {
            replaceItemsInternal(id, permissionCodes, now());
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
        permissionGroupItemMapper.delete(new LambdaQueryWrapper<PermissionGroupItem>()
                .eq(PermissionGroupItem::getGroupId, id));
        userPermissionGroupMapper.delete(new LambdaQueryWrapper<UserPermissionGroup>()
                .eq(UserPermissionGroup::getGroupId, id));
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
        ResultBean validation = validateCodes(permissionCodes);
        if (validation != null) {
            return validation;
        }
        replaceItemsInternal(id, permissionCodes, now());
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
        snapshot.put("permissionCodes", itemCodes(groupId));

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
        ResultBean codeValidation = validateCodes(codes);
        if (codeValidation != null) {
            return codeValidation;
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
            group.setDataScopeType(String.valueOf(snapshot.get("dataScopeType")));
        }
        group.setUpdateBy(currentUsername());
        group.setUpdateTime(now());
        permissionGroupMapper.updateById(group);
        replaceItemsInternal(groupId, codes, now());
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
        data.put("userType", user.getUserType());
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
            return ResultBean.error("数据范围只能是 ROLE_DEFAULT/BOARD_MODERATOR");
        }
        return null;
    }

    private ResultBean validateCodes(List<String> permissionCodes) {
        if (permissionCodes == null) {
            return null;
        }
        for (String code : permissionCodes) {
            Permission permission = Permission.fromCode(code);
            if (permission == null) {
                return ResultBean.error("未知权限点: " + code);
            }
            if (FORBIDDEN_GRANTS.contains(code)) {
                return ResultBean.error("权限组不能授予该权限点: " + code);
            }
        }
        return null;
    }

    private void normalizeGroup(PermissionGroup group, String now, boolean create) {
        if (StringUtils.isBlank(group.getScopeType())) {
            group.setScopeType("ADMIN");
        }
        if (StringUtils.isBlank(group.getDataScopeType())) {
            group.setDataScopeType("ROLE_DEFAULT");
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

    private void replaceItemsInternal(Integer groupId, List<String> permissionCodes, String now) {
        permissionGroupItemMapper.delete(new LambdaQueryWrapper<PermissionGroupItem>()
                .eq(PermissionGroupItem::getGroupId, groupId));
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }
        Set<String> unique = new LinkedHashSet<>(permissionCodes);
        for (String code : unique) {
            PermissionGroupItem item = new PermissionGroupItem();
            item.setGroupId(groupId);
            item.setPermissionCode(code);
            item.setEffect("grant");
            item.setCreateTime(now);
            permissionGroupItemMapper.insert(item);
        }
    }

    private List<String> itemCodes(Integer groupId) {
        List<PermissionGroupItem> items = permissionGroupItemMapper.selectList(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getGroupId, groupId)
                        .eq(PermissionGroupItem::getEffect, "grant")
                        .orderByAsc(PermissionGroupItem::getId));
        return items.stream()
                .map(PermissionGroupItem::getPermissionCode)
                .collect(Collectors.toList());
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
        List<User> users = userMapper.selectBatchIds(userIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.getId());
            row.put("username", user.getUsername());
            row.put("nickname", user.getNickname());
            row.put("userType", user.getUserType());
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
        row.put("permissionCount", permissionGroupItemMapper.selectCount(
                new LambdaQueryWrapper<PermissionGroupItem>()
                        .eq(PermissionGroupItem::getGroupId, group.getId())));
        row.put("userCount", userPermissionGroupMapper.selectCount(
                new LambdaQueryWrapper<UserPermissionGroup>()
                        .eq(UserPermissionGroup::getGroupId, group.getId())));
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
