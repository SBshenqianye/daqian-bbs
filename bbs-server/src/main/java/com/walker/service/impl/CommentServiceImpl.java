package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.CommentMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.service.ArticleService;
import com.walker.service.CommentService;
import com.walker.service.NotificationService;
import com.walker.utils.ContentQualityUtil;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommentParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {


    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ArticleService articleService;
    /**
     * 保存用户的评论 一级
     * @param commentParam
     * @return
     */
    @Override
    public ResultBean saveUserComment(CommentParam commentParam) {

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String day = format.format(date);

        // ── 内容质量检测：垃圾内容标记为不可见，不计入积分 ──
        ContentQualityUtil.QualityResult quality = ContentQualityUtil.checkContent(null, commentParam.getCommentContent());

        Comment comment = new Comment();
        comment.setCommentContent(commentParam.getCommentContent());
        comment.setCommentUserId(commentParam.getCommentUserId());
        comment.setCommentArticleId(commentParam.getCommentArticleId());
        comment.setCommentTime(day);
        // 垃圾内容标记为不可见
        comment.setEnable(quality.isPassed() ? 1 : 0);
        this.save(comment);

        // 通知文章作者（非自己时）
        if (quality.isPassed()) {
            try {
                Article article = articleService.getById(commentParam.getCommentArticleId());
                if (article != null && !article.getUserId().equals(commentParam.getCommentUserId())) {
                    String title = "有人评论了你的帖子「" + article.getArticleTitle() + "」";
                    notificationService.createNotification(
                            article.getUserId(),
                            commentParam.getCommentUserId(),
                            "comment",
                            title,
                            "article",
                            commentParam.getCommentArticleId()
                    );
                }
            } catch (Exception e) {
                // 通知失败不影响评论发布
            }
        }

        if (quality.isSpam()) {
            return ResultBean.success("评论成功，但内容被判定为低质量，暂不展示且不计入积分");
        }
        return ResultBean.success("评论成功！");
    }

    /**
     * 通过文章ID获取文章的评论和回复
     * @param articleId
     * @return
     */
    @Override
    public List<Comment> queryCommentReply(Integer articleId) {

        LambdaQueryWrapper<Comment> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Comment::getCommentArticleId,articleId);

        return commentMapper.selectList(lambdaQueryWrapper);

    }

    @Override
    public ResultBean deleteCommentById(Integer commentId) {

        commentMapper.deleteById(commentId);
        return ResultBean.success("删除成功");
    }
}
