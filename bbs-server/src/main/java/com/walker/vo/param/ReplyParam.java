package com.walker.vo.param;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode
@Accessors(chain = true)
@ApiModel(value = "Reply对象",description = "")
public class ReplyParam {

    @ApiModelProperty(value = "评论人的ID",required = true)
    private Integer replyUserId;

    @ApiModelProperty(value = "评论的对象的ID",required = true)
    private Integer replyToUserId;

    @ApiModelProperty(value = "评论的ID",required = true)
    private Integer commentId;

    @ApiModelProperty(value = "评论的内容",required = true)
    private String replyContent;

    @ApiModelProperty(value = "评论Id",required = true)
    private Integer replyId;

    @ApiModelProperty(value = "是否匿名(0=否,1=是)")
    private Integer isAnonymous;
}
