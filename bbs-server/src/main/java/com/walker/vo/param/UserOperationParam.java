package com.walker.vo.param;

import lombok.Data;

/**
 * @Author chengQing
 * @Date 2026/3/10 11:17
 * @PackageName:com.walker.vo.param
 * @ClassName: UserOperationParam
 * @Description: 用户操作参数实体类
 */
@Data
public class UserOperationParam {
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 用户ids,逗号隔开
     */
    private String userIds;
    /**
     * 兼容旧角色类型 01：普通用户，02：管理员，04：版主
     */
    private String roleType;

    /** 主权限组ID（优先于 roleType） */
    private Integer permissionGroupId;

}
