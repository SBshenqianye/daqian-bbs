package com.walker.controller;

import com.walker.service.LoginLogService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录积分控制器
 */
@Api(tags = "LoginLogController")
@RestController
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    @ApiOperation(value = "记录每日登录")
    @PostMapping("/user/dailyLogin")
    public ResultBean dailyLogin(@RequestParam Integer userId) {
        return loginLogService.dailyLogin(userId);
    }

    @ApiOperation(value = "浏览心跳上报（每60秒一次）")
    @PostMapping("/user/browseHeartbeat")
    public ResultBean browseHeartbeat(@RequestParam Integer userId) {
        return loginLogService.browseHeartbeat(userId);
    }
}
