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
 * 权限组-权限点（MVP 仅 grant）。
 */
@Getter
@Setter
@TableName("bbs_permission_group_item")
@ApiModel(value = "PermissionGroupItem对象", description = "权限组-权限点")
public class PermissionGroupItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("group_id")
    private Integer groupId;

    @TableField("permission_code")
    private String permissionCode;

    private String effect;

    @TableField("create_time")
    private String createTime;
}
