package com.walker.controller;

import com.walker.service.AuditLogService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询（仅超级管理员）。
 */
@Api(tags = "AuditLogController")
@RestController
@RequestMapping("/admin/audit")
public class AuditLogController {

    private static final String FULL_TIME_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$";
    private static final String DATE_PATTERN = "^\\d{4}-\\d{2}-\\d{2}$";

    @Autowired
    private AuditLogService auditLogService;

    @ApiOperation(value = "分页查询审计日志")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/list")
    public ResultBean list(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "operatorId", required = false) Integer operatorId,
            @RequestParam(value = "operatorName", required = false) String operatorName,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "targetType", required = false) String targetType,
            @RequestParam(value = "targetId", required = false) String targetId) {

        String normalizedStart = normalizeTime(startTime, false);
        String normalizedEnd = normalizeTime(endTime, true);
        if (normalizedStart == null && StringUtils.isNotBlank(startTime)) {
            return ResultBean.error("开始时间格式应为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        }
        if (normalizedEnd == null && StringUtils.isNotBlank(endTime)) {
            return ResultBean.error("结束时间格式应为 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        }

        return ResultBean.success("查询成功", auditLogService.list(
                page, size, module, action, operatorId, operatorName,
                result, normalizedStart, normalizedEnd, targetType, targetId));
    }

    /**
     * 支持完整时间或日期；日期结束时间补 23:59:59，避免漏掉当天记录。
     */
    private String normalizeTime(String value, boolean endOfDay) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches(FULL_TIME_PATTERN)) {
            return trimmed;
        }
        if (trimmed.matches(DATE_PATTERN)) {
            return trimmed + (endOfDay ? " 23:59:59" : " 00:00:00");
        }
        return null;
    }
}
