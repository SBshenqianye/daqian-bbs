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

            // 如果单位已匹配到 or 被创建，检测部门
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                String unitOrgNo = existingOrg != null ? existingOrg.getOrgNo() : "AUTO";
                SaOrg existingDept = findDeptByName(pair.deptName, unitOrgNo);
                if (existingDept != null || existingOrg == null) {
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + ",dept_matched");
                } else {
                    preview.setDeptName(pair.deptName);
                    preview.setAction(preview.getAction() + ",dept_created");
                }
            }

            previews.add(preview);
        }
        return previews;
    }

    /**
     * 执行组织导入：创建不存在的组织，返回 org_name → org_no 映射
     *
     * @param rows Excel行数据
     * @return Map<orgName, orgNo> 以及 Map<deptName, orgNo>
     */
    public OrgImportResult importOrgs(List<UserExcelRow> rows) {
        // 提取唯一 (orgName, deptName) 组合
        Set<OrgPair> uniquePairs = extractOrgPairs(rows);

        Map<String, String> orgNoMap = new HashMap<>();  // orgName → orgNo
        Map<String, String> deptNoMap = new HashMap<>(); // deptName → orgNo
        int createdCount = 0;

        // 先导入全部单位
        for (OrgPair pair : uniquePairs) {
            String unitOrgNo;

            // 1. 查找或创建单位（单位名称）
            SaOrg existingOrg = findOrgByName(pair.orgName);
            if (existingOrg != null) {
                unitOrgNo = existingOrg.getOrgNo();
            } else {
                // 创建新单位
                unitOrgNo = generateOrgNo(ROOT_ORG_NO, 7);
                SaOrg newOrg = new SaOrg();
                newOrg.setId(getNextId());
                newOrg.setOrgNo(unitOrgNo);
                newOrg.setOrgName(pair.orgName);
                newOrg.setPOrgNo(ROOT_ORG_NO);
                newOrg.setOrgTree(ROOT_ORG_NO + "|" + unitOrgNo);
                newOrg.setIsDelete(0);
                saOrgMapper.insert(newOrg);
                createdCount++;
            }
            orgNoMap.put(pair.orgName, unitOrgNo);

            // 2. 查找或创建部门（部门名称）
            if (pair.deptName != null && !pair.deptName.isEmpty()) {
                SaOrg existingDept = findDeptByName(pair.deptName, unitOrgNo);
                if (existingDept != null) {
                    deptNoMap.put(pair.deptName, existingDept.getOrgNo());
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
                    deptNoMap.put(pair.deptName, deptOrgNo);
                    createdCount++;
                }
            }
        }

        OrgImportResult result = new OrgImportResult();
        result.orgNoMap = orgNoMap;
        result.deptNoMap = deptNoMap;
        result.createdCount = createdCount;
        return result;
    }

    /**
     * 根据用户行数据查找最匹配的 org_no
     * 优先用部门名，其次用单位名
     */
    public String findBestOrgNo(UserExcelRow row, Map<String, String> orgNoMap, Map<String, String> deptNoMap) {
        // 优先匹配部门
        if (row.getDeptName() != null && deptNoMap.containsKey(row.getDeptName())) {
            return deptNoMap.get(row.getDeptName());
        }
        // 其次匹配单位
        if (row.getOrgName() != null && orgNoMap.containsKey(row.getOrgName())) {
            return orgNoMap.get(row.getOrgName());
        }
        return ROOT_ORG_NO; // 兜底
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

    /** (单位, 部门) 组合，用于去重 */
    private static class OrgPair {
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
        public Map<String, String> orgNoMap;
        public Map<String, String> deptNoMap;
        public int createdCount;
    }
}
