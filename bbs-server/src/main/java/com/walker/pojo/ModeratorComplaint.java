package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 版主投诉表
 */
@TableName("bbs_moderator_complaint")
@ApiModel(value = "ModeratorComplaint对象", description = "版主投诉")
public class ModeratorComplaint {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键ID")
    private Integer id;

    @ApiModelProperty("投诉人用户ID")
    @TableField("reporter_id")
    private Integer reporterId;

    @ApiModelProperty("被投诉版主用户ID")
    @TableField("moderator_id")
    private Integer moderatorId;

    @ApiModelProperty("被投诉版主管理的标签ID")
    @TableField("label_id")
    private Integer labelId;

    @ApiModelProperty("投诉内容")
    @TableField("content")
    private String content;

    @ApiModelProperty("处理状态(pending/accepted/rejected)")
    @TableField("status")
    private String status;

    @ApiModelProperty("审核人ID")
    @TableField("reviewer_id")
    private Integer reviewerId;

    @ApiModelProperty("审核备注")
    @TableField("review_remark")
    private String reviewRemark;

    @ApiModelProperty("审核时间")
    @TableField("review_time")
    private String reviewTime;

    @ApiModelProperty("投诉时间")
    @TableField("create_time")
    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getReporterId() { return reporterId; }
    public void setReporterId(Integer reporterId) { this.reporterId = reporterId; }
    public Integer getModeratorId() { return moderatorId; }
    public void setModeratorId(Integer moderatorId) { this.moderatorId = moderatorId; }
    public Integer getLabelId() { return labelId; }
    public void setLabelId(Integer labelId) { this.labelId = labelId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getReviewerId() { return reviewerId; }
    public void setReviewerId(Integer reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public String getReviewTime() { return reviewTime; }
    public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
