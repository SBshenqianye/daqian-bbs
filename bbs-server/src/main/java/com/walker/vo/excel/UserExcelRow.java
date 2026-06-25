package com.walker.vo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入行数据映射
 * 对应Excel列: 时间 | 人员编号 | 姓名 | 单位名称 | 部门名称 | 身份证号
 */
@Data
public class UserExcelRow {

    @ExcelProperty("时间")
    private String time;

    @ExcelProperty("人员编号")
    private String personnelId;

    @ExcelProperty("姓名")
    private String nickname;

    @ExcelProperty("单位名称")
    private String orgName;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("身份证号")
    private String idCard;
}
