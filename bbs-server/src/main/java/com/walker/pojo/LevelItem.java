package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_level_item")
public class LevelItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("profile_id")
    private Integer profileId;
    @TableField("level_index")
    private Integer levelIndex;
    @TableField("major_index")
    private Integer majorIndex;
    @TableField("major_name")
    private String majorName;
    @TableField("minor_index")
    private Integer minorIndex;
    @TableField("display_name")
    private String displayName;
    @TableField("points_threshold")
    private Integer pointsThreshold;
    @TableField("icon_key")
    private String iconKey;
    @TableField("color_token")
    private String colorToken;
    @TableField("enabled")
    private Integer enabled;

    @TableField("icon_svg")
    private String iconSvg;

    @TableField("badge_name")
    private String badgeName;

    @TableField("description")
    private String description;
}
