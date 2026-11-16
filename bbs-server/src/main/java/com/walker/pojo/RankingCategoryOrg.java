package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 排行榜分类成员组织。
 */
@Data
@TableName("bbs_ranking_category_org")
@ApiModel(value = "RankingCategoryOrg对象", description = "排行榜分类成员组织")
public class RankingCategoryOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("分类ID")
    @TableField("category_id")
    private Integer categoryId;

    @ApiModelProperty("组织编号")
    @TableField("org_no")
    private String orgNo;

    @ApiModelProperty("是否包含下级(0=否,1=是)")
    @TableField("include_descendants")
    private Integer includeDescendants;

    @ApiModelProperty("排序序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
