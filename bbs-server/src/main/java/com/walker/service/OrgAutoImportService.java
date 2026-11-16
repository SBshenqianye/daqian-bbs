package com.walker.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.mapper.OrgImportBatchMapper;
import com.walker.mapper.OrgImportItemMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.OrgImportBatch;
import com.walker.pojo.OrgImportItem;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.vo.ResultBean;
import com.walker.vo.excel.OrgImportRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 组织自动导入：校验、差异、应用、回滚。
 *
 * <p>安全规则：仅新增/改名可自动应用；出现移动、停用、影响用户等不安全变更时整批停止，
 * 留下批次等待管理员确认后手动应用。</p>
 */
@Service
public class OrgAutoImportService {

    private static final Logger log = LoggerFactory.getLogger(OrgAutoImportService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private OrgImportBatchMapper batchMapper;
    @Autowired private OrgImportItemMapper itemMapper;
    @Autowired private SaOrgMapper orgMapper;
    @Autowired private UserMapper userMapper;

    @Value("${bbs.org-import.dir:}")
    private String importDir;

    public ResultBean validate(MultipartFile file) {
        if (file == null || file.isEmpty()) return ResultBean.error("请选择文件");
        try {
            byte[] bytes = file.getBytes();
            return validateContent(new java.io.ByteArrayInputStream(bytes), file.getOriginalFilename());
        } catch (Exception e) {
            return ResultBean.error("解析失败：" + e.getMessage());
        }
    }

    private ResultBean validateContent(InputStream is, String fileName) throws Exception {
        List<OrgImportRow> rows = EasyExcel.read(is).head(OrgImportRow.class).sheet().doReadSync();
        if (rows == null || rows.isEmpty()) return ResultBean.error("文件没有数据行");

        List<SaOrg> all = orgMapper.selectList(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getIsDelete, 0));
        Map<String, SaOrg> dbMap = new HashMap<>();
        for (SaOrg org : all) dbMap.put(org.getOrgNo(), org);

        List<String> errors = new ArrayList<>();
        Map<String, OrgImportRow> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            OrgImportRow row = rows.get(i);
            int line = i + 2;
            row.setOrgNo(trim(row.getOrgNo()));
            row.setOrgName(trim(row.getOrgName()));
            row.setPOrgNo(trim(row.getPOrgNo()));
            if (row.getOrgNo() == null || row.getOrgNo().isEmpty()) { errors.add("第" + line + "行：单位编号为空"); continue; }
            if (row.getOrgName() == null || row.getOrgName().isEmpty()) { errors.add("第" + line + "行：单位名称为空"); continue; }
            if (rowMap.containsKey(row.getOrgNo())) { errors.add("第" + line + "行：单位编号重复 " + row.getOrgNo()); continue; }
            rowMap.put(row.getOrgNo(), row);
        }
        // 父级与环校验
        for (OrgImportRow row : rowMap.values()) {
            String p = row.getPOrgNo();
            if (p != null && !p.isEmpty() && !dbMap.containsKey(p) && !rowMap.containsKey(p) && !"51101".equals(p)) {
                errors.add("单位 " + row.getOrgNo() + " 的父级 " + p + " 不存在");
            }
            Set<String> seen = new HashSet<>();
            String cur = row.getOrgNo();
            while (cur != null && rowMap.containsKey(cur)) {
                if (!seen.add(cur)) { errors.add("单位 " + row.getOrgNo() + " 存在父子环"); break; }
                cur = rowMap.get(cur).getPOrgNo();
            }
        }
        if (!errors.isEmpty()) {
            OrgImportBatch batch = newBatch(fileName, rows.size(), 0, 0, 0, 0, errors.size(), errors, 1);
            return ResultBean.success("校验未通过，已生成错误批次 #" + batch.getId(), batch);
        }

        List<OrgImportItem> items = new ArrayList<>();
        int add = 0, update = 0, move = 0, disable = 0;
        for (OrgImportRow row : rowMap.values()) {
            SaOrg old = dbMap.get(row.getOrgNo());
            OrgImportItem item = new OrgImportItem();
            item.setOrgNo(row.getOrgNo());
            if (old == null) {
                item.setAction("ADD"); add++;
            } else {
                String oldJson = toJson(old);
                item.setOldJson(oldJson);
                if ("0".equals(row.getStatus()) || "false".equalsIgnoreCase(row.getStatus())) {
                    item.setAction("DISABLE"); disable++;
                } else if (row.getPOrgNo() != null && !row.getPOrgNo().isEmpty()
                        && !row.getPOrgNo().equals(old.getPOrgNo())) {
                    item.setAction("MOVE"); move++;
                } else if (row.getOrgName() != null && !row.getOrgName().equals(old.getOrgName())) {
                    item.setAction("UPDATE"); update++;
                } else {
                    item.setAction("NOOP");
                }
            }
            item.setNewJson(toJson(row));
            item.setStatus("PENDING");
            items.add(item);
        }
        int safeFlag = (move + disable) == 0 ? 1 : 0;
        OrgImportBatch batch = newBatch(fileName, rows.size(), add, update, move, disable, 0, null, safeFlag);
        for (OrgImportItem item : items) {
            item.setBatchId(batch.getId());
            itemMapper.insert(item);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batch", batch);
        data.put("items", items);
        data.put("safe", safeFlag == 1);
        return ResultBean.success(safeFlag == 1 ? "校验通过，可自动应用" : "存在不安全变更，已停止自动应用", data);
    }

