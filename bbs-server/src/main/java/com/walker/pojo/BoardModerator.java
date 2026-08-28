package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 版块管理员
 */
@Getter
@Setter
@TableName("bbs_board_moderator")
@ApiModel(value = "BoardModerator对象", description = "版块管理员")
public class BoardModerator implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("关联标签(版块)ID")
    @TableField("label_id")
    private Integer labelId;

    @ApiModelProperty("角色类型(moderator/admin)")
    @TableField("role_type")
    private String roleType;

    @ApiModelProperty("状态(1=有效,0=撤销)")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("任命时间")
    @TableField("appoint_time")
    private String appointTime;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
