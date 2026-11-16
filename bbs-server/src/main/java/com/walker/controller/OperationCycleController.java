package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.pojo.OperationCycle;
import com.walker.service.OperationCycleService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "运营周期")
@RestController
public class OperationCycleController {

    @Autowired private OperationCycleService cycleService;

    @ApiOperation("管理端：周期列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/operation-cycle/list")
    public ResultBean list() { return cycleService.list(); }

    @ApiOperation("管理端：保存周期")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_CYCLE_SAVE", targetType = "OPERATION_CYCLE", targetId = "#cycle.id")
    @PostMapping("/admin/operation-cycle/save")
    public ResultBean save(@RequestBody OperationCycle cycle) { return cycleService.save(cycle); }

    @ApiOperation("管理端：激活周期")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_CYCLE_ACTIVATE", targetType = "OPERATION_CYCLE", targetId = "#body['id']")
    @PostMapping("/admin/operation-cycle/activate")
    public ResultBean activate(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("id");
        return cycleService.activate(id == null ? null : Integer.valueOf(String.valueOf(id)));
    }

    @ApiOperation("管理端：周期配置列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/operation-cycle/config/{id}")
    public ResultBean configs(@PathVariable Integer id) { return cycleService.listConfigs(id); }

    @ApiOperation("管理端：保存周期配置")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @PostMapping("/admin/operation-cycle/config/save")
    public ResultBean saveConfig(@RequestBody Map<String, Object> body) {
        Object cycleId = body == null ? null : body.get("cycleId");
        Object key = body == null ? null : body.get("configKey");
        Object value = body == null ? null : body.get("configValue");
        return cycleService.saveConfig(
                cycleId == null ? null : Integer.valueOf(String.valueOf(cycleId)),
                key == null ? null : String.valueOf(key),
                value == null ? null : String.valueOf(value));
    }
}
