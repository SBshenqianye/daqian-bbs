package com.walker.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.walker.config.audit.AuditLog;
import com.walker.pojo.SensitiveWord;
import com.walker.service.SensitiveWordService;
import com.walker.vo.ResultBean;
import com.walker.vo.excel.SensitiveWordExcelRow;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
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
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
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
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_ADD",
            targetType = "SENSITIVE_WORD", targetId = "#keyword", snapshot = false)
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
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_DELETE",
            targetType = "SENSITIVE_WORD", targetId = "#id")
    @GetMapping("/sensitiveWord/delSensitiveWord")
    public ResultBean delSensitiveWord(@RequestParam("id") Integer id) {
        return sensitiveWordService.delSensitiveWord(id);
    }

    /**
     * 方法描述 开启/关闭某个敏感词的前台遮蔽
     * 关闭后该词仍参与命中统计与质量检测，只是不再被打码。
     * @param id 敏感词id
     * @param enabled 1=打码，0=不打码
     * @return ResultBean
     */
    @ApiOperation("修改敏感词是否参与脱敏遮蔽")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_MASK",
            targetType = "SENSITIVE_WORD", targetId = "#id")
    @PostMapping("/sensitiveWord/updateMaskEnabled")
    public ResultBean updateMaskEnabled(@RequestParam("id") Integer id,
                                        @RequestParam("enabled") Integer enabled) {
        return sensitiveWordService.updateMaskEnabled(id, enabled);
    }

    /**
     * 方法描述 用三档动作一次性设置分类与遮蔽（后台下拉用，避免两次请求产生中间态）
     * @param id 敏感词id
     * @param action mask=严禁词且打码，monitor=严禁词仅监控，warn=垃圾词警告放行
     * @return ResultBean
     */
    @ApiOperation("修改敏感词处置方式（打码/仅监控/警告放行）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_ACTION",
            targetType = "SENSITIVE_WORD", targetId = "#id")
    @PostMapping("/sensitiveWord/updateAction")
    public ResultBean updateAction(@RequestParam("id") Integer id,
                                   @RequestParam("action") String action) {
        return sensitiveWordService.updateAction(id, action);
    }

    /**
     * 方法描述 修改敏感词分类（严禁词 / 垃圾词）
     * 垃圾词走"警告放行"：内容照常显示、不计分、命中不计入审核阈值。
     * @param id 敏感词id
     * @param category sensitive=严禁词，spam=垃圾词
     * @return ResultBean
     */
    @ApiOperation("修改敏感词分类")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_CATEGORY",
            targetType = "SENSITIVE_WORD", targetId = "#id")
    @PostMapping("/sensitiveWord/updateCategory")
    public ResultBean updateCategory(@RequestParam("id") Integer id,
                                     @RequestParam("category") String category) {
        return sensitiveWordService.updateCategory(id, category);
    }

    /**
     * 方法描述 导出敏感词为 Excel 文件
     * @param response HTTP 响应
     */
    @ApiOperation("导出敏感词为 Excel")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
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
                .inMemory(true)
                .sheet("敏感词列表")
                .doWrite(excelRows);
    }

    /**
     * 方法描述 导入敏感词（从 Excel 文件）
     * @param file 上传的 Excel 文件
     * @return ResultBean 导入结果
     */
    @ApiOperation("从 Excel 导入敏感词")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SENSITIVE_IMPORT",
            targetType = "SENSITIVE_WORD", targetId = "'IMPORT'", snapshot = false)
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
