package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_reminder_log")
public class ReminderLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("rule_id")
    private Integer ruleId;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Integer targetId;
    @TableField("user_id")
    private Integer userId;
    @TableField("batch_key")
    private String batchKey;
    @TableField("sent_at")
    private String sentAt;
}
