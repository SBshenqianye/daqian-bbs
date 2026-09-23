package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 违规记录
 */
@Getter
@Setter
@TableName("bbs_violation")
@ApiModel(value = "Violation对象", description = "违规记录")
public class Violation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("违规用户ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("违规类型(对应字典violation)")
    @TableField("violation_type")
    private String violationType;

    @ApiModelProperty("扣减积分")
    @TableField("points_deducted")
    private Integer pointsDeducted;

    @ApiModelProperty("关联类型(article/comment/reply)")
    @TableField("related_type")
    private String relatedType;

    @ApiModelProperty("关联ID")
    @TableField("related_id")
    private Integer relatedId;

    @ApiModelProperty("操作管理员ID")
    @TableField("operator_id")
    private Integer operatorId;

    @ApiModelProperty("备注说明")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("状态(active=生效中/cancelled=已取消)")
    @TableField("status")
    private String status;

    @ApiModelProperty("取消原因")
    @TableField("cancel_reason")
    private String cancelReason;

    @ApiModelProperty("取消操作人ID")
    @TableField("cancel_operator_id")
    private Integer cancelOperatorId;

    @ApiModelProperty("取消时间")
    @TableField("cancel_time")
    private String cancelTime;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
