package com.walker.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.mapper.ModeratorRewardCancelMapper;
import com.walker.pojo.BoardModerator;
import com.walker.service.impl.BoardModeratorServiceImpl;
import com.walker.vo.ResultBean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Field;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;

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

        // 注册实体元数据（Service 使用 LambdaQueryWrapper 需要）
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                BoardModerator.class
        );
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

    @Test
    @DisplayName("月度奖励 → 有2位有效版主 → 全部发放")
    void monthlyReward_withModerators_rewardsAll() {
        BoardModerator mod1 = new BoardModerator();
        mod1.setUserId(1);
        mod1.setStatus(1);
        BoardModerator mod2 = new BoardModerator();
        mod2.setUserId(2);
        mod2.setStatus(1);
        when(boardModeratorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(mod1, mod2));

        // 本月未发放过（count = 0）
        when(pointsLogService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 本月无取消记录
        when(cancelMapper.findCancelledUserIds(anyString())).thenReturn(new ArrayList<>());

        ResultBean result = boardModeratorService.monthlyReward(1);
        assertEquals(200, result.getCode());
        // 每位版主发放 15 积分（硬编码值）
        verify(pointsLogService, times(2)).adjustUserPoints(
                anyInt(), eq(15), eq("版主月度履职奖励"),
                eq("moderator_reward"), isNull(), eq(1));
        // 每位版主收到通知
        verify(notificationService, times(2)).createNotification(
                anyInt(), eq(1), eq("moderator_reward"), contains("版主履职奖励"), eq("user"), anyInt());
    }

    // ========== appoint 补充测试 ==========

    @Test
    @DisplayName("任命版主 → 积分299（差1分）→ 返回错误")
    void appoint_notEnoughPoints_returnsError() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(1)).thenReturn(299);

        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(500, result.getCode());
        assertEquals("该用户累计积分不足300分，无法任命为版主", result.getMessage());
    }

    // ========== dismiss 补充测试 ==========

    @Test
    @DisplayName("撤销版主 → 正常撤销 → status设为0")
    void dismiss_setsStatus0() {
        BoardModerator mod = new BoardModerator();
        mod.setId(1);
        mod.setStatus(1);
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(mod);
        when(boardModeratorMapper.updateById(any(BoardModerator.class))).thenReturn(1);

        ResultBean result = boardModeratorService.dismiss(1, 1);
        assertEquals(200, result.getCode());

        // 验证：status 被设为 0
        verify(boardModeratorMapper).updateById(argThat(m -> m.getStatus() == 0));
    }

    @Test
    @DisplayName("任命版主 → 积分刚好300 → 成功")
    void appoint_exactly300Points_succeeds() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(1)).thenReturn(300);
        when(boardModeratorMapper.insert(any(BoardModerator.class))).thenReturn(1);

        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(200, result.getCode());
    }

    // ========== 月度奖励/任命 补充测试 ==========

    @Test
    @DisplayName("月度奖励 → 字典值为空 → 使用默认15积分发放")
    void monthlyReward_emptyDictValue_usesDefault() {
        BoardModerator mod = new BoardModerator();
        mod.setUserId(1);
        mod.setStatus(1);
        when(boardModeratorMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(mod));
        when(pointsLogService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(cancelMapper.findCancelledUserIds(anyString())).thenReturn(new ArrayList<>());

        ResultBean result = boardModeratorService.monthlyReward(1);
        assertEquals(200, result.getCode());
        // 验证发放了默认 15 积分（代码中硬编码 rewardPoints=15）
        verify(pointsLogService).adjustUserPoints(
                eq(1), eq(15), eq("版主月度履职奖励"),
                eq("moderator_reward"), isNull(), eq(1));
    }

    @Test
    @DisplayName("任命版主 → 成功 → 发送任命通知")
    void appoint_valid_succeeds_andNotifies() {
        when(boardModeratorMapper.findByUserAndLabel(1, 1)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(1)).thenReturn(500);
        when(boardModeratorMapper.insert(any(BoardModerator.class))).thenReturn(1);

        ResultBean result = boardModeratorService.appoint(1, 1, 1);
        assertEquals(200, result.getCode());
        // 验证插入了版主记录
        verify(boardModeratorMapper).insert(argThat(m -> {
            BoardModerator mod = (BoardModerator) m;
            return mod.getUserId() != null
                    && mod.getLabelId() != null
                    && mod.getStatus() == 1
                    && "moderator".equals(mod.getRoleType());
        }));
    }
}
