package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 可复用 SVG 素材（勋章 / 等级图标等），由管理端绘制器保存。
 */
@Data
@TableName("bbs_svg_asset")
public class SvgAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("asset_name")
    private String assetName;

    @TableField("category")
    private String category;

    @TableField("svg")
    private String svg;

    @TableField("created_by")
    private String createdBy;

    @TableField("create_time")
    private String createTime;

    @TableField("update_time")
    private String updateTime;
}
