package com.walker.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 回复列表项 VO — 用于"我回复的"和"回复我的"页面
 */
@Setter
@Getter
@Accessors(chain = true)
@ApiModel(value = "ReplyListItemVO", description = "回复列表项")
public class ReplyListItemVO {

    @ApiModelProperty("文章ID")
    private Integer articleId;

    @ApiModelProperty("文章标题")
    private String articleTitle;

    @ApiModelProperty("文章封面")
    private String articleImage;

    @ApiModelProperty("评论ID")
    private Integer commentId;

    @ApiModelProperty("回复ID")
    private Integer replyId;

    @ApiModelProperty("回复/评论内容")
    private String content;

    @ApiModelProperty("回复时间")
    private String time;

    @ApiModelProperty("对方用户ID")
    private Integer fromUserId;

    @ApiModelProperty("对方昵称")
    private String fromNickname;

    @ApiModelProperty("对方头像")
    private String fromPortrait;

    @ApiModelProperty("对方单位名称")
    private String fromOrgName;

    @ApiModelProperty("对方完整单位名称")
    private String fromOrgNameFull;

    @ApiModelProperty("被回复用户ID（回复我的时使用）")
    private Integer toUserId;

    @ApiModelProperty("被回复用户昵称")
    private String toNickname;

    @ApiModelProperty("回复关系描述（如某人回复某人）")
    private String replyRelation;
}
