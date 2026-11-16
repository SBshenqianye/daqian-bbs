package com.walker.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("bbs_org_import_item")
public class OrgImportItem implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("batch_id")
    private Integer batchId;
    private String action;
    @TableField("org_no")
    private String orgNo;
    @TableField("old_json")
    private String oldJson;
    @TableField("new_json")
    private String newJson;
    private String status;
    private String message;
}
