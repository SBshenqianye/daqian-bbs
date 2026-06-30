package com.walker.vo.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 管理员修改用户信息参数
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "AdminUserUpdateParam", description = "管理员修改用户信息参数")
public class AdminUserUpdateParam {

    @ApiModelProperty(value = "用户ID", required = true)
    private Integer id;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("所属单位编号")
    private String orgNo;

    @ApiModelProperty("用户类型(1=普通用户,2=管理员)")
    private String userType;

    @ApiModelProperty("是否禁用(0=活跃,1=禁用)")
    private Integer isAlive;

    @ApiModelProperty("是否重置密码(true=重置为默认密码1234@abcD)")
    private Boolean resetPassword;

    @ApiModelProperty("人员编号")
    private String personnelId;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("用户名（更改需谨慎，涉及登录账号）")
    private String username;

    @ApiModelProperty("密码（明文传入，后端自动BCrypt加密）")
    private String password;
}
