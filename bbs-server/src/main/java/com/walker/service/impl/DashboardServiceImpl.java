package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.AppealMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ReportMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.Reply;
import com.walker.pojo.Report;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.service.AuthorizationService;
import com.walker.service.ContentBoardResolver;
import com.walker.service.CurrentUserService;
import com.walker.service.DashboardService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端仪表盘统计实现（#7）。
 * 全部使用 MyBatis-Plus 条件 count，MySQL / PostgreSQL 双库天然兼容，无需原生 SQL 迁移。
 * 2026-10-13（#6/#7）：新增 版主投诉 / 采纳审批 / 精华审批 三项待办计数，供侧栏红点与首页浮动快捷栏共用。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private ModeratorComplaintMapper moderatorComplaintMapper;

    @Autowired
    private FeaturedRecommendationMapper featuredRecommendationMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private ContentBoardResolver contentBoardResolver;

    @Override
    public ResultBean getCounts() {
        User currentUser = currentUserService.getCurrentUser();
        List<Integer> boardIds = authorizationService.getVisibleBoardIds(currentUser);
        if (boardIds == null) {
            return ResultBean.success("查询成功", globalCounts());
        }
        if (boardIds.isEmpty()) {
            return ResultBean.success("查询成功", emptyCounts());
        }
        return ResultBean.success("查询成功", boardCounts(boardIds));
    }

    /** 全局管理员口径（保持原有行为） */
    private Map<String, Object> globalCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();

        // 存量（@TableLogic 自动排除已删除）
        counts.put("userCount", userMapper.selectCount(null));
        counts.put("articleCount", articleMapper.selectCount(null));
        counts.put("commentCount", commentMapper.selectCount(null));

        // 待办
        counts.put("reportPending", reportMapper.selectCount(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, "pending")));
        counts.put("appealPending", appealMapper.selectCount(
                new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "pending")));
        // 进行中违规（已取消 cancelled 不计入）
        counts.put("violationActive", violationMapper.selectCount(
                new LambdaQueryWrapper<Violation>().eq(Violation::getStatus, "active")));
        // 待审核帖子（enable=0），与帖子管理"未审核"Tab 口径一致
        counts.put("articlePending", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getEnable, 0)));
        // 精华帖
        counts.put("featuredCount", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getIsFeatured, 1)));

        // 2026-10-13（#6/#7）：新增待办计数，供侧栏红点与首页浮动快捷栏共用
        // 版主投诉待审（status=pending）
        counts.put("moderatorComplaintPending", moderatorComplaintMapper.selectCount(
                new LambdaQueryWrapper<ModeratorComplaint>().eq(ModeratorComplaint::getStatus, "pending")));
        // 精华审批待审（status=pending）
        counts.put("featuredPending", featuredRecommendationMapper.selectCount(
                new LambdaQueryWrapper<FeaturedRecommendation>().eq(FeaturedRecommendation::getStatus, "pending")));
        // 采纳审批待审：回复与评论 adoptStatus=1 之和（与 /reply/admin/pendingAdopts 口径一致，全量口径不按版主过滤）
        Long adoptReplyPending = replyMapper.selectCount(
                new LambdaQueryWrapper<Reply>().eq(Reply::getAdoptStatus, 1));
        Long adoptCommentPending = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getAdoptStatus, 1));
        counts.put("adoptPending", adoptReplyPending + adoptCommentPending);

        return counts;
    }

    /** 版主尚未分配板块：业务计数全部为 0，全局用户/评论指标也置 0 */
    private Map<String, Object> emptyCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("userCount", 0L);
        counts.put("articleCount", 0L);
        counts.put("commentCount", 0L);
        counts.put("reportPending", 0L);
        counts.put("appealPending", 0L);
        counts.put("violationActive", 0L);
        counts.put("articlePending", 0L);
        counts.put("featuredCount", 0L);
        counts.put("moderatorComplaintPending", 0L);
        counts.put("featuredPending", 0L);
        counts.put("adoptPending", 0L);
        return counts;
    }

    /** MODERATOR 口径：只统计自己的板块；全局用户/评论指标置 0（前端隐藏） */
    private Map<String, Object> boardCounts(List<Integer> boardIds) {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("userCount", 0L);
        counts.put("commentCount", 0L);
        counts.put("moderatorComplaintPending", 0L);

        counts.put("articleCount", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().in(Article::getArticleLabelId, boardIds)));
        counts.put("articlePending", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 0)
                        .in(Article::getArticleLabelId, boardIds)));
        counts.put("featuredCount", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getIsFeatured, 1)
                        .in(Article::getArticleLabelId, boardIds)));

        long reportPending = 0;
        for (Report report : reportMapper.selectList(
                new LambdaQueryWrapper<Report>().eq(Report::getStatus, "pending"))) {
            Integer labelId = contentBoardResolver.resolveLabelId(report.getTargetType(), report.getTargetId());
            if (labelId != null && boardIds.contains(labelId)) reportPending++;
        }
        counts.put("reportPending", reportPending);

        long violationActive = 0;
        for (Violation violation : violationMapper.selectList(
                new LambdaQueryWrapper<Violation>().eq(Violation::getStatus, "active"))) {
            Integer labelId = contentBoardResolver.resolveLabelId(violation.getRelatedType(), violation.getRelatedId());
            if (labelId != null && boardIds.contains(labelId)) violationActive++;
        }
        counts.put("violationActive", violationActive);

        long appealPending = 0;
        for (Appeal appeal : appealMapper.selectList(
                new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "pending"))) {
            Integer labelId = resolveAppealLabelId(appeal);
            if (labelId != null && boardIds.contains(labelId)) appealPending++;
        }
        counts.put("appealPending", appealPending);

        long featuredPending = 0;
        for (FeaturedRecommendation recommendation : featuredRecommendationMapper.selectList(
                new LambdaQueryWrapper<FeaturedRecommendation>()
                        .eq(FeaturedRecommendation::getStatus, "pending"))) {
            Integer labelId = resolveFeaturedLabelId(recommendation);
            if (labelId != null && boardIds.contains(labelId)) featuredPending++;
        }
        counts.put("featuredPending", featuredPending);

        long adoptPending = 0;
        for (Reply reply : replyMapper.selectList(
                new LambdaQueryWrapper<Reply>().eq(Reply::getAdoptStatus, 1))) {
            Integer labelId = contentBoardResolver.resolveLabelId("reply", reply.getReplyId());
            if (labelId != null && boardIds.contains(labelId)) adoptPending++;
        }
        for (Comment comment : commentMapper.selectList(
                new LambdaQueryWrapper<Comment>().eq(Comment::getAdoptStatus, 1))) {
            Integer labelId = contentBoardResolver.resolveLabelId("comment", comment.getCommentId());
            if (labelId != null && boardIds.contains(labelId)) adoptPending++;
        }
        counts.put("adoptPending", adoptPending);

        return counts;
    }

    private Integer resolveAppealLabelId(Appeal appeal) {
        if (appeal == null || !"violation".equals(appeal.getAppealType()) || appeal.getRelatedId() == null) {
            return null;
        }
        Violation violation = violationMapper.selectById(appeal.getRelatedId());
        if (violation == null) {
            return null;
        }
        return contentBoardResolver.resolveLabelId(violation.getRelatedType(), violation.getRelatedId());
    }

    private Integer resolveFeaturedLabelId(FeaturedRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        if (recommendation.getLabelId() != null && recommendation.getLabelId() > 0) {
            return recommendation.getLabelId();
        }
        Article article = articleMapper.selectById(recommendation.getArticleId());
        return article != null ? article.getArticleLabelId() : null;
    }
}
