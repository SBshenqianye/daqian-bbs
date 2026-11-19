package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.LevelItemMapper;
import com.walker.mapper.LevelProfileMapper;
import com.walker.mapper.SvgAssetMapper;
import com.walker.pojo.LevelItem;
import com.walker.pojo.LevelProfile;
import com.walker.pojo.SvgAsset;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SVG 素材库：管理端绘制器保存成果，勋章/等级图标直接引用。
 */
@Service
public class SvgAssetService {

    @Autowired
    private SvgAssetMapper svgAssetMapper;

    @Autowired
    private LevelProfileMapper levelProfileMapper;

    @Autowired
    private LevelItemMapper levelItemMapper;

    private static final String CHART_CATEGORY = "chart";
    private static final String[] CHART_PROFILE_NAMES = {"方案一 · 智荟成长", "方案二 · 电网匠心"};
    private static final String[] TONE_COLORS = {
            "#16a34a", "#0284c7", "#d97706", "#ca8a04",
            "#7c3aed", "#4f46e5", "#db2777", "#dc2626"
    };
    private static final String[] TONE_SOFT = {
            "#dcfce7", "#e0f2fe", "#fef3c7", "#fef9c3",
            "#ede9fe", "#e0e7ff", "#fce7f3", "#fee2e2"
    };
    private static final String STAR_PATH = "M12 1.9l3.05 6.18 6.82.99-4.94 4.81 1.17 6.79L12 17.48l-6.1 3.19 1.17-6.79L2.13 9.07l6.82-.99z";
    private static final String MOON_PATH = "M13.9 1.9a10.1 10.1 0 108.2 15.1A8.6 8.6 0 0113.9 1.9z";
    private static final String CROWN_PATH = "M2.9 18.2 4.4 6.4l5 4.3L12 4.2l2.6 6.5 5-4.3 1.5 11.8z";

