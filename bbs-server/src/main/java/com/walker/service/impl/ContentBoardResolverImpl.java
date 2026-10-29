package com.walker.service.impl;

import com.walker.mapper.CommentMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.service.ArticleService;
import com.walker.service.ContentBoardResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 内容所属板块解析实现。
 *
 * <p>违规/举报关联的内容可能已被逻辑删除，因此使用 raw 查询，
 * 保证“取消违规/复核历史举报”时仍能判断原内容所属板块。</p>
 */
@Service
public class ContentBoardResolverImpl implements ContentBoardResolver {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Override
    public Integer resolveLabelId(String targetType, Integer targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        if ("article".equals(targetType)) {
            Article article = articleService.getArticleByIdRaw(targetId);
            return article != null ? article.getArticleLabelId() : null;
        }
        if ("comment".equals(targetType)) {
            Comment comment = commentMapper.selectByIdRaw(targetId);
            if (comment == null || comment.getCommentArticleId() == null) {
                return null;
            }
            Article article = articleService.getArticleByIdRaw(comment.getCommentArticleId());
            return article != null ? article.getArticleLabelId() : null;
        }
        if ("reply".equals(targetType)) {
            Reply reply = replyMapper.selectByIdRaw(targetId);
            if (reply == null || reply.getCommentId() == null) {
                return null;
            }
            Comment comment = commentMapper.selectByIdRaw(reply.getCommentId());
            if (comment == null || comment.getCommentArticleId() == null) {
                return null;
            }
            Article article = articleService.getArticleByIdRaw(comment.getCommentArticleId());
            return article != null ? article.getArticleLabelId() : null;
        }
        return null;
    }
}
