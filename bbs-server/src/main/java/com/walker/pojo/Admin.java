package com.walker.pojo;

import com.walker.annotation.NormalizeFilePath;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author walker
 * @since 2023-02-23
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("bbs_admin")
@ApiModel(value = "Admin对象", description = "")
public class Admin implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("管理员id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("管理员名称")
    private String username;

    @ApiModelProperty("管理员密码")
    private String password;

    @ApiModelProperty("管理员头像")
    @NormalizeFilePath
    private String portrait;

    @ApiModelProperty("是否禁用")
    private Integer isAlive;

    @ApiModelProperty("逻辑删除")
    @TableLogic
    private Integer isDelete;

    @ApiModelProperty("注册时间")
    private String createTime;
}
