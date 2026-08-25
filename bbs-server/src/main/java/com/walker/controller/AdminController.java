package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.PointsLog;
import com.walker.service.AdminService;
import com.walker.service.PointsLogService;
import com.walker.vo.ResultBean;
import com.walker.vo.param.AdminParam;
import com.walker.vo.param.PointsAdjustParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2023-02-23
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private PointsLogService pointsLogService;



    @ApiOperation(value = "管理员登录")
    @PostMapping("/login")
    public ResultBean login(@RequestBody AdminParam adminParam){
        if (adminParam != null){
            return adminService.login(adminParam.getUsername(),adminParam.getPassword());
        }
        return ResultBean.error("用户名或密码不能为空！");
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

    @ApiOperation(value = "调整用户积分")
    @PostMapping("/points/adjust")
    public ResultBean adjustUserPoints(@RequestBody PointsAdjustParam param){
        if (param.getUserId() == null || param.getPointsChange() == null) {
            return ResultBean.error("用户ID和积分变动不能为空");
        }
        return pointsLogService.adjustUserPoints(
                param.getUserId(),
                param.getPointsChange(),
                param.getReason(),
                param.getRelatedType(),
                param.getRelatedId(),
                null // operatorId 可从登录上下文获取
        );
    }

    @ApiOperation(value = "查询用户积分调整记录")
    @GetMapping("/points/log/{userId}")
    public ResultBean getPointsLogByUserId(@PathVariable Integer userId){
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        List<PointsLog> logs = pointsLogService.list(
                new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, userId)
                        .orderByDesc(PointsLog::getCreateTime)
        );
        return ResultBean.success("查询成功", logs);
    }

    @ApiOperation(value = "查询用户手动积分调整总额")
    @GetMapping("/points/total/{userId}")
    public ResultBean getPointsTotal(@PathVariable Integer userId){
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        Integer total = pointsLogService.getPointsAdjustment(userId);
        return ResultBean.success("查询成功", total);
    }

    @ApiOperation(value = "撤销一条积分调整记录")
    @PostMapping("/points/undo/{logId}")
    public ResultBean undoPointsLog(@PathVariable Integer logId){
        if (logId == null) {
            return ResultBean.error("记录ID不能为空");
        }
        return pointsLogService.undoPointsLog(logId, null);
    }

}
