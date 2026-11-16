package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_operation_cycle")
public class OperationCycle implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("cycle_code")
    private String cycleCode;
    @TableField("cycle_name")
    private String cycleName;
    @TableField("start_date")
    private String startDate;
    @TableField("end_date")
    private String endDate;
    private Integer status;
    @TableField("create_time")
    private String createTime;
    @TableField("update_time")
    private String updateTime;
}
