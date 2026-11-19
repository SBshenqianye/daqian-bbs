package com.walker.controller;

import com.walker.config.audit.AuditLog;
import com.walker.config.security.Permission;
import com.walker.pojo.RankingCategory;
import com.walker.service.RankingCategoryService;
import com.walker.vo.ResultBean;
import com.walker.vo.param.RankingOrgRankParam;
import com.walker.vo.param.RankingSelectionParam;
import com.walker.vo.param.RankingSortItem;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排行榜分类与分类组织榜。
 */
@Api(tags = "排行榜分类")
@RestController
public class RankingCategoryController {

    @Autowired
    private RankingCategoryService rankingCategoryService;

    @ApiOperation("用户端：启用中的排行榜分类（含系统内置“内江供电公司本部”）")
    @GetMapping("/common/ranking/categories")
    public ResultBean publicCategories() {
        return rankingCategoryService.listPublicCategories();
    }

    @ApiOperation("用户端：按分类查询组织榜（人均排序，排除测试账号）")
    @PostMapping("/common/ranking/org")
    public ResultBean orgRank(@RequestBody RankingOrgRankParam param) {
        if (param == null) {
            param = new RankingOrgRankParam();
        }
        return rankingCategoryService.getOrgRank(param.getCategoryId(), param.getStartTime(), param.getEndTime());
    }

    @ApiOperation("管理端：全部分类（含停用、含成员）")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_READ)")
    @GetMapping("/admin/ranking/categories")
    public ResultBean adminCategories() {
        return rankingCategoryService.listAdminCategories();
    }

    @ApiOperation("管理端：新建/编辑分类")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_CATEGORY_SAVE",
            targetType = "RANKING_CATEGORY", targetId = "#category.id")
    @PostMapping("/admin/ranking/category/save")
    public ResultBean saveCategory(@RequestBody RankingCategory category) {
        return rankingCategoryService.saveCategory(category);
    }

    @ApiOperation("管理端：删除分类")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_CATEGORY_DELETE",
            targetType = "RANKING_CATEGORY", targetId = "#body['id']")
    @PostMapping("/admin/ranking/category/delete")
    public ResultBean deleteCategory(@RequestBody Map<String, Object> body) {
        Integer id = body == null ? null : toInt(body.get("id"));
        return rankingCategoryService.deleteCategory(id);
    }

    @ApiOperation("管理端：拖入组织")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_ORG_ADD",
            targetType = "RANKING_CATEGORY", targetId = "#body['categoryId']")
    @PostMapping("/admin/ranking/category/org/add")
    public ResultBean addOrg(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ResultBean.error("参数不能为空");
        }
        Integer categoryId = toInt(body.get("categoryId"));
        String orgNo = body.get("orgNo") == null ? null : String.valueOf(body.get("orgNo"));
        Boolean includeDescendants = body.get("includeDescendants") == null
                ? Boolean.TRUE : Boolean.valueOf(String.valueOf(body.get("includeDescendants")));
        return rankingCategoryService.addOrg(categoryId, orgNo, includeDescendants);
    }

    @ApiOperation("管理端：设置成员是否包含下级")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_ORG_UPDATE",
            targetType = "RANKING_CATEGORY", targetId = "#body['categoryId']")
    @PostMapping("/admin/ranking/category/org/update")
    public ResultBean updateOrg(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ResultBean.error("参数不能为空");
        }
        Integer categoryId = toInt(body.get("categoryId"));
        String orgNo = body.get("orgNo") == null ? null : String.valueOf(body.get("orgNo"));
        Boolean includeDescendants = body.get("includeDescendants") == null
                ? Boolean.TRUE : Boolean.valueOf(String.valueOf(body.get("includeDescendants")));
        return rankingCategoryService.updateOrgDescendants(categoryId, orgNo, includeDescendants);
    }

    @ApiOperation("管理端：移除成员")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_ORG_REMOVE",
            targetType = "RANKING_CATEGORY", targetId = "#body['categoryId']")
    @PostMapping("/admin/ranking/category/org/remove")
    public ResultBean removeOrg(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return ResultBean.error("参数不能为空");
        }
        Integer categoryId = toInt(body.get("categoryId"));
        String orgNo = body.get("orgNo") == null ? null : String.valueOf(body.get("orgNo"));
        return rankingCategoryService.removeOrg(categoryId, orgNo);
    }

    @ApiOperation("管理端：恢复内置分类默认配置")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_CATEGORY_RESET",
            targetType = "RANKING_CATEGORY", targetId = "#body['id']")
    @PostMapping("/admin/ranking/category/resetDefault")
    public ResultBean resetDefault(@RequestBody Map<String, Object> body) {
        Integer id = body == null ? null : toInt(body.get("id"));
        return rankingCategoryService.resetCategoryToDefault(id);
    }

    @ApiOperation("管理端：拖动排序保存")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_CATEGORY_SORT",
            targetType = "RANKING_CATEGORY", targetId = "'ALL'", snapshot = false)
    @PostMapping("/admin/ranking/category/sort")
    public ResultBean sortCategories(@RequestBody List<RankingSortItem> items) {
        return rankingCategoryService.updateSortOrders(items);
    }

    @ApiOperation("管理端：查询内置分类默认参与单位")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @GetMapping("/admin/ranking/category/defaultMembers")
    public ResultBean defaultMembers(@RequestParam("id") Integer id) {
        return rankingCategoryService.getDefaultMembers(id);
    }

    @ApiOperation("管理端：三态树保存成员")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_RANKING_ORG_SELECTION",
            targetType = "RANKING_CATEGORY", targetId = "#param.categoryId")
    @PostMapping("/admin/ranking/category/org/setSelection")
    public ResultBean setSelection(@RequestBody RankingSelectionParam param) {
        if (param == null) {
            return ResultBean.error("参数不能为空");
        }
        return rankingCategoryService.saveSelection(
                param.getCategoryId(), param.getCheckedKeys(), param.getHalfCheckedKeys());
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
