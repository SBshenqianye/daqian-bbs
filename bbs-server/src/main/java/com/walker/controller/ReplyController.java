package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.service.*;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ReplyParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 回复控制器（含采纳审批流程）
 */
@Api(tags = "ReplyController")
@RestController
@RequestMapping("/reply")
public class ReplyController {

    @Autowired private ReplyService replyService;
    @Autowired private UserService userService;
    @Autowired private ArticleService articleService;
    @Autowired private PointsLogService pointsLogService;
    @Autowired private NotificationService notificationService;
    @Autowired private CommentService commentService;
    @Autowired private ArticleLabelService articleLabelService;
    @Autowired private BoardModeratorService boardModeratorService;

    private static final String QUESTION_LABEL_NAME = "问题求助";
    private static final int ADOPT_POINTS = 5;

    /** 从 Map 参数安全提取 Integer */
    private static Integer toInt(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    // ==================== 基础回复 ====================

    @ApiOperation(value = "保存用户的回复")
    @PutMapping("/userReply")
    public ResultBean userReply(@RequestBody ReplyParam replyParam) {
        ResultBean result = replyService.saveUserReply(replyParam);
        if (replyParam.getCommentId() != null) {
            try {
                Comment comment = commentService.getById(replyParam.getCommentId());
                if (comment != null && comment.getCommentArticleId() != null) {
                    commentService.checkHotBonus(comment.getCommentArticleId());
                }
            } catch (Exception e) { /* 热度检查失败不影响 */ }
        }
        return result;
    }

    @ApiOperation(value = "通过id删除用户评论")
    @PostMapping("/deleteReplyById")
    public ResultBean deleteReplyById(@RequestBody ReplyParam replyParam) {
        return replyService.deleteReplyById(replyParam.getReplyId());
    }

    // ==================== 采纳审批流程（支持评论和回复） ====================

    @ApiOperation(value = "楼主采纳（提交审批）- 支持评论和回复")
    @PostMapping("/article/adoptReply")
    public ResultBean adoptReply(@RequestBody Map<String, Object> params) {
        Integer replyId = toInt(params, "replyId");
        Integer commentId = toInt(params, "commentId");
        Integer articleId = toInt(params, "articleId");
        Integer userId = toInt(params, "userId");

        if ((replyId == null && commentId == null) || articleId == null || userId == null) {
            return ResultBean.error("参数不完整");
        }

        // 获取文章
        com.walker.pojo.Article article = articleService.queryArticleById(articleId);
        if (article == null) return ResultBean.error("文章不存在");

        // 校验：操作人必须是文章作者
        if (!userId.equals(article.getUserId())) return ResultBean.error("只有文章作者才能采纳");

        // 校验：帖子必须是"问题求助"标签
        if (article.getArticleLabelId() == null) return ResultBean.error("该帖子不是问题求助类型，无法采纳");
        ArticleLabel label = articleLabelService.getById(article.getArticleLabelId());
        if (label == null || !QUESTION_LABEL_NAME.equals(label.getLabelName())) {
            return ResultBean.error("该帖子不是问题求助类型，无法采纳");
        }

        // 校验：同一文章最多只能有一条采纳（待审批或已确认）
        boolean alreadyHasAdopt = hasPendingOrConfirmedAdopt(articleId);
        if (alreadyHasAdopt) return ResultBean.error("该文章已有采纳申请或最佳解答，不可重复采纳");

        // ---- 回复采纳 ----
        if (replyId != null) {
            Reply reply = replyService.getById(replyId);
            if (reply == null) return ResultBean.error("回复不存在");
            if (userId.equals(reply.getReplyUserId())) return ResultBean.error("不能采纳自己的回复");
            if (reply.getAdoptStatus() != null && reply.getAdoptStatus() != 0) {
                return ResultBean.error(adoptStatusMsg(reply.getAdoptStatus()));
            }
            reply.setAdoptStatus(1);
            replyService.updateById(reply);
            notifyModerators(article, replyId, "reply", userId);
            return ResultBean.success("采纳申请已提交，等待管理员审核");
        }

        // ---- 评论采纳 ----
        if (commentId != null) {
            Comment comment = commentService.getById(commentId);
            if (comment == null) return ResultBean.error("评论不存在");
            if (userId.equals(comment.getCommentUserId())) return ResultBean.error("不能采纳自己的评论");
            if (comment.getAdoptStatus() != null && comment.getAdoptStatus() != 0) {
                return ResultBean.error(adoptStatusMsg(comment.getAdoptStatus()));
            }
            comment.setAdoptStatus(1);
            commentService.updateById(comment);
            notifyModerators(article, commentId, "comment", userId);
            return ResultBean.success("采纳申请已提交，等待管理员审核");
        }

        return ResultBean.error("参数错误");
    }

    @ApiOperation(value = "管理员审批采纳 - 支持评论和回复")
    @PostMapping("/admin/approveAdopt")
    public ResultBean approveAdopt(@RequestBody Map<String, Object> params) {
        Integer replyId = toInt(params, "replyId");
        Integer commentId = toInt(params, "commentId");
        Integer articleId = toInt(params, "articleId");
        String action = (String) params.get("action");
        Integer adminId = toInt(params, "adminId");

        if ((replyId == null && commentId == null) || articleId == null || action == null || adminId == null) {
            return ResultBean.error("参数不完整");
        }
        if (!"confirm".equals(action) && !"reject".equals(action)) return ResultBean.error("操作类型无效");

        com.walker.pojo.Article article = articleService.queryArticleById(articleId);
        if (article == null) return ResultBean.error("文章不存在");

        // ---- 回复审批 ----
        if (replyId != null) {
            Reply reply = replyService.getById(replyId);
            if (reply == null) return ResultBean.error("回复不存在");
            if (reply.getAdoptStatus() == null || reply.getAdoptStatus() != 1) return ResultBean.error("该回复不在待审批状态");

            if ("confirm".equals(action)) {
                reply.setAdoptStatus(2);
                reply.setIsAdopted(1);
                replyService.updateById(reply);

                int adoptCount = pointsLogService.countAdoptPointsForArticle(reply.getReplyUserId(), articleId);
                if (adoptCount <= 0) {
                    pointsLogService.adjustUserPoints(reply.getReplyUserId(), ADOPT_POINTS, "最佳解答采纳积分",
                            "adopt", replyId, adminId);
                    String title = "恭喜！您的回复被采纳为最佳答案，获得+" + ADOPT_POINTS + "积分" + articleTitleSuffix(article);
                    notificationService.createNotification(reply.getReplyUserId(), adminId, "adopt", title, "reply", replyId);
                } else {
                    String title = "您的回复被采纳为最佳解答" + articleTitleSuffix(article) + "，但该帖已有最佳解答积分";
                    notificationService.createNotification(reply.getReplyUserId(), adminId, "adopt", title, "reply", replyId);
                }
                return ResultBean.success("确认采纳成功");
            } else {
                reply.setAdoptStatus(3);
                reply.setIsAdopted(0);
                replyService.updateById(reply);
                String title = "您在" + articleTitleSuffix(article) + "中的采纳申请已被拒绝";
                notificationService.createNotification(article.getUserId(), adminId, "adopt_rejected", title, "reply", replyId);
                return ResultBean.success("已拒绝");
            }
        }

        // ---- 评论审批 ----
        if (commentId != null) {
            Comment comment = commentService.getById(commentId);
            if (comment == null) return ResultBean.error("评论不存在");
            if (comment.getAdoptStatus() == null || comment.getAdoptStatus() != 1) return ResultBean.error("该评论不在待审批状态");

            if ("confirm".equals(action)) {
                comment.setAdoptStatus(2);
                commentService.updateById(comment);

                int adoptCount = pointsLogService.countAdoptPointsForArticle(comment.getCommentUserId(), articleId);
                if (adoptCount <= 0) {
                    pointsLogService.adjustUserPoints(comment.getCommentUserId(), ADOPT_POINTS, "最佳解答采纳积分",
                            "adopt", commentId, adminId);
                    String title = "恭喜！您的评论被采纳为最佳答案，获得+" + ADOPT_POINTS + "积分" + articleTitleSuffix(article);
                    notificationService.createNotification(comment.getCommentUserId(), adminId, "adopt", title, "comment", commentId);
                } else {
                    String title = "您的评论被采纳为最佳解答" + articleTitleSuffix(article) + "，但该帖已有最佳解答积分";
                    notificationService.createNotification(comment.getCommentUserId(), adminId, "adopt", title, "comment", commentId);
                }
                return ResultBean.success("确认采纳成功");
            } else {
                comment.setAdoptStatus(3);
                commentService.updateById(comment);
                String title = "您在" + articleTitleSuffix(article) + "中的采纳申请已被拒绝";
                notificationService.createNotification(article.getUserId(), adminId, "adopt_rejected", title, "comment", commentId);
                return ResultBean.success("已拒绝");
            }
        }

        return ResultBean.error("参数错误");
    }

    @ApiOperation(value = "获取待审批采纳列表（管理员，含评论和回复）")
    @PostMapping("/admin/pendingAdopts")
    public ResultBean pendingAdopts(@RequestBody Map<String, Object> params) {
        Integer page = toInt(params, "page");
        if (page == null) page = 1;
        Integer size = toInt(params, "size");
        if (size == null) size = 20;
        Integer adminId = toInt(params, "adminId");
        if (adminId == null) return ResultBean.error("参数不完整");

        boolean isSuperAdmin = adminId.equals(1);
        List<Map<String, Object>> allRecords = new ArrayList<>();

        // 1. 查询待审批的回复
        LambdaQueryWrapper<Reply> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(Reply::getAdoptStatus, 1).orderByDesc(Reply::getReplyId);
        List<Reply> pendingReplies = replyService.list(replyWrapper);
        for (Reply reply : pendingReplies) {
            Comment comment = commentService.getById(reply.getCommentId());
            if (comment == null) continue;
            com.walker.pojo.Article article = articleService.queryArticleById(comment.getCommentArticleId());
            if (article == null) continue;
            if (!isSuperAdmin && !boardModeratorService.isModerator(adminId, article.getArticleLabelId())) continue;

            com.walker.pojo.User replyUser = userService.queryUserinfoById(reply.getReplyUserId());
            com.walker.pojo.User articleUser = userService.queryUserinfoById(article.getUserId());
            ArticleLabel lb = article.getArticleLabelId() != null ? articleLabelService.getById(article.getArticleLabelId()) : null;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "reply");
            item.put("id", reply.getReplyId());
            item.put("articleId", article.getArticleId());
            item.put("articleTitle", article.getArticleTitle());
            item.put("content", reply.getReplyContent());
            item.put("authorId", reply.getReplyUserId());
            item.put("authorName", replyUser != null ? replyUser.getNickname() : "");
            item.put("articleAuthorId", article.getUserId());
            item.put("articleAuthorName", articleUser != null ? articleUser.getNickname() : "");
            item.put("time", reply.getReplyTime());
            item.put("labelName", lb != null ? lb.getLabelName() : "");
            allRecords.add(item);
        }

        // 2. 查询待审批的评论
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getAdoptStatus, 1).orderByDesc(Comment::getCommentId);
        List<Comment> pendingComments = commentService.list(commentWrapper);
        for (Comment comment : pendingComments) {
            com.walker.pojo.Article article = articleService.queryArticleById(comment.getCommentArticleId());
            if (article == null) continue;
            if (!isSuperAdmin && !boardModeratorService.isModerator(adminId, article.getArticleLabelId())) continue;

            com.walker.pojo.User commentUser = userService.queryUserinfoById(comment.getCommentUserId());
            com.walker.pojo.User articleUser = userService.queryUserinfoById(article.getUserId());
            ArticleLabel lb = article.getArticleLabelId() != null ? articleLabelService.getById(article.getArticleLabelId()) : null;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "comment");
            item.put("id", comment.getCommentId());
            item.put("articleId", article.getArticleId());
            item.put("articleTitle", article.getArticleTitle());
            item.put("content", comment.getCommentContent());
            item.put("authorId", comment.getCommentUserId());
            item.put("authorName", commentUser != null ? commentUser.getNickname() : "");
            item.put("articleAuthorId", article.getUserId());
            item.put("articleAuthorName", articleUser != null ? articleUser.getNickname() : "");
            item.put("time", comment.getCommentTime());
            item.put("labelName", lb != null ? lb.getLabelName() : "");
            allRecords.add(item);
        }

        // 按时间倒序排列，然后分页
        allRecords.sort((a, b) -> {
            String t1 = (String) a.get("time");
            String t2 = (String) b.get("time");
            if (t1 == null) t1 = "";
            if (t2 == null) t2 = "";
            return t2.compareTo(t1);
        });

        int total = allRecords.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Map<String, Object>> paged = from < total ? allRecords.subList(from, to) : new ArrayList<>();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", paged);
        data.put("total", total);
        return ResultBean.success("查询成功", data);
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查该文章是否已有待审批(1)或已确认(2)的采纳
     * 同一文章最多只能有一条采纳
     */
    private boolean hasPendingOrConfirmedAdopt(Integer articleId) {
        // 查 bbs_reply：通过 comment 找到属于该文章的回复
        LambdaQueryWrapper<Reply> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.in(Reply::getAdoptStatus, 1, 2);
        List<Reply> adoptReplies = replyService.list(replyWrapper);
        for (Reply r : adoptReplies) {
            Comment c = commentService.getById(r.getCommentId());
            if (c != null && articleId.equals(c.getCommentArticleId())) return true;
        }
        // 查 bbs_comment：直接按文章ID查
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getCommentArticleId, articleId)
                      .in(Comment::getAdoptStatus, 1, 2);
        return commentService.count(commentWrapper) > 0;
    }

    private void notifyModerators(com.walker.pojo.Article article, Integer relatedId, String relatedType, Integer fromUserId) {
        String articleTitle = article.getArticleTitle();
        String notifyTitle = "文章《" + (articleTitle != null ? articleTitle : "") + "》有待审批的最佳解答采纳";

        LambdaQueryWrapper<BoardModerator> modWrapper = new LambdaQueryWrapper<>();
        modWrapper.eq(BoardModerator::getLabelId, article.getArticleLabelId())
                  .eq(BoardModerator::getStatus, 1);
        List<BoardModerator> moderators = boardModeratorService.list(modWrapper);

        Set<Integer> notified = new HashSet<>();
        for (BoardModerator mod : moderators) {
            if (mod.getUserId() != null && !mod.getUserId().equals(fromUserId) && notified.add(mod.getUserId())) {
                notificationService.createNotification(mod.getUserId(), fromUserId,
                        "adopt_pending", notifyTitle, relatedType, relatedId);
            }
        }
        if (notified.add(1)) {
            notificationService.createNotification(1, fromUserId, "adopt_pending", notifyTitle, relatedType, relatedId);
        }
    }

    private String adoptStatusMsg(Integer status) {
        if (status == null) return "无法采纳";
        switch (status) {
            case 1: return "该内容已有待审批的采纳申请";
            case 2: return "该内容已被采纳";
            case 3: return "该内容的采纳申请已被拒绝";
            default: return "无法采纳";
        }
    }

    private String articleTitleSuffix(com.walker.pojo.Article article) {
        String t = article.getArticleTitle();
        return t != null ? "（《" + t + "》）" : "";
    }
}
