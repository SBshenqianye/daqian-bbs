package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
    // 以下字段允许被清空：updateById 默认忽略 null，会导致「自动图标 / 清空徽章」保存不生效
    @TableField(value = "icon_key", updateStrategy = FieldStrategy.IGNORED)
    private String iconKey;
    @TableField(value = "color_token", updateStrategy = FieldStrategy.IGNORED)
    private String colorToken;
    @TableField("enabled")
    private Integer enabled;

    @TableField(value = "icon_svg", updateStrategy = FieldStrategy.IGNORED)
    private String iconSvg;

    @TableField(value = "badge_name", updateStrategy = FieldStrategy.IGNORED)
    private String badgeName;

    @TableField("description")
    private String description;
}
