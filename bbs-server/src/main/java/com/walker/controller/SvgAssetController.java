package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.pojo.SvgAsset;
import com.walker.service.SvgAssetService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * SVG 素材库（管理端绘制器）。
 */
@Api(tags = "SVG素材库")
@RestController
public class SvgAssetController {

    @Autowired
    private SvgAssetService svgAssetService;

    @ApiOperation("管理端：素材列表")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/svg-asset/list")
    public ResultBean list(@RequestParam(required = false) String category) {
        return svgAssetService.list(category);
    }

    @ApiOperation("管理端：保存素材")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SVG_ASSET_SAVE", targetType = "SVG_ASSET", targetId = "#asset.id")
    @PostMapping("/admin/svg-asset/save")
    public ResultBean save(@RequestBody SvgAsset asset) {
        return svgAssetService.save(asset);
    }

    @ApiOperation("管理端：批量补齐内置素材（按名称+分类幂等）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @PostMapping("/admin/svg-asset/ensure")
    public ResultBean ensure(@RequestBody java.util.List<SvgAsset> assets) {
        return svgAssetService.ensure(assets);
    }

    @ApiOperation("管理端：删除素材")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_SVG_ASSET_DELETE", targetType = "SVG_ASSET", targetId = "#body['id']")
    @PostMapping("/admin/svg-asset/delete")
    public ResultBean delete(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("id");
        Integer value = null;
        if (id != null) {
            try {
                value = Integer.valueOf(String.valueOf(id));
            } catch (Exception ignore) {
                value = null;
            }
        }
        return svgAssetService.delete(value);
    }
}
