package com.walker.controller;

import com.walker.service.BoardModeratorService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 版块管理员控制器
 */
@Api(tags = "BoardModeratorController")
@RestController
public class BoardModeratorController {

    @Autowired
    private BoardModeratorService boardModeratorService;

    @ApiOperation(value = "任命版主")
    @PostMapping("/admin/moderator/appoint")
    public ResultBean appoint(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer labelId = (Integer) params.get("labelId");
        Integer operatorId = (Integer) params.get("operatorId");
        return boardModeratorService.appoint(userId, labelId, operatorId);
    }

    @ApiOperation(value = "撤销版主")
    @PostMapping("/admin/moderator/dismiss")
    public ResultBean dismiss(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer labelId = (Integer) params.get("labelId");
        return boardModeratorService.dismiss(userId, labelId);
    }

    @ApiOperation(value = "版主列表")
    @PostMapping("/admin/moderator/list")
    public ResultBean listModerators(@RequestBody Map<String, Object> params) {
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return boardModeratorService.listModerators(page, size);
    }

    @ApiOperation(value = "检查用户是否为版主")
    @GetMapping("/common/moderator/check")
    public ResultBean checkModerator(@RequestParam Integer userId, @RequestParam Integer labelId) {
        boolean isMod = boardModeratorService.isModerator(userId, labelId);
        Map<String, Object> data = new HashMap<>();
        data.put("isModerator", isMod);
        return ResultBean.success("查询成功", data);
    }

    @ApiOperation(value = "发放本月版主履职奖励（每月一次性15积分）")
    @PostMapping("/admin/moderator/monthlyReward")
    public ResultBean monthlyReward(@RequestBody Map<String, Object> params) {
        Integer operatorId = params.get("operatorId") != null
                ? Integer.parseInt(params.get("operatorId").toString()) : null;
        return boardModeratorService.monthlyReward(operatorId);
    }
}
