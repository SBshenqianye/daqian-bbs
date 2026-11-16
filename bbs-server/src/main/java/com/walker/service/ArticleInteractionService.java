package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.walker.mapper.ArticleLikeMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.ArticleUserMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleLike;
import com.walker.pojo.ArticleUser;
import com.walker.pojo.User;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 帖子点赞、收藏、分享。
 */
@Service
public class ArticleInteractionService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleUserMapper articleUserMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public ResultBean toggleLike(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) {
            return ResultBean.error("参数不完整");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getIsDelete() != null && article.getIsDelete() == 1) {
            return ResultBean.error("帖子不存在");
        }
        ArticleLike exists = articleLikeMapper.selectOne(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .eq(ArticleLike::getArticleId, articleId)
                .last("LIMIT 1"));
        if (exists != null) {
            articleLikeMapper.deleteById(exists.getId());
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getArticleId, articleId)
                    .setSql("article_good_num = GREATEST(COALESCE(article_good_num, 0) - 1, 0)"));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, article.getUserId())
                    .setSql("good = GREATEST(COALESCE(good, 0) - 1, 0)"));
            return ResultBean.success("已取消点赞", interactionMap(userId, articleId));
        }
        ArticleLike like = new ArticleLike();
        like.setUserId(userId);
        like.setArticleId(articleId);
        like.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        articleLikeMapper.insert(like);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .setSql("article_good_num = COALESCE(article_good_num, 0) + 1"));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, article.getUserId())
                .setSql("good = COALESCE(good, 0) + 1"));
        return ResultBean.success("点赞成功", interactionMap(userId, articleId));
    }

    @Transactional
    public ResultBean toggleFavorite(Integer userId, Integer articleId) {
        if (userId == null || articleId == null) {
            return ResultBean.error("参数不完整");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getIsDelete() != null && article.getIsDelete() == 1) {
            return ResultBean.error("帖子不存在");
        }
        ArticleUser exists = articleUserMapper.selectOne(new LambdaQueryWrapper<ArticleUser>()
                .eq(ArticleUser::getUserId, userId)
                .eq(ArticleUser::getArticleId, articleId)
                .last("LIMIT 1"));
        if (exists != null) {
            articleUserMapper.deleteById(exists.getId());
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getArticleId, articleId)
                    .setSql("favorite_num = GREATEST(COALESCE(favorite_num, 0) - 1, 0)"));
            return ResultBean.success("已取消收藏", interactionMap(userId, articleId));
        }
        ArticleUser favorite = new ArticleUser();
        favorite.setUserId(userId);
        favorite.setArticleId(articleId);
        favorite.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        articleUserMapper.insert(favorite);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .setSql("favorite_num = COALESCE(favorite_num, 0) + 1"));
        return ResultBean.success("收藏成功", interactionMap(userId, articleId));
    }

    public ResultBean share(Integer articleId) {
        if (articleId == null) {
            return ResultBean.error("参数不完整");
        }
        int updated = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getArticleId, articleId)
                .setSql("share_num = COALESCE(share_num, 0) + 1"));
        return updated > 0 ? ResultBean.success("分享成功") : ResultBean.error("帖子不存在");
    }

    public ResultBean getInteraction(Integer userId, Integer articleId) {
        if (articleId == null) {
            return ResultBean.error("参数不完整");
        }
        return ResultBean.success("查询成功", interactionMap(userId, articleId));
    }

    private Map<String, Object> interactionMap(Integer userId, Integer articleId) {
        Map<String, Object> data = new LinkedHashMap<>();
        Article article = articleMapper.selectById(articleId);
        data.put("articleId", articleId);
        data.put("likes", article == null ? 0 : (article.getArticleGoodNum() == null ? 0 : article.getArticleGoodNum()));
        data.put("favorites", article == null ? 0 : (article.getFavoriteNum() == null ? 0 : article.getFavoriteNum()));
        data.put("shares", article == null ? 0 : (article.getShareNum() == null ? 0 : article.getShareNum()));
        data.put("liked", false);
        data.put("favorited", false);
        if (userId != null) {
            data.put("liked", articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, userId)
                    .eq(ArticleLike::getArticleId, articleId)) > 0);
            data.put("favorited", articleUserMapper.selectCount(new LambdaQueryWrapper<ArticleUser>()
                    .eq(ArticleUser::getUserId, userId)
                    .eq(ArticleUser::getArticleId, articleId)) > 0);
        }
        return data;
    }
}
