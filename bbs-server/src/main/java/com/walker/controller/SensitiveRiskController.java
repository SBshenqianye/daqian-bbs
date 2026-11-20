package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.service.SensitiveHitService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 敏感词风险画像：全量用户热力方阵、风险排行榜、单用户命中详情、内容命中详情。
 *
 * <p>权限点 sensitive:read（版主/管理员/超管可授予）；版主的数据范围仍按其板块任职过滤。</p>
 */
@Api(tags = "SensitiveRiskController")
@RestController
@RequestMapping("/admin/sensitive")
public class SensitiveRiskController {

    @Autowired
    private SensitiveHitService sensitiveHitService;

    @ApiOperation("全量用户热力方阵（累计命中）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).SENSITIVE_READ)")
    @GetMapping("/risk/grid")
    public ResultBean grid() {
        return ResultBean.success("查询成功", sensitiveHitService.grid());
    }

    @ApiOperation("风险排行榜（近 N 天 + 近 12 周热力条）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).SENSITIVE_READ)")
    @GetMapping("/risk/rank")
    public ResultBean rank(@RequestParam(value = "page", defaultValue = "1") Integer page,
                           @RequestParam(value = "size", defaultValue = "20") Integer size,
                           @RequestParam(value = "days", defaultValue = "30") Integer days) {
        return sensitiveHitService.rank(page, size, days);
    }

    @ApiOperation("单个风险用户详情")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).SENSITIVE_READ)")
    @GetMapping("/risk/user/{userId}")
    public ResultBean userDetail(@PathVariable("userId") Integer userId,
                                 @RequestParam(value = "page", defaultValue = "1") Integer page,
                                 @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return sensitiveHitService.userDetail(userId, page, size);
    }

    @ApiOperation("内容命中详情（命中词 + 原文上下文）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).SENSITIVE_READ)")
    @AuditLog(module = "ARTICLE", action = "ARTICLE_SENSITIVE_VIEW", snapshot = false)
    @GetMapping("/hit")
    public ResultBean hitDetail(@RequestParam("targetType") String targetType,
                                @RequestParam("targetId") Integer targetId) {
        return sensitiveHitService.hitDetail(targetType, targetId);
    }
}
