package com.walker.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.walker.pojo.SensitiveWord;
import com.walker.service.SensitiveWordService;
import com.walker.vo.ResultBean;
import com.walker.vo.excel.SensitiveWordExcelRow;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/4/8 17:51
 * @PackageName:com.walker.controller
 * @ClassName: SensitiveWordController
 * @Description: 敏感词控制层
 */
@Api(tags = "SensitiveWordController")
@RestController
public class SensitiveWordController {
    @Autowired
    private SensitiveWordService sensitiveWordService;

    /**
     * 方法描述 查询敏感词列表（全量，兼容旧接口）
     * @author chengQing
     * @date 2026/4/8 18:02
     * @return ResultBean
     */
    @GetMapping("/sensitiveWord/getList")
    public ResultBean getList() {
        List<SensitiveWord> list = sensitiveWordService.getList();
        return ResultBean.success("成功获取！", list);
    }

    /**
     * 方法描述 分页查询敏感词列表
     * @param page 页码（从1开始，默认1）
     * @param size 每页条数（默认20）
     * @param keyword 搜索关键词（可选）
     * @return ResultBean 包含分页数据
     */
    @ApiOperation("分页查询敏感词列表")
    @GetMapping("/sensitiveWord/getPage")
    public ResultBean getPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return sensitiveWordService.getPage(page, size, keyword);
    }

    /**
     * 方法描述 添加敏感词
     * @author chengQing
     * @date 2026/4/9 10:45
     * @param keyword 敏感词
     * @return ResultBean
     */
    @GetMapping("/sensitiveWord/addSensitiveWord")
    public ResultBean addSensitiveWord(@RequestParam("keyword") String keyword) {
        return sensitiveWordService.addSensitiveWord(keyword);
    }

    /**
     * 方法描述 删除敏感词
     * @author chengQing
     * @date 2026/4/9 10:53
     * @param id 敏感词id
     * @return ResultBean
     */
    @GetMapping("/sensitiveWord/delSensitiveWord")
    public ResultBean delSensitiveWord(@RequestParam("id") Integer id) {
        return sensitiveWordService.delSensitiveWord(id);
    }

    /**
     * 方法描述 导出敏感词为 Excel 文件
     * @param response HTTP 响应
     */
    @ApiOperation("导出敏感词为 Excel")
    @GetMapping("/sensitiveWord/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        // 查询所有敏感词
        List<SensitiveWord> list = sensitiveWordService.getList();

        // 构建 Excel 数据
        List<SensitiveWordExcelRow> excelRows = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            SensitiveWordExcelRow row = new SensitiveWordExcelRow();
            row.setRowNum(i + 1);
            row.setKeyword(list.get(i).getKeyword());
            excelRows.add(row);
        }

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("敏感词列表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 写入 Excel
        EasyExcel.write(response.getOutputStream(), SensitiveWordExcelRow.class)
                .sheet("敏感词列表")
                .doWrite(excelRows);
    }

    /**
     * 方法描述 导入敏感词（从 Excel 文件）
     * @param file 上传的 Excel 文件
     * @return ResultBean 导入结果
     */
    @ApiOperation("从 Excel 导入敏感词")
    @PostMapping("/sensitiveWord/importExcel")
    public ResultBean importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResultBean.error("请选择要导入的文件！");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            return ResultBean.error("只支持 .xlsx 或 .xls 格式的 Excel 文件！");
        }

        try {
            List<String> keywords = new ArrayList<>();

            // 使用 EasyExcel 读取，每500行批量处理
            EasyExcel.read(file.getInputStream(), SensitiveWordExcelRow.class,
                    new PageReadListener<SensitiveWordExcelRow>(rows -> {
                        for (SensitiveWordExcelRow row : rows) {
                            if (row.getKeyword() != null && !row.getKeyword().trim().isEmpty()) {
                                keywords.add(row.getKeyword().trim());
                            }
                        }
                    })).sheet().doRead();

            if (keywords.isEmpty()) {
                return ResultBean.error("Excel 文件中没有读取到有效的敏感词！");
            }

            // 批量导入
            return sensitiveWordService.batchAdd(keywords);

        } catch (Exception e) {
            return ResultBean.error("导入失败：" + e.getMessage());
        }
    }
}
