package com.walker.controller;

import com.walker.service.AppealService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 申诉管理控制器
 */
@Api(tags = "AppealController")
@RestController
public class AppealController {

    @Autowired
    private AppealService appealService;

    @ApiOperation(value = "用户提交申诉")
    @PostMapping("/user/appeal/submit")
    public ResultBean submitAppeal(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        String appealType = (String) params.get("appealType");
        Integer relatedId = params.get("relatedId") != null ? Integer.parseInt(params.get("relatedId").toString()) : null;
        String content = (String) params.get("content");
        return appealService.submitAppeal(userId, appealType, relatedId, content);
    }

    @ApiOperation(value = "管理员审核申诉")
    @PostMapping("/admin/appeal/review")
    public ResultBean reviewAppeal(@RequestBody Map<String, Object> params) {
        Integer appealId = (Integer) params.get("appealId");
        Integer reviewerId = (Integer) params.get("reviewerId");
        String status = (String) params.get("status");
        String remark = (String) params.get("remark");
        return appealService.reviewAppeal(appealId, reviewerId, status, remark);
    }

    @ApiOperation(value = "管理员查看申诉列表")
    @PostMapping("/admin/appeal/list")
    public ResultBean listAppeals(@RequestBody Map<String, Object> params) {
        String status = (String) params.get("status");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return appealService.listAppeals(status, page, size);
    }

    @ApiOperation(value = "用户查看自己的申诉记录")
    @PostMapping("/user/appeal/myList")
    public ResultBean listMyAppeals(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return appealService.listMyAppeals(userId, page, size);
    }
}
