package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author chengQing
 * @Date 2026/3/6 17:19
 * @PackageName:com.walker.pojo
 * @ClassName: Dict
 * @Description: 数据字典
 */
@TableName("bbs_dict")
@ApiModel(value = "Dict对象", description = "数据字典")
@Data
public class Dict implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("字典id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("字典类型-键")
    private String dictType;

    @ApiModelProperty("值")
    private String dictValue;

    @ApiModelProperty("中文翻译")
    private String dictLabel;

    @ApiModelProperty("排序序号")
    private Integer dictSort;

    @ApiModelProperty("创建人")
    private String createBy;

    @ApiModelProperty("创建时间")
    private String createTime;

    @ApiModelProperty("更新人")
    private String updateBy;

    @ApiModelProperty("更新时间")
    private String updateTime;

    @ApiModelProperty("备注说明")
    private String remark;

    @ApiModelProperty("键(违规类型标识)")
    private String dictKey;
}
