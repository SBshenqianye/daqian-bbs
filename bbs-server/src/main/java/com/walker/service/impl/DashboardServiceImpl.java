package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.AppealMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.ReportMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
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

        return ResultBean.success("查询成功", counts);
    }
}
