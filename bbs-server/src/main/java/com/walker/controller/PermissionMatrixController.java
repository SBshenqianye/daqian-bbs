package com.walker.controller;

import com.walker.service.PermissionMatrixService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限矩阵（只读，Phase A）。
 *
 * <p>仅超级管理员可访问；不提供任何写接口，不修改鉴权逻辑。</p>
 */
@Api(tags = "PermissionMatrixController")
@RestController
@RequestMapping("/admin/permission")
public class PermissionMatrixController {

    @Autowired
    private PermissionMatrixService permissionMatrixService;

    @ApiOperation(value = "权限矩阵：角色→权限点、权限点→角色、接口→注解（只读）")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/matrix")
    public ResultBean matrix() {
        return ResultBean.success("查询成功", permissionMatrixService.getMatrix());
    }
}
