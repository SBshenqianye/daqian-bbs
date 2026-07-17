package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 系统配置表
 */
@Getter
@Setter
@TableName("bbs_system_config")
@Accessors(chain = true)
@ApiModel(value = "SystemConfig对象", description = "系统配置表")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("唯一标识")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("配置键")
    private String configKey;

    @ApiModelProperty("配置值")
    private String configValue;

    @ApiModelProperty("配置名称/说明")
    private String configLabel;

    @ApiModelProperty("配置分组")
    private String configGroup;

    @ApiModelProperty("输入类型（text/textarea/json）")
    private String configType;

    @ApiModelProperty("排序序号")
    private Integer sortOrder;

    @ApiModelProperty("备注说明")
    private String remark;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("修改人")
    private String updateBy;

    @ApiModelProperty("修改时间")
    private String updateTime;
}
