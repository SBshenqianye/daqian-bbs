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
 * 组织导入服务
 * 从Excel提取组织信息并写入 bbs_sa_org
 * 支持按名称匹配已有组织和自动生成 org_no
 */
@Service
public class OrgImportService {

    @Autowired
    private SaOrgMapper saOrgMapper;

    /** 根节点 org_no（内江市） */
    private static final String ROOT_ORG_NO = "51404";

    /**
     * 预览组织导入（不存库，仅返回匹配/创建结果）
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
                preview.setAction("created");
            }

            // 检测部门
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                if (existingOrg != null) {
                    SaOrg existingDept = findDeptByName(pair.deptName, existingOrg.getOrgNo());
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + (existingDept != null ? ",dept_matched" : ",dept_created"));
                } else {
                    // 单位不存在则部门也需创建
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + ",dept_created");
                }
            }

            previews.add(preview);
        }
        return previews;
    }

    /**
     * 执行组织导入：创建不存在的组织，返回 (orgName, deptName) → org_no 映射
     *
     * @param rows Excel行数据
     * @return OrgImportResult，包含 orgNoByPair 复合键映射
     */
    public OrgImportResult importOrgs(List<UserExcelRow> rows) {
        // 提取唯一 (orgName, deptName) 组合
        Set<OrgPair> uniquePairs = extractOrgPairs(rows);

        // 复合键 (orgName, deptName) → 最终分配的组织编号
        // 若有部门则用部门编号，若仅有单位则用单位编号
        Map<OrgPair, String> orgNoByPair = new HashMap<>();
        int createdCount = 0;

        for (OrgPair pair : uniquePairs) {
            // 1. 查找或创建单位（单位名称）
            SaOrg existingOrg = findOrgByName(pair.orgName);
            final String unitOrgNo;
            if (existingOrg != null) {
                unitOrgNo = existingOrg.getOrgNo();
            } else {
                String newUnitNo = generateOrgNo(ROOT_ORG_NO, 7);
                SaOrg newOrg = new SaOrg();
                newOrg.setId(getNextId());
                newOrg.setOrgNo(newUnitNo);
                newOrg.setOrgName(pair.orgName);
                newOrg.setPOrgNo(ROOT_ORG_NO);
                newOrg.setOrgTree(ROOT_ORG_NO + "|" + newUnitNo);
                newOrg.setIsDelete(0);
                saOrgMapper.insert(newOrg);
                unitOrgNo = newUnitNo;
                createdCount++;
            }

            // 2. 查找或创建部门，用复合键存储最终结果
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                SaOrg existingDept = findDeptByName(pair.deptName, unitOrgNo);
                if (existingDept != null) {
                    orgNoByPair.put(pair, existingDept.getOrgNo());
                } else {
                    String deptOrgNo = generateOrgNo(unitOrgNo, 9);
                    SaOrg newDept = new SaOrg();
                    newDept.setId(getNextId());
                    newDept.setOrgNo(deptOrgNo);
                    newDept.setOrgName(pair.deptName);
                    newDept.setPOrgNo(unitOrgNo);
                    newDept.setOrgTree(ROOT_ORG_NO + "|" + unitOrgNo + "|" + deptOrgNo);
                    newDept.setIsDelete(0);
                    saOrgMapper.insert(newDept);
                    orgNoByPair.put(pair, deptOrgNo);
                    createdCount++;
                }
            } else {
                // 无部门，直接使用单位编号
                orgNoByPair.put(pair, unitOrgNo);
            }
        }

        OrgImportResult result = new OrgImportResult();
        result.orgNoByPair = orgNoByPair;
        result.createdCount = createdCount;
        return result;
    }

    /**
     * 根据用户行数据查找最匹配的 org_no
     * 使用 (orgName, deptName) 复合键精确定位
     */
    public String findBestOrgNo(UserExcelRow row, Map<OrgPair, String> orgNoByPair) {
        OrgPair key = new OrgPair();
        key.orgName = row.getOrgName();
        key.deptName = row.getDeptName();
        String orgNo = orgNoByPair.get(key);
        return orgNo != null ? orgNo : ROOT_ORG_NO;
    }

    // ==================== 内部辅助方法 ====================

    /** 按单位名称查找 */
    private SaOrg findOrgByName(String orgName) {
        List<SaOrg> list = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgName, orgName)
                        .eq(SaOrg::getIsDelete, 0)
                        .last("LIMIT 1")
        );
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /** 按部门名称和父级编号查找 */
    private SaOrg findDeptByName(String deptName, String pOrgNo) {
        List<SaOrg> list = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgName, deptName)
                        .eq(SaOrg::getPOrgNo, pOrgNo)
                        .eq(SaOrg::getIsDelete, 0)
                        .last("LIMIT 1")
        );
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    /** 自动生成 org_no：查询同级最大编号后 +1 */
    private String generateOrgNo(String parentOrgNo, int targetLength) {
        List<SaOrg> siblings = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getPOrgNo, parentOrgNo)
                        .eq(SaOrg::getIsDelete, 0)
        );
        long maxNo = siblings.stream()
                .map(SaOrg::getOrgNo)
                .filter(no -> no.length() == targetLength)
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L);
        if (maxNo > 0L) return String.valueOf(maxNo + 1L);
        return parentOrgNo + "01";
    }

    /** 获取下一个可用的 id */
    private int getNextId() {
        List<SaOrg> all = saOrgMapper.selectList(null);
        return all.stream().mapToInt(SaOrg::getId).max().orElse(0) + 1;
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
        /** 复合键 (orgName, deptName) → 最终分配的组织编号 */
        public Map<OrgPair, String> orgNoByPair;
        public int createdCount;
    }
}
