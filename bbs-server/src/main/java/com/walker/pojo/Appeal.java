package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 申诉记录
 */
@Getter
@Setter
@TableName("bbs_appeal")
@ApiModel(value = "Appeal对象", description = "申诉记录")
public class Appeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("申诉人ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("申诉类型(violation/points/other)")
    @TableField("appeal_type")
    private String appealType;

    @ApiModelProperty("关联的违规/积分记录ID")
    @TableField("related_id")
    private Integer relatedId;

    @ApiModelProperty("申诉内容")
    @TableField("content")
    private String content;

    @ApiModelProperty("状态(pending/accepted/rejected)")
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

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
