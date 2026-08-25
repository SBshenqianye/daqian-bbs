package com.walker.controller;

import com.walker.service.ViolationService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 违规管理控制器
 */
@Api(tags = "ViolationController")
@RestController
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    @ApiOperation(value = "管理员记录违规并扣分")
    @PostMapping("/admin/violation/add")
    public ResultBean addViolation(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        String violationType = (String) params.get("violationType");
        String relatedType = (String) params.get("relatedType");
        Integer relatedId = params.get("relatedId") != null ? Integer.parseInt(params.get("relatedId").toString()) : null;
        Integer operatorId = (Integer) params.get("operatorId");
        String remark = (String) params.get("remark");
        return violationService.addViolation(userId, violationType, relatedType, relatedId, operatorId, remark);
    }

    @ApiOperation(value = "管理员查看违规列表")
    @PostMapping("/admin/violation/list")
    public ResultBean listViolations(@RequestBody Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? Integer.parseInt(params.get("userId").toString()) : null;
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return violationService.listViolations(userId, page, size);
    }

    @ApiOperation(value = "用户查看自己的违规记录")
    @PostMapping("/user/violation/myList")
    public ResultBean listMyViolations(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return violationService.listMyViolations(userId, page, size);
    }
}
