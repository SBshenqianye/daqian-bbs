package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.pojo.Notification;
import com.walker.service.NotificationService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @ApiOperation(value = "获取通知列表（分页）")
    @GetMapping("/list")
    public ResultBean getNotificationList(@RequestParam Integer userId,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationService.page(pageParam, wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }
}
