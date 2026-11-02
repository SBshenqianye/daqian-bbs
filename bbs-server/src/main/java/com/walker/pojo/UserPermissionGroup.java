package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户-权限组。
 */
@Getter
@Setter
@TableName("bbs_user_permission_group")
@ApiModel(value = "UserPermissionGroup对象", description = "用户-权限组")
public class UserPermissionGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("group_id")
    private Integer groupId;

    private Integer priority;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private String createTime;
}
