package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论 / 回帖点赞（targetType: comment | reply）。
 */
@Data
@TableName("bbs_thumb")
public class Thumb implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Integer targetId;

    @TableField("create_time")
    private String createTime;
}
