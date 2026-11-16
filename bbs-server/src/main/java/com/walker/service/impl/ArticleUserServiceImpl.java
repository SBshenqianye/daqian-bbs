package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleUserMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleUser;
import com.walker.pojo.User;
import com.walker.service.ArticleService;
import com.walker.service.ArticleUserService;
import com.walker.service.UserService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/12/10 18:03
 */
@Service
public class ArticleUserServiceImpl extends ServiceImpl<ArticleUserMapper, ArticleUser> implements ArticleUserService {

    @Autowired
    private ArticleUserMapper articleUserMapper;

    @Autowired
    ArticleService articleService;

    @Resource
    UserService userService;

    @Override
    public ResultBean saveUserCollection(Integer userId, Integer articleId) {
        LambdaQueryWrapper<ArticleUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ArticleUser::getArticleId, articleId)
                .eq(ArticleUser::getUserId, userId);
        ArticleUser articleUser1 = articleUserMapper.selectOne(lambdaQueryWrapper);
        if (articleUser1 == null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            String currentDate = simpleDateFormat.format(new Date());

            ArticleUser articleUser = new ArticleUser();
            articleUser.setArticleId(articleId);
            articleUser.setUserId(userId);
            articleUser.setCreateTime(currentDate);
            articleUserMapper.insert(articleUser);

            // 文章表更新收藏数（收藏不再增加点赞/获赞）
            articleService.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Article>()
                    .eq(Article::getArticleId, articleId)
                    .setSql("favorite_num = COALESCE(favorite_num, 0) + 1"));

            return ResultBean.success("收藏成功！");
        }
        return ResultBean.error("该文章已经收藏过了，请不要重复收藏");

    }

    @Override
    public List<Article> getMyCollection(Integer userId) {
        LambdaQueryWrapper<ArticleUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(ArticleUser::getUserId, userId);
        List<ArticleUser> articleUsers = articleUserMapper.selectList(lambdaQueryWrapper);
        List<Integer> articleIds = articleUsers.stream().map(ArticleUser::getArticleId).collect(Collectors.toList());

        return articleService.getArticlesByIds(articleIds);
    }

    @Override
    public List<ArticleUser> getArticleUserByArticleIds(List<Integer> articleIds) {

        if (articleIds != null){
            return articleUserMapper.selectList(
                    new LambdaQueryWrapper<ArticleUser>()
                            .in(ArticleUser::getArticleId,articleIds)
            );
        }
        return null;
    }
}
