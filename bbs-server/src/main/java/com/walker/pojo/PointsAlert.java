package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 积分预警去重记录（如“当月累计扣分达到阈值”）。
 */
@Data
@TableName("bbs_points_alert")
public class PointsAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("user_id")
    private Integer userId;

    @TableField("alert_type")
    private String alertType;

    /** 统计周期，如 2026-11 */
    @TableField("period")
    private String period;

    @TableField("threshold")
    private Integer threshold;

    @TableField("points")
    private Integer points;

    @TableField("create_time")
    private String createTime;
}
