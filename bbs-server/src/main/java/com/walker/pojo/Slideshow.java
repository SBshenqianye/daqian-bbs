package com.walker.pojo;

import com.walker.annotation.NormalizeFilePath;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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
@Accessors(chain = true)
@TableName("bbs_slideshow")
@ApiModel(value = "Slideshow对象", description = "")
public class Slideshow implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("轮播图的id")
    @TableId(value = "slideshow_id", type = IdType.AUTO)
    private Integer slideshowId;

    @ApiModelProperty("轮播图的地址")
    @TableField("image_url")
    @NormalizeFilePath
    private String imageUrl;

    @ApiModelProperty("顺序（数字越大，越靠前）")
    @TableField("successive")
    private Integer successive;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;


}
