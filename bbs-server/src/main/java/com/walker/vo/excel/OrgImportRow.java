package com.walker.vo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 组织导入 Excel 行（模板列：单位编号/单位名称/父级单位编号/状态/版本）。
 */
@Data
public class OrgImportRow {
    @ExcelProperty("单位编号")
    private String orgNo;
    @ExcelProperty("单位名称")
    private String orgName;
    @ExcelProperty("父级单位编号")
    private String pOrgNo;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("版本")
    private String externalVersion;
}
