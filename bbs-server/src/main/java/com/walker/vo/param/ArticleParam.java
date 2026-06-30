package com.walker.vo.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户发布文章实体类
 *
 */
@Data
@EqualsAndHashCode
@Accessors(chain = true)
@ApiModel(value = "Article对象",description = "")
public class ArticleParam {


    @ApiModelProperty(value = "文章的ID",required = true)
    private Integer articleId;

    @ApiModelProperty(value = "标签的ID",required = true)
    private Integer articleLabelId;

    @ApiModelProperty(value = "文章作者",required = true)
    private String articleAuthor;

    @ApiModelProperty(value = "文章标题",required = true)
    private String articleTitle;

    @ApiModelProperty(value = "文章摘要",required = true)
    private String articleSummary;

    @ApiModelProperty(value = "文章类型ID",required = true)
    private Integer articleTypeId;

    @ApiModelProperty(value = "文章内容",required = true)
    private String articleContent;

    @ApiModelProperty(value = "文章类型HTML",required = true)
    private String articleContentHtml;

    @ApiModelProperty(value = "文章封面图片",required = false)
    private String articleImage;

    @ApiModelProperty(value = "发布文章的用户ID",required = true)
    private Integer userId;

    @ApiModelProperty(value = "文章发布的社区的ID",required = true)
    private Integer articleCommunityId;

    @ApiModelProperty(value = "文章发布的时间",required = true)
    private String createTime;

    @ApiModelProperty(value = "文章附件Ids",required = true)
    private Integer [] files;
}
