package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @Author chengQing
 * @Date 2026/3/3 14:34
 * @PackageName:com.walker.pojo
 * @ClassName: SaOrg
 * @Description: 单位实体类
 */
@TableName("bbs_sa_org")
@ApiModel(value = "SaOrg对象", description = "")
@Data
public class SaOrg implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id")
    private Integer id;

    @ApiModelProperty("单位编号")
    @TableField("org_no")
    private String orgNo;

    @ApiModelProperty("单位名称")
    @TableField("org_name")
    private String orgName;

    @ApiModelProperty("父级单位编号")
    @TableField("p_org_no")
    private String pOrgNo;

    @ApiModelProperty("单位树")
    @TableField("org_tree")
    private String orgTree;

    @ApiModelProperty("是否删除")
    @TableField("is_delete")
    private Integer isDelete;

    @ApiModelProperty("是否参与排名:0=否,1=是")
    @TableField("is_ranking_selected")
    private Integer isRankingSelected;

    @ApiModelProperty("是否显示在用户前台:0=否,1=是")
    @TableField("is_display_selected")
    private Integer isDisplaySelected;
}
