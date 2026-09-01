package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 版主履职奖励取消记录
 */
@TableName("bbs_moderator_reward_cancel")
@ApiModel(value = "ModeratorRewardCancel对象", description = "版主履职奖励取消记录")
public class ModeratorRewardCancel {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("年月(yyyy-MM)")
    @TableField("year_month")
    private String yearMonth;

    @ApiModelProperty("被取消奖励的版主用户ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("操作人ID")
    @TableField("operator_id")
    private Integer operatorId;

    @ApiModelProperty("取消原因")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getOperatorId() { return operatorId; }
    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
