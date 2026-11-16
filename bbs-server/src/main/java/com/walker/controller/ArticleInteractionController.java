package com.walker.controller;

import com.walker.service.ArticleInteractionService;
import com.walker.service.CurrentUserService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 帖子点赞/收藏/分享。
 */
@Api(tags = "帖子互动")
@RestController
public class ArticleInteractionController {

    @Autowired
    private ArticleInteractionService articleInteractionService;

    @Autowired
    private CurrentUserService currentUserService;

    @ApiOperation("点赞/取消点赞（登录用户）")
    @PostMapping("/article/like")
    public ResultBean like(@RequestBody Map<String, Object> body) {
        Integer userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResultBean.error("请先登录");
        }
        return articleInteractionService.toggleLike(userId, toInt(body.get("articleId")));
    }

    @ApiOperation("收藏/取消收藏（登录用户）")
    @PostMapping("/article/favorite")
    public ResultBean favorite(@RequestBody Map<String, Object> body) {
        Integer userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResultBean.error("请先登录");
        }
        return articleInteractionService.toggleFavorite(userId, toInt(body.get("articleId")));
    }

    @ApiOperation("分享计数")
    @PostMapping("/article/share")
    public ResultBean share(@RequestBody Map<String, Object> body) {
        return articleInteractionService.share(toInt(body.get("articleId")));
    }

    @ApiOperation("当前用户对帖子的点赞/收藏状态")
    @GetMapping("/common/article/interaction")
    public ResultBean interaction(@RequestParam Integer articleId) {
        return articleInteractionService.getInteraction(currentUserService.getCurrentUserId(), articleId);
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (Exception e) { return null; }
    }
}
