package com.walker.controller;

import com.walker.service.ReportService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 举报管理控制器
 */
@Api(tags = "ReportController")
@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @ApiOperation(value = "用户提交举报")
    @PostMapping("/article/report")
    public ResultBean submitReport(@RequestBody Map<String, Object> params) {
        Integer reporterId = (Integer) params.get("reporterId");
        String targetType = (String) params.get("targetType");
        Integer targetId = (Integer) params.get("targetId");
        String violationType = (String) params.get("violationType");
        String reason = (String) params.get("reason");
        return reportService.submitReport(reporterId, targetType, targetId, violationType, reason);
    }

    @ApiOperation(value = "管理员审核举报")
    @PostMapping("/admin/report/review")
    public ResultBean reviewReport(@RequestBody Map<String, Object> params) {
        Integer reportId = (Integer) params.get("reportId");
        Integer reviewerId = (Integer) params.get("reviewerId");
        String status = (String) params.get("status");
        String remark = (String) params.get("remark");
        // 驳回时可勾选"恶意举报"并扣分（分值正整数，前端从字典 false_report 取默认）
        Boolean malicious = Boolean.TRUE.equals(params.get("malicious"));
        Integer deductPoints = params.get("deductPoints") != null
                ? Integer.valueOf(params.get("deductPoints").toString()) : null;
        return reportService.reviewReport(reportId, reviewerId, status, remark, malicious, deductPoints);
    }

    @ApiOperation(value = "管理员查看举报列表")
    @PostMapping("/admin/report/list")
    public ResultBean listReports(@RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return reportService.listReports(status, page, size);
    }

    @ApiOperation(value = "管理员查看举报列表（按举报目标分组折叠，避免同内容刷屏）")
    @PostMapping("/admin/report/listGrouped")
    public ResultBean listReportsGrouped(@RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return reportService.listReportsGrouped(status, page, size);
    }

    @ApiOperation(value = "用户查看自己的举报记录")
    @PostMapping("/user/report/myList")
    public ResultBean listMyReports(@RequestBody Map<String, Object> params) {
        Integer reporterId = (Integer) params.get("reporterId");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return reportService.listMyReports(reporterId, page, size);
    }

    @ApiOperation(value = "前置查询：当前用户是否已举报过该内容（用于点击举报时即时提醒）")
    @PostMapping("/user/report/check")
    public ResultBean checkReported(@RequestBody Map<String, Object> params) {
        Integer reporterId = (Integer) params.get("reporterId");
        String targetType = (String) params.get("targetType");
        Integer targetId = (Integer) params.get("targetId");
        return reportService.checkReported(reporterId, targetType, targetId);
    }
}
