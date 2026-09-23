package com.walker.controller;

import com.walker.service.DashboardService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端仪表盘统计控制器（#7）
 */
@Api(tags = "DashboardController")
@RestController
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @ApiOperation(value = "仪表盘概览统计（用户/帖子/评论/待办等）")
    @PostMapping("/admin/dashboard/counts")
    public ResultBean counts() {
        return dashboardService.getCounts();
    }
}
