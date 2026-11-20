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
 * 敏感词命中记录：一条内容一行（编辑不重复计数）。
 * 只用于风险画像与命中统计，不做处置。
 */
@TableName("bbs_sensitive_hit")
@ApiModel(value = "SensitiveHit对象", description = "敏感词命中记录")
@Data
public class SensitiveHit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("作者用户id")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("内容类型(article/comment/reply)")
    @TableField("target_type")
    private String targetType;

    @ApiModelProperty("内容id")
    @TableField("target_id")
    private Integer targetId;

    @ApiModelProperty("所属板块id")
    @TableField("label_id")
    private Integer labelId;

    @ApiModelProperty("命中词（逗号拼接，最多10个）")
    @TableField("hit_words")
    private String hitWords;

    @ApiModelProperty("命中词次")
    @TableField("hit_count")
    private Integer hitCount;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
