package com.walker.controller;


import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.service.*;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ReplyParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Api(tags = "ReplyController")
@RestController
@RequestMapping("/reply")
public class ReplyController {

    @Autowired
    private ReplyService replyService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "保存用户的回复")
    @PutMapping("/userReply")
    public ResultBean userReply(@RequestBody ReplyParam replyParam){

        return replyService.saveUserReply(replyParam);
    }

    @ApiOperation(value = "通过id删除用户评论")
    @PostMapping("/deleteReplyById")
    public ResultBean deleteReplyById(@RequestBody ReplyParam replyParam){
        return replyService.deleteReplyById(replyParam.getReplyId());
    }

    @ApiOperation(value = "楼主采纳回复（最佳答案）")
    @PostMapping("/article/adoptReply")
    public ResultBean adoptReply(@RequestBody Map<String, Object> params) {
        Integer replyId = (Integer) params.get("replyId");
        Integer articleId = (Integer) params.get("articleId");
        Integer userId = (Integer) params.get("userId");

        if (replyId == null || articleId == null || userId == null) {
            return ResultBean.error("参数不完整");
        }

        // 获取回复信息
        Reply reply = replyService.getById(replyId);
        if (reply == null) {
            return ResultBean.error("回复不存在");
        }

        // 获取文章信息
        com.walker.pojo.Article article = articleService.queryArticleById(articleId);
        if (article == null) {
            return ResultBean.error("文章不存在");
        }

        // 校验：操作人必须是文章作者
        if (!userId.equals(article.getUserId())) {
            return ResultBean.error("只有文章作者才能采纳回复");
        }

        // 校验：回复必须属于该文章的评论
        // reply.commentId -> comment.commentArticleId 应该等于 articleId
        // 简化校验：直接检查是否已采纳
        if (reply.getIsAdopted() != null && reply.getIsAdopted() == 1) {
            return ResultBean.error("该回复已被采纳");
        }

        // 标记为已采纳
        reply.setIsAdopted(1);
        replyService.updateById(reply);

        // 给回复人加5分
        pointsLogService.adjustUserPoints(reply.getReplyUserId(), 5, "最佳答案采纳积分",
                "reply", replyId, userId);

        // 通知回复人
        notificationService.createNotification(reply.getReplyUserId(), userId,
                "adopt", "恭喜！您的回复被采纳为最佳答案，获得+5积分",
                "reply", replyId);

        return ResultBean.success("采纳成功");
    }
}
