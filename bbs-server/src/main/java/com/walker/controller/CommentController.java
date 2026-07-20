package com.walker.controller;


import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.service.CommentService;
import com.walker.service.ReplyService;
import com.walker.service.UserService;
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

    @ApiOperation(value = "保存用户的评论(一级评论)")
    @PutMapping("/comment/userComment")
    public ResultBean userComment(@RequestBody CommentParam commentParam){

        return commentService.saveUserComment(commentParam);
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
                commentReplyVO.setDeptName(user.getDeptName());

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
                        replyVO.setDeptName(userVO1.getDeptName());

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
}