    public ResultBean apply(Integer batchId, boolean confirm) {
        OrgImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) return ResultBean.error("批次不存在");
        if (!"PENDING".equals(batch.getStatus()) && !"INVALID".equals(batch.getStatus())) {
            return ResultBean.error("批次已处理");
        }
        if (Integer.valueOf(0).equals(batch.getSafeFlag()) && !confirm) {
            return ResultBean.error("存在移动/停用等不安全变更，已停止；请确认后手动应用");
        }
        List<OrgImportItem> items = itemMapper.selectList(new LambdaQueryWrapper<OrgImportItem>()
                .eq(OrgImportItem::getBatchId, batchId).eq(OrgImportItem::getStatus, "PENDING"));
        Date now = new Date();
        String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
        try {
            for (OrgImportItem item : items) {
                if ("NOOP".equals(item.getAction())) { item.setStatus("SKIPPED"); itemMapper.updateById(item); continue; }
                OrgImportRow row = objectMapper.readValue(item.getNewJson(), OrgImportRow.class);
                SaOrg old = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>()
                        .eq(SaOrg::getOrgNo, item.getOrgNo()).last("LIMIT 1"));
                if ("ADD".equals(item.getAction())) {
                    SaOrg parent = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>()
                            .eq(SaOrg::getOrgNo, row.getPOrgNo()).eq(SaOrg::getIsDelete, 0).last("LIMIT 1"));
                    if (parent == null && !"51101".equals(row.getPOrgNo())) throw new IllegalStateException("父级不存在: " + row.getPOrgNo());
                    Integer maxId = orgMapper.selectList(null).stream().map(SaOrg::getId)
                            .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
                    SaOrg org = new SaOrg();
                    org.setId(maxId + 1);
                    org.setOrgNo(row.getOrgNo());
                    org.setOrgName(row.getOrgName());
                    org.setPOrgNo(row.getPOrgNo());
                    org.setOrgTree((parent != null ? parent.getOrgTree() : "51101") + "|" + row.getOrgNo());
                    org.setIsDelete(0);
                    org.setIsRankingSelected(0);
                    org.setIsDisplaySelected(1);
                    orgMapper.insert(org);
                } else if ("UPDATE".equals(item.getAction()) && old != null) {
                    old.setOrgName(row.getOrgName());
                    orgMapper.updateById(old);
                } else if ("MOVE".equals(item.getAction()) && old != null) {
                    SaOrg parent = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>()
                            .eq(SaOrg::getOrgNo, row.getPOrgNo()).eq(SaOrg::getIsDelete, 0).last("LIMIT 1"));
                    if (parent == null) throw new IllegalStateException("父级不存在: " + row.getPOrgNo());
                    old.setPOrgNo(row.getPOrgNo());
                    old.setOrgTree(parent.getOrgTree() + "|" + old.getOrgNo());
                    orgMapper.updateById(old);
                } else if ("DISABLE".equals(item.getAction()) && old != null) {
                    Long users = userMapper.selectCount(new LambdaQueryWrapper<User>()
                            .eq(User::getOrgNo, old.getOrgNo()).eq(User::getIsDelete, 0));
                    if (users != null && users > 0) throw new IllegalStateException("组织仍有用户，不能停用: " + old.getOrgNo());
                    old.setIsDelete(1);
                    orgMapper.updateById(old);
                }
                item.setStatus("APPLIED");
                item.setMessage("应用成功");
                itemMapper.updateById(item);
            }
            batch.setStatus("APPLIED");
            batch.setEndTime(nowStr);
            batchMapper.updateById(batch);
            return ResultBean.success("组织导入已应用");
        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorJson(e.getMessage());
            batch.setEndTime(nowStr);
            batchMapper.updateById(batch);
            return ResultBean.error("应用失败，已停止：" + e.getMessage());
        }
    }

    @Transactional
    public ResultBean rollback(Integer batchId) {
        OrgImportBatch batch = batchMapper.selectById(batchId);
        if (batch == null) return ResultBean.error("批次不存在");
        List<OrgImportItem> items = itemMapper.selectList(new LambdaQueryWrapper<OrgImportItem>()
                .eq(OrgImportItem::getBatchId, batchId).eq(OrgImportItem::getStatus, "APPLIED"));
        try {
            for (OrgImportItem item : items) {
                SaOrg old = objectMapper.readValue(item.getOldJson() == null ? "{}" : item.getOldJson(), SaOrg.class);
                if ("ADD".equals(item.getAction())) {
                    orgMapper.delete(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, item.getOrgNo()));
                } else if ("UPDATE".equals(item.getAction()) && old.getOrgNo() != null) {
                    SaOrg org = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, old.getOrgNo()).last("LIMIT 1"));
                    if (org != null) { org.setOrgName(old.getOrgName()); orgMapper.updateById(org); }
                } else if ("MOVE".equals(item.getAction()) && old.getOrgNo() != null) {
                    SaOrg org = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, old.getOrgNo()).last("LIMIT 1"));
                    if (org != null) { org.setPOrgNo(old.getPOrgNo()); org.setOrgTree(old.getOrgTree()); orgMapper.updateById(org); }
                } else if ("DISABLE".equals(item.getAction()) && old.getOrgNo() != null) {
                    SaOrg org = orgMapper.selectOne(new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, old.getOrgNo()).last("LIMIT 1"));
                    if (org != null) { org.setIsDelete(0); orgMapper.updateById(org); }
                }
                item.setStatus("ROLLED_BACK");
                itemMapper.updateById(item);
            }
            batch.setStatus("ROLLED_BACK");
            batchMapper.updateById(batch);
            return ResultBean.success("已回滚");
        } catch (Exception e) {
            return ResultBean.error("回滚失败：" + e.getMessage());
        }
    }

    public ResultBean listBatches() {
        return ResultBean.success("查询成功", batchMapper.selectList(new LambdaQueryWrapper<OrgImportBatch>()
                .orderByDesc(OrgImportBatch::getId).last("LIMIT 50")));
    }

    public ResultBean batchDetail(Integer batchId) {
        return ResultBean.success("查询成功", itemMapper.selectList(new LambdaQueryWrapper<OrgImportItem>()
                .eq(OrgImportItem::getBatchId, batchId)));
    }

    @Scheduled(cron = "0 */10 * * * ?")
    public void scanDirectory() {
        if (importDir == null || importDir.trim().isEmpty()) return;
        File dir = new File(importDir);
        if (!dir.isDirectory()) return;
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".xlsx"));
        if (files == null) return;
        for (File file : files) {
            try (FileInputStream is = new FileInputStream(file)) {
                ResultBean result = validateContent(is, file.getName());
                if (result.getCode() != 200) { move(file, "failed"); continue; }
                Object obj = result.getObj();
                if (!(obj instanceof Map)) { move(file, "failed"); continue; }
                Map<?, ?> data = (Map<?, ?>) obj;
                Object batchObj = data.get("batch");
                if (!(batchObj instanceof OrgImportBatch)) { move(file, "failed"); continue; }
                OrgImportBatch batch = (OrgImportBatch) batchObj;
                if (Integer.valueOf(1).equals(batch.getSafeFlag())) {
                    apply(batch.getId(), true);
                    move(file, "processed");
                } else {
                    move(file, "stopped");
                }
            } catch (Exception e) {
                log.warn("org import scan failed: {}", e.getMessage());
                move(file, "failed");
            }
        }
    }

    private void move(File file, String sub) {
        try {
            Path target = Paths.get(importDir, sub);
            Files.createDirectories(target);
            Files.move(file.toPath(), target.resolve(file.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.warn("move org import file failed: {}", e.getMessage());
        }
    }

    private OrgImportBatch newBatch(String fileName, int total, int add, int update, int move, int disable,
                                    int errors, List<String> errorList, int safeFlag) {
        OrgImportBatch batch = new OrgImportBatch();
        batch.setFileName(fileName);
        batch.setStatus(errors > 0 ? "INVALID" : "PENDING");
        batch.setTotalCount(total);
        batch.setAddCount(add);
        batch.setUpdateCount(update);
        batch.setMoveCount(move);
        batch.setDisableCount(disable);
        batch.setErrorCount(errors);
        batch.setErrorJson(errorList == null ? null : toJson(errorList));
        batch.setSafeFlag(safeFlag);
        batch.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        batchMapper.insert(batch);
        return batch;
    }

    private String sha256(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) digest.update(buf, 0, n);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception e) { return null; }
    }
}
