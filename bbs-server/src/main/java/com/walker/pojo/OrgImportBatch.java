package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_org_import_batch")
public class OrgImportBatch implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("file_name")
    private String fileName;
    @TableField("file_hash")
    private String fileHash;
    private String status;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("add_count")
    private Integer addCount;
    @TableField("update_count")
    private Integer updateCount;
    @TableField("move_count")
    private Integer moveCount;
    @TableField("disable_count")
    private Integer disableCount;
    @TableField("error_count")
    private Integer errorCount;
    @TableField("error_json")
    private String errorJson;
    @TableField("safe_flag")
    private Integer safeFlag;
    @TableField("operator_id")
    private Integer operatorId;
    @TableField("start_time")
    private String startTime;
    @TableField("end_time")
    private String endTime;
}
