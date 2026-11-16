package com.walker.controller;

import com.walker.mapper.AccountBadgeMapper;
import com.walker.service.CurrentUserService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 个人中心 Tab 红点：返回各栏目最新动态时间，由前端与“上次查看时间”比较。
 */
@Api(tags = "个人中心红点")
@RestController
public class AccountBadgeController {

    @Autowired
    private AccountBadgeMapper accountBadgeMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @ApiOperation("个人中心各 Tab 最新动态时间")
    @GetMapping("/user/account/badges")
    public ResultBean badges() {
        Integer userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResultBean.error("请先登录");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorites", safe(() -> accountBadgeMapper.favoritesLatest(userId)));
        data.put("replies", safe(() -> accountBadgeMapper.repliesLatest(userId)));
        data.put("points", safe(() -> accountBadgeMapper.pointsLatest(userId)));
        data.put("violations", safe(() -> accountBadgeMapper.violationsLatest(userId)));
        data.put("appeals", safe(() -> accountBadgeMapper.appealsLatest(userId)));
        data.put("complaint", safe(() -> accountBadgeMapper.complaintsLatest(userId)));
        data.put("reports", safe(() -> accountBadgeMapper.reportsLatest(userId)));
        return ResultBean.success("查询成功", data);
    }

    private String safe(java.util.function.Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }
}
