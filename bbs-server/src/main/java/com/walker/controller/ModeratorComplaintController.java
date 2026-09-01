package com.walker.controller;

import com.walker.service.ModeratorComplaintService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 版主投诉控制器
 */
@Api(tags = "ModeratorComplaintController")
@RestController
public class ModeratorComplaintController {

    @Autowired
    private ModeratorComplaintService complaintService;

    @ApiOperation(value = "用户提交版主投诉")
    @PostMapping("/user/moderatorComplaint/submit")
    public ResultBean submit(@RequestBody Map<String, Object> params) {
        Integer reporterId = params.get("reporterId") != null ? Integer.parseInt(params.get("reporterId").toString()) : null;
        Integer moderatorId = params.get("moderatorId") != null ? Integer.parseInt(params.get("moderatorId").toString()) : null;
        Integer labelId = params.get("labelId") != null ? Integer.parseInt(params.get("labelId").toString()) : null;
        String content = (String) params.get("content");
        return complaintService.submit(reporterId, moderatorId, labelId, content);
    }

    @ApiOperation(value = "管理员审核版主投诉")
    @PostMapping("/admin/moderatorComplaint/review")
    public ResultBean review(@RequestBody Map<String, Object> params) {
        Integer complaintId = params.get("complaintId") != null ? Integer.parseInt(params.get("complaintId").toString()) : null;
        String status = (String) params.get("status");
        String remark = (String) params.get("remark");
        Integer reviewerId = params.get("reviewerId") != null ? Integer.parseInt(params.get("reviewerId").toString()) : null;
        return complaintService.review(complaintId, status, remark, reviewerId);
    }

    @ApiOperation(value = "查询投诉列表（管理员端）")
    @PostMapping("/admin/moderatorComplaint/list")
    public ResultBean listComplaints(@RequestBody Map<String, Object> params) {
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        String status = (String) params.get("status");
        return complaintService.listComplaints(page, size, status);
    }

    @ApiOperation(value = "查询用户自己的投诉记录")
    @PostMapping("/user/moderatorComplaint/myList")
    public ResultBean listMyComplaints(@RequestBody Map<String, Object> params) {
        Integer reporterId = params.get("reporterId") != null ? Integer.parseInt(params.get("reporterId").toString()) : null;
        return complaintService.listMyComplaints(reporterId);
    }
}
