package com.walker.vo.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 积分调整请求参数
 */
@Data
@ApiModel(value = "积分调整请求参数", description = "管理员手动调整用户积分")
public class PointsAdjustParam {

    @ApiModelProperty("用户ID")
    private Integer userId;

    @ApiModelProperty("积分变动（正数加分，负数扣分）")
    private Integer pointsChange;

    @ApiModelProperty("调整原因")
    private String reason;

    @ApiModelProperty("关联类型（article/comment/reply/manual）")
    private String relatedType;

    @ApiModelProperty("关联ID（帖子ID/评论ID/回复ID）")
    private Integer relatedId;
}
