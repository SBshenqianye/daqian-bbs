package com.walker.controller;


import com.walker.mapper.DictMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.service.*;
import com.walker.vo.CommentReplyVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommentParam;
import com.walker.vo.ReplyVO;
import com.walker.vo.param.ReplyParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Api(tags = "CommentController")
@RestController

public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private SaOrgService saOrgService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "保存用户的评论(一级评论)")
    @PutMapping("/comment/userComment")
    public ResultBean userComment(@RequestBody CommentParam commentParam){

        ResultBean result = commentService.saveUserComment(commentParam);

        // 帖子热度奖励检查：评论发布后检查该文章的有效回复数
        if (commentParam.getCommentArticleId() != null && commentParam.getCommentUserId() != null) {
            try {
                checkHotBonus(commentParam.getCommentArticleId());
            } catch (Exception e) {
                // 热度检查失败不影响评论发布
            }
        }

        return result;
    }

    /**
     * 检查帖子热度奖励：有效回复数超过阈值且未发过热度奖励时，给作者加1分
     */
    private void checkHotBonus(Integer articleId) {
        Article article = articleService.queryArticleById(articleId);
        if (article == null || article.getIsHotBonus() != null && article.getIsHotBonus() == 1) {
            return; // 文章不存在或已发过热度奖励
        }

        // 获取热度阈值（默认10）
        int threshold = 10;
        try {
            String val = dictMapper.selectValueByType("hot_threshold");
            if (val != null) threshold = Integer.parseInt(val);
        } catch (Exception e) {
            // 使用默认值
        }

        // 统计该文章的有效评论数（评论也算回复）
        // 这里简化处理：统计 comment 数 + reply 数
        // 使用已有的 queryCommentReply 获取评论列表
        List<Comment> comments = commentService.queryCommentReply(articleId);
        int totalReplies = 0;
        for (Comment c : comments) {
            List<Reply> replies = replyService.queryReplyByCommentId(c.getCommentId());
            totalReplies += replies.size();
        }

        if (totalReplies >= threshold) {
            // 触发热度奖励
            article.setIsHotBonus(1);
            articleService.updateById(article);

            pointsLogService.adjustUserPoints(article.getUserId(), 1, "帖子热度奖励（回复数达" + totalReplies + "条）",
                    "hot_bonus", articleId, null);

            // 通知作者
            notificationService.createNotification(article.getUserId(), null,
                    "hot_bonus", "恭喜！您的帖子获得热度奖励+1分",
                    "article", articleId);
        }
    }


    @ApiOperation(value = "通过文章ID获取评论和回复")
    @PostMapping("/common/comment/getCommentReply/{articleId}")
    public List<CommentReplyVO> getCommentReply(@PathVariable("articleId") Integer articleId){

        List<Comment> commentList =  commentService.queryCommentReply(articleId);

        List<CommentReplyVO> commentReplyVOList = new ArrayList<CommentReplyVO>();

        if(!commentList.isEmpty()){
            for(int i=0; i < commentList.size();i++){

                CommentReplyVO commentReplyVO = new CommentReplyVO();

                Comment comment = commentList.get(i);
                Integer commentId = comment.getCommentId();

                commentReplyVO.setCommentId(commentId);
                commentReplyVO.setCommentTime(comment.getCommentTime());
                commentReplyVO.setCommentContent(comment.getCommentContent());
                commentReplyVO.setInputShow(false);

                //得到回复用户的id，通过id查询用户信息
                Integer userId = comment.getCommentUserId();
                User user = userService.queryUserinfoById(userId);

                commentReplyVO.setUserId(userId);
                commentReplyVO.setPortrait(user.getPortrait());
                commentReplyVO.setNickname(user.getNickname());
                commentReplyVO.setOrgName(user.getOrgName());
                commentReplyVO.setOrgNameFull(resolveFullOrgName(user.getOrgNo(), user.getOrgName()));
                commentReplyVO.setDeptName(user.getDeptName());
                commentReplyVO.setPoints(pointsLogService.getPointsAdjustment(userId));

                //通过回复的Id去获取回复内容

                List<Reply> replyList = replyService.queryReplyByCommentId(commentId);

                List<ReplyVO> replyVOList = new ArrayList<ReplyVO>();

                if(!replyList.isEmpty()){

                    for(int j = 0;j < replyList.size();j++){

                        ReplyVO replyVO = new ReplyVO();
                        Reply reply = replyList.get(j);

                        replyVO.setReplyId(reply.getReplyId());
                        replyVO.setReplyContent(reply.getReplyContent());
                        replyVO.setReplyTime(reply.getReplyTime());
                        replyVO.setInputShow(false);

                        Integer fromUserId = reply.getReplyUserId();
                        User userVO1 = userService.queryUserinfoById(fromUserId);

                        replyVO.setReplyUserId(fromUserId);
                        replyVO.setPortrait(userVO1.getPortrait());
                        replyVO.setNickname(userVO1.getNickname());
                        replyVO.setOrgName(userVO1.getOrgName());
                        replyVO.setOrgNameFull(resolveFullOrgName(userVO1.getOrgNo(), userVO1.getOrgName()));
                        replyVO.setDeptName(userVO1.getDeptName());
                        replyVO.setPoints(pointsLogService.getPointsAdjustment(fromUserId));
                        replyVO.setIsAdopted(reply.getIsAdopted());

                        Integer toUserId = reply.getReplyToUserId();
                        User userVO2 = userService.queryUserinfoById(toUserId);

                        replyVO.setReplyToUserId(userVO2.getId());
                        replyVO.setReplyToNickname(userVO2.getNickname());


                        replyVOList.add(replyVO);

                    }
                }

                commentReplyVO.setReply(replyVOList);

                commentReplyVOList.add(commentReplyVO);
            }

        }


        return commentReplyVOList;

    }


    @ApiOperation(value = "通过评论id删除评论")
    @PostMapping("/comment/deleteCommentById")
    public ResultBean deleteCommentById(@RequestBody ReplyParam replyParam){
        return commentService.deleteCommentById(replyParam.getCommentId());
    }

    /**
     * 按显示层级解析组织的完整名称（user.orgName 可能已被 display 过滤覆盖）
     */
    private String resolveFullOrgName(String orgNo, String fallbackName) {
        return saOrgService.resolveDisplayOrgName(orgNo, fallbackName);
    }
}
