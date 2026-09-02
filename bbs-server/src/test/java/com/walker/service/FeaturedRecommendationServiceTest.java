package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.User;
import com.walker.service.impl.FeaturedRecommendationServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeaturedRecommendationServiceTest {

    @InjectMocks
    private FeaturedRecommendationServiceImpl recommendationService;

    @Mock
    private FeaturedRecommendationMapper recommendationMapper;

    @Mock
    private ArticleService articleService;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private DictService dictService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = recommendationService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(recommendationService, recommendationMapper);
    }

    @Test
    @DisplayName("推荐精华 → 参数不完整 → 返回错误")
    void recommend_missingParams_returnsError() {
        ResultBean result = recommendationService.recommend(null, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("推荐精华 → 文章不存在 → 返回错误")
    void recommend_articleNotFound_returnsError() {
        when(articleService.getById(999)).thenReturn(null);
        ResultBean result = recommendationService.recommend(999, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("推荐精华 → 已是精华帖 → 返回错误")
    void recommend_alreadyFeatured_returnsError() {
        Article article = new Article();
        article.setArticleId(1);
        article.setIsFeatured(1);
        when(articleService.getById(1)).thenReturn(article);
        ResultBean result = recommendationService.recommend(1, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("推荐精华 → 已有待审推荐 → 返回错误")
    void recommend_existingPending_returnsError() {
        Article article = new Article();
        article.setArticleId(1);
        article.setIsFeatured(0);
        when(articleService.getById(1)).thenReturn(article);
        when(recommendationMapper.countPendingByArticle(1)).thenReturn(1);
        ResultBean result = recommendationService.recommend(1, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("推荐精华 → 正常提交 → 通知超管")
    void recommend_valid_succeeds() {
        Article article = new Article();
        article.setArticleId(1);
        article.setIsFeatured(0);
        article.setArticleTitle("测试帖子");
        when(articleService.getById(1)).thenReturn(article);
        when(recommendationMapper.countPendingByArticle(1)).thenReturn(0);
        when(recommendationMapper.insert(any(FeaturedRecommendation.class))).thenReturn(1);

        User recommender = new User();
        recommender.setNickname("推荐人");
        when(userService.getById(1)).thenReturn(recommender);

        ResultBean result = recommendationService.recommend(1, 1, 1);
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(1), eq(1), eq("featured_recommend"), contains("推荐"), eq("article"), eq(1));
    }

    @Test
    @DisplayName("审核推荐 → 参数不完整 → 返回错误")
    void review_missingParams_returnsError() {
        ResultBean result = recommendationService.review(null, "approved", "好", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核推荐 → 无效状态 → 返回错误")
    void review_invalidStatus_returnsError() {
        ResultBean result = recommendationService.review(1, "invalid", "备注", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核推荐 → 记录不存在 → 返回错误")
    void review_notFound_returnsError() {
        when(recommendationMapper.selectById(999)).thenReturn(null);
        ResultBean result = recommendationService.review(999, "approved", "好", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核推荐 → 已处理 → 返回错误")
    void review_alreadyProcessed_returnsError() {
        FeaturedRecommendation rec = new FeaturedRecommendation();
        rec.setId(1);
        rec.setStatus("approved");
        when(recommendationMapper.selectById(1)).thenReturn(rec);
        ResultBean result = recommendationService.review(1, "approved", "好", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("审核推荐 → 通过 → 设为精华并加分")
    void review_approve_setsFeatured() {
        FeaturedRecommendation rec = new FeaturedRecommendation();
        rec.setId(1);
        rec.setArticleId(10);
        rec.setRecommenderId(2);
        rec.setStatus("pending");
        when(recommendationMapper.selectById(1)).thenReturn(rec);
        when(recommendationMapper.updateById(any(FeaturedRecommendation.class))).thenReturn(1);

        Article article = new Article();
        article.setArticleId(10);
        article.setUserId(3);
        article.setArticleTitle("好帖子");
        when(articleService.getById(10)).thenReturn(article);
        when(dictService.getValueByKey("featured")).thenReturn("10");
        when(articleService.updateById(any(Article.class))).thenReturn(true);

        ResultBean result = recommendationService.review(1, "approved", "好", 1);
        assertEquals(200, result.getCode());
        verify(articleService).updateById(argThat(a -> ((Article) a).getIsFeatured() == 1));
        verify(pointsLogService).adjustUserPoints(eq(3), eq(10), eq("精华帖奖励积分"), eq("featured"), eq(10), eq(1));
        verify(notificationService).createNotification(eq(3), eq(1), eq("adopt"), contains("精华帖"), eq("article"), eq(10));
    }

    @Test
    @DisplayName("审核推荐 → 拒绝 → 通知推荐人")
    void review_reject_notifiesRecommender() {
        FeaturedRecommendation rec = new FeaturedRecommendation();
        rec.setId(1);
        rec.setArticleId(10);
        rec.setRecommenderId(2);
        rec.setStatus("pending");
        when(recommendationMapper.selectById(1)).thenReturn(rec);
        when(recommendationMapper.updateById(any(FeaturedRecommendation.class))).thenReturn(1);

        ResultBean result = recommendationService.review(1, "rejected", "不符合标准", 1);
        assertEquals(200, result.getCode());
        verify(notificationService).createNotification(eq(2), eq(1), eq("featured_review"), contains("已拒绝"), eq("article"), eq(10));
    }
}
