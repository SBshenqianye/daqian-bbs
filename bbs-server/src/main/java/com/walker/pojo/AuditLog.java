package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 关键操作审计日志。
 *
 * <p>只追加、不更新；写入失败不得影响业务主流程。</p>
 */
@Getter
@Setter
@TableName("bbs_audit_log")
@ApiModel(value = "AuditLog对象", description = "关键操作审计日志")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("操作人用户ID；系统任务可为空")
    @TableField("operator_id")
    private Integer operatorId;

    @ApiModelProperty("操作人用户名")
    @TableField("operator_name")
    private String operatorName;

    @ApiModelProperty("业务模块")
    private String module;

    @ApiModelProperty("操作")
    private String action;

    @ApiModelProperty("目标类型")
    @TableField("target_type")
    private String targetType;

    @ApiModelProperty("目标ID；批量操作存 ALL 或逗号分隔")
    @TableField("target_id")
    private String targetId;

    @ApiModelProperty("变更前快照（已脱敏）")
    @TableField("before_json")
    private String beforeJson;

    @ApiModelProperty("变更后快照（已脱敏）")
    @TableField("after_json")
    private String afterJson;

    @ApiModelProperty("HTTP 方法")
    @TableField("request_method")
    private String requestMethod;

    @ApiModelProperty("请求路径")
    @TableField("request_path")
    private String requestPath;

    @ApiModelProperty("客户端 IP")
    private String ip;

    @ApiModelProperty("User-Agent")
    @TableField("user_agent")
    private String userAgent;

    @ApiModelProperty("结果：success/fail")
    private String result;

    @ApiModelProperty("失败原因")
    @TableField("error_msg")
    private String errorMsg;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private String createTime;
}
