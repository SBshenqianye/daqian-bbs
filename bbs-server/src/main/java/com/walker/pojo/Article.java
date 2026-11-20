package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.walker.utils.SensitiveWordUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author walker
 * @since 2022/05/20 14:26
 */
@Getter
@Setter
@TableName("bbs_article")
@Accessors(chain = true)
@ApiModel(value = "Article对象", description = "")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("文章id")
    @TableId(value = "article_id", type = IdType.AUTO)
    private Integer articleId;

    @ApiModelProperty("标签id")
    @TableField("article_label_id")
    private Integer articleLabelId;

    @ApiModelProperty("文章作者")
    @TableField("article_author")
    private String articleAuthor;

    @ApiModelProperty("文章标题")
    @TableField("article_title")
    private String articleTitle;

    @ApiModelProperty("文章摘要")
    @TableField("article_summary")
    private String articleSummary;

    @ApiModelProperty("文章类型")
    @TableField("article_type_id")
    private Integer articleTypeId;

    @ApiModelProperty("文章内容")
    @TableField("article_content")
    private String articleContent;

    @ApiModelProperty("文章内容html")
    @TableField("article_content_html")
    private String articleContentHtml;

    @ApiModelProperty("文章中的图片")
    @TableField("article_image")
    private String articleImage;

    @ApiModelProperty("发布文章的用户的id")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("文章获赞数量")
    @TableField("article_good_num")
    private Integer articleGoodNum;

    @ApiModelProperty("文章的浏览量")
    @TableField("article_view_num")
    private Integer articleViewNum;

    @ApiModelProperty("文章发布的时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty("是否推荐该文章")
    @TableField("recommend")
    private String recommend;

    @ApiModelProperty("文章是否被审核通过")
    @TableField("enable")
    private Integer enable;

    @ApiModelProperty("逻辑删除")
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @ApiModelProperty("是否为精华帖(0=否,1=是)")
    @TableField("is_featured")
    private Integer isFeatured;

    @ApiModelProperty("热度奖励是否已发放(0=否,1=是)")
    @TableField("is_hot_bonus")
    private Integer isHotBonus;

    @ApiModelProperty("最后回复时间")
    @TableField("last_reply_time")
    private String lastReplyTime;

    @ApiModelProperty("最后活跃时间")
    @TableField("last_activity_time")
    private String lastActivityTime;

    @ApiModelProperty("评论+回复数（冗余）")
    @TableField("comment_num")
    private Integer commentNumCount;

    @ApiModelProperty("收藏数（冗余）")
    @TableField("favorite_num")
    private Integer favoriteNum;

    @ApiModelProperty("分享数（冗余）")
    @TableField("share_num")
    private Integer shareNum;

    @ApiModelProperty("是否匿名发布(0=否,1=是)")
    @TableField("is_anonymous")
    private Integer isAnonymous;

    @ApiModelProperty("设为精华时间")
    @TableField("featured_time")
    private String featuredTime;

    @ApiModelProperty("标签名称（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String articleLabelName;

    @ApiModelProperty("标签用途类型（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String articleLabelType;

    @ApiModelProperty("标签是否允许匿名（关联查询，非数据库字段）")
    @TableField(exist = false)
    private Integer labelAllowAnonymous;

    @ApiModelProperty("标签是否不计分（关联查询，非数据库字段）")
    @TableField(exist = false)
    private Integer labelNoPoints;

    @ApiModelProperty("作者等级序号（关联查询，非数据库字段）")
    @TableField(exist = false)
    private Integer authorLevelIndex;

    @ApiModelProperty("作者等级自定义图标 SVG（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String authorIconSvg;

    @ApiModelProperty("用户头像")
    @TableField(exist = false)
    private String portrait;

    @ApiModelProperty("评论数量")
    @TableField(exist = false)
    private Integer commentNum;

    @ApiModelProperty("作者单位名称（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String authorOrgName;

    @ApiModelProperty("作者完整单位名称（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String authorOrgNameFull;

    @ApiModelProperty("作者部门名称（关联查询，非数据库字段）")
    @TableField(exist = false)
    private String authorDeptName;

    @ApiModelProperty("建议反馈是否已被采纳（关联查询，非数据库字段）")
    @TableField(exist = false)
    private Boolean isSuggestionAdopted;

    @ApiModelProperty("当前登录用户是否为作者（非数据库字段；匿名帖前端据此判断能否举报）")
    @TableField(exist = false)
    private Boolean selfAuthored;
}
