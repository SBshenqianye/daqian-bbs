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
     * 逻辑：查找我发出的所有回复(楼中楼) + 我发出的所有评论(楼层) → 找到对应的文章 → 去重
     */
    @ApiOperation(value = "我回复的帖子列表")
    @GetMapping("/myReplies")
    public ResultBean getMyReplies(@RequestParam Integer userId,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer size) {

        // 1. 查找我发出的所有楼中楼回复
        List<Reply> myReplies = replyService.list(
                new LambdaQueryWrapper<Reply>()
                        .eq(Reply::getReplyUserId, userId)
                        .orderByDesc(Reply::getReplyTime)
        );

        // 提取去重的评论ID → 文章映射（楼中楼回复 → 评论 → 文章）
        Map<Integer, Reply> latestReplyByComment = new LinkedHashMap<>();
        for (Reply reply : myReplies) {
            if (!latestReplyByComment.containsKey(reply.getCommentId())) {
                latestReplyByComment.put(reply.getCommentId(), reply);
            }
        }
        List<Integer> replyCommentIds = new ArrayList<>(latestReplyByComment.keySet());
        Map<Integer, Integer> replyCommentToArticle = new LinkedHashMap<>();
        if (!replyCommentIds.isEmpty()) {
            List<Comment> replyComments = commentService.listByIds(replyCommentIds);
            for (Comment c : replyComments) {
                replyCommentToArticle.putIfAbsent(c.getCommentId(), c.getCommentArticleId());
            }
        }

        // 2. 查找我发出的所有楼层评论
        List<Comment> myComments = commentService.list(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getCommentUserId, userId)
                        .orderByDesc(Comment::getCommentTime)
        );

        // 3. 合并：按文章ID去重，保留最新的互动记录
        // key: articleId, value: { articleId, commentId, replyId, content, time, source }
        LinkedHashMap<Integer, Map<String, Object>> articleInteractions = new LinkedHashMap<>();

        // 先加入楼层评论（按时间顺序，后来的如果更旧会覆盖，但我们按时间倒序所以不会）
        for (Comment c : myComments) {
            Integer articleId = c.getCommentArticleId();
            if (articleId == null) continue;
            if (!articleInteractions.containsKey(articleId)) {
                Map<String, Object> interaction = new HashMap<>();
                interaction.put("articleId", articleId);
                interaction.put("commentId", c.getCommentId());
                interaction.put("replyId", null);
                interaction.put("content", c.getCommentContent());
                interaction.put("time", c.getCommentTime());
                interaction.put("source", "comment");
                articleInteractions.put(articleId, interaction);
            }
        }

        // 再加入楼中楼回复（如果文章已有记录但更新，则覆盖）
        for (Map.Entry<Integer, Reply> entry : latestReplyByComment.entrySet()) {
            Integer commentId = entry.getKey();
            Reply reply = entry.getValue();
            Integer articleId = replyCommentToArticle.get(commentId);
            if (articleId == null) continue;

            Map<String, Object> existing = articleInteractions.get(articleId);
            if (existing == null) {
                // 文章首次出现
                Map<String, Object> interaction = new HashMap<>();
                interaction.put("articleId", articleId);
                interaction.put("commentId", commentId);
                interaction.put("replyId", reply.getReplyId());
                interaction.put("content", reply.getReplyContent());
                interaction.put("time", reply.getReplyTime());
                interaction.put("source", "reply");
                articleInteractions.put(articleId, interaction);
            } else {
                // 文章已有记录，如果回复更新则覆盖为回复内容
                String existingTime = (String) existing.get("time");
                String replyTime = reply.getReplyTime();
                if (replyTime != null && (existingTime == null || replyTime.compareTo(existingTime) > 0)) {
                    existing.put("commentId", commentId);
                    existing.put("replyId", reply.getReplyId());
                    existing.put("content", reply.getReplyContent());
                    existing.put("time", reply.getReplyTime());
                    existing.put("source", "reply");
                }
            }
        }

        // 4. 按最新互动时间排序（最新的在前）
        List<Map<String, Object>> sortedList = new ArrayList<>(articleInteractions.values());
        sortedList.sort((a, b) -> {
            String t1 = (String) a.getOrDefault("time", "");
            String t2 = (String) b.getOrDefault("time", "");
            return t2.compareTo(t1);
        });

        // 5. 查询文章信息并构建返回结果
        List<Integer> articleIds = sortedList.stream()
                .map(m -> (Integer) m.get("articleId"))
                .distinct()
                .collect(Collectors.toList());

        if (articleIds.isEmpty()) {
            return ResultBean.success("查询成功", Collections.emptyMap());
        }

        List<Article> articles = articleService.listByIds(articleIds);
        Map<Integer, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getArticleId, a -> a, (a, b) -> a));

        List<ReplyListItemVO> resultList = new ArrayList<>();
        for (Map<String, Object> interaction : sortedList) {
            Integer articleId = (Integer) interaction.get("articleId");
            Article article = articleMap.get(articleId);
            if (article == null) continue;

            ReplyListItemVO item = new ReplyListItemVO();
            item.setArticleId(articleId);
            item.setArticleTitle(article.getArticleTitle());
            item.setArticleImage(article.getArticleImage());
            item.setCommentId((Integer) interaction.get("commentId"));
            item.setReplyId((Integer) interaction.get("replyId"));
            item.setContent((String) interaction.get("content"));
            item.setTime((String) interaction.get("time"));
            resultList.add(item);
        }

        // 6. 分页
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
