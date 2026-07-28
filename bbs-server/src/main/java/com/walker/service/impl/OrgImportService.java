package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.SaOrg;
import com.walker.vo.excel.ImportPreviewVO;
import com.walker.vo.excel.UserExcelRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织匹配服务
 * 从Excel提取组织信息，在已有 bbs_sa_org 树中按名称匹配
 * 不创建新组织，匹配不到的记录在 unmatchedPairs 中
 */
@Service
public class OrgImportService {

    @Autowired
    private SaOrgMapper saOrgMapper;

    /** 根节点 org_no（内江市） */
    private static final String ROOT_ORG_NO = "51404";

    /**
     * 预览组织匹配结果（不存库，仅返回匹配/未匹配结果）
     */
    public List<ImportPreviewVO.OrgPreview> previewOrgs(List<UserExcelRow> rows) {
        // 提取唯一 (orgName, deptName) 组合
        Set<OrgPair> uniquePairs = extractOrgPairs(rows);

        List<ImportPreviewVO.OrgPreview> previews = new ArrayList<>();
        for (OrgPair pair : uniquePairs) {
            ImportPreviewVO.OrgPreview preview = new ImportPreviewVO.OrgPreview();

            // 检测单位是否已存在
            SaOrg existingOrg = findOrgByName(pair.orgName);
            if (existingOrg != null) {
                preview.setOrgName(pair.orgName);
                preview.setAction("matched");
            } else {
                preview.setOrgName(pair.orgName);
                preview.setAction("unmatched");
            }

            // 检测部门
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                if (existingOrg != null) {
                    SaOrg existingDept = findDeptByName(pair.deptName, existingOrg.getOrgNo());
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + (existingDept != null ? ",dept_matched" : ",dept_unmatched"));
                } else {
                    // 单位不存在则部门也无法匹配
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + ",dept_unmatched");
                }
            }

            previews.add(preview);
        }
        return previews;
    }

    /**
     * 匹配组织：仅从已有组织架构中查找，不创建新组织
     * 返回 (orgName, deptName) → org_no 映射，匹配不到的排除在外
     *
     * @param rows Excel行数据
     * @return OrgImportResult，包含 orgNoByPair 映射和 unmatchedPairs 列表
     */
    public OrgImportResult importOrgs(List<UserExcelRow> rows) {
        // 提取唯一 (orgName, deptName) 组合
        Set<OrgPair> uniquePairs = extractOrgPairs(rows);

        // 复合键 (orgName, deptName) → 最终分配的组织编号
        Map<OrgPair, String> orgNoByPair = new HashMap<>();
        List<OrgPair> unmatchedPairs = new ArrayList<>();

        for (OrgPair pair : uniquePairs) {
            // 1. 查找单位（只匹配，不创建）
            SaOrg existingOrg = findOrgByName(pair.orgName);
            if (existingOrg == null) {
                unmatchedPairs.add(pair);
                continue;
            }

            // 2. 若有部门，尝试在单位下查找；部门不匹配仍可回退到单位
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                SaOrg existingDept = findDeptByName(pair.deptName, existingOrg.getOrgNo());
                if (existingDept != null) {
                    orgNoByPair.put(pair, existingDept.getOrgNo());
                } else {
                    // 部门名称不在当前单位下 → 使用单位编号
                    orgNoByPair.put(pair, existingOrg.getOrgNo());
                }
            } else {
                // 无部门，直接使用单位编号
                orgNoByPair.put(pair, existingOrg.getOrgNo());
            }
        }

        OrgImportResult result = new OrgImportResult();
        result.orgNoByPair = orgNoByPair;
        result.unmatchedPairs = unmatchedPairs;
        return result;
    }

    /**
     * 根据用户行数据查找最匹配的 org_no
     * 使用 (orgName, deptName) 复合键精确定位
     * @return 匹配到的 org_no，匹配不到返回 null
     */
    public String findBestOrgNo(UserExcelRow row, Map<OrgPair, String> orgNoByPair) {
        OrgPair key = new OrgPair();
        key.orgName = row.getOrgName();
        key.deptName = row.getDeptName();
        return orgNoByPair.get(key);
    }

    // ==================== 内部辅助方法 ====================

    /** 按单位名称查找（精确匹配） */
    private SaOrg findOrgByName(String orgName) {
        if (orgName == null || orgName.trim().isEmpty()) return null;
        List<SaOrg> list = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgName, orgName.trim())
                        .eq(SaOrg::getIsDelete, 0)
                        .last("LIMIT 1")
        );
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /** 按部门名称和父级编号查找 */
    private SaOrg findDeptByName(String deptName, String pOrgNo) {
        if (deptName == null || deptName.trim().isEmpty()) return null;
        List<SaOrg> list = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgName, deptName.trim())
                        .eq(SaOrg::getPOrgNo, pOrgNo)
                        .eq(SaOrg::getIsDelete, 0)
                        .last("LIMIT 1")
        );
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /** 提取唯一 (单位名称, 部门名称) 组合 */
    private Set<OrgPair> extractOrgPairs(List<UserExcelRow> rows) {
        return rows.stream()
                .map(row -> {
                    OrgPair pair = new OrgPair();
                    pair.orgName = row.getOrgName();
                    pair.deptName = row.getDeptName();
                    return pair;
                })
                .collect(Collectors.toSet());
    }

    /** (单位, 部门) 组合，用于去重和复合键查找 */
    public static class OrgPair {
        String orgName;
        String deptName;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrgPair orgPair = (OrgPair) o;
            return Objects.equals(orgName, orgPair.orgName) &&
                    Objects.equals(deptName, orgPair.deptName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orgName, deptName);
        }
    }

    /** 组织导入结果 */
    public static class OrgImportResult {
        /** 复合键 (orgName, deptName) → 最终分配的组织编号（仅包含匹配成功的） */
        public Map<OrgPair, String> orgNoByPair;
        /** 匹配不到的 (orgName, deptName) 列表 */
        public List<OrgPair> unmatchedPairs;
        /** 未匹配数量 */
        public int getUnmatchedCount() {
            return unmatchedPairs != null ? unmatchedPairs.size() : 0;
        }
    }
}
