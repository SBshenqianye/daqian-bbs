package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.SvgAssetMapper;
import com.walker.pojo.SvgAsset;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * SVG 素材库：管理端绘制器保存成果，勋章/等级图标直接引用。
 */
@Service
public class SvgAssetService {

    @Autowired
    private SvgAssetMapper svgAssetMapper;

    public ResultBean list(String category) {
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

    /**
     * 幂等写入内置素材：按「名称 + 分类」判重，只补齐缺失项。
     * 供前端把内置图标（如 8 枚等级图标）一次性同步进素材库，重复调用不会产生重复数据。
     */
    @Transactional
    public ResultBean ensure(List<SvgAsset> assets) {
        if (assets == null || assets.isEmpty()) {
            return ResultBean.success("无需处理", 0);
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        int created = 0;
        for (SvgAsset asset : assets) {
            if (asset == null || !StringUtils.hasText(asset.getAssetName())) continue;
            String svg = sanitize(asset.getSvg());
            if (svg == null) continue;
            String category = StringUtils.hasText(asset.getCategory()) ? asset.getCategory() : "other";
            Long exists = svgAssetMapper.selectCount(new LambdaQueryWrapper<SvgAsset>()
                    .eq(SvgAsset::getAssetName, asset.getAssetName())
                    .eq(SvgAsset::getCategory, category));
            if (exists != null && exists > 0) continue;
            SvgAsset row = new SvgAsset();
            row.setAssetName(asset.getAssetName());
            row.setCategory(category);
            row.setSvg(svg);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            svgAssetMapper.insert(row);
            created++;
        }
        return ResultBean.success("已补充 " + created + " 条素材", created);
    }

    public ResultBean delete(Integer id) {        if (id == null) {
            return ResultBean.error("缺少素材ID");
        }
        svgAssetMapper.deleteById(id);
        return ResultBean.success("删除成功");
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
