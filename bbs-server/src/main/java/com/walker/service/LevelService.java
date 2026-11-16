package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.pojo.LevelItem;
import com.walker.pojo.LevelProfile;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 等级配置与用户等级计算。
 */
@Service
public class LevelService {

    @Autowired
    private LevelProfileMapper profileMapper;

    @Autowired
    private LevelItemMapper itemMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private com.walker.mapper.PointsLogMapper pointsLogMapper;

    public ResultBean getProfilePayload() {
        LevelProfile profile = getActiveProfile();
        if (profile == null) {
            return ResultBean.success("查询成功", null);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", profile);
        data.put("items", listItems(profile.getId()));
        return ResultBean.success("查询成功", data);
    }

    public LevelProfile getActiveProfile() {
        List<LevelProfile> list = profileMapper.selectList(new LambdaQueryWrapper<LevelProfile>()
                .eq(LevelProfile::getStatus, 1)
                .orderByDesc(LevelProfile::getId)
                .last("LIMIT 1"));
        return CollectionUtils.isEmpty(list) ? null : list.get(0);
    }

    public List<LevelItem> listItems(Integer profileId) {
        return itemMapper.selectList(new LambdaQueryWrapper<LevelItem>()
                .eq(LevelItem::getProfileId, profileId)
                .eq(LevelItem::getEnabled, 1)
                .orderByAsc(LevelItem::getLevelIndex));
    }

    public Map<String, Object> getUserLevel(Integer userId) {
        int totalPoints = 0;
        try {
            Integer points = pointsLogService.getPointsAdjustment(userId);
            totalPoints = points == null ? 0 : points;
        } catch (Exception ignore) {
            // 积分查询失败时按 0 处理
        }
        LevelProfile profile = getActiveProfile();
        List<LevelItem> items = profile == null ? new ArrayList<>() : listItems(profile.getId());
        if (items.isEmpty()) {
            int level = totalPoints / 100;
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("level", level);
            data.put("levelIndex", level);
            data.put("levelName", "Lv." + level);
            data.put("majorName", "");
            data.put("minorIndex", 0);
            data.put("totalPoints", totalPoints);
            data.put("nextThreshold", (level + 1) * 100);
            data.put("progress", totalPoints % 100);
            data.put("iconKey", "level-" + Math.min(8, Math.max(1, level / 3 + 1)));
            return data;
        }
        LevelItem current = items.get(0);
        LevelItem next = null;
        for (int i = 0; i < items.size(); i++) {
            if (totalPoints >= items.get(i).getPointsThreshold()) {
                current = items.get(i);
                next = i + 1 < items.size() ? items.get(i + 1) : null;
            } else {
                break;
            }
        }
        int progress = 100;
        if (next != null) {
            int span = Math.max(1, next.getPointsThreshold() - current.getPointsThreshold());
            progress = Math.max(0, Math.min(100, (int) Math.round(
                    (totalPoints - current.getPointsThreshold()) * 100.0 / span)));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("level", current.getLevelIndex());
        data.put("levelIndex", current.getLevelIndex());
        data.put("levelName", current.getDisplayName());
        data.put("majorName", current.getMajorName());
        data.put("minorIndex", current.getMinorIndex());
        data.put("totalPoints", totalPoints);
        data.put("nextThreshold", next == null ? null : next.getPointsThreshold());
        data.put("progress", progress);
        data.put("iconKey", current.getIconKey());
        data.put("colorToken", current.getColorToken());
        data.put("iconSvg", current.getIconSvg());
        return data;
    }

    /** 按累计积分与等级项解析等级序号（1 基） */
    public int resolveLevelIndex(int totalPoints, List<LevelItem> items) {
        if (items == null || items.isEmpty()) {
            return Math.max(1, totalPoints / 100);
        }
        int level = items.get(0).getLevelIndex() == null ? 1 : items.get(0).getLevelIndex();
        for (LevelItem item : items) {
            int threshold = item.getPointsThreshold() == null ? 0 : item.getPointsThreshold();
            if (totalPoints >= threshold) {
                level = item.getLevelIndex() == null ? level : item.getLevelIndex();
            } else {
                break;
            }
        }
        return Math.max(1, level);
    }

    /** 单个用户等级序号（轻量，供头像角标使用） */
    public Integer getLevelIndex(Integer userId) {
        if (userId == null) return null;
        int points = 0;
        try {
            Integer p = pointsLogService.getPointsAdjustment(userId);
            points = p == null ? 0 : p;
        } catch (Exception ignore) {
            // 查询失败按 0 分处理
        }
        LevelProfile profile = getActiveProfile();
        List<LevelItem> items = profile == null ? new ArrayList<>() : listItems(profile.getId());
        return resolveLevelIndex(points, items);
    }

    /** 批量用户等级序号，列表页一次算完，避免 N+1 查询 */
    public Map<Integer, Integer> getLevelIndexMap(Collection<Integer> userIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) return result;
        Set<Integer> ids = userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) return result;

        Map<Integer, Integer> pointsMap = new HashMap<>();
        try {
            List<Map<String, Object>> rows = pointsLogMapper.sumPointsByUserIds(ids);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    if (row == null) continue;
                    Object uid = row.get("userId") != null ? row.get("userId") : row.get("user_id");
                    Object total = row.get("total") != null ? row.get("total") : row.get("TOTAL");
                    if (uid instanceof Number) {
                        pointsMap.put(((Number) uid).intValue(),
                                total instanceof Number ? ((Number) total).intValue() : 0);
                    }
                }
            }
        } catch (Exception ignore) {
            // 批量查询失败时按 0 分处理，不影响主流程
        }

        LevelProfile profile = getActiveProfile();
        List<LevelItem> items = profile == null ? new ArrayList<>() : listItems(profile.getId());
        for (Integer id : ids) {
            result.put(id, resolveLevelIndex(pointsMap.getOrDefault(id, 0), items));
        }
        return result;
    }

    @Transactional
    public ResultBean saveProfile(LevelProfile profile, List<LevelItem> items) {        if (profile == null || profile.getMajorCount() == null || profile.getMinorPerMajor() == null) {
            return ResultBean.error("大阶段数和小阶段数不能为空");
        }
        if (profile.getMajorCount() < 1 || profile.getMinorPerMajor() < 1) {
            return ResultBean.error("大阶段数和小阶段数必须大于 0");
        }
        if (CollectionUtils.isEmpty(items)) {
            return ResultBean.error("等级项不能为空");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (profile.getId() == null) {
            profile.setProfileName(profile.getProfileName() == null ? "默认等级体系" : profile.getProfileName());
            if (profile.getStatus() == null) {
                Long active = profileMapper.selectCount(new LambdaQueryWrapper<LevelProfile>()
                        .eq(LevelProfile::getStatus, 1));
                profile.setStatus(active != null && active > 0 ? 0 : 1);
            }
            profile.setCreateTime(now);
            profile.setUpdateTime(now);
            profileMapper.insert(profile);
        } else {
            profile.setUpdateTime(now);
            profileMapper.updateById(profile);
            itemMapper.delete(new LambdaQueryWrapper<LevelItem>().eq(LevelItem::getProfileId, profile.getId()));
        }
        int lastThreshold = -1;
        for (int i = 0; i < items.size(); i++) {
            LevelItem item = items.get(i);
            int threshold = item.getPointsThreshold() == null ? 0 : item.getPointsThreshold();
            if (threshold < lastThreshold) {
                return ResultBean.error("积分门槛必须单调递增");
            }
            lastThreshold = threshold;
            item.setId(null);
            item.setProfileId(profile.getId());
            item.setLevelIndex(item.getLevelIndex() == null ? i + 1 : item.getLevelIndex());
            item.setEnabled(1);
            item.setIconSvg(sanitizeIconSvg(item.getIconSvg()));
            itemMapper.insert(item);
        }
        return ResultBean.success("保存成功");
    }

    /**
     * 自定义等级图标安全清洗：只接受 SVG，去掉脚本、事件属性、外链与 foreignObject。
     */
    private String sanitizeIconSvg(String svg) {
        if (svg == null) return null;
        String trimmed = svg.trim();
        if (trimmed.isEmpty()) return null;
        String lower = trimmed.toLowerCase();
        if (!lower.contains("<svg") || trimmed.length() > 100000) return null;
        if (lower.contains("<script") || lower.contains("foreignobject")
                || lower.contains("javascript:") || lower.contains("onload=")
                || lower.contains("onerror=") || lower.contains("onclick=")
                || lower.contains("href=\"http") || lower.contains("href='http")) {
            return null;
        }
        return trimmed;
    }

    /** 切换启用方案（同时只有一个启用） */
    @Transactional
    public ResultBean activateProfile(Integer id) {
        if (id == null) return ResultBean.error("缺少方案ID");
        LevelProfile target = profileMapper.selectById(id);
        if (target == null) return ResultBean.error("方案不存在");
        profileMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LevelProfile>()
                .set(LevelProfile::getStatus, 0));
        target.setStatus(1);
        target.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        profileMapper.updateById(target);
        return ResultBean.success("已切换到「" + target.getProfileName() + "」");
    }

    /** 复制一份方案（用于快速派生新方案） */
    @Transactional
    public ResultBean cloneProfile(Integer id, String name) {
        if (id == null) return ResultBean.error("缺少方案ID");
        LevelProfile source = profileMapper.selectById(id);
        if (source == null) return ResultBean.error("方案不存在");
        String newName = name == null || name.trim().isEmpty()
                ? source.getProfileName() + " 副本"
                : name.trim();
        Long same = profileMapper.selectCount(new LambdaQueryWrapper<LevelProfile>()
                .eq(LevelProfile::getProfileName, newName));
        if (same != null && same > 0) return ResultBean.error("方案名称已存在");

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        LevelProfile copy = new LevelProfile();
        copy.setProfileName(newName);
        copy.setMajorCount(source.getMajorCount());
        copy.setMinorPerMajor(source.getMinorPerMajor());
        copy.setStatus(0);
        copy.setCreateTime(now);
        copy.setUpdateTime(now);
        profileMapper.insert(copy);

        List<LevelItem> items = itemMapper.selectList(new LambdaQueryWrapper<LevelItem>()
                .eq(LevelItem::getProfileId, source.getId())
                .orderByAsc(LevelItem::getLevelIndex));
        for (LevelItem item : items) {
            LevelItem row = new LevelItem();
            row.setProfileId(copy.getId());
            row.setLevelIndex(item.getLevelIndex());
            row.setMajorIndex(item.getMajorIndex());
            row.setMajorName(item.getMajorName());
            row.setMinorIndex(item.getMinorIndex());
            row.setDisplayName(item.getDisplayName());
            row.setPointsThreshold(item.getPointsThreshold());
            row.setIconKey(item.getIconKey());
            row.setColorToken(item.getColorToken());
            row.setIconSvg(item.getIconSvg());
            row.setBadgeName(item.getBadgeName());
            row.setDescription(item.getDescription());
            row.setEnabled(1);
            itemMapper.insert(row);
        }
        return ResultBean.success("已复制为「" + newName + "」", copy.getId());
    }

    /** 删除方案（启用中的方案不允许删除） */
    @Transactional
    public ResultBean deleteProfile(Integer id) {
        if (id == null) return ResultBean.error("缺少方案ID");
        LevelProfile target = profileMapper.selectById(id);
        if (target == null) return ResultBean.error("方案不存在");
        if (Integer.valueOf(1).equals(target.getStatus())) {
            return ResultBean.error("启用中的方案不能删除，请先切换到其他方案");
        }
        Long total = profileMapper.selectCount(null);
        if (total != null && total <= 1) {
            return ResultBean.error("至少保留一套等级方案");
        }
        itemMapper.delete(new LambdaQueryWrapper<LevelItem>().eq(LevelItem::getProfileId, id));
        profileMapper.deleteById(id);
        return ResultBean.success("删除成功");
    }

    public ResultBean listAdminProfiles() {        List<LevelProfile> profiles = profileMapper.selectList(new LambdaQueryWrapper<LevelProfile>()
                .orderByDesc(LevelProfile::getStatus)
                .orderByDesc(LevelProfile::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (LevelProfile p : profiles) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("profile", p);
            row.put("items", listItems(p.getId()));
            result.add(row);
        }
        return ResultBean.success("查询成功", result);
    }
}
