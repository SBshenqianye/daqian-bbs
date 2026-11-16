package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ThumbMapper;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.Thumb;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评论 / 楼中楼回复点赞。回帖点赞不产生积分，只做互动计数。
 */
@Service
public class ThumbService {

    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_REPLY = "reply";

    @Autowired
    private ThumbMapper thumbMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Transactional
    public ResultBean toggle(Integer userId, String targetType, Integer targetId) {
        if (userId == null || targetId == null) {
            return ResultBean.error("参数不完整");
        }
        String type = normalizeType(targetType);
        if (type == null) {
            return ResultBean.error("不支持的点赞对象");
        }

        Thumb exists = thumbMapper.selectOne(new LambdaQueryWrapper<Thumb>()
                .eq(Thumb::getUserId, userId)
                .eq(Thumb::getTargetType, type)
                .eq(Thumb::getTargetId, targetId)
                .last("LIMIT 1"));

        boolean liked;
        if (exists != null) {
            thumbMapper.deleteById(exists.getId());
            changeCount(type, targetId, -1);
            liked = false;
        } else {
            if (!targetExists(type, targetId)) {
                return ResultBean.error("内容不存在或已删除");
            }
            Thumb thumb = new Thumb();
            thumb.setUserId(userId);
            thumb.setTargetType(type);
            thumb.setTargetId(targetId);
            thumb.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            thumbMapper.insert(thumb);
            changeCount(type, targetId, 1);
            liked = true;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetType", type);
        data.put("targetId", targetId);
        data.put("likes", currentCount(type, targetId));
        data.put("liked", liked);
        return ResultBean.success(liked ? "已点赞" : "已取消点赞", data);
    }

    /** 当前用户对一批对象的点赞状态 */
    public Set<Integer> likedIds(Integer userId, String targetType, Collection<Integer> targetIds) {
        Set<Integer> result = new HashSet<>();
        if (userId == null || targetIds == null || targetIds.isEmpty()) return result;
        String type = normalizeType(targetType);
        if (type == null) return result;
        List<Thumb> list = thumbMapper.selectList(new LambdaQueryWrapper<Thumb>()
                .eq(Thumb::getUserId, userId)
                .eq(Thumb::getTargetType, type)
                .in(Thumb::getTargetId, targetIds));
        for (Thumb t : list) {
            if (t.getTargetId() != null) result.add(t.getTargetId());
        }
        return result;
    }

    public Map<String, Object> countsFor(String targetType, Integer targetId) {
        Map<String, Object> data = new HashMap<>();
        data.put("likes", currentCount(targetType, targetId));
        return data;
    }

    private String normalizeType(String targetType) {
        if (targetType == null) return null;
        String t = targetType.trim().toLowerCase();
        if (TYPE_COMMENT.equals(t) || TYPE_REPLY.equals(t)) return t;
        return null;
    }

    private boolean targetExists(String type, Integer targetId) {
        if (TYPE_COMMENT.equals(type)) {
            Comment c = commentMapper.selectById(targetId);
            return c != null && !Integer.valueOf(1).equals(c.getIsDelete());
        }
        Reply r = replyMapper.selectById(targetId);
        return r != null && !Integer.valueOf(1).equals(r.getIsDelete());
    }

    private void changeCount(String type, Integer targetId, int delta) {
        if (TYPE_COMMENT.equals(type)) {
            commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                    .eq(Comment::getCommentId, targetId)
                    .setSql("comment_good_num = GREATEST(COALESCE(comment_good_num, 0) + (" + delta + "), 0)"));
        } else {
            replyMapper.update(null, new LambdaUpdateWrapper<Reply>()
                    .eq(Reply::getReplyId, targetId)
                    .setSql("reply_good_num = GREATEST(COALESCE(reply_good_num, 0) + (" + delta + "), 0)"));
        }
    }

    private int currentCount(String type, Integer targetId) {
        try {
            if (TYPE_COMMENT.equals(type)) {
                Comment c = commentMapper.selectById(targetId);
                return c == null || c.getGoodNum() == null ? 0 : c.getGoodNum();
            }
            Reply r = replyMapper.selectById(targetId);
            return r == null || r.getGoodNum() == null ? 0 : r.getGoodNum();
        } catch (Exception e) {
            return 0;
        }
    }
}
