package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import com.walker.service.impl.DashboardServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 管理端仪表盘统计测试（#7）。
 * 2026-10-13（#6/#7）：新增 版主投诉 / 采纳审批 / 精华审批 三项待办计数的口径验证。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private ViolationMapper violationMapper;

    @Mock
    private AppealMapper appealMapper;

    @Mock
    private ModeratorComplaintMapper moderatorComplaintMapper;

    @Mock
    private FeaturedRecommendationMapper featuredRecommendationMapper;

    @Mock
    private ReplyMapper replyMapper;

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 解析方法引用需要实体 TableInfo（lambda 缓存），先初始化全部相关实体
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Article.class);
        TableInfoHelper.initTableInfo(assistant, Comment.class);
        TableInfoHelper.initTableInfo(assistant, Report.class);
        TableInfoHelper.initTableInfo(assistant, Violation.class);
        TableInfoHelper.initTableInfo(assistant, Appeal.class);
        TableInfoHelper.initTableInfo(assistant, Reply.class);
        TableInfoHelper.initTableInfo(assistant, ModeratorComplaint.class);
        TableInfoHelper.initTableInfo(assistant, FeaturedRecommendation.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    @DisplayName("统计 → 返回全部计数且新字段口径正确")
    void getCounts_returnsAllPendingCounts() {
        // 存量（selectCount(null)）
        when(userMapper.selectCount(isNull())).thenReturn(100L);
        when(articleMapper.selectCount(isNull())).thenReturn(50L);
        when(commentMapper.selectCount(isNull())).thenReturn(200L);
        // 帖子：待审（enable=0）与精华总数（is_featured=1）
        when(articleMapper.selectCount(argThat(w -> w != null && String.valueOf(w.getSqlSegment()).contains("enable")))).thenReturn(3L);
        when(articleMapper.selectCount(argThat(w -> w != null && String.valueOf(w.getSqlSegment()).contains("is_featured")))).thenReturn(4L);
        // 评论：采纳待审（adopt_status=1）
        when(commentMapper.selectCount(argThat(w -> w != null && String.valueOf(w.getSqlSegment()).contains("adopt_status")))).thenReturn(7L);
        // 各待办（status 条件，mapper 互不相同，无歧义）
        when(reportMapper.selectCount(notNull())).thenReturn(2L);
        when(violationMapper.selectCount(notNull())).thenReturn(1L);
        when(appealMapper.selectCount(notNull())).thenReturn(6L);
        when(moderatorComplaintMapper.selectCount(notNull())).thenReturn(8L);
        when(featuredRecommendationMapper.selectCount(notNull())).thenReturn(9L);
        // 回复：采纳待审（adopt_status=1）
        when(replyMapper.selectCount(notNull())).thenReturn(5L);

        ResultBean result = dashboardService.getCounts();
        assertEquals(200, result.getCode());

        Map<String, Object> counts = (Map<String, Object>) result.getObj();
        assertNotNull(counts);

        // 存量
        assertEquals(100L, counts.get("userCount"));
        assertEquals(50L, counts.get("articleCount"));
        assertEquals(200L, counts.get("commentCount"));
        // 原有待办
        assertEquals(2L, counts.get("reportPending"));
        assertEquals(6L, counts.get("appealPending"));
        assertEquals(1L, counts.get("violationActive"));
        assertEquals(3L, counts.get("articlePending"));
        assertEquals(4L, counts.get("featuredCount"));
        // 新增待办（2026-10-13 #6/#7）
        assertEquals(8L, counts.get("moderatorComplaintPending"));
        assertEquals(9L, counts.get("featuredPending"));
        // 采纳待审 = 回复 + 评论之和
        assertEquals(12L, counts.get("adoptPending"));
    }
}
