package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.User;
import com.walker.service.ArticleService;
import com.walker.service.DictService;
import com.walker.service.FeaturedRecommendationService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.UserService;
import com.walker.utils.ConstantUtil;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FeaturedRecommendationServiceImpl extends ServiceImpl<FeaturedRecommendationMapper, FeaturedRecommendation>
        implements FeaturedRecommendationService {

    @Autowired
    private FeaturedRecommendationMapper recommendationMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private DictService dictService;

    @Override
    @Transactional
    public ResultBean recommend(Integer articleId, Integer recommenderId, Integer labelId) {
        if (articleId == null || recommenderId == null) {
            return ResultBean.error("参数不完整");
        }

        Article article = articleService.getById(articleId);
        if (article == null) {
            return ResultBean.error("文章不存在");
        }
        if (article.getIsFeatured() != null && article.getIsFeatured() == 1) {
            return ResultBean.error("该帖子已是精华帖");
        }

        // 检查是否已有待审推荐
        int pendingCount = recommendationMapper.countPendingByArticle(articleId);
        if (pendingCount > 0) {
            return ResultBean.error("该帖子已有待审核的精华推荐");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        FeaturedRecommendation rec = new FeaturedRecommendation();
        rec.setArticleId(articleId);
        rec.setRecommenderId(recommenderId);
        rec.setLabelId(labelId);
        rec.setStatus("pending");
        rec.setCreateTime(fmt.format(new Date()));
        this.save(rec);

        // 通知超级管理员
        User recommender = userService.getById(recommenderId);
        String recommenderName = recommender != null ? recommender.getNickname() : "版主#" + recommenderId;
        String articleTitle = article.getArticleTitle() != null ? article.getArticleTitle() : "";
        notificationService.createNotification(1, recommenderId, "featured_recommend",
                "精华帖推荐：「" + recommenderName + "」推荐《" + articleTitle + "》",
                "article", articleId);

        return ResultBean.success("推荐已提交，等待总运营审核");
    }

    @Override
    @Transactional
    public ResultBean review(Integer recommendationId, String status, String remark, Integer reviewerId) {
        if (recommendationId == null || status == null) {
            return ResultBean.error("参数不完整");
        }
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            return ResultBean.error("无效的审核状态");
        }

        FeaturedRecommendation rec = this.getById(recommendationId);
        if (rec == null) {
            return ResultBean.error("推荐记录不存在");
        }
        if (!"pending".equals(rec.getStatus())) {
            return ResultBean.error("该推荐已处理");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rec.setStatus(status);
        rec.setReviewerId(reviewerId);
        rec.setReviewRemark(remark);
        rec.setReviewTime(fmt.format(new Date()));
        this.updateById(rec);

        if ("approved".equals(status)) {
            // 设为精华帖并加分
            Article article = articleService.getById(rec.getArticleId());
            if (article != null) {
                int featuredPoints = 10;
                try {
                    String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                    if (val != null) featuredPoints = Integer.parseInt(val);
                } catch (Exception e) { /* use default */ }

                article.setIsFeatured(1);
                articleService.updateById(article);
                pointsLogService.adjustUserPoints(article.getUserId(), featuredPoints, "精华帖奖励积分",
                        "featured", rec.getArticleId(), reviewerId);

                // 通知帖子作者
                notificationService.createNotification(article.getUserId(), reviewerId, "adopt",
                        "恭喜！您的帖子《" + (article.getArticleTitle() != null ? article.getArticleTitle() : "") + "》被评定为精华帖，获得+" + featuredPoints + "积分",
                        "article", rec.getArticleId());
            }
        }

        // 通知推荐人
        String statusLabel = "approved".equals(status) ? "已通过" : "已拒绝";
        notificationService.createNotification(rec.getRecommenderId(), reviewerId, "featured_review",
                "精华帖推荐审核结果：" + statusLabel + (remark != null ? "（" + remark + "）" : ""),
                "article", rec.getArticleId());

        return ResultBean.success("审核完成" + ("approved".equals(status) ? "，已设为精华帖" : ""));
    }

    @Override
    public ResultBean listRecommendations(Integer page, Integer size, String status) {
        Page<FeaturedRecommendation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FeaturedRecommendation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FeaturedRecommendation::getStatus, status);
        }
        wrapper.orderByDesc(FeaturedRecommendation::getCreateTime);
        Page<FeaturedRecommendation> result = this.page(pageParam, wrapper);

        // 填充用户和文章信息
        List<Map<String, Object>> records = new ArrayList<>();
        Set<Integer> userIds = new HashSet<>();
        Set<Integer> articleIds = new HashSet<>();
        for (FeaturedRecommendation r : result.getRecords()) {
            userIds.add(r.getRecommenderId());
            articleIds.add(r.getArticleId());
        }
        Map<Integer, User> userMap = new HashMap<>();
        Map<Integer, Article> articleMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            for (User u : users) userMap.put(u.getId(), u);
        }
        if (!articleIds.isEmpty()) {
            List<Article> articles = articleService.listByIds(articleIds);
            for (Article a : articles) articleMap.put(a.getId(), a);
        }

        for (FeaturedRecommendation r : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("articleId", r.getArticleId());
            Article article = articleMap.get(r.getArticleId());
            map.put("articleTitle", article != null ? article.getArticleTitle() : "");
            map.put("recommenderId", r.getRecommenderId());
            User recommender = userMap.get(r.getRecommenderId());
            map.put("recommenderName", recommender != null ? recommender.getNickname() : "");
            map.put("status", r.getStatus());
            map.put("reviewRemark", r.getReviewRemark());
            map.put("reviewTime", r.getReviewTime());
            map.put("createTime", r.getCreateTime());
            records.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }
}
