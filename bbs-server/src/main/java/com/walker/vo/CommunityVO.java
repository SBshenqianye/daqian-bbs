package com.walker.vo;

import com.walker.annotation.NormalizeFilePath;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@ApiModel(value = "Community对象",description = "")
public class CommunityVO {

    @ApiModelProperty("社区id")
    private Integer communityId;

    @ApiModelProperty("社区名称")
    private String communityName;

    @ApiModelProperty("社区介绍")
    private String communityIntroduce;

    @ApiModelProperty("社区的照片")
    @NormalizeFilePath
    private String communityImage;

    @ApiModelProperty("社区创建者的昵称")
    private String createUserNickname;
}
