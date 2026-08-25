package com.walker.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.service.ArticleService;
import com.walker.service.CommentService;
import com.walker.service.NotificationService;
import com.walker.service.ReplyService;
import com.walker.service.SaOrgService;
import com.walker.service.UserService;
import com.walker.vo.ReplyListItemVO;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 回复列表控制器 — "我回复的"和"回复我的"功能
 */
@Api(tags = "ReplyListController")
@RestController
@RequestMapping("/reply")
public class ReplyListController {

    @Autowired
    private ReplyService replyService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Autowired
    private SaOrgService saOrgService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 我回复的 — 获取我参与讨论的文章列表
     * 逻辑：查找我发出的所有回复 → 找到对应的评论 → 找到对应的文章 → 去重
     */
    @ApiOperation(value = "我回复的帖子列表")
    @GetMapping("/myReplies")
    public ResultBean getMyReplies(@RequestParam Integer userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer size) {
        // 1. 查找我发出的所有回复
        List<Reply> myReplies = replyService.list(
                new LambdaQueryWrapper<Reply>()
                        .eq(Reply::getReplyUserId, userId)
                        .orderByDesc(Reply::getReplyTime)
        );

        // 2. 提取去重的评论ID，按最新回复时间排序
        Map<Integer, Reply> latestReplyByComment = new LinkedHashMap<>();
        for (Reply reply : myReplies) {
            Integer commentId = reply.getCommentId();
            if (!latestReplyByComment.containsKey(commentId)) {
                latestReplyByComment.put(commentId, reply);
            }
        }

        // 3. 通过评论找到文章
        List<Integer> commentIds = new ArrayList<>(latestReplyByComment.keySet());
        if (commentIds.isEmpty()) {
            return ResultBean.success("查询成功", Collections.emptyMap());
        }

        List<Comment> comments = commentService.listByIds(commentIds);
        Map<Integer, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getCommentId, c -> c, (a, b) -> a));

        // 4. 获取去重的文章ID（保持顺序）
        LinkedHashMap<Integer, Comment> articleCommentMap = new LinkedHashMap<>();
        for (Comment c : comments) {
            articleCommentMap.putIfAbsent(c.getCommentArticleId(), c);
        }
        List<Integer> articleIds = new ArrayList<>(articleCommentMap.keySet());

        // 5. 查询文章信息
        List<Article> articles = articleService.listByIds(articleIds);
        Map<Integer, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getArticleId, a -> a, (a, b) -> a));

        // 6. 构建返回结果
        List<ReplyListItemVO> resultList = new ArrayList<>();
        for (Map.Entry<Integer, Comment> entry : articleCommentMap.entrySet()) {
            Integer articleId = entry.getKey();
            Comment comment = entry.getValue();
            Reply latestReply = latestReplyByComment.get(comment.getCommentId());
            Article article = articleMap.get(articleId);

            if (article == null) continue;

            ReplyListItemVO item = new ReplyListItemVO();
            item.setArticleId(articleId);
            item.setArticleTitle(article.getArticleTitle());
            item.setArticleImage(article.getArticleImage());
            item.setCommentId(comment.getCommentId());
            item.setReplyId(latestReply != null ? latestReply.getReplyId() : null);
            item.setContent(latestReply != null ? latestReply.getReplyContent() : comment.getCommentContent());
            item.setTime(latestReply != null ? latestReply.getReplyTime() : comment.getCommentTime());

            resultList.add(item);
        }

        // 7. 分页（简单内存分页，数据量不大）
        int total = resultList.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<ReplyListItemVO> pageList = fromIndex < total ? resultList.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResultBean.success("查询成功", result);
    }

    /**
     * 回复我的 — 获取别人对我的回复/评论列表
     * 逻辑：
     * 1. 我的帖子下有人评论 → 评论信息
     * 2. 我的帖子下有人回复别人 → 回复信息
     * 3. 我的评论下有人回复 → 回复信息
     * 4. 有人回复我的回复 → 回复信息
     */
    @ApiOperation(value = "回复我的列表")
    @GetMapping("/repliedToMe")
    public ResultBean getRepliedToMe(@RequestParam Integer userId,
                                     @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "20") Integer size) {

        List<ReplyListItemVO> resultList = new ArrayList<>();

        // 1. 查找我的文章
        List<Article> myArticles = articleService.list(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .select(Article::getArticleId, Article::getArticleTitle, Article::getArticleImage)
        );
        Set<Integer> myArticleIds = myArticles.stream()
                .map(Article::getArticleId)
                .collect(Collectors.toSet());
        Map<Integer, Article> myArticleMap = myArticles.stream()
                .collect(Collectors.toMap(Article::getArticleId, a -> a, (a, b) -> a));

        // 2. 查找我的评论
        List<Comment> myComments = commentService.list(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getCommentUserId, userId)
                        .select(Comment::getCommentId, Comment::getCommentArticleId)
        );
        Map<Integer, Integer> myCommentArticleMap = myComments.stream()
                .collect(Collectors.toMap(Comment::getCommentId, Comment::getCommentArticleId, (a, b) -> a));
        Set<Integer> myCommentIds = myComments.stream()
                .map(Comment::getCommentId)
                .collect(Collectors.toSet());

        // 3. 别人在我文章下的评论
        if (!myArticleIds.isEmpty()) {
            List<Comment> commentsOnMyArticles = commentService.list(
                    new LambdaQueryWrapper<Comment>()
                            .in(Comment::getCommentArticleId, myArticleIds)
                            .ne(Comment::getCommentUserId, userId)
                            .orderByDesc(Comment::getCommentTime)
            );
            for (Comment c : commentsOnMyArticles) {
                User fromUser = userService.getById(c.getCommentUserId());
                if (fromUser == null) continue;

                ReplyListItemVO item = new ReplyListItemVO();
                item.setArticleId(c.getCommentArticleId());
                Article art = myArticleMap.get(c.getCommentArticleId());
                item.setArticleTitle(art != null ? art.getArticleTitle() : "");
                item.setCommentId(c.getCommentId());
                item.setContent(c.getCommentContent());
                item.setTime(c.getCommentTime());
                item.setFromUserId(fromUser.getId());
                item.setFromNickname(fromUser.getNickname());
                item.setFromPortrait(fromUser.getPortrait());
                if (fromUser.getOrgNo() != null) {
                    item.setFromOrgName(saOrgService.resolveDisplayOrgName(fromUser.getOrgNo(), fromUser.getOrgName()));
                }
                item.setReplyRelation("评论了你的帖子");
                resultList.add(item);
            }
        }

        // 4. 别人在我帖子下回复别人（但不是回复我，因为上面已经包含了）
        if (!myArticleIds.isEmpty()) {
            List<Comment> allCommentsOnMyArticles = commentService.list(
                    new LambdaQueryWrapper<Comment>()
                            .in(Comment::getCommentArticleId, myArticleIds)
                            .select(Comment::getCommentId)
            );
            Set<Integer> allCommentIdsOnMyArticles = allCommentsOnMyArticles.stream()
                    .map(Comment::getCommentId)
                    .collect(Collectors.toSet());

            if (!allCommentIdsOnMyArticles.isEmpty()) {
                List<Reply> repliesOnMyArticles = replyService.list(
                        new LambdaQueryWrapper<Reply>()
                                .in(Reply::getCommentId, allCommentIdsOnMyArticles)
                                .ne(Reply::getReplyUserId, userId)
                                .orderByDesc(Reply::getReplyTime)
                );
                for (Reply r : repliesOnMyArticles) {
                    User fromUser = userService.getById(r.getReplyUserId());
                    if (fromUser == null) continue;

                    Comment parentComment = commentService.getById(r.getCommentId());
                    Integer articleId = parentComment != null ? parentComment.getCommentArticleId() : null;

                    ReplyListItemVO item = new ReplyListItemVO();
                    item.setArticleId(articleId);
                    if (articleId != null) {
                        Article art = myArticleMap.get(articleId);
                        item.setArticleTitle(art != null ? art.getArticleTitle() : "");
                    }
                    item.setReplyId(r.getReplyId());
                    item.setCommentId(r.getCommentId());
                    item.setContent(r.getReplyContent());
                    item.setTime(r.getReplyTime());
                    item.setFromUserId(fromUser.getId());
                    item.setFromNickname(fromUser.getNickname());
                    item.setFromPortrait(fromUser.getPortrait());
                    if (fromUser.getOrgNo() != null) {
                        item.setFromOrgName(saOrgService.resolveDisplayOrgName(fromUser.getOrgNo(), fromUser.getOrgName()));
                    }

                    // 构建回复关系描述
                    if (r.getReplyToUserId() != null && !r.getReplyToUserId().equals(userId)) {
                        User toUser = userService.getById(r.getReplyToUserId());
                        if (toUser != null) {
                            item.setToUserId(toUser.getId());
                            item.setToNickname(toUser.getNickname());
                            item.setReplyRelation("回复了 " + toUser.getNickname());
                        }
                    } else {
                        item.setReplyRelation("回复了你");
                    }

                    // 避免重复：如果这条回复已经在"我的评论被回复"中出现，跳过
                    boolean duplicate = resultList.stream()
                            .anyMatch(existing -> existing.getReplyId() != null
                                    && existing.getReplyId().equals(r.getReplyId()));
                    if (!duplicate) {
                        resultList.add(item);
                    }
                }
            }
        }

        // 5. 别人回复我的评论（我评论了别人的帖子，别人在我的评论下回复）
        // 我的评论不在我自己帖子下的部分
        Set<Integer> myCommentIdsNotOnMyArticles = myCommentIds.stream()
                .filter(cid -> !myArticleIds.contains(myCommentArticleMap.get(cid)))
                .collect(Collectors.toSet());

        if (!myCommentIdsNotOnMyArticles.isEmpty()) {
            List<Reply> repliesToMyComments = replyService.list(
                    new LambdaQueryWrapper<Reply>()
                            .in(Reply::getCommentId, myCommentIdsNotOnMyArticles)
                            .ne(Reply::getReplyUserId, userId)
                            .orderByDesc(Reply::getReplyTime)
            );
            for (Reply r : repliesToMyComments) {
                User fromUser = userService.getById(r.getReplyUserId());
                if (fromUser == null) continue;

                Comment parentComment = commentService.getById(r.getCommentId());
                Integer articleId = parentComment != null ? parentComment.getCommentArticleId() : null;

                ReplyListItemVO item = new ReplyListItemVO();
                item.setArticleId(articleId);
                if (articleId != null) {
                    Article art = articleService.getById(articleId);
                    item.setArticleTitle(art != null ? art.getArticleTitle() : "");
                }
                item.setReplyId(r.getReplyId());
                item.setCommentId(r.getCommentId());
                item.setContent(r.getReplyContent());
                item.setTime(r.getReplyTime());
                item.setFromUserId(fromUser.getId());
                item.setFromNickname(fromUser.getNickname());
                item.setFromPortrait(fromUser.getPortrait());
                if (fromUser.getOrgNo() != null) {
                    item.setFromOrgName(saOrgService.resolveDisplayOrgName(fromUser.getOrgNo(), fromUser.getOrgName()));
                }
                item.setReplyRelation("回复了你的评论");

                boolean duplicate = resultList.stream()
                        .anyMatch(existing -> existing.getReplyId() != null
                                && existing.getReplyId().equals(r.getReplyId()));
                if (!duplicate) {
                    resultList.add(item);
                }
            }
        }

        // 6. 按时间排序（最新的在前）
        resultList.sort((a, b) -> {
            String t1 = a.getTime() != null ? a.getTime() : "";
            String t2 = b.getTime() != null ? b.getTime() : "";
            return t2.compareTo(t1);
        });

        // 7. 分页
        int total = resultList.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<ReplyListItemVO> pageList = fromIndex < total ? resultList.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return ResultBean.success("查询成功", result);
    }
}
