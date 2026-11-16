package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_level_profile")
public class LevelProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("profile_name")
    private String profileName;
    @TableField("major_count")
    private Integer majorCount;
    @TableField("minor_per_major")
    private Integer minorPerMajor;
    @TableField("status")
    private Integer status;
    @TableField("create_time")
    private String createTime;
    @TableField("update_time")
    private String updateTime;
}
