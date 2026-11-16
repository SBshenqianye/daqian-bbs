package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.service.DictService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 精华公示/展示卡。
 */
@Api(tags = "精华公示")
@RestController
public class FeaturedPublicController {

    @Autowired private FeaturedRecommendationMapper recommendationMapper;
    @Autowired private ArticleMapper articleMapper;
    @Autowired private DictService dictService;

    @ApiOperation("公示中的精华帖")
    @GetMapping("/common/featured/notices")
    public ResultBean notices() {
        List<FeaturedRecommendation> list = recommendationMapper.selectList(
                new LambdaQueryWrapper<FeaturedRecommendation>()
                        .eq(FeaturedRecommendation::getPublicStatus, "PUBLICIZING")
                        .orderByDesc(FeaturedRecommendation::getPublicStartTime)
                        .last("LIMIT 20"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (FeaturedRecommendation rec : list) {
            Article article = articleMapper.selectById(rec.getArticleId());
            if (article == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("recommendationId", rec.getId());
            row.put("articleId", article.getArticleId());
            row.put("title", article.getArticleTitle());
            row.put("summary", article.getArticleSummary());
            row.put("author", Integer.valueOf(1).equals(article.getIsAnonymous()) ? "匿名用户" : article.getArticleAuthor());
            row.put("cover", article.getArticleImage());
            row.put("reason", rec.getFeaturedReason());
            row.put("publicStartTime", rec.getPublicStartTime());
            row.put("publicEndTime", rec.getPublicEndTime());
            row.put("publicStatus", rec.getPublicStatus());
            row.put("createTime", article.getCreateTime());
            result.add(row);
        }
        return ResultBean.success("查询成功", result);
    }

    @ApiOperation("精华展示卡")
    @GetMapping("/common/featured/showcase")
    public ResultBean showcase(@RequestParam(defaultValue = "3") Integer limit) {
        int size = limit == null || limit < 1 ? 3 : Math.min(limit, 20);
        try {
            String cfg = dictService.getValueByKey("featured_showcase_count");
            if (cfg != null && !cfg.trim().isEmpty()) size = Math.min(20, Integer.parseInt(cfg.trim()));
        } catch (Exception ignore) { }
        List<Article> list = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 1)
                .eq(Article::getIsFeatured, 1)
                .orderByDesc(Article::getFeaturedTime)
                .orderByDesc(Article::getCreateTime)
                .last("LIMIT " + size));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Article a : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("articleId", a.getArticleId());
            row.put("title", a.getArticleTitle());
            row.put("summary", a.getArticleSummary());
            row.put("author", Integer.valueOf(1).equals(a.getIsAnonymous()) ? "匿名用户" : a.getArticleAuthor());
            row.put("cover", a.getArticleImage());
            row.put("featuredTime", a.getFeaturedTime());
            row.put("views", a.getArticleViewNum());
            row.put("likes", a.getArticleGoodNum());
            row.put("favorites", a.getFavoriteNum());
            row.put("labelId", a.getArticleLabelId());
            result.add(row);
        }
        return ResultBean.success("查询成功", result);
    }

    @ApiOperation("管理员手动结束公示")
    @org.springframework.security.access.prepost.PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).FEATURED_REVIEW)")
    @PostMapping("/admin/featured/public/complete")
    public ResultBean complete(@RequestBody Map<String, Object> body) {
        Object id = body == null ? null : body.get("recommendationId");
        if (id == null) return ResultBean.error("缺少推荐ID");
        FeaturedRecommendation rec = recommendationMapper.selectById(Integer.valueOf(String.valueOf(id)));
        if (rec == null) return ResultBean.error("推荐不存在");
        rec.setPublicStatus("PUBLISHED");
        rec.setPublicEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        recommendationMapper.updateById(rec);
        return ResultBean.success("已结束公示");
    }

    @Scheduled(cron = "0 10 * * * ?")
    public void finishExpiredPublicity() {
        List<FeaturedRecommendation> list = recommendationMapper.selectList(
                new LambdaQueryWrapper<FeaturedRecommendation>()
                        .eq(FeaturedRecommendation::getPublicStatus, "PUBLICIZING")
                        .isNotNull(FeaturedRecommendation::getPublicEndTime));
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (FeaturedRecommendation rec : list) {
            if (rec.getPublicEndTime() != null && rec.getPublicEndTime().compareTo(now) <= 0) {
                rec.setPublicStatus("PUBLISHED");
                recommendationMapper.updateById(rec);
            }
        }
    }
}
