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
 * 权限预设方案（权限组快照）。
 */
@Getter
@Setter
@TableName("bbs_permission_preset")
@ApiModel(value = "PermissionPreset对象", description = "权限预设方案")
public class PermissionPreset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("preset_code")
    private String presetCode;

    @TableField("preset_name")
    private String presetName;

    @TableField("snapshot_json")
    private String snapshotJson;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private String createTime;
}
