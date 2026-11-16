package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.pojo.LevelItem;
import com.walker.pojo.LevelProfile;
import com.walker.service.LevelService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "等级体系")
@RestController
public class LevelController {

    @Autowired
    private LevelService levelService;

    @ApiOperation("用户端：当前启用的等级配置")
    @GetMapping("/common/level/profile")
    public ResultBean publicProfile() {
        return levelService.getProfilePayload();
    }

    @ApiOperation("用户端：当前用户等级")
    @GetMapping("/common/level/user/{userId}")
    public ResultBean userLevel(@PathVariable Integer userId) {
        return ResultBean.success("查询成功", levelService.getUserLevel(userId));
    }

    @ApiOperation("管理端：等级配置列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/level/profiles")
    public ResultBean adminProfiles() {
        return levelService.listAdminProfiles();
    }

    @ApiOperation("管理端：保存等级配置")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_LEVEL_SAVE", targetType = "LEVEL_PROFILE", targetId = "#body['profile']['id']")
    @PostMapping("/admin/level/save")
    @SuppressWarnings("unchecked")
    public ResultBean save(@RequestBody Map<String, Object> body) {        if (body == null) {
            return ResultBean.error("参数不能为空");
        }
        Object profileObj = body.get("profile");
        Object itemsObj = body.get("items");
        if (!(profileObj instanceof Map) || !(itemsObj instanceof List)) {
            return ResultBean.error("参数格式错误");
        }
        Map<String, Object> p = (Map<String, Object>) profileObj;
        LevelProfile profile = new LevelProfile();
        profile.setId(toInt(p.get("id")));
        profile.setProfileName(p.get("profileName") == null ? null : String.valueOf(p.get("profileName")));
        profile.setMajorCount(toInt(p.get("majorCount")));
        profile.setMinorPerMajor(toInt(p.get("minorPerMajor")));
        profile.setStatus(toInt(p.get("status")));
        List<LevelItem> items = new java.util.ArrayList<>();
        for (Object obj : (List<Object>) itemsObj) {
            if (!(obj instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) obj;
            LevelItem item = new LevelItem();
            item.setLevelIndex(toInt(m.get("levelIndex")));
            item.setMajorIndex(toInt(m.get("majorIndex")));
            item.setMajorName(m.get("majorName") == null ? "" : String.valueOf(m.get("majorName")));
            item.setMinorIndex(toInt(m.get("minorIndex")));
            item.setDisplayName(m.get("displayName") == null ? "" : String.valueOf(m.get("displayName")));
            item.setPointsThreshold(toInt(m.get("pointsThreshold")));
            item.setIconKey(m.get("iconKey") == null ? null : String.valueOf(m.get("iconKey")));
            item.setColorToken(m.get("colorToken") == null ? null : String.valueOf(m.get("colorToken")));
            item.setIconSvg(m.get("iconSvg") == null ? null : String.valueOf(m.get("iconSvg")));
            item.setBadgeName(m.get("badgeName") == null ? null : String.valueOf(m.get("badgeName")));
            item.setDescription(m.get("description") == null ? null : String.valueOf(m.get("description")));
            items.add(item);
        }
        return levelService.saveProfile(profile, items);
    }

    @ApiOperation("管理端：切换等级方案")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_LEVEL_ACTIVATE", targetType = "LEVEL_PROFILE", targetId = "#body['id']")
    @PostMapping("/admin/level/activate")
    public ResultBean activate(@RequestBody Map<String, Object> body) {
        return levelService.activateProfile(toInt(body == null ? null : body.get("id")));
    }

    @ApiOperation("管理端：复制等级方案")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_LEVEL_CLONE", targetType = "LEVEL_PROFILE", targetId = "#body['id']")
    @PostMapping("/admin/level/clone")
    public ResultBean clone(@RequestBody Map<String, Object> body) {
        Object name = body == null ? null : body.get("name");
        return levelService.cloneProfile(toInt(body == null ? null : body.get("id")),
                name == null ? null : String.valueOf(name));
    }

    @ApiOperation("管理端：删除等级方案")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_LEVEL_DELETE", targetType = "LEVEL_PROFILE", targetId = "#body['id']")
    @PostMapping("/admin/level/delete")
    public ResultBean delete(@RequestBody Map<String, Object> body) {
        return levelService.deleteProfile(toInt(body == null ? null : body.get("id")));
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return null; }
    }
}
