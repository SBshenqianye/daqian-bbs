package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.walker.pojo.Comment;
import com.walker.pojo.Notification;
import com.walker.pojo.Reply;
import com.walker.service.CommentService;
import com.walker.service.NotificationService;
import com.walker.service.ReplyService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知控制器
 */
@Api(tags = "NotificationController")
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private CommentService commentService;

    @ApiOperation(value = "获取用户未读通知数量（全部合计）")
    @GetMapping("/unreadCount")
    public ResultBean getUnreadCount(@RequestParam Integer userId) {
        int count = notificationService.getUnreadCount(userId);
        return ResultBean.success("查询成功", count);
    }

    @ApiOperation(value = "获取用户未读通知汇总（分类独立计数，total=各分类之和）")
    @GetMapping("/unreadSummary")
    public ResultBean getUnreadSummary(@RequestParam Integer userId) {
        Map<String, Object> summary = notificationService.getUnreadSummary(userId);
        return ResultBean.success("查询成功", summary);
    }

    @ApiOperation(value = "标记通知为已读（优先按分类 category，其次按类型 type，都为空则全部）")
    @PostMapping("/markRead")
    public ResultBean markRead(@RequestParam Integer userId,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String category) {
        return notificationService.markRead(userId, type, category);
    }

    @ApiOperation(value = "标记所有通知为已读")
    @PostMapping("/markAllRead")
    public ResultBean markAllRead(@RequestParam Integer userId) {
        return notificationService.markAllRead(userId);
    }

    @ApiOperation(value = "获取通知列表（分页，可按分类过滤）")
    @GetMapping("/list")
    public ResultBean getNotificationList(@RequestParam Integer userId,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer size) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Notification::getCategory, category);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationService.page(pageParam, wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    /**
     * 解析通知关联内容的文章定位（供前端点击通知跳转文章详情）。
     * 通知表只存 relatedType(reply/comment/article) + relatedId(replyId/commentId/articleId)，
     * reply/comment 需要经 评论→文章 两跳才能拿到 articleId。
     *
     * @return { articleId, commentId?, replyId? }，无法解析时字段缺失
     */
    @ApiOperation(value = "解析通知关联内容对应的文章定位（replyId/commentId → articleId）")
    @GetMapping("/resolveTarget")
    public ResultBean resolveTarget(@RequestParam String relatedType,
                                    @RequestParam Integer relatedId) {
        Map<String, Object> target = new LinkedHashMap<>();
        if (relatedId == null) {
            return ResultBean.success("查询成功", target);
        }
        if ("article".equals(relatedType)) {
            target.put("articleId", relatedId);
        } else if ("reply".equals(relatedType)) {
            Reply reply = replyService.getById(relatedId);
            if (reply != null) {
                target.put("replyId", reply.getReplyId());
                Comment comment = commentService.getById(reply.getCommentId());
                if (comment != null) {
                    target.put("commentId", comment.getCommentId());
                    target.put("articleId", comment.getCommentArticleId());
                }
            }
        } else if ("comment".equals(relatedType)) {
            Comment comment = commentService.getById(relatedId);
            if (comment != null) {
                target.put("commentId", comment.getCommentId());
                target.put("articleId", comment.getCommentArticleId());
            }
        }
        return ResultBean.success("查询成功", target);
    }
}
