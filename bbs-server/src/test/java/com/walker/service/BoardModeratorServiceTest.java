package com.walker.service;

import com.walker.mapper.BoardModeratorMapper;
import com.walker.pojo.BoardModerator;
import com.walker.service.impl.BoardModeratorServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 版主生命周期：撤销 → 重新任命。
 *
 * <p>对应设计承诺「撤销版主后可重新任命」（权限架构重构 §11）。
 * 关键点：撤销只把 status 置 0，重新任命时必须复用同一条历史记录，
 * 否则会撞唯一键 uk_board_mod_user_label。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardModeratorServiceTest {

    @InjectMocks
    private BoardModeratorServiceImpl service;

    @Mock
    private BoardModeratorMapper boardModeratorMapper;

    @Mock
    private PointsLogService pointsLogService;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() throws Exception {
        Field baseMapper = service.getClass().getSuperclass().getDeclaredField("baseMapper");
        baseMapper.setAccessible(true);
        baseMapper.set(service, boardModeratorMapper);
        // 操作人=登录态用户 1
        when(currentUserService.getCurrentUserId()).thenReturn(1);
    }

    private BoardModerator moderator(int id, int userId, int labelId, int status) {
        BoardModerator m = new BoardModerator();
        m.setId(id);
        m.setUserId(userId);
        m.setLabelId(labelId);
        m.setStatus(status);
        m.setRoleType("moderator");
        return m;
    }

    @Test
    @DisplayName("撤销版主：status 置 0，不物理删除")
    void dismiss_setsStatusToZero() {
        BoardModerator active = moderator(77, 5, 10, 1);
        when(boardModeratorMapper.findByUserAndLabel(5, 10)).thenReturn(active);
        when(boardModeratorMapper.updateById(any(BoardModerator.class))).thenReturn(1);

        ResultBean<?> result = service.dismiss(5, 10);

        assertEquals(200L, result.getCode());
        assertEquals(0, active.getStatus());
        verify(boardModeratorMapper).updateById(active);
        verify(boardModeratorMapper, never()).deleteById(any(Integer.class));
    }

    @Test
    @DisplayName("撤销后可重新任命：复用 status=0 的历史记录，不再 insert 新行")
    void reappointAfterDismiss_reusesHistoryRow() {
        BoardModerator history = moderator(77, 5, 10, 0);
        when(boardModeratorMapper.findByUserAndLabel(5, 10)).thenReturn(null);   // 当前无有效任职
        when(pointsLogService.getPointsAdjustment(5)).thenReturn(300);           // 满足 300 分门槛
        when(boardModeratorMapper.findAnyByUserAndLabel(5, 10)).thenReturn(history);
        when(boardModeratorMapper.updateById(any(BoardModerator.class))).thenReturn(1);
        when(userService.getById(5)).thenReturn(null);                          // 账号升级走空分支

        ResultBean<?> result = service.appoint(5, 10, 1);

        assertEquals(200L, result.getCode());
        assertEquals(1, history.getStatus(), "重新任命必须把历史记录恢复为有效");
        assertNotNull(history.getAppointTime(), "重新任命必须刷新任命时间");
        verify(boardModeratorMapper).updateById(history);
        verify(boardModeratorMapper, never()).insert(any(BoardModerator.class));
    }

    @Test
    @DisplayName("积分不足 300 时不能任命")
    void appoint_rejectsWhenPointsBelowThreshold() {
        when(boardModeratorMapper.findByUserAndLabel(5, 10)).thenReturn(null);
        when(pointsLogService.getPointsAdjustment(5)).thenReturn(299);

        ResultBean<?> result = service.appoint(5, 10, 1);

        assertEquals(500L, result.getCode());
        verify(boardModeratorMapper, never()).insert(any(BoardModerator.class));
    }
}
