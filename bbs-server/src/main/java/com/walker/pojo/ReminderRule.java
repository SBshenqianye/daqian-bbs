package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_reminder_rule")
public class ReminderRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("rule_code")
    private String ruleCode;
    @TableField("rule_name")
    private String ruleName;
    @TableField("event_type")
    private String eventType;
    @TableField("permission_code")
    private String permissionCode;
    @TableField("scope_type")
    private String scopeType;
    private Integer enabled;
    @TableField("delay_minutes")
    private Integer delayMinutes;
    @TableField("repeat_minutes")
    private Integer repeatMinutes;
    @TableField("max_per_day")
    private Integer maxPerDay;
    @TableField("quiet_start")
    private String quietStart;
    @TableField("quiet_end")
    private String quietEnd;
    @TableField("escalate_minutes")
    private Integer escalateMinutes;
    @TableField("escalate_permission_code")
    private String escalatePermissionCode;
    @TableField("title_template")
    private String titleTemplate;
    @TableField("content_template")
    private String contentTemplate;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("create_time")
    private String createTime;
    @TableField("update_time")
    private String updateTime;
}
