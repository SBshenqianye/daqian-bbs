package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.pojo.ReminderRule;
import com.walker.service.ReminderService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Api(tags = "待办提醒")
@RestController
public class ReminderController {

    @Autowired private ReminderService reminderService;

    @ApiOperation("管理端：提醒规则列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/reminder/rules")
    public ResultBean listRules() { return reminderService.listRules(); }

    @ApiOperation("管理端：可选权限点（枚举）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/reminder/permissions")
    public ResultBean permissions() { return reminderService.listPermissions(); }

    @ApiOperation("管理端：保存提醒规则")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_REMINDER_SAVE", targetType = "REMINDER_RULE", targetId = "#rule.id")
    @PostMapping("/admin/reminder/save")
    public ResultBean save(@RequestBody ReminderRule rule) { return reminderService.saveRule(rule); }
}
