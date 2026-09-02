package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.Article;
import com.walker.pojo.Report;
import com.walker.mapper.ReportMapper;
import com.walker.service.impl.ReportServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReportService 单元测试（Mockito，不启动 Spring 容器）
 *
 * 覆盖 submitReport 的 5 个边界场景：
 * 1. 参数不完整
 * 2. 不能举报自己发布的内容
 * 3. 该内容已被核实处理（confirmed），不再接受举报
 * 4. 单人单日举报频率限制
 * 5. 同一内容重复举报（pending 期间）
 * 6. 正常提交举报成功
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ArticleService articleService;

    @Mock
    private CommentService commentService;

    @Mock
    private ReplyService replyService;

    @Mock
    private UserService userService;

    /** 公共测试参数 */
    private static final int REPORTER_ID = 100;
    private static final String TARGET_TYPE = "article";
    private static final int TARGET_ID = 1;

    @BeforeEach
    void setUp() {
        // Mockito annotations already initialized by @ExtendWith
    }

    // ========== submitReport 测试 ==========

    @Test
    @DisplayName("参数不完整 → 返回错误")
    void submitReport_missingParams_returnsError() {
        // reporterId 为 null
        ResultBean result = reportService.submitReport(null, TARGET_TYPE, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());
        assertEquals("参数不完整", result.getMessage());

        // targetType 为 null
        result = reportService.submitReport(REPORTER_ID, null, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());

        // targetId 为 null
        result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, null, "spam", "理由");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("不能举报自己发布的内容 → 返回错误")
    void submitReport_selfReport_returnsError() {
        // 模拟：文章作者就是举报人自己
        Article article = new Article();
        article.setUserId(REPORTER_ID);  // 作者 == 举报人
        when(articleService.getById(TARGET_ID)).thenReturn(article);

        ResultBean result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());
        assertEquals("不能举报自己发布的内容", result.getMessage());
    }

    @Test
    @DisplayName("该内容已被核实处理（confirmed）→ 不再接受举报")
    void submitReport_alreadyConfirmed_returnsError() {
        // 模拟：文章作者是别人（通过自举报检查）
        Article article = new Article();
        article.setUserId(999);
        when(articleService.getById(TARGET_ID)).thenReturn(article);

        // 模拟：已存在 confirmed 记录
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(1L)   // confirmedCount = 1（第一个 selectCount 调用）
                .thenReturn(0L);  // todayCount（不会到这里，但防万一）

        ResultBean result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());
        assertEquals("该内容已被核实处理，无需重复举报", result.getMessage());
    }

    @Test
    @DisplayName("单人单日举报达到上限 → 返回错误")
    void submitReport_dailyLimit_returnsError() {
        // 模拟：文章作者是别人
        Article article = new Article();
        article.setUserId(999);
        when(articleService.getById(TARGET_ID)).thenReturn(article);

        // 模拟：无 confirmed 记录（通过第一个检查），但今日已达上限
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)   // confirmedCount = 0
                .thenReturn(10L); // todayCount = 10（达到上限）

        ResultBean result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());
        assertEquals("举报过于频繁，请明天再试", result.getMessage());
    }

    @Test
    @DisplayName("同一内容重复举报（pending 期间）→ 返回错误")
    void submitReport_duplicatePending_returnsError() {
        // 模拟：文章作者是别人
        Article article = new Article();
        article.setUserId(999);
        when(articleService.getById(TARGET_ID)).thenReturn(article);

        // 模拟：无 confirmed、今日未超限、但已有 pending 记录
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)   // confirmedCount = 0
                .thenReturn(3L)   // todayCount = 3（未超限）
                .thenReturn(1L);  // 已有 pending 记录

        ResultBean result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, TARGET_ID, "spam", "理由");
        assertEquals(500, result.getCode());
        assertEquals("您已举报过该内容，请等待审核", result.getMessage());
    }

    @Test
    @DisplayName("正常提交举报 → 保存记录并通知超管")
    void submitReport_validReport_succeeds() {
        // 模拟：文章作者是别人
        Article article = new Article();
        article.setUserId(999);
        when(articleService.getById(TARGET_ID)).thenReturn(article);

        // 模拟：无 confirmed(0)、今日未超限(0)、无重复 pending(0)、保存后 pendingCount=1
        // 注意：不能分两次 when()，后者会覆盖前者
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L)   // confirmedCount
                .thenReturn(0L)   // todayCount
                .thenReturn(0L)   // existing pending
                .thenReturn(1L);  // pendingCount after save（通知超管）

        // 模拟：保存成功
        when(reportMapper.insert(any(Report.class))).thenReturn(1);

        ResultBean result = reportService.submitReport(REPORTER_ID, TARGET_TYPE, TARGET_ID, "spam", "测试举报理由");
        assertEquals(200, result.getCode());
        assertEquals("举报已提交，等待审核", result.getMessage());

        // 验证：确实保存了记录
        verify(reportMapper, atLeastOnce()).insert(any(Report.class));
    }
}
