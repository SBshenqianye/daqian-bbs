package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * @since 2022/05/20 22:07
 */
@Getter
@Setter
@TableName("bbs_article_label")
@ApiModel(value = "ArticleLabel对象", description = "")
public class ArticleLabel implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("文章标签的id")
    @TableId(value = "label_id", type = IdType.AUTO)
    private Integer labelId;

    @ApiModelProperty("标签名称")
    @TableField("label_name")
    private String labelName;

    @ApiModelProperty("标签是否禁用")
    private Integer enabled;

    @ApiModelProperty("标签图标")
    @TableField("icon")
    private String icon;

    @ApiModelProperty("标签描述")
    @TableField("description")
    private String description;

}
