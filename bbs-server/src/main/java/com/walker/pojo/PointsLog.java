package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 积分调整日志
 */
@Getter
@Setter
@TableName("bbs_points_log")
@ApiModel(value = "PointsLog对象", description = "积分调整日志")
public class PointsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("积分变动（正数加分，负数扣分）")
    @TableField("points_change")
    private Integer pointsChange;

    @ApiModelProperty("调整原因")
    @TableField("reason")
    private String reason;

    @ApiModelProperty("关联类型（article/comment/reply/manual/undo）")
    @TableField("related_type")
    private String relatedType;

    @ApiModelProperty("关联ID（帖子ID/评论ID/回复ID）")
    @TableField("related_id")
    private Integer relatedId;

    @ApiModelProperty("操作人ID（管理员）")
    @TableField("operator_id")
    private Integer operatorId;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty("是否已被撤销(0=否,1=是)")
    @TableField("is_reversed")
    private Integer isReversed;

    @ApiModelProperty("被哪条撤销记录撤销（记录ID）")
    @TableField("reversed_by")
    private Integer reversedBy;

    @ApiModelProperty("此记录撤销了哪条原始记录（记录ID）")
    @TableField("reversing_record")
    private Integer reversingRecord;
}
