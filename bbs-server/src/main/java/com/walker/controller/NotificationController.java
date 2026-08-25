package com.walker.controller;

import com.walker.service.NotificationService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@Api(tags = "NotificationController")
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "获取用户未读通知数量")
    @GetMapping("/unreadCount")
    public ResultBean getUnreadCount(@RequestParam Integer userId) {
        int count = notificationService.getUnreadCount(userId);
        return ResultBean.success("查询成功", count);
    }

    @ApiOperation(value = "标记通知为已读")
    @PostMapping("/markRead")
    public ResultBean markRead(@RequestParam Integer userId,
                               @RequestParam(required = false) String type) {
        return notificationService.markRead(userId, type);
    }

    @ApiOperation(value = "标记所有通知为已读")
    @PostMapping("/markAllRead")
    public ResultBean markAllRead(@RequestParam Integer userId) {
        return notificationService.markAllRead(userId);
    }
}
