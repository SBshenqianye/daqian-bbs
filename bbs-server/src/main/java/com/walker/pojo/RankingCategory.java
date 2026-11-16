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
 * 排行榜分类（自定义榜单视图，不改变组织架构）。
 */
@Data
@TableName("bbs_ranking_category")
@ApiModel(value = "RankingCategory对象", description = "排行榜分类")
public class RankingCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("分类编码")
    @TableField("category_code")
    private String categoryCode;

    @ApiModelProperty("分类名称")
    @TableField("category_name")
    private String categoryName;

    @ApiModelProperty("分类说明")
    @TableField("description")
    private String description;

    @ApiModelProperty("排序序号")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty("是否启用(0=否,1=是)")
    @TableField("enabled")
    private Integer enabled;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty("更新时间")
    @TableField("update_time")
    private String updateTime;
}
