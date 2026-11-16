package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_achievement")
public class Achievement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("achievement_code")
    private String achievementCode;
    @TableField("achievement_name")
    private String achievementName;
    private String description;
    @TableField("icon_svg")
    private String iconSvg;
    private String category;
    @TableField("rule_json")
    private String ruleJson;
    @TableField("rule_logic")
    private String ruleLogic;
    @TableField("preset")
    private Integer preset;
    private Integer enabled;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private String createTime;
    @TableField("update_time")
    private String updateTime;
}
