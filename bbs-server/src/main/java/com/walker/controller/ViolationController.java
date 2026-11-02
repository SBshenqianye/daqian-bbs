package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.service.ViolationService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 违规管理控制器
 */
@Api(tags = "ViolationController")
@RestController
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    @ApiOperation(value = "管理员记录违规并扣分")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @AuditLog(module = "VIOLATION", action = "VIOLATION_ADD",
            targetType = "VIOLATION_TARGET",
            targetId = "#params['relatedType'] + ':' + #params['relatedId']",
            snapshot = false)
    @PostMapping("/admin/violation/add")
    public ResultBean addViolation(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        String violationType = (String) params.get("violationType");
        String relatedType = (String) params.get("relatedType");
        Integer relatedId = params.get("relatedId") != null ? Integer.parseInt(params.get("relatedId").toString()) : null;
        Integer operatorId = (Integer) params.get("operatorId");
        String remark = (String) params.get("remark");
        return violationService.addViolation(userId, violationType, relatedType, relatedId, operatorId, remark);
    }

    @ApiOperation(value = "管理员查看违规列表")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @PostMapping("/admin/violation/list")
    public ResultBean listViolations(@RequestBody Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? Integer.parseInt(params.get("userId").toString()) : null;
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return violationService.listViolations(userId, page, size);
    }

    @ApiOperation(value = "用户查看自己的违规记录")
    @PostMapping("/user/violation/myList")
    public ResultBean listMyViolations(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return violationService.listMyViolations(userId, page, size);
    }

    @ApiOperation(value = "管理员取消违规（回滚扣分+恢复内容，操作人以JWT为准）")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    @AuditLog(module = "VIOLATION", action = "VIOLATION_CANCEL",
            targetType = "VIOLATION", targetId = "#params['violationId']")
    @PostMapping("/admin/violation/cancel")
    public ResultBean cancelViolation(@RequestBody Map<String, Object> params) {
        Integer violationId = params.get("violationId") != null ? Integer.parseInt(params.get("violationId").toString()) : null;
        String reason = (String) params.get("reason");
        return violationService.cancelViolation(violationId, reason);
    }
}
