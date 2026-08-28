package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通知对象
 */
@Getter
@Setter
@TableName("bbs_notification")
@ApiModel(value = "Notification对象", description = "通知")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("通知id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty("被通知的用户id")
    @TableField("user_id")
    private Integer userId;

    @ApiModelProperty("触发通知的用户id")
    @TableField("from_user_id")
    private Integer fromUserId;

    @ApiModelProperty("通知类型(reply/comment/favorite)")
    @TableField("type")
    private String type;

    @ApiModelProperty("通知分类(interaction=互动消息,system=系统通知)，由 NotificationCategory 注册表按 type 归组")
    @TableField("category")
    private String category;

    @ApiModelProperty("通知标题")
    @TableField("title")
    private String title;

    @ApiModelProperty("关联类型(article/comment/reply)")
    @TableField("related_type")
    private String relatedType;

    @ApiModelProperty("关联ID")
    @TableField("related_id")
    private Integer relatedId;

    @ApiModelProperty("是否已读(0=未读,1=已读)")
    @TableField("is_read")
    private Integer isRead;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
