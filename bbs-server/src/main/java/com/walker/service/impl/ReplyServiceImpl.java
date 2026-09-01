package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ReplyMapper;
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
import com.walker.vo.param.ReplyParam;
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
public class ReplyServiceImpl extends ServiceImpl<ReplyMapper, Reply> implements ReplyService {

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DictService dictService;
    /**
     * 通过评论获取回复
     * @param commentId
     * @return
     */
    @Override
    public List<Reply> queryReplyByCommentId(Integer commentId) {

        LambdaQueryWrapper<Reply> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Reply::getCommentId,commentId);

        return replyMapper.selectList(lambdaQueryWrapper);
    }


    /**
     * 保存用户的评论
     * @param replyParam
     * @return
     */
    @Override
    public ResultBean saveUserReply(ReplyParam replyParam) {

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String day = format.format(date);

        // ── 内容质量检测：垃圾内容标记为不可见，不计入积分 ──
        ContentQualityUtil.QualityResult quality = ContentQualityUtil.checkContent(null, replyParam.getReplyContent());

        Reply reply = new Reply();
        reply.setReplyContent(replyParam.getReplyContent());
        reply.setReplyTime(day);
        reply.setReplyUserId(replyParam.getReplyUserId());
        reply.setReplyToUserId(replyParam.getReplyToUserId());
        reply.setCommentId(replyParam.getCommentId());
        // 垃圾内容标记为不可见
        reply.setEnable(quality.isPassed() ? 1 : 0);
        this.save(reply);

        // 回复积分：只有通过质量检测的回复才计分，且同一篇帖子下同一用户最多3次
        // 运营方案：回帖人在自己的帖子内进行回复不再获得积分
        Comment commentForArticle = null;
        if (quality.isPassed()) {
            // 通过评论找到文章ID，检查该用户在此文章下已获得的回帖积分次数
            commentForArticle = commentService.getById(replyParam.getCommentId());
            if (commentForArticle != null) {
                Article article = articleService.getById(commentForArticle.getCommentArticleId());
                // 自己帖子内回复不计分
                if (article != null && !article.getUserId().equals(replyParam.getReplyUserId())) {
                    int existingCount = pointsLogService.countReplyPointsForArticle(
                            replyParam.getReplyUserId(), commentForArticle.getCommentArticleId());
                    if (existingCount < 3) {
                        int replyPoints = 1; // default
                        try {
                            String val = dictService.getValueByKey(ConstantUtil.MANA_REPLY);
                            if (val != null) replyPoints = Integer.parseInt(val);
                        } catch (Exception e) { /* use default */ }
                        pointsLogService.adjustUserPoints(replyParam.getReplyUserId(), replyPoints, "回复积分",
                                "reply", reply.getReplyId(), null);
                    }
                }
            }
        }

        // 通知被回复的用户（非自己时）
        if (quality.isPassed() && replyParam.getReplyToUserId() != null
                && !replyParam.getReplyToUserId().equals(replyParam.getReplyUserId())) {
            try {
                if (commentForArticle == null) {
                    commentForArticle = commentService.getById(replyParam.getCommentId());
                }
                String commentPreview = commentForArticle != null ? commentForArticle.getCommentContent() : "";
                if (commentPreview.length() > 20) {
                    commentPreview = commentPreview.substring(0, 20) + "...";
                }
                String title = "有人回复了你的评论「" + commentPreview + "」";
                notificationService.createNotification(
                        replyParam.getReplyToUserId(),
                        replyParam.getReplyUserId(),
                        "reply",
                        title,
                        "reply",
                        reply.getReplyId()
                );
            } catch (Exception e) {
                // 通知失败不影响回复发布
            }
        }

        if (quality.isSpam()) {
            return ResultBean.success("回复成功，但内容被判定为低质量，暂不展示且不计入积分");
        }
        return ResultBean.success("回复成功！");
    }

    @Override
    public ResultBean deleteReplyById(Integer replyId) {
        // 删除前先获取回复信息，用于扣回积分
        Reply reply = replyMapper.selectById(replyId);
        if (reply != null && reply.getEnable() != null && reply.getEnable() == 1) {
            int replyPoints = 1; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_REPLY);
                if (val != null) replyPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(reply.getReplyUserId(), -replyPoints, "删除回复扣回积分",
                    "reply", replyId, null);
        }
        replyMapper.deleteById(replyId);
        return ResultBean.success("删除成功");
    }
}
