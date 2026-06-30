package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleFileMapper;
import com.walker.pojo.ArticleFile;
import com.walker.pojo.User;
import com.walker.service.ArticleFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/12 9:47
 * @PackageName:com.walker.service.impl
 * @ClassName: ArticleFileServiceImpl
 * @Description: 文章附件实现层
 */
@Service
public class ArticleFileServiceImpl extends ServiceImpl<ArticleFileMapper, ArticleFile> implements ArticleFileService {

    @Autowired
    private ArticleFileMapper articleFileMapper;

    @Override
    public int addArticleFile(ArticleFile articleFile) {
        return articleFileMapper.insert(articleFile);
    }

    @Override
    public int updateArticleFile(Integer[] fileIds, Integer articleId) {
        return articleFileMapper.updateArticleFile(fileIds, articleId);
    }

    @Override
    public List<ArticleFile> getArticleFileByArticleId(Integer articleId) {
        return articleFileMapper.selectList(
                new LambdaQueryWrapper<ArticleFile>()
                    .like(ArticleFile::getArticleId,articleId)
                    .orderByAsc(ArticleFile::getFileId)
        );
    }

    @Override
    public int unBindArticleFile(Integer articleId) {
        return articleFileMapper.unBindArticleFile(articleId);
    }

    @Override
    public List<ArticleFile> getArticleFileByKeywords(String keywords) {
        return articleFileMapper.selectList(
                new LambdaQueryWrapper<ArticleFile>()
                        .like(ArticleFile::getFileName,keywords)
        );
    }
}
