package com.walker.pojo;

import com.walker.annotation.NormalizeFilePath;
import com.baomidou.mybatisplus.annotation.*;
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
@TableName("bbs_community")
@Accessors(chain = true)
@ApiModel(value = "Community对象", description = "")
public class Community implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("社区id")
    @TableId(value = "community_id", type = IdType.AUTO)
    private Integer communityId;

    @ApiModelProperty("社区名称")
    @TableField("community_name")
    private String communityName;

    @ApiModelProperty("社区介绍")
    @TableField("community_introduce")
    private String communityIntroduce;

    @ApiModelProperty("社区的照片")
    @TableField("community_image")
    @NormalizeFilePath
    private String communityImage;

    @ApiModelProperty("社区用户数量")
    @TableField("community_user_num")
    private Integer communityUserNum;

    @ApiModelProperty("创建改社区的用户id")
    @TableField("create_user_id")
    private Integer createUserId;

    @ApiModelProperty("社区的创建时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty("社区是否被审核通过")
    @TableField("enable")
    private Integer enable;

    @ApiModelProperty("逻辑删除")
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;


}
