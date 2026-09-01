package com.walker.vo.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 敏感词 Excel 导入/导出行数据映射
 * 对应Excel列: 序号 | 敏感词
 */
@Data
public class SensitiveWordExcelRow {

    @ExcelProperty("序号")
    private Integer rowNum;

    @ExcelProperty("敏感词")
    private String keyword;
}
