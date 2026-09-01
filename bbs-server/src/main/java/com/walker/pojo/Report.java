package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 实名举报记录
 */
@Getter
@Setter
@TableName("bbs_report")
@ApiModel(value = "Report对象", description = "实名举报记录")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("举报人ID")
    @TableField("reporter_id")
    private Integer reporterId;

    @ApiModelProperty("举报目标类型(article/comment/reply)")
    @TableField("target_type")
    private String targetType;

    @ApiModelProperty("被举报内容ID")
    @TableField("target_id")
    private Integer targetId;

    @ApiModelProperty("举报原因")
    @TableField("reason")
    private String reason;

    @ApiModelProperty("违规类型(spam/plagiarism/illegal/attack/leak)")
    @TableField("violation_type")
    private String violationType;

    @ApiModelProperty("状态(pending/confirmed/rejected)")
    @TableField("status")
    private String status;

    @ApiModelProperty("审核人ID")
    @TableField("reviewer_id")
    private Integer reviewerId;

    @ApiModelProperty("审核时间")
    @TableField("review_time")
    private String reviewTime;

    @ApiModelProperty("审核备注")
    @TableField("review_remark")
    private String reviewRemark;

    @ApiModelProperty("是否已给举报人发积分(0=否,1=是)")
    @TableField("points_awarded")
    private Integer pointsAwarded;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
