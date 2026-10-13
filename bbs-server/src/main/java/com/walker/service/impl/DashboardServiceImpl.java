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
import com.walker.pojo.Violation;
import com.walker.service.DashboardService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
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

    @Override
    public ResultBean getCounts() {
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

        return ResultBean.success("查询成功", counts);
    }
}
