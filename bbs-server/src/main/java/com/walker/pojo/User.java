package com.walker.pojo;

import com.walker.annotation.NormalizeFilePath;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 *
 * </p>
 *
 * @author walker
 * @since 2022/04/17 12:51
 */
@Getter
@Setter
@ToString
@TableName("bbs_user")
@Accessors(chain = true)
@ApiModel(value = "User对象", description = "")
public class User implements Serializable,UserDetails {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("用户登录名")
    private String username;

    @ApiModelProperty("用户密码")
    private String password;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("用户头像")
    @NormalizeFilePath
    private String portrait;

    @ApiModelProperty("用户性别")
    private String gender;

    @ApiModelProperty("用户介绍")
    private String introduce;

    @ApiModelProperty("用户城市")
    private String city;

    @ApiModelProperty("粉丝数量")
    private Integer fans;

    @ApiModelProperty("关注数量")
    private Integer attention;

    @ApiModelProperty("获赞数量")
    private Integer good;

    @ApiModelProperty("是否被禁")
    @TableField("is_alive")
    private Integer isAlive;

    @ApiModelProperty("逻辑删除")
    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @ApiModelProperty("添加时间")
    @TableField("create_time")
    private String createTime;

    @ApiModelProperty("电话")
    private String phone;

    @ApiModelProperty("单位编号")
    @TableField("org_no")
    private String orgNo;

    @ApiModelProperty("用户类型")
    @TableField("user_type")
    private String userType;

    @ApiModelProperty("人员编号")
    @TableField("personnel_id")
    private String personnelId;

    @ApiModelProperty("身份证号")
    @TableField("id_card")
    private String idCard;

    @ApiModelProperty("是否首次登录(1=需改密码,0=已修改)")
    @TableField("is_first_login")
    private Integer isFirstLogin;

    @ApiModelProperty("单位名称（非数据库字段，查询时动态填充）")
    @TableField(exist = false)
    private String orgName;

    @ApiModelProperty("部门名称（非数据库字段，查询时动态填充）")
    @TableField(exist = false)
    private String deptName;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
