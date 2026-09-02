package com.walker.service;

import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.SaOrg;
import com.walker.service.impl.OrgImportService;
import com.walker.vo.excel.ImportPreviewVO;
import com.walker.vo.excel.UserExcelRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgImportServiceTest {

    @InjectMocks
    private OrgImportService orgImportService;

    @Mock
    private SaOrgMapper saOrgMapper;

    // ==================== previewOrgs ====================

    @Test
    @DisplayName("预览组织匹配 → 单位匹配成功")
    void previewOrgs_orgMatched() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName(null);

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        org.setOrgName("内江供电公司");
        when(saOrgMapper.selectList(any())).thenReturn(Arrays.asList(org));

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getAction().contains("matched"));
    }

    @Test
    @DisplayName("预览组织匹配 → 单位不存在 → unmatched")
    void previewOrgs_orgNotFound() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("不存在的单位");
        row.setDeptName(null);
        when(saOrgMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getAction().contains("unmatched"));
    }

    @Test
    @DisplayName("预览组织匹配 → 单位匹配+部门匹配")
    void previewOrgs_bothMatched() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName("运检部");

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        org.setOrgName("内江供电公司");
        SaOrg dept = new SaOrg();
        dept.setOrgNo("5140401");
        dept.setOrgName("运检部");
        dept.setPOrgNo("51404");
        // 第一次查单位，第二次查部门
        when(saOrgMapper.selectList(any()))
                .thenReturn(Arrays.asList(org))
                .thenReturn(Arrays.asList(dept));

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertTrue(result.get(0).getAction().contains("dept_matched"));
    }

    @Test
    @DisplayName("预览组织匹配 → 单位匹配+部门不匹配")
    void previewOrgs_deptNotFound() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName("不存在的部门");

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        when(saOrgMapper.selectList(any()))
                .thenReturn(Arrays.asList(org))
                .thenReturn(Collections.emptyList());

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertTrue(result.get(0).getAction().contains("dept_unmatched"));
    }

    @Test
    @DisplayName("预览组织匹配 → 国网前缀兼容")
    void previewOrgs_guowangPrefix() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("国网内江供电公司");
        row.setDeptName(null);

        // 精确匹配不到，去掉"国网"后匹配到
        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        org.setOrgName("内江供电公司");
        when(saOrgMapper.selectList(any()))
                .thenReturn(Collections.emptyList())  // 精确匹配
                .thenReturn(Arrays.asList(org));       // 去国网前缀匹配

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertTrue(result.get(0).getAction().contains("matched"));
    }

    @Test
    @DisplayName("预览组织匹配 → 无单位名称 → 不处理")
    void previewOrgs_nullOrgName() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName(null);
        row.setDeptName(null);

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("预览组织匹配 → 多行数据去重")
    void previewOrgs_deduplication() {
        UserExcelRow row1 = new UserExcelRow();
        row1.setOrgName("内江供电公司");
        row1.setDeptName("运检部");
        UserExcelRow row2 = new UserExcelRow();
        row2.setOrgName("内江供电公司");
        row2.setDeptName("运检部");

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        SaOrg dept = new SaOrg();
        dept.setOrgNo("5140401");
        when(saOrgMapper.selectList(any()))
                .thenReturn(Arrays.asList(org))
                .thenReturn(Arrays.asList(dept));

        List<ImportPreviewVO.OrgPreview> result = orgImportService.previewOrgs(Arrays.asList(row1, row2));

        // 去重后只有1条
        assertEquals(1, result.size());
    }

    // ==================== importOrgs ====================

    @Test
    @DisplayName("导入组织 → 匹配成功返回orgNo映射")
    void importOrgs_matchSucceeds() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName(null);

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        when(saOrgMapper.selectList(any())).thenReturn(Arrays.asList(org));

        OrgImportService.OrgImportResult result = orgImportService.importOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertEquals(0, result.getUnmatchedCount());
        assertFalse(result.orgNoByPair.isEmpty());
    }

    @Test
    @DisplayName("导入组织 → 匹配失败返回unmatched列表")
    void importOrgs_matchFailed() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("不存在的单位");
        row.setDeptName(null);
        when(saOrgMapper.selectList(any())).thenReturn(Collections.emptyList());

        OrgImportService.OrgImportResult result = orgImportService.importOrgs(Arrays.asList(row));

        assertNotNull(result);
        assertEquals(1, result.getUnmatchedCount());
        assertTrue(result.orgNoByPair.isEmpty());
    }

    @Test
    @DisplayName("导入组织 → 有部门且匹配成功")
    void importOrgs_deptMatched() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName("运检部");

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        SaOrg dept = new SaOrg();
        dept.setOrgNo("5140401");
        when(saOrgMapper.selectList(any()))
                .thenReturn(Arrays.asList(org))
                .thenReturn(Arrays.asList(dept));

        OrgImportService.OrgImportResult result = orgImportService.importOrgs(Arrays.asList(row));

        assertEquals(0, result.getUnmatchedCount());
    }

    @Test
    @DisplayName("导入组织 → 有部门但不匹配 → 回退到单位编号")
    void importOrgs_deptNotMatch_fallbackToOrg() throws Exception {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName("不存在的部门");

        SaOrg org = new SaOrg();
        org.setOrgNo("51404");
        when(saOrgMapper.selectList(any()))
                .thenReturn(Arrays.asList(org))
                .thenReturn(Collections.emptyList());

        OrgImportService.OrgImportResult result = orgImportService.importOrgs(Arrays.asList(row));

        assertEquals(0, result.getUnmatchedCount());
        // 回退到单位编号 — 用反射构造 key
        OrgImportService.OrgPair key = new OrgImportService.OrgPair();
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(key, "内江供电公司");
        java.lang.reflect.Field deptNameField = OrgImportService.OrgPair.class.getDeclaredField("deptName");
        deptNameField.setAccessible(true);
        deptNameField.set(key, "不存在的部门");
        assertEquals("51404", result.orgNoByPair.get(key));
    }

    // ==================== findBestOrgNo ====================

    @Test
    @DisplayName("查找最佳组织编号 → 匹配成功")
    void findBestOrgNo_found() throws Exception {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("内江供电公司");
        row.setDeptName(null);

        Map<OrgImportService.OrgPair, String> map = new HashMap<>();
        OrgImportService.OrgPair key = new OrgImportService.OrgPair();
        // OrgPair fields are package-private, use reflection
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(key, "内江供电公司");
        java.lang.reflect.Field deptNameField = OrgImportService.OrgPair.class.getDeclaredField("deptName");
        deptNameField.setAccessible(true);
        deptNameField.set(key, null);
        map.put(key, "51404");

        String result = orgImportService.findBestOrgNo(row, map);

        assertEquals("51404", result);
    }

    @Test
    @DisplayName("查找最佳组织编号 → 未匹配返回null")
    void findBestOrgNo_notFound() {
        UserExcelRow row = new UserExcelRow();
        row.setOrgName("不存在");
        row.setDeptName(null);

        String result = orgImportService.findBestOrgNo(row, new HashMap<>());

        assertNull(result);
    }

    // ==================== OrgPair equals/hashCode ====================

    @Test
    @DisplayName("OrgPair equals → 相同orgName和deptName → true")
    void orgPair_equals_sameValues() throws Exception {
        OrgImportService.OrgPair p1 = new OrgImportService.OrgPair();
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(p1, "内江");
        java.lang.reflect.Field deptNameField = OrgImportService.OrgPair.class.getDeclaredField("deptName");
        deptNameField.setAccessible(true);
        deptNameField.set(p1, "运检");

        OrgImportService.OrgPair p2 = new OrgImportService.OrgPair();
        orgNameField.set(p2, "内江");
        deptNameField.set(p2, "运检");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("OrgPair equals → 不同值 → false")
    void orgPair_equals_differentValues() throws Exception {
        OrgImportService.OrgPair p1 = new OrgImportService.OrgPair();
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(p1, "内江");
        java.lang.reflect.Field deptNameField = OrgImportService.OrgPair.class.getDeclaredField("deptName");
        deptNameField.setAccessible(true);
        deptNameField.set(p1, "运检");

        OrgImportService.OrgPair p2 = new OrgImportService.OrgPair();
        orgNameField.set(p2, "内江");
        deptNameField.set(p2, "营销");

        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("OrgPair equals → null → false")
    void orgPair_equals_null() throws Exception {
        OrgImportService.OrgPair p1 = new OrgImportService.OrgPair();
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(p1, "内江");

        assertNotEquals(null, p1);
    }

    @Test
    @DisplayName("OrgPair equals → 自身 → true")
    void orgPair_equals_self() throws Exception {
        OrgImportService.OrgPair p1 = new OrgImportService.OrgPair();
        java.lang.reflect.Field orgNameField = OrgImportService.OrgPair.class.getDeclaredField("orgName");
        orgNameField.setAccessible(true);
        orgNameField.set(p1, "内江");

        assertEquals(p1, p1);
    }

    // ==================== OrgImportResult ====================

    @Test
    @DisplayName("OrgImportResult getUnmatchedCount → null列表返回0")
    void orgImportResult_nullUnmatched() {
        OrgImportService.OrgImportResult result = new OrgImportService.OrgImportResult();
        result.unmatchedPairs = null;

        assertEquals(0, result.getUnmatchedCount());
    }

    @Test
    @DisplayName("OrgImportResult getUnmatchedCount → 返回实际数量")
    void orgImportResult_countUnmatched() {
        OrgImportService.OrgImportResult result = new OrgImportService.OrgImportResult();
        result.unmatchedPairs = new ArrayList<>();

        assertEquals(0, result.getUnmatchedCount());
    }
}
