package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.mapper.ModeratorRewardCancelMapper;
import com.walker.pojo.BoardModerator;
import com.walker.service.impl.BoardModeratorServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardModeratorServiceTest {

    @InjectMocks
    private BoardModeratorServiceImpl boardModeratorService;

    @Mock
    private BoardModeratorMapper boardModeratorMapper;

    @Mock
    private ModeratorRewardCancelMapper cancelMapper;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DictService dictService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapperField = boardModeratorService.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(boardModeratorService, boardModeratorMapper);
    }

    @Test
    @DisplayName("任命版主 → 参数不完整 → 返回错误")
    void appoint_missingParams_returnsError() {
        ResultBean result = boardModeratorService.appoint(null, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("任命版主 → 已是版主 → 返回错误")
    void appoint_alreadyModerator_returnsError() {
        BoardModerator existing = new BoardModerator();
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(existing);
        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("任命版主 → 积分不足300 → 返回错误")
    void appoint_insufficientPoints_returnsError() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(1)).thenReturn(200);
        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("任命版主 → 正常任命成功")
    void appoint_valid_succeeds() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(1)).thenReturn(300);
        when(boardModeratorMapper.insert(any(BoardModerator.class))).thenReturn(1);

        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("撤销版主 → 参数不完整 → 返回错误")
    void dismiss_missingParams_returnsError() {
        ResultBean result = boardModeratorService.dismiss(null, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销版主 → 不是版主 → 返回错误")
    void dismiss_notModerator_returnsError() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        ResultBean result = boardModeratorService.dismiss(1, 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("撤销版主 → 正常撤销")
    void dismiss_valid_succeeds() {
        BoardModerator mod = new BoardModerator();
        mod.setId(1);
        mod.setStatus(1);
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(mod);
        when(boardModeratorMapper.updateById(any(BoardModerator.class))).thenReturn(1);

        ResultBean result = boardModeratorService.dismiss(1, 1);
        assertEquals(200, result.getCode());
    }

    @Test
    @DisplayName("判断是否版主 → 是 → 返回true")
    void isModerator_is_true() {
        BoardModerator mod = new BoardModerator();
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(mod);
        assertTrue(boardModeratorService.isModerator(1, 1));
    }

    @Test
    @DisplayName("判断是否版主 → 否 → 返回false")
    void isModerator_isNot_false() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        assertFalse(boardModeratorService.isModerator(1, 1));
    }

    @Test
    @DisplayName("月度奖励 → 无有效版主 → 返回提示")
    void monthlyReward_noModerators_returnsEmpty() {
        when(boardModeratorMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        ResultBean result = boardModeratorService.monthlyReward(1);
        assertEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("当前无有效版主"));
    }

    @Test
    @DisplayName("取消奖励 → userId为空 → 返回错误")
    void cancelReward_nullUserId_returnsError() {
        ResultBean result = boardModeratorService.cancelReward(null, 1, "备注");
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("恢复奖励 → userId为空 → 返回错误")
    void restoreReward_nullUserId_returnsError() {
        ResultBean result = boardModeratorService.restoreReward(null);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("恢复奖励 → 未被取消 → 返回错误")
    void restoreReward_notCancelled_returnsError() {
        when(cancelMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        ResultBean result = boardModeratorService.restoreReward(1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("恢复奖励 → 已被取消 → 成功恢复")
    void restoreReward_cancelled_succeeds() {
        when(cancelMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        ResultBean result = boardModeratorService.restoreReward(1);
        assertEquals(200, result.getCode());
    }
}
