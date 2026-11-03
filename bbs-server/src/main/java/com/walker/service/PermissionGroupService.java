package com.walker.service;

import com.walker.pojo.PermissionGroup;
import com.walker.vo.ResultBean;

import java.util.List;
import java.util.Map;

/**
 * 权限组（系统角色 + 临时权限组）。
 */
public interface PermissionGroupService {

    List<Map<String, Object>> listGroups();

    Map<String, Object> getGroup(Integer id);

    ResultBean createGroup(PermissionGroup group, List<String> permissionCodes);

    ResultBean updateGroup(Integer id, PermissionGroup group, List<String> permissionCodes);

    ResultBean deleteGroup(Integer id);

    ResultBean replaceItems(Integer id, List<String> permissionCodes);

    ResultBean assignUsers(Integer id, List<Integer> userIds);

    ResultBean removeUser(Integer id, Integer userId);

    List<Map<String, Object>> listPresets();

    ResultBean createPreset(String presetCode, String presetName, Integer groupId);

    ResultBean applyPreset(Integer presetId, Integer groupId);

    Map<String, Object> effective(Integer userId);
}
