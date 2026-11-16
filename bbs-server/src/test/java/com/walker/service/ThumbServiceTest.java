package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ThumbMapper;
import com.walker.pojo.Reply;
import com.walker.pojo.Thumb;
import com.walker.service.impl.CommentServiceImpl;
import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThumbServiceTest {

    @InjectMocks
    private ThumbService thumbService;

    @Mock
    private ThumbMapper thumbMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ReplyMapper replyMapper;

    private Reply reply(Integer id, int goodNum) {
        Reply r = new Reply();
        r.setReplyId(id);
        r.setGoodNum(goodNum);
        r.setIsDelete(0);
        return r;
    }

    @Test
    @DisplayName("回帖点赞 → 无历史记录 → 新增并计数 +1")
    void toggleReply_firstTime_likes() {
        when(replyMapper.selectById(1)).thenReturn(reply(1, 0));
        when(thumbMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(thumbMapper.insert(any(Thumb.class))).thenReturn(1);
        when(replyMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ResultBean result = thumbService.toggle(9, "reply", 1);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getObj();
        assertNotNull(data);
        assertEquals(Boolean.TRUE, data.get("liked"));
        assertEquals("reply", data.get("targetType"));
        verify(thumbMapper).insert(any(Thumb.class));
        verify(replyMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    @DisplayName("回帖点赞 → 已点赞 → 取消并计数 -1")
    void toggleReply_secondTime_cancels() {
        Thumb existing = new Thumb();
        existing.setId(5);
        when(replyMapper.selectById(1)).thenReturn(reply(1, 1));
        when(thumbMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(thumbMapper.deleteById(5)).thenReturn(1);
        when(replyMapper.update(isNull(), any(Wrapper.class))).thenReturn(1);

        ResultBean result = thumbService.toggle(9, "reply", 1);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getObj();
        assertEquals(Boolean.FALSE, data.get("liked"));
        verify(thumbMapper).deleteById(5);
    }

    @Test
    @DisplayName("点赞不存在的内容 → 返回业务错误")
    void toggleReply_missingTarget_error() {
        when(replyMapper.selectById(404)).thenReturn(null);
        when(thumbMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        ResultBean result = thumbService.toggle(9, "reply", 404);

        assertEquals(500, result.getCode());
        verify(thumbMapper, never()).insert(any(Thumb.class));
    }

    @Test
    @DisplayName("不支持的点赞类型 → 返回业务错误")
    void toggle_unsupportedType_error() {
        ResultBean result = thumbService.toggle(9, "article", 1);
        assertEquals(500, result.getCode());
    }

    @Test
    @DisplayName("批量查询点赞状态 → 返回已点赞 ID 集合")
    void likedIds_returnsSet() {
        Thumb a = new Thumb();
        a.setTargetId(11);
        Thumb b = new Thumb();
        b.setTargetId(12);
        when(thumbMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(a, b));

        Set<Integer> ids = thumbService.likedIds(9, "reply", Arrays.asList(11, 12, 13));

        assertTrue(ids.contains(11));
        assertTrue(ids.contains(12));
        assertFalse(ids.contains(13));
    }

    @Test
    @DisplayName("未登录/空参数 → 批量查询返回空集合")
    void likedIds_anonymous_returnsEmpty() {
        assertTrue(thumbService.likedIds(null, "reply", Arrays.asList(1, 2)).isEmpty());
        assertTrue(thumbService.likedIds(9, "reply", java.util.Collections.emptyList()).isEmpty());
    }
}
