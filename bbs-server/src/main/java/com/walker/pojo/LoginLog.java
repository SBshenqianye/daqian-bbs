package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 每日登录浏览记录
 */
@Getter
@Setter
@TableName("bbs_login_log")
@ApiModel(value = "LoginLog对象", description = "每日登录浏览记录")
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("登录日期(YYYY-MM-DD)")
    @TableField("login_date")
    private String loginDate;

    @ApiModelProperty("登录时间")
    @TableField("login_time")
    private String loginTime;

    @ApiModelProperty("有效浏览分钟数")
    @TableField("browse_minutes")
    private Integer browseMinutes;

    @ApiModelProperty("是否已发积分(0=否,1=是)")
    @TableField("points_awarded")
    private Integer pointsAwarded;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
