package com.walker.controller;


import com.walker.vo.ResultBean;
import com.walker.service.UserService;
import com.walker.vo.param.UserRegisterParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 注册
 */

@Api(tags = "RegisterController")
@RestController
public class RegisterController {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "注册")
    @PostMapping("/common/register")
    public ResultBean register(@Validated @RequestBody UserRegisterParam userRegisterParam, HttpServletRequest request){
        return ResultBean.error("注册功能已关闭，请联系管理员创建账号");
    }
}
