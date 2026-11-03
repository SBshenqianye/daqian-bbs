package com.walker.controller;

import com.walker.mapper.SaOrgMapper;
import com.walker.config.audit.AuditContext;
import com.walker.config.audit.AuditLog;
import com.walker.pojo.SaOrg;
import com.walker.service.SaOrgService;
import com.walker.vo.ResultBean;
import com.walker.vo.SaOrgTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author chengQing
 * @Date 2026/3/3 14:45
 * @PackageName:com.walker.controller
 * @ClassName: SaOrgController
 * @Description: 单位控制层
 */
@Api(tags = "SaOrgController")
@RestController
public class SaOrgController {

    @Autowired
    private SaOrgService saOrgService;

    @Autowired
    private SaOrgMapper saOrgMapper;
    /**
     * 查询单位树形结构
     */
    @ApiOperation("查询单位树形结构")
    @GetMapping("/common/saOrgTree")
    public ResultBean getSaOrgTree() {
        return ResultBean.success("成功获取！",saOrgService.getOrgTree());
    }

    /**
     * 方法描述 删除单位
     * @author chengQing
     * @date 2026/4/8 17:02
     * @param orgNo 单位编号
     * @return ResultBean 返回封装结果
     */
    @ApiOperation("删除单位")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_DELETE",
            targetType = "ORG", targetId = "#orgNo")
    @GetMapping("/saOrg/deleteSaOrgByOrgNo")
    public ResultBean deleteSaOrgByOrgNo(@RequestParam("orgNo") String orgNo) {
        return saOrgService.deleteSaOrgByOrgNo(orgNo);
    }

    /**
     * 方法描述 添加单位
     * @author chengQing
     * @date 2026/4/8 17:04
     * @param pOrgNo 父级单位
     * @param orgName 单位名称
     * @return ResultBean 返回封装结果
     */
    @ApiOperation("添加单位")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_CREATE",
            targetType = "ORG", snapshot = false)
    @GetMapping("/saOrg/addSaOrg")
    public ResultBean addSaOrg(@RequestParam("pOrgNo") String pOrgNo, @RequestParam("orgName") String orgName) {
        return saOrgService.addSaOrg(pOrgNo, orgName);
    }

    @ApiOperation("获取所有单位及排名选中状态")
    @GetMapping("/common/saOrg/rankingOrgs")
    public ResultBean getRankingOrgs() {
        List<SaOrg> allOrgs = saOrgMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getIsDelete, 0));
        return ResultBean.success("获取成功", allOrgs);
    }

    @ApiOperation("批量更新排名选中状态")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_RANKING_UPDATE",
            targetType = "ORG", targetId = "'ALL'", snapshot = false)
    @PostMapping("/common/saOrg/batchUpdateRanking")
    public ResultBean batchUpdateRanking(@RequestBody Map<String, Boolean> rankingMap) {
        if (rankingMap == null || rankingMap.isEmpty()) {
            return ResultBean.error("参数不能为空");
        }
        for (Map.Entry<String, Boolean> entry : rankingMap.entrySet()) {
            SaOrg org = new SaOrg();
            org.setIsRankingSelected(entry.getValue() ? 1 : 0);
            saOrgMapper.update(org,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SaOrg>()
                            .eq(SaOrg::getOrgNo, entry.getKey())
            );
        }
        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("scope", "all");
        after.put("affected", rankingMap.size());
        AuditContext.setAfter(after);
        return ResultBean.success("更新成功");
    }

    @ApiOperation("批量更新显示选中状态")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).CONFIG_WRITE)")
    @AuditLog(module = "CONFIG", action = "CONFIG_ORG_DISPLAY_UPDATE",
            targetType = "ORG", targetId = "'ALL'", snapshot = false)
    @PostMapping("/common/saOrg/batchUpdateDisplay")
    public ResultBean batchUpdateDisplay(@RequestBody Map<String, Boolean> displayMap) {
        if (displayMap == null || displayMap.isEmpty()) {
            return ResultBean.error("参数不能为空");
        }
        saOrgService.batchUpdateDisplay(displayMap);
        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("scope", "all");
        after.put("affected", displayMap.size());
        AuditContext.setAfter(after);
        return ResultBean.success("更新成功");
    }

}
