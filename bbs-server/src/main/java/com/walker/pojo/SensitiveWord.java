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
 * @Author chengQing
 * @Date 2026/4/8 17:49
 * @PackageName:com.walker.pojo
 * @ClassName: SensitiveWord
 * @Description: TODO
 */
@TableName("bbs_sensitive_word")
@ApiModel(value = "SensitiveWord对象", description = "")
@Data
public class SensitiveWord implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("敏感词id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("敏感词")
    @TableField("keyword")
    private String keyword;

    /**
     * 是否参与脱敏遮蔽：1=前台打码（默认），0=保留监控但不打码。
     * 灌水/误伤词设为 0 后，正文照常显示，仍参与命中统计与质量检测。
     */
    @ApiModelProperty("是否参与脱敏遮蔽(1=是,0=否)")
    @TableField("mask_enabled")
    private Integer maskEnabled;

    /**
     * 词表分类：sensitive=严禁词（打码/审核），spam=垃圾灌水词（警告放行）。
     * 两类共用一张表，匹配逻辑相同，差别只在命中后的处置。
     */
    @ApiModelProperty("分类(sensitive=严禁词,spam=垃圾词)")
    @TableField("category")
    private String category;
}
