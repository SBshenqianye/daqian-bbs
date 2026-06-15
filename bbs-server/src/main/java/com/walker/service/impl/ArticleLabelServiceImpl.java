package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.walker.mapper.ArticleLabelMapper;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.User;
import com.walker.service.ArticleLabelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/05/20 22:07
 */
@Service
public class ArticleLabelServiceImpl extends ServiceImpl<ArticleLabelMapper, ArticleLabel> implements ArticleLabelService {

    @Autowired
    private ArticleLabelMapper articleLabelMapper;

    /**
     * 获取所有的标签
     * @return
     */
    @Override
    public List<ArticleLabel> queryAllArticleLabel() {

        return articleLabelMapper.selectList(
                new LambdaQueryWrapper<ArticleLabel>()
                        .eq(ArticleLabel::getEnabled, 1)
        );

    }

    @Override
    public boolean existsByLabelName(String labelName) {
        return lambdaQuery().eq(ArticleLabel::getLabelName, labelName).count() > 0;
    }

    @Override
    public boolean existsByLabelNameExcludeId(String labelName, Integer labelId) {
        return lambdaQuery()
                .eq(ArticleLabel::getLabelName, labelName)
                .ne(ArticleLabel::getLabelId, labelId)
                .count() > 0;
    }

    @Override
    public PageInfo<ArticleLabel> getAllArticleLabelByPageAndSearch(int pageIndex, int pageSize, String searchInfo) {
        PageHelper.startPage(pageIndex,pageSize);
        List<ArticleLabel> articleLabels = articleLabelMapper.selectList(
                new LambdaQueryWrapper<ArticleLabel>()
                        .like(ArticleLabel::getLabelName,searchInfo)
        );
        return new PageInfo<>(articleLabels);
    }
}
