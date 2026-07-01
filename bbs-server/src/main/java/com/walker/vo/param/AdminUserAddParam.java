package com.walker.vo.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 管理员新增用户参数
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "AdminUserAddParam", description = "管理员新增用户参数")
public class AdminUserAddParam {

    @ApiModelProperty(value = "用户名", required = true)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ApiModelProperty(value = "用户昵称", required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty(value = "所属单位编号", required = true)
    @NotBlank(message = "单位不能为空")
    private String orgNo;

    @ApiModelProperty("用户类型(1=普通用户,2=管理员)")
    private String userType;

    @ApiModelProperty("是否禁用(0=活跃,1=禁用)")
    private Integer isAlive;

    @ApiModelProperty("人员编号")
    private String personnelId;

    @ApiModelProperty("身份证号")
    private String idCard;

    @ApiModelProperty("自定义密码（不传则默认 1234@abcD，用户首次登录需修改）")
    private String password;
}
