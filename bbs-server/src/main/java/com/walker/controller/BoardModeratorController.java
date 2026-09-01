package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.User;
import com.walker.service.BoardModeratorService;
import com.walker.service.UserService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 版块管理员控制器
 */
@Api(tags = "BoardModeratorController")
@RestController
public class BoardModeratorController {

    @Autowired
    private BoardModeratorService boardModeratorService;

    @Autowired
    private UserService userService;

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

    @ApiOperation(value = "查询指定板块的版主列表（公开接口，用于投诉选择版主）")
    @GetMapping("/common/moderator/listByLabel")
    public ResultBean listModeratorsByLabel(@RequestParam Integer labelId) {
        LambdaQueryWrapper<BoardModerator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BoardModerator::getLabelId, labelId)
                .eq(BoardModerator::getStatus, 1);
        List<BoardModerator> moderators = boardModeratorService.list(wrapper);
        if (moderators.isEmpty()) {
            return ResultBean.success("该板块暂无版主", new ArrayList<>());
        }
        return ResultBean.success("查询成功", buildModeratorInfoList(moderators));
    }

    @ApiOperation(value = "查询所有有效版主列表（公开接口，用于投诉跨板块搜索版主）")
    @GetMapping("/common/moderator/listAll")
    public ResultBean listAllModerators() {
        LambdaQueryWrapper<BoardModerator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BoardModerator::getStatus, 1);
        List<BoardModerator> moderators = boardModeratorService.list(wrapper);
        if (moderators.isEmpty()) {
            return ResultBean.success("暂无版主", new ArrayList<>());
        }
        return ResultBean.success("查询成功", buildModeratorInfoList(moderators));
    }

    /**
     * 构建版主信息列表（含昵称、头像、所属板块名）
     */
    private List<Map<String, Object>> buildModeratorInfoList(List<BoardModerator> moderators) {
        // 填充用户昵称
        Set<Integer> userIds = new HashSet<>();
        for (BoardModerator m : moderators) userIds.add(m.getUserId());
        Map<Integer, String> nameMap = new HashMap<>();
        Map<Integer, String> avatarMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            for (User u : users) {
                nameMap.put(u.getId(), u.getNickname());
                avatarMap.put(u.getId(), u.getPortrait());
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BoardModerator m : moderators) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", m.getUserId());
            item.put("nickname", nameMap.getOrDefault(m.getUserId(), "用户#" + m.getUserId()));
            item.put("avatar", avatarMap.get(m.getUserId()));
            item.put("labelId", m.getLabelId());
            result.add(item);
        }
        return result;
    }

    @ApiOperation(value = "发放本月版主履职奖励（每月一次性15积分）")
    @PostMapping("/admin/moderator/monthlyReward")
    public ResultBean monthlyReward(@RequestBody Map<String, Object> params) {
        Integer operatorId = params.get("operatorId") != null
                ? Integer.parseInt(params.get("operatorId").toString()) : null;
        return boardModeratorService.monthlyReward(operatorId);
    }

    @ApiOperation(value = "取消版主本月履职奖励")
    @PostMapping("/admin/moderator/cancelReward")
    public ResultBean cancelReward(@RequestBody Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? Integer.parseInt(params.get("userId").toString()) : null;
        Integer operatorId = params.get("operatorId") != null ? Integer.parseInt(params.get("operatorId").toString()) : null;
        String remark = (String) params.get("remark");
        return boardModeratorService.cancelReward(userId, operatorId, remark);
    }

    @ApiOperation(value = "恢复版主本月履职奖励")
    @PostMapping("/admin/moderator/restoreReward")
    public ResultBean restoreReward(@RequestBody Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? Integer.parseInt(params.get("userId").toString()) : null;
        return boardModeratorService.restoreReward(userId);
    }

    @ApiOperation(value = "查询本月被取消奖励的版主列表")
    @PostMapping("/admin/moderator/cancelledRewards")
    public ResultBean cancelledRewards() {
        return boardModeratorService.listCancelledRewards();
    }

    @ApiOperation(value = "手动触发自动发放（测试用）")
    @PostMapping("/admin/moderator/triggerAutoReward")
    public ResultBean triggerAutoReward() {
        return boardModeratorService.autoMonthlyReward();
    }
}
