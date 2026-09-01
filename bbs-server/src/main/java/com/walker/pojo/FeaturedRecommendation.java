package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 精华帖推荐审批表
 */
@TableName("bbs_featured_recommendation")
@ApiModel(value = "FeaturedRecommendation对象", description = "精华帖推荐审批")
public class FeaturedRecommendation {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键ID")
    private Integer id;

    @ApiModelProperty("推荐文章ID")
    @TableField("article_id")
    private Integer articleId;

    @ApiModelProperty("推荐人(版主)ID")
    @TableField("recommender_id")
    private Integer recommenderId;

    @ApiModelProperty("推荐人管理的标签ID")
    @TableField("label_id")
    private Integer labelId;

    @ApiModelProperty("审批状态(pending/approved/rejected)")
    @TableField("status")
    private String status;

    @ApiModelProperty("审核人(总运营)ID")
    @TableField("reviewer_id")
    private Integer reviewerId;

    @ApiModelProperty("审核备注")
    @TableField("review_remark")
    private String reviewRemark;

    @ApiModelProperty("审核时间")
    @TableField("review_time")
    private String reviewTime;

    @ApiModelProperty("推荐时间")
    @TableField("create_time")
    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getArticleId() { return articleId; }
    public void setArticleId(Integer articleId) { this.articleId = articleId; }
    public Integer getRecommenderId() { return recommenderId; }
    public void setRecommenderId(Integer recommenderId) { this.recommenderId = recommenderId; }
    public Integer getLabelId() { return labelId; }
    public void setLabelId(Integer labelId) { this.labelId = labelId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getReviewerId() { return reviewerId; }
    public void setReviewerId(Integer reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
    public String getReviewTime() { return reviewTime; }
    public void setReviewTime(String reviewTime) { this.reviewTime = reviewTime; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
