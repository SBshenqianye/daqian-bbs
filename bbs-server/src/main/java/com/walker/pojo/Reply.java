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
@TableName("bbs_reply")
@ApiModel(value = "Reply对象", description = "")
public class Reply implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("回复的id")
    @TableId(value = "reply_id", type = IdType.AUTO)
    private Integer replyId;

    @ApiModelProperty("回复的具体内容")
    @TableField("reply_content")
    private String replyContent;

    @ApiModelProperty("回复的对象的Id")
    @TableField("reply_to_user_id")
    private Integer replyToUserId;

    @ApiModelProperty("回复的评论的ID")
    @TableField("comment_id")
    private Integer commentId;

    @ApiModelProperty("回复的用户id")
    @TableField("reply_user_id")
    private Integer replyUserId;

    @ApiModelProperty("回复时间")
    @TableField("reply_time")
    private String replyTime;

    @ApiModelProperty("回复是否被审核通过")
    @TableField("enable")
    private Integer enable;

    @ApiModelProperty("逻辑删除")
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @ApiModelProperty("是否被采纳(0=否,1=是)")
    @TableField("is_adopted")
    private Integer isAdopted;

}
