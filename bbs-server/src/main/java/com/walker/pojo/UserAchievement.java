package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_user_achievement")
public class UserAchievement implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId;
    @TableField("achievement_id")
    private Integer achievementId;
    @TableField("progress_json")
    private String progressJson;
    private Integer unlocked;
    @TableField("unlocked_at")
    private String unlockedAt;
    private String source;
}
