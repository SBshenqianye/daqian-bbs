package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Getter
@Setter
@TableName("bbs_comment")
@ApiModel(value = "Comment对象", description = "")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("评论的id")
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Integer commentId;

    @ApiModelProperty("评论的内容")
    @TableField("comment_content")
    private String commentContent;

    @ApiModelProperty("评论的文章id")
    @TableField("comment_article_id")
    private Integer commentArticleId;

    @ApiModelProperty("评论的用户id")
    @TableField("comment_user_id")
    private Integer commentUserId;

    @ApiModelProperty("评论时间")
    @TableField("comment_time")
    private String commentTime;

    @ApiModelProperty("评论是否被审核通过")
    @TableField("enable")
    private Integer enable;

    @ApiModelProperty("逻辑删除")
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @ApiModelProperty("采纳审批状态(0=未采纳,1=待审批,2=已确认,3=已拒绝)")
    @TableField("adopt_status")
    private Integer adoptStatus;
}
