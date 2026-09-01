package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.CommentMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Dict;
import com.walker.pojo.Reply;
import com.walker.service.ArticleService;
import com.walker.service.CommentService;
import com.walker.service.DictService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.ReplyService;
import com.walker.utils.ConstantUtil;
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

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private DictService dictService;

    @Autowired
    private ReplyService replyService;

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

        // 评论积分：只有通过质量检测的评论才计分，且同一篇帖子下同一用户最多3次
        // 运营方案：回帖人在自己的帖子内进行回复不再获得积分
        if (quality.isPassed()) {
            Article article = articleService.getById(commentParam.getCommentArticleId());
            // 自己帖子内评论不计分
            if (article != null && !article.getUserId().equals(commentParam.getCommentUserId())) {
                // 检查该用户在此文章下已获得的回帖积分次数
                int existingCount = pointsLogService.countReplyPointsForArticle(
                        commentParam.getCommentUserId(), commentParam.getCommentArticleId());
                if (existingCount < 3) {
                    int replyPoints = 1; // default
                    try {
                        String val = dictService.getValueByKey(ConstantUtil.MANA_REPLY);
                        if (val != null) replyPoints = Integer.parseInt(val);
                    } catch (Exception e) { /* use default */ }
                    pointsLogService.adjustUserPoints(commentParam.getCommentUserId(), replyPoints, "评论积分",
                            "comment", comment.getCommentId(), null);
                }
            }
        }

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
        // 删除前先获取评论信息，用于扣回积分
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getEnable() != null && comment.getEnable() == 1) {
            int replyPoints = 1; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_REPLY);
                if (val != null) replyPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(comment.getCommentUserId(), -replyPoints, "删除评论扣回积分",
                    "comment", commentId, null);
        }
        commentMapper.deleteById(commentId);
        return ResultBean.success("删除成功");
    }

    @Override
    public void checkHotBonus(Integer articleId) {
        Article article = articleService.queryArticleById(articleId);
        if (article == null || (article.getIsHotBonus() != null && article.getIsHotBonus() == 1)) {
            return; // 文章不存在或已发过热度奖励
        }

        // 获取热度阈值（默认10）
        int threshold = 10;
        try {
            String val = dictService.getValueByKey("hot_threshold");
            if (val != null) threshold = Integer.parseInt(val);
        } catch (Exception e) {
            // 使用默认值
        }

        // 统计该文章的有效互动数 = 评论数 + 回复数
        List<Comment> comments = queryCommentReply(articleId);
        int totalInteractions = comments.size(); // 楼层评论也算
        for (Comment c : comments) {
            List<Reply> replies = replyService.queryReplyByCommentId(c.getCommentId());
            totalInteractions += replies.size();
        }

        if (totalInteractions >= threshold) {
            // 触发热度奖励
            article.setIsHotBonus(1);
            articleService.updateById(article);

            pointsLogService.adjustUserPoints(article.getUserId(), 1,
                    "帖子热度奖励（互动数达" + totalInteractions + "条）",
                    "hot_bonus", articleId, null);

            // 通知作者
            notificationService.createNotification(article.getUserId(), null,
                    "hot_bonus", "恭喜！您的帖子获得热度奖励+1分",
                    "article", articleId);
        }
    }
}
