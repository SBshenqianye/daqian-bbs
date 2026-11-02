package com.walker.controller;

import com.walker.config.audit.AuditContext;
import com.walker.config.audit.AuditLog;
import com.walker.pojo.PermissionGroup;
import com.walker.service.PermissionGroupService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 权限组管理（Phase B/C，仅超级管理员）。
 */
@Api(tags = "PermissionGroupController")
@RestController
@RequestMapping("/admin/permission")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PermissionGroupController {

    @Autowired
    private PermissionGroupService permissionGroupService;

    @ApiOperation("权限组列表")
    @GetMapping("/groups")
    public ResultBean listGroups() {
        return ResultBean.success("查询成功", permissionGroupService.listGroups());
    }

    @ApiOperation("权限组详情")
    @GetMapping("/groups/{id}")
    public ResultBean getGroup(@PathVariable Integer id) {
        Map<String, Object> group = permissionGroupService.getGroup(id);
        return group == null ? ResultBean.error("权限组不存在") : ResultBean.success("查询成功", group);
    }

    @ApiOperation("新增权限组")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_CREATE",
            targetType = "PERMISSION_GROUP")
    @PostMapping("/groups")
    public ResultBean createGroup(@RequestBody Map<String, Object> body) {
        ResultBean result = permissionGroupService.createGroup(
                toGroup(body), toStringList(body.get("permissionCodes")));
        if (result.getCode() == 200 && result.getObj() != null) {
            AuditContext.setTargetId(String.valueOf(result.getObj()));
        }
        return result;
    }

    @ApiOperation("修改权限组")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_UPDATE",
            targetType = "PERMISSION_GROUP", targetId = "#id")
    @PutMapping("/groups/{id}")
    public ResultBean updateGroup(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return permissionGroupService.updateGroup(id, toGroup(body),
                body.containsKey("permissionCodes") ? toStringList(body.get("permissionCodes")) : null);
    }

    @ApiOperation("删除权限组")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_DELETE",
            targetType = "PERMISSION_GROUP", targetId = "#id")
    @DeleteMapping("/groups/{id}")
    public ResultBean deleteGroup(@PathVariable Integer id) {
        return permissionGroupService.deleteGroup(id);
    }

    @ApiOperation("更新权限组权限点")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_ITEMS",
            targetType = "PERMISSION_GROUP", targetId = "#id")
    @PostMapping("/groups/{id}/items")
    public ResultBean replaceItems(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return permissionGroupService.replaceItems(id, toStringList(body.get("permissionCodes")));
    }

    @ApiOperation("批量添加用户到权限组")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_ASSIGN_USERS",
            targetType = "PERMISSION_GROUP", targetId = "#id")
    @PostMapping("/groups/{id}/users")
    public ResultBean assignUsers(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        return permissionGroupService.assignUsers(id, toIntList(body.get("userIds")));
    }

    @ApiOperation("从权限组移除用户")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_GROUP_REMOVE_USER",
            targetType = "PERMISSION_GROUP", targetId = "#id")
    @DeleteMapping("/groups/{id}/users/{userId}")
    public ResultBean removeUser(@PathVariable Integer id, @PathVariable Integer userId) {
        return permissionGroupService.removeUser(id, userId);
    }

    @ApiOperation("预设方案列表")
    @GetMapping("/presets")
    public ResultBean listPresets() {
        return ResultBean.success("查询成功", permissionGroupService.listPresets());
    }

    @ApiOperation("保存预设方案")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_PRESET_CREATE",
            targetType = "PERMISSION_PRESET")
    @PostMapping("/presets")
    public ResultBean createPreset(@RequestBody Map<String, Object> body) {
        ResultBean result = permissionGroupService.createPreset(
                stringValue(body.get("presetCode")),
                stringValue(body.get("presetName")),
                intValue(body.get("groupId")));
        if (result.getCode() == 200 && result.getObj() != null) {
            AuditContext.setTargetId(String.valueOf(result.getObj()));
        }
        return result;
    }

    @ApiOperation("应用预设方案")
    @AuditLog(module = "PERMISSION", action = "PERMISSION_PRESET_APPLY",
            targetType = "PERMISSION_GROUP", targetId = "#groupId")
    @PostMapping("/presets/{id}/apply")
    public ResultBean applyPreset(@PathVariable Integer id, @RequestParam Integer groupId) {
        return permissionGroupService.applyPreset(id, groupId);
    }

    @ApiOperation("查看用户有效权限")
    @GetMapping("/effective/{userId}")
    public ResultBean effective(@PathVariable Integer userId) {
        Map<String, Object> data = permissionGroupService.effective(userId);
        return data == null ? ResultBean.error("用户不存在") : ResultBean.success("查询成功", data);
    }

    private PermissionGroup toGroup(Map<String, Object> body) {
        PermissionGroup group = new PermissionGroup();
        group.setGroupCode(stringValue(body.get("groupCode")));
        group.setGroupName(stringValue(body.get("groupName")));
        group.setDescription(stringValue(body.get("description")));
        group.setScopeType(stringValue(body.get("scopeType")));
        group.setDataScopeType(stringValue(body.get("dataScopeType")));
        group.setEnabled(intValue(body.get("enabled")));
        group.setSortOrder(intValue(body.get("sortOrder")));
        return group;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    result.add(String.valueOf(item).trim());
                }
            }
        }
        return result;
    }

    private List<Integer> toIntList(Object value) {
        List<Integer> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                Integer id = intValue(item);
                if (id != null) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
