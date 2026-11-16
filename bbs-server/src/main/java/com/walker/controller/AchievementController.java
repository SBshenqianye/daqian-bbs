package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.pojo.Achievement;
import com.walker.service.AchievementService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Api(tags = "成就与勋章")
@RestController
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    @ApiOperation("管理端：成就列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/achievement/list")
    public ResultBean list() {
        return achievementService.listAdmin();
    }

    @ApiOperation("管理端：保存成就")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ACHIEVEMENT_SAVE", targetType = "ACHIEVEMENT", targetId = "#achievement.id")
    @PostMapping("/admin/achievement/save")
    public ResultBean save(@RequestBody Achievement achievement) {
        return achievementService.save(achievement);
    }

    @ApiOperation("管理端：删除成就")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ACHIEVEMENT_DELETE", targetType = "ACHIEVEMENT", targetId = "#body['id']")
    @PostMapping("/admin/achievement/delete")
    public ResultBean delete(@RequestBody java.util.Map<String, Object> body) {
        Object id = body == null ? null : body.get("id");
        return achievementService.delete(id == null ? null : Integer.valueOf(String.valueOf(id)));
    }

    @ApiOperation("管理端：下载勋章模板")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/achievement/medal/template")
    public ResultBean template() {
        return ResultBean.success("查询成功", achievementService.medalTemplate());
    }

    @ApiOperation("管理端：导入勋章（SVG/PNG）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @PostMapping("/admin/achievement/medal/import")
    public ResultBean importMedal(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return ResultBean.error("请选择文件");
        try {
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            String content;
            if (name.endsWith(".png")) {
                content = "data:image/png;base64," + Base64.getEncoder().encodeToString(file.getBytes());
            } else {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            String sanitized = achievementService.sanitizeMedal(content);
            if (sanitized == null) return ResultBean.error("勋章文件格式不安全或不受支持");
            return ResultBean.success("导入成功", sanitized);
        } catch (Exception e) {
            return ResultBean.error("导入失败：" + e.getMessage());
        }
    }

    @ApiOperation("用户端：用户成就墙")
    @GetMapping("/common/achievement/user/{userId}")
    public ResultBean userAchievements(@PathVariable Integer userId) {
        return achievementService.listUserAchievements(userId);
    }
}
