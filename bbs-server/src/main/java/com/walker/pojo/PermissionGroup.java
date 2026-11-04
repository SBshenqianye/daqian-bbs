package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 权限组：账号权限的唯一来源，默认内置组与自建组地位一致。
 */
@Getter
@Setter
@TableName("bbs_permission_group")
@ApiModel(value = "PermissionGroup对象", description = "权限组")
public class PermissionGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("group_code")
    private String groupCode;

    @TableField("group_name")
    private String groupName;

    private String description;

    @TableField("scope_type")
    private String scopeType;

    @TableField("data_scope_type")
    private String dataScopeType;

    private Integer enabled;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private String createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private String updateTime;
}
