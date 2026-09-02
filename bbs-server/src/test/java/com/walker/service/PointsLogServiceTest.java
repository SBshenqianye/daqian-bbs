package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.mapper.PointsLogMapper;
import com.walker.pojo.PointsLog;
import com.walker.service.impl.PointsLogServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsLogServiceTest {

    @InjectMocks
    private PointsLogServiceImpl pointsLogService;

    @Mock
    private PointsLogMapper pointsLogMapper;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = pointsLogService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(pointsLogService, pointsLogMapper);
    }

    // ========== addPointsLog ==========

    @Test
    @DisplayName("新增积分记录 → 参数不完整 → 返回错误")
    void addPointsLog_missingParams_returnsError() {
        PointsLog log = new PointsLog();
        log.setUserId(null);
        log.setPointsChange(null);

        ResultBean result = pointsLogService.addPointsLog(log);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("新增积分记录 → 正常保存")
    void addPointsLog_valid_succeeds() {
        PointsLog log = new PointsLog();
        log.setUserId(1);
        log.setPointsChange(10);

        when(pointsLogMapper.insert(any(PointsLog.class))).thenReturn(1);

        ResultBean result = pointsLogService.addPointsLog(log);
        assertEquals(200, result.getCode());
    }

    // ========== adjustUserPoints ==========

    @Test
    @DisplayName("调整积分 → 参数不完整 → 返回错误")
    void adjustUserPoints_missingParams_returnsError() {
        ResultBean result = pointsLogService.adjustUserPoints(null, 10, "reason", null, null, null);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("调整积分 → 正常加分")
    void adjustUserPoints_positivePoints_succeeds() {
        when(pointsLogMapper.insert(any(PointsLog.class))).thenReturn(1);

        ResultBean result = pointsLogService.adjustUserPoints(1, 5, "发帖积分", "article", 1, null);
        assertEquals(200, result.getCode());
    }

    // ========== undoPointsLog ==========

    @Test
    @DisplayName("撤销积分 → 记录ID为空 → 返回错误")
    void undoPointsLog_nullId_returnsError() {
        ResultBean result = pointsLogService.undoPointsLog(null, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销积分 → 记录不存在 → 返回错误")
    void undoPointsLog_notFound_returnsError() {
        when(pointsLogMapper.selectById(999)).thenReturn(null);

        ResultBean result = pointsLogService.undoPointsLog(999, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销积分 → 已被撤销 → 返回错误")
    void undoPointsLog_alreadyReversed_returnsError() {
        PointsLog original = new PointsLog();
        original.setId(1);
        original.setIsReversed(1);
        when(pointsLogMapper.selectById(1)).thenReturn(original);

        ResultBean result = pointsLogService.undoPointsLog(1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销积分 → 撤销记录本身 → 返回错误")
    void undoPointsLog_undoRecord_returnsError() {
        PointsLog original = new PointsLog();
        original.setId(1);
        original.setRelatedType("undo");
        original.setIsReversed(0);
        when(pointsLogMapper.selectById(1)).thenReturn(original);

        ResultBean result = pointsLogService.undoPointsLog(1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销积分 → 正常撤销 → 创建反向记录并标记原记录")
    void undoPointsLog_valid_succeeds() {
        PointsLog original = new PointsLog();
        original.setId(1);
        original.setUserId(10);
        original.setPointsChange(5);
        original.setRelatedType("article");
        original.setIsReversed(0);
        when(pointsLogMapper.selectById(1)).thenReturn(original);
        when(pointsLogMapper.insert(any(PointsLog.class))).thenReturn(1);
        when(pointsLogMapper.updateById(any(PointsLog.class))).thenReturn(1);

        ResultBean result = pointsLogService.undoPointsLog(1, 1);
        assertEquals(200, result.getCode());
        verify(pointsLogMapper).insert(argThat(log -> log.getPointsChange() == -5 && "undo".equals(log.getRelatedType())));
        verify(pointsLogMapper).updateById(argThat(log -> log.getIsReversed() == 1));
    }

    // ========== countReplyPointsForArticle ==========

    @Test
    @DisplayName("统计回帖积分次数 → 参数为空 → 返回0")
    void countReplyPoints_nullParams_returnsZero() {
        assertEquals(0, pointsLogService.countReplyPointsForArticle(null, 1));
        assertEquals(0, pointsLogService.countReplyPointsForArticle(1, null));
    }

    @Test
    @DisplayName("统计回帖积分次数 → 正常返回")
    void countReplyPoints_valid_returnsCount() {
        when(pointsLogMapper.countReplyPointsForArticle(1, 10)).thenReturn(2);
        assertEquals(2, pointsLogService.countReplyPointsForArticle(1, 10));
    }

    @Test
    @DisplayName("统计回帖积分次数 → mapper返回null → 返回0")
    void countReplyPoints_nullResult_returnsZero() {
        when(pointsLogMapper.countReplyPointsForArticle(1, 10)).thenReturn(null);
        assertEquals(0, pointsLogService.countReplyPointsForArticle(1, 10));
    }

    // ========== countAdoptPointsForArticle ==========

    @Test
    @DisplayName("统计采纳积分次数 → 参数为空 → 返回0")
    void countAdoptPoints_nullParams_returnsZero() {
        assertEquals(0, pointsLogService.countAdoptPointsForArticle(null, 1));
        assertEquals(0, pointsLogService.countAdoptPointsForArticle(1, null));
    }

    // ========== countSuggestionAdoptForArticle ==========

    @Test
    @DisplayName("统计建议采纳次数 → 参数为空 → 返回0")
    void countSuggestionAdopt_nullParams_returnsZero() {
        assertEquals(0, pointsLogService.countSuggestionAdoptForArticle(null));
    }

    // ========== getPointsAdjustment ==========

    @Test
    @DisplayName("查询用户积分调整总和 → 返回结果")
    void getPointsAdjustment_returnsSum() {
        when(pointsLogMapper.sumPointsChangeByUserId(1)).thenReturn(50);
        assertEquals(50, pointsLogService.getPointsAdjustment(1));
    }
}
