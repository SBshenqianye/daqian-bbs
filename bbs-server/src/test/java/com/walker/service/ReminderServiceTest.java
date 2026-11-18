package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.ReminderLogMapper;
import com.walker.mapper.ReminderRuleMapper;
import com.walker.pojo.Article;
import com.walker.pojo.ReminderLog;
import com.walker.pojo.ReminderRule;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderServiceTest {

    @InjectMocks
    private ReminderService service;

    @Mock private ReminderRuleMapper ruleMapper;
    @Mock private ReminderLogMapper logMapper;
    @Mock private ArticleMapper articleMapper;
    @Mock private AuthorizationService authorizationService;
    @Mock private NotificationService notificationService;
    @Mock private ContentBoardResolver contentBoardResolver;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Article.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ReminderRule.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ReminderLog.class);
    }

    @Test
    @DisplayName("免打扰：普通时段与跨午夜时段均正确判断")
    void quietHours() {
        assertFalse(ReminderService.inQuietHours(null, null, LocalTime.of(12, 0)));
        assertTrue(ReminderService.inQuietHours("12:00", "14:00", LocalTime.of(13, 0)));
        assertFalse(ReminderService.inQuietHours("12:00", "14:00", LocalTime.of(15, 0)));
        // 跨午夜 22:00 - 08:00
        assertTrue(ReminderService.inQuietHours("22:00", "08:00", LocalTime.of(23, 30)));
        assertTrue(ReminderService.inQuietHours("22:00", "08:00", LocalTime.of(7, 30)));
        assertFalse(ReminderService.inQuietHours("22:00", "08:00", LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("延迟提醒：未到延迟时间不提醒，到达后提醒")
    void delayReached() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 12, 0);
        assertFalse(ReminderService.isMatured("2026-11-18 11:55:00", 10, now));
        assertTrue(ReminderService.isMatured("2026-11-18 11:50:00", 10, now));
        assertTrue(ReminderService.isMatured(null, 10, now));
    }

    @Test
    @DisplayName("按板块范围：只提醒能管理该板块的版主")
    void scan_boardScope_onlyNotifiesModeratorOfBoard() {
        ReminderRule rule = new ReminderRule();
        rule.setId(1);
        rule.setRuleCode("article_pending");
        rule.setRuleName("待审核帖子");
        rule.setEventType("article_pending");
        rule.setPermissionCode("article:review");
        rule.setScopeType("BOARD");
        rule.setEnabled(1);
        rule.setDelayMinutes(0);
        rule.setRepeatMinutes(60);
        rule.setMaxPerDay(8);
        when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(rule));

        Article article = new Article();
        article.setArticleId(100);
        article.setArticleLabelId(5);
        article.setEnable(0);
        article.setCreateTime("2026-11-18 10:00:00");
        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(article));

        when(authorizationService.getUserIdsWithPermission(any())).thenReturn(Arrays.asList(2, 3));
        when(authorizationService.canModerateBoard(eq(2), eq(5))).thenReturn(true);
        when(authorizationService.canModerateBoard(eq(3), eq(5))).thenReturn(false);
        when(logMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(logMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        service.scan();

        verify(notificationService).createNotification(eq(2), isNull(), eq("reminder_pending"),
                any(String.class), eq("reminder"), eq(1));
        verify(notificationService, never()).createNotification(eq(3), isNull(), eq("reminder_pending"),
                any(String.class), eq("reminder"), eq(1));
    }

    @Test
    @DisplayName("每日上限：达到上限后不再提醒")
    void scan_maxPerDay_blocksFurtherReminders() {
        ReminderRule rule = new ReminderRule();
        rule.setId(1);
        rule.setRuleCode("article_pending");
        rule.setRuleName("待审核帖子");
        rule.setEventType("article_pending");
        rule.setPermissionCode("article:review");
        rule.setScopeType("ALL");
        rule.setEnabled(1);
        rule.setDelayMinutes(0);
        rule.setRepeatMinutes(60);
        rule.setMaxPerDay(1);
        when(ruleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(rule));

        Article article = new Article();
        article.setArticleId(100);
        article.setArticleLabelId(5);
        article.setCreateTime("2026-11-18 10:00:00");
        when(articleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(article));

        when(authorizationService.getUserIdsWithPermission(any())).thenReturn(Collections.singletonList(2));
        when(logMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        service.scan();

        verify(notificationService, never()).createNotification(anyInt(), any(), any(String.class),
                any(String.class), any(String.class), anyInt());
    }
}