    public ResultBean list(String category) {
        // 打开素材库时顺带补齐两套等级图表；图表内容变化时会自动覆盖更新
        try {
            ensureLevelChartAssets(false);
        } catch (Exception ignore) {
            // 生成失败不影响素材列表
        }
        LambdaQueryWrapper<SvgAsset> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq(SvgAsset::getCategory, category.trim());
        }
        wrapper.orderByDesc(SvgAsset::getUpdateTime).orderByDesc(SvgAsset::getId);
        return ResultBean.success("查询成功", svgAssetMapper.selectList(wrapper));
    }

    public ResultBean save(SvgAsset asset) {
        if (asset == null || !StringUtils.hasText(asset.getSvg())) {
            return ResultBean.error("图形内容不能为空");
        }
        String svg = sanitize(asset.getSvg());
        if (svg == null) {
            return ResultBean.error("图形内容不安全或不是合法 SVG");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        asset.setSvg(svg);
        if (!StringUtils.hasText(asset.getAssetName())) {
            asset.setAssetName("未命名素材");
        }
        if (!StringUtils.hasText(asset.getCategory())) {
            asset.setCategory("other");
        }
        if (asset.getId() == null) {
            asset.setCreateTime(now);
            asset.setUpdateTime(now);
            svgAssetMapper.insert(asset);
        } else {
            SvgAsset old = svgAssetMapper.selectById(asset.getId());
            if (old == null) {
                return ResultBean.error("素材不存在");
            }
            asset.setCreateTime(old.getCreateTime());
            asset.setUpdateTime(now);
            svgAssetMapper.updateById(asset);
        }
        return ResultBean.success("保存成功", asset.getId());
    }

    public ResultBean delete(Integer id) {
        if (id == null) {
            return ResultBean.error("缺少素材ID");
        }
        svgAssetMapper.deleteById(id);
        return ResultBean.success("删除成功");
    }

    /** 管理端手动重新生成两套等级图表素材。 */
    public ResultBean generateLevelChartAssets() {
        int count = ensureLevelChartAssets(true);
        return ResultBean.success("已生成 " + count + " 套等级图表", count);
    }

    /**
     * 根据两套内置等级方案生成“等级图表”素材。
     * 幂等：按素材名查找，内容未变化不重复写；force=true 时强制刷新。
     */
    private int ensureLevelChartAssets(boolean force) {
        int changed = 0;
        for (String profileName : CHART_PROFILE_NAMES) {
            LevelProfile profile = levelProfileMapper.selectOne(new LambdaQueryWrapper<LevelProfile>()
                    .eq(LevelProfile::getProfileName, profileName)
                    .last("LIMIT 1"));
            if (profile == null) continue;
            List<LevelItem> items = levelItemMapper.selectList(new LambdaQueryWrapper<LevelItem>()
                    .eq(LevelItem::getProfileId, profile.getId())
                    .orderByAsc(LevelItem::getLevelIndex));
            if (items == null || items.isEmpty()) continue;

            String svg = sanitize(buildLevelChart(profile, items));
            if (svg == null) continue;
            String assetName = "等级图表 · " + shortProfileName(profile.getProfileName());
            SvgAsset existing = svgAssetMapper.selectOne(new LambdaQueryWrapper<SvgAsset>()
                    .eq(SvgAsset::getAssetName, assetName)
                    .last("LIMIT 1"));
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (existing == null) {
                SvgAsset asset = new SvgAsset();
                asset.setAssetName(assetName);
                asset.setCategory(CHART_CATEGORY);
                asset.setSvg(svg);
                asset.setCreatedBy("系统");
                asset.setCreateTime(now);
                asset.setUpdateTime(now);
                svgAssetMapper.insert(asset);
                changed++;
            } else if (force || !svg.equals(existing.getSvg())) {
                existing.setSvg(svg);
                existing.setCategory(CHART_CATEGORY);
                existing.setUpdateTime(now);
                svgAssetMapper.updateById(existing);
                changed++;
            }
        }
        return changed;
    }

    /** 生成一张 3 列 × N 行的等级图表 SVG。 */
    String buildLevelChart(LevelProfile profile, List<LevelItem> items) {
        int cols = 3;
        int rows = Math.max(1, (items.size() + cols - 1) / cols);
        int tileW = 330;
        int tileH = 92;
        int gapX = 15;
        int gapY = 10;
        int margin = 30;
        int header = 118;
        int footer = 34;
        int width = margin * 2 + cols * tileW + (cols - 1) * gapX;
        int height = header + rows * tileH + Math.max(0, rows - 1) * gapY + footer + margin;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
                .append(width).append(" ").append(height).append("\" width=\"")
                .append(width).append("\" height=\"").append(height).append("\">");
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"#f8fafc\"/>");
        svg.append("<rect width=\"").append(width).append("\" height=\"6\" fill=\"#2563eb\"/>");
        svg.append("<text x=\"30\" y=\"52\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" ")
                .append("font-size=\"26\" font-weight=\"700\" fill=\"#0f172a\">大千智荟论坛 · 等级图表</text>");
        svg.append("<text x=\"30\" y=\"82\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" ")
                .append("font-size=\"13\" fill=\"#64748b\">")
                .append(esc(profile.getProfileName())).append(" · ").append(items.size())
                .append(" 级 · 累计积分门槛</text>");
        svg.append("<text x=\"").append(width - 30).append("\" y=\"52\" text-anchor=\"end\" ")
                .append("font-family=\"Arial, 'Microsoft YaHei', sans-serif\" font-size=\"13\" fill=\"#94a3b8\">")
                .append("Lv.1 → Lv.").append(items.size()).append("</text>");
        svg.append("<line x1=\"30\" y1=\"100\" x2=\"").append(width - 30)
                .append("\" y2=\"100\" stroke=\"#e2e8f0\"/>");

        for (int i = 0; i < items.size(); i++) {
            LevelItem item = items.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = margin + col * (tileW + gapX);
            int y = header + row * (tileH + gapY);
            int major = item.getMajorIndex() == null ? 1 : item.getMajorIndex();
            String color = TONE_COLORS[(major - 1) % TONE_COLORS.length];
            String soft = TONE_SOFT[(major - 1) % TONE_SOFT.length];

            svg.append("<g>");
            svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" width=\"").append(tileW).append("\" height=\"").append(tileH)
                    .append("\" rx=\"12\" fill=\"#ffffff\" stroke=\"#e2e8f0\"/>");
            svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" width=\"6\" height=\"").append(tileH)
                    .append("\" rx=\"3\" fill=\"").append(color).append("\"/>");
            svg.append(iconRow(item, x + 14, y + 16, 28));
            svg.append("<text x=\"").append(x + 108).append("\" y=\"").append(y + 34)
                    .append("\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" font-size=\"15\" ")
                    .append("font-weight=\"600\" fill=\"#0f172a\">")
                    .append(esc(item.getDisplayName())).append("</text>");
            svg.append("<text x=\"").append(x + 108).append("\" y=\"").append(y + 54)
                    .append("\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" font-size=\"11\" fill=\"#64748b\">")
                    .append(esc(item.getMajorName())).append(" · Lv.").append(item.getLevelIndex()).append("</text>");
            svg.append("<text x=\"").append(x + 108).append("\" y=\"").append(y + 76)
                    .append("\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" font-size=\"12\" ")
                    .append("font-weight=\"600\" fill=\"").append(color).append("\">累计 ")
                    .append(item.getPointsThreshold() == null ? 0 : item.getPointsThreshold()).append(" 分</text>");
            if (item.getBadgeName() != null && !item.getBadgeName().trim().isEmpty()) {
                svg.append("<text x=\"").append(x + tileW - 12).append("\" y=\"").append(y + tileH - 14)
                        .append("\" text-anchor=\"end\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" ")
                        .append("font-size=\"10\" fill=\"").append(color).append("\" opacity=\"0.85\">")
                        .append(esc(item.getBadgeName())).append("</text>");
            }
            svg.append("<circle cx=\"").append(x + tileW - 22).append("\" cy=\"").append(y + 22)
                    .append("\" r=\"12\" fill=\"").append(soft).append("\"/>");
            svg.append("<text x=\"").append(x + tileW - 22).append("\" y=\"").append(y + 26)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"10\" ")
                    .append("font-weight=\"700\" fill=\"").append(color).append("\">")
                    .append(item.getLevelIndex()).append("</text>");
            svg.append("</g>");
        }

        svg.append("<text x=\"30\" y=\"").append(height - 16)
                .append("\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" font-size=\"10\" fill=\"#94a3b8\">")
                .append("由等级管理自动生成 · 图标按大阶段递进</text>");
        svg.append("<text x=\"").append(width - 30).append("\" y=\"").append(height - 16)
                .append("\" text-anchor=\"end\" font-family=\"Arial, 'Microsoft YaHei', sans-serif\" ")
                .append("font-size=\"10\" fill=\"#94a3b8\">共 ").append(items.size()).append(" 级</text>");
        svg.append("</svg>");
        return svg.toString();
    }

    private String iconRow(LevelItem item, int x, int y, int height) {
        List<String> keys = glyphsForMajor(
                item.getMajorIndex() == null ? 1 : item.getMajorIndex(),
                item.getMinorIndex() == null ? 1 : item.getMinorIndex());
        double scale = height / 24.0;
        double gap = 1.6 * scale;
        double total = keys.size() * 24 * scale + Math.max(0, keys.size() - 1) * gap;
        double startX = x + (84 - total) / 2.0;
        String color = TONE_COLORS[((item.getMajorIndex() == null ? 1 : item.getMajorIndex()) - 1)
                % TONE_COLORS.length];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            sb.append("<g transform=\"translate(")
                    .append(fmt(startX + i * (24 * scale + gap))).append(" ").append(y)
                    .append(") scale(").append(fmt(scale)).append(")\">")
                    .append(glyphMarkup(keys.get(i), color)).append("</g>");
        }
        return sb.toString();
    }

    private List<String> glyphsForMajor(int major, int minor) {
        List<String> keys = new ArrayList<>();
        int count = Math.max(1, Math.min(6, minor));
        if (major <= 4) {
            String key = new String[]{"star", "moon", "sun", "crown"}[Math.max(1, major) - 1];
            for (int i = 0; i < count; i++) keys.add(key);
        } else if (major <= 7) {
            keys.add("crown");
            String satellite = new String[]{"star", "moon", "sun"}[major - 5];
            for (int i = 0; i < count; i++) keys.add(satellite);
        } else {
            keys.add("crownGem");
            for (int i = 0; i < count; i++) keys.add("star");
        }
        return keys;
    }

    private String glyphMarkup(String key, String color) {
        if ("star".equals(key)) {
            return "<path fill=\"" + color + "\" d=\"" + STAR_PATH + "\"/>";
        }
        if ("moon".equals(key)) {
            return "<path fill=\"" + color + "\" d=\"" + MOON_PATH + "\"/>";
        }
        if ("sun".equals(key)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<circle cx=\"12\" cy=\"12\" r=\"4.7\" fill=\"").append(color).append("\"/>");
            sb.append("<g stroke=\"").append(color).append("\" stroke-width=\"2.1\" stroke-linecap=\"round\">")
                    .append("<line x1=\"12\" y1=\"1.7\" x2=\"12\" y2=\"4.7\"/>")
                    .append("<line x1=\"12\" y1=\"19.3\" x2=\"12\" y2=\"22.3\"/>")
                    .append("<line x1=\"1.7\" y1=\"12\" x2=\"4.7\" y2=\"12\"/>")
                    .append("<line x1=\"19.3\" y1=\"12\" x2=\"22.3\" y2=\"12\"/>")
                    .append("<line x1=\"4.6\" y1=\"4.6\" x2=\"6.8\" y2=\"6.8\"/>")
                    .append("<line x1=\"17.2\" y1=\"17.2\" x2=\"19.4\" y2=\"19.4\"/>")
                    .append("<line x1=\"19.4\" y1=\"4.6\" x2=\"17.2\" y2=\"6.8\"/>")
                    .append("<line x1=\"6.8\" y1=\"17.2\" x2=\"4.6\" y2=\"19.4\"/>")
                    .append("</g>");
            return sb.toString();
        }
        if ("crownGem".equals(key)) {
            return "<path fill=\"" + color + "\" d=\"" + CROWN_PATH + "\"/>"
                    + "<rect x=\"3.3\" y=\"19\" width=\"17.4\" height=\"2.5\" rx=\"1.25\" fill=\"" + color + "\"/>"
                    + "<circle cx=\"12\" cy=\"10.4\" r=\"1.85\" fill=\"#ffffff\" opacity=\"0.92\"/>";
        }
        return "<path fill=\"" + color + "\" d=\"" + CROWN_PATH + "\"/>"
                + "<rect x=\"3.3\" y=\"19\" width=\"17.4\" height=\"2.5\" rx=\"1.25\" fill=\"" + color + "\"/>";
    }

    private String shortProfileName(String profileName) {
        if (profileName == null) return "等级方案";
        int idx = profileName.indexOf('·');
        return idx >= 0 ? profileName.substring(idx + 1).trim() : profileName.trim();
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** 只接受纯 SVG，去掉脚本/事件/外链 */
    public String sanitize(String svg) {
        if (svg == null) return null;
        String trimmed = svg.trim();
        if (trimmed.isEmpty() || trimmed.length() > 200000) return null;
        String lower = trimmed.toLowerCase();
        if (!lower.contains("<svg")) return null;
        if (lower.contains("<script") || lower.contains("foreignobject")
                || lower.contains("javascript:") || lower.contains("onload=")
                || lower.contains("onerror=") || lower.contains("onclick=")
                || lower.contains("onmouseover=") || lower.contains("href=\"http")
                || lower.contains("href='http")) {
            return null;
        }
        return trimmed;
    }

    public List<SvgAsset> listAll() {
        return svgAssetMapper.selectList(null);
    }
}
