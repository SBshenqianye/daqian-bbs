package com.walker.controller;

import com.alibaba.excel.EasyExcel;
import com.walker.config.audit.AuditLog;
import com.walker.service.OrgAutoImportService;
import com.walker.vo.ResultBean;
import com.walker.vo.excel.OrgImportRow;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

@Api(tags = "组织自动导入")
@RestController
public class OrgImportController {

    @Autowired private OrgAutoImportService orgAutoImportService;

    @ApiOperation("管理端：下载组织导入模板（base64 xlsx）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/org/import/template")
    public ResultBean template() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OrgImportRow sample = new OrgImportRow();
            sample.setOrgNo("5140499");
            sample.setOrgName("示例单位");
            sample.setPOrgNo("51404");
            sample.setStatus("1");
            sample.setExternalVersion("2026");
            EasyExcel.write(out, OrgImportRow.class).sheet("组织导入").doWrite(Arrays.asList(sample));
            return ResultBean.success("查询成功", Base64.getEncoder().encodeToString(out.toByteArray()));
        } catch (Exception e) {
            return ResultBean.error("生成模板失败：" + e.getMessage());
        }
    }

    @ApiOperation("管理端：上传并校验组织文件")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_IMPORT_VALIDATE", targetType = "ORG_IMPORT", targetId = "'UPLOAD'")
    @PostMapping("/admin/org/import/validate")
    public ResultBean validate(@RequestParam("file") MultipartFile file) {
        return orgAutoImportService.validate(file);
    }

    @ApiOperation("管理端：应用组织导入批次")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_IMPORT_APPLY", targetType = "ORG_IMPORT", targetId = "#body['batchId']")
    @PostMapping("/admin/org/import/apply")
    public ResultBean apply(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("batchId");
        boolean confirm = body != null && Boolean.TRUE.equals(body.get("confirm"));
        return orgAutoImportService.apply(id == null ? null : Integer.valueOf(String.valueOf(id)), confirm);
    }

    @ApiOperation("管理端：导入批次列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/org/import/batches")
    public ResultBean batches() { return orgAutoImportService.listBatches(); }

    @ApiOperation("管理端：批次明细")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/org/import/batch/{id}")
    public ResultBean detail(@PathVariable Integer id) { return orgAutoImportService.batchDetail(id); }

    @ApiOperation("管理端：回滚导入批次")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_IMPORT_ROLLBACK", targetType = "ORG_IMPORT", targetId = "#body['batchId']")
    @PostMapping("/admin/org/import/rollback")
    public ResultBean rollback(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("batchId");
        return orgAutoImportService.rollback(id == null ? null : Integer.valueOf(String.valueOf(id)));
    }
}
