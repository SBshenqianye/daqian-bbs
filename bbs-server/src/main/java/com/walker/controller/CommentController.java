package com.walker.controller;


import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.service.*;
import com.walker.vo.CommentReplyVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.CommentParam;
import com.walker.vo.ReplyVO;
import com.walker.vo.param.ReplyParam;
import com.walker.utils.SensitiveWordUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private LevelService levelService;

    @Autowired
    private ThumbService thumbService;

    @Autowired
    private CurrentUserService currentUserService;

    @ApiOperation(value = "保存用户的评论(一级评论)")
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).COMMENT_CREATE)")
    @PutMapping("/comment/userComment")
    public ResultBean userComment(@RequestBody CommentParam commentParam){

        ResultBean result = commentService.saveUserComment(commentParam);

        // 帖子热度奖励检查：评论发布后检查该文章的有效互动数
        if (commentParam.getCommentArticleId() != null && commentParam.getCommentUserId() != null) {
            try {
                commentService.checkHotBonus(commentParam.getCommentArticleId());
            } catch (Exception e) {
                // 热度检查失败不影响评论发布
            }
        }

        return result;
    }


    @ApiOperation(value = "通过文章ID获取评论和回复")
    @PostMapping("/common/comment/getCommentReply/{articleId}")
    public List<CommentReplyVO> getCommentReply(@PathVariable("articleId") Integer articleId){

        List<Comment> commentList =  commentService.queryCommentReply(articleId);

        List<CommentReplyVO> commentReplyVOList = new ArrayList<CommentReplyVO>();

        if(!commentList.isEmpty()){

            // 批量准备：等级角标、当前用户点赞状态（避免逐条查询）
            java.util.Set<Integer> levelUserIds = new java.util.HashSet<>();
            java.util.List<Integer> commentIds = new java.util.ArrayList<>();
            java.util.List<Integer> replyIds = new java.util.ArrayList<>();
            java.util.Map<Integer, List<Reply>> repliesByComment = new java.util.HashMap<>();
            for (Comment c : commentList) {
                if (c.getCommentId() != null) commentIds.add(c.getCommentId());
                if (c.getCommentUserId() != null && !Integer.valueOf(1).equals(c.getIsAnonymous())) {
                    levelUserIds.add(c.getCommentUserId());
                }
                List<Reply> rs = replyService.queryReplyByCommentId(c.getCommentId());
                repliesByComment.put(c.getCommentId(), rs);
                for (Reply r : rs) {
                    if (r.getReplyId() != null) replyIds.add(r.getReplyId());
                    if (r.getReplyUserId() != null && !Integer.valueOf(1).equals(r.getIsAnonymous())) {
                        levelUserIds.add(r.getReplyUserId());
                    }
                    if (r.getReplyToUserId() != null) levelUserIds.add(r.getReplyToUserId());
                }
            }
            java.util.Map<Integer, Integer> levelMap = levelService.getLevelIndexMap(levelUserIds);
            Integer currentUserId = currentUserService.getCurrentUserId();
            java.util.Set<Integer> likedCommentIds = thumbService.likedIds(currentUserId, ThumbService.TYPE_COMMENT, commentIds);
            java.util.Set<Integer> likedReplyIds = thumbService.likedIds(currentUserId, ThumbService.TYPE_REPLY, replyIds);
            for(int i=0; i < commentList.size();i++){

                CommentReplyVO commentReplyVO = new CommentReplyVO();

                Comment comment = commentList.get(i);
                Integer commentId = comment.getCommentId();

                commentReplyVO.setCommentId(commentId);
                commentReplyVO.setCommentTime(comment.getCommentTime());
                commentReplyVO.setCommentContent(SensitiveWordUtil.desensitize(comment.getCommentContent()));
                commentReplyVO.setInputShow(false);

                //得到回复用户的id，通过id查询用户信息
                Integer userId = comment.getCommentUserId();
                boolean anonymous = Integer.valueOf(1).equals(comment.getIsAnonymous());
                User user = anonymous ? null : userService.queryUserinfoById(userId);

                commentReplyVO.setIsAnonymous(anonymous ? 1 : 0);
                commentReplyVO.setUserId(anonymous ? null : userId);
                commentReplyVO.setPortrait(anonymous || user == null ? null : user.getPortrait());
                commentReplyVO.setNickname(anonymous ? "匿名用户" : (user == null ? "用户" : user.getNickname()));
                commentReplyVO.setOrgName(anonymous || user == null ? null : user.getOrgName());
                commentReplyVO.setOrgNameFull(anonymous || user == null ? null : resolveFullOrgName(user.getOrgNo(), user.getOrgName()));
                commentReplyVO.setDeptName(anonymous || user == null ? null : user.getDeptName());
                commentReplyVO.setPoints(anonymous || user == null ? null : pointsLogService.getPointsAdjustment(userId));
                commentReplyVO.setAdoptStatus(comment.getAdoptStatus());
                commentReplyVO.setLevelIndex(anonymous ? null : levelMap.get(userId));
                commentReplyVO.setGoodNum(comment.getGoodNum() == null ? 0 : comment.getGoodNum());
                commentReplyVO.setLiked(likedCommentIds.contains(commentId));

                //通过回复的Id去获取回复内容

                List<Reply> replyList = repliesByComment.getOrDefault(commentId, new ArrayList<>());

                List<ReplyVO> replyVOList = new ArrayList<ReplyVO>();

                if(!replyList.isEmpty()){

                    for(int j = 0;j < replyList.size();j++){

                        ReplyVO replyVO = new ReplyVO();
                        Reply reply = replyList.get(j);

                        replyVO.setReplyId(reply.getReplyId());
                        replyVO.setReplyContent(SensitiveWordUtil.desensitize(reply.getReplyContent()));
                        replyVO.setReplyTime(reply.getReplyTime());
                        replyVO.setInputShow(false);

                        Integer fromUserId = reply.getReplyUserId();
                        boolean replyAnonymous = Integer.valueOf(1).equals(reply.getIsAnonymous());
                        User userVO1 = replyAnonymous ? null : userService.queryUserinfoById(fromUserId);

                        replyVO.setReplyUserId(replyAnonymous ? null : fromUserId);
                        replyVO.setIsAnonymous(replyAnonymous ? 1 : 0);
                        replyVO.setPortrait(replyAnonymous || userVO1 == null ? null : userVO1.getPortrait());
                        replyVO.setNickname(replyAnonymous ? "匿名用户" : (userVO1 == null ? "用户" : userVO1.getNickname()));
                        replyVO.setOrgName(replyAnonymous || userVO1 == null ? null : userVO1.getOrgName());
                        replyVO.setOrgNameFull(replyAnonymous || userVO1 == null ? null : resolveFullOrgName(userVO1.getOrgNo(), userVO1.getOrgName()));
                        replyVO.setDeptName(replyAnonymous || userVO1 == null ? null : userVO1.getDeptName());
                        replyVO.setPoints(replyAnonymous || userVO1 == null ? null : pointsLogService.getPointsAdjustment(fromUserId));
                        replyVO.setIsAdopted(reply.getIsAdopted());
                        replyVO.setAdoptStatus(reply.getAdoptStatus());
                        replyVO.setLevelIndex(replyAnonymous ? null : levelMap.get(fromUserId));
                        replyVO.setGoodNum(reply.getGoodNum() == null ? 0 : reply.getGoodNum());
                        replyVO.setLiked(likedReplyIds.contains(reply.getReplyId()));

                        Integer toUserId = reply.getReplyToUserId();
                        User userVO2 = toUserId == null ? null : userService.queryUserinfoById(toUserId);

                        replyVO.setReplyToUserId(userVO2 == null ? toUserId : userVO2.getId());
                        replyVO.setReplyToNickname(userVO2 == null ? null : userVO2.getNickname());


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
    @PreAuthorize("@authz.hasPermission(principal, T(com.walker.config.security.Permission).COMMENT_DELETE_OWN)")
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
