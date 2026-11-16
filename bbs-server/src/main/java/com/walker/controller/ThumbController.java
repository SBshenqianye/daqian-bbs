package com.walker.controller;

import com.walker.service.CurrentUserService;
import com.walker.service.ThumbService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 评论 / 回帖点赞。
 */
@Api(tags = "评论回帖点赞")
@RestController
public class ThumbController {

    @Autowired
    private ThumbService thumbService;

    @Autowired
    private CurrentUserService currentUserService;

    @ApiOperation("点赞/取消点赞（评论、回帖）")
    @PostMapping("/common/thumb")
    public ResultBean toggle(@RequestBody Map<String, Object> body) {
        Integer userId = currentUserService.getCurrentUserId();
        if (userId == null) {
            return ResultBean.error("请先登录");
        }
        if (body == null) {
            return ResultBean.error("参数不完整");
        }
        String targetType = body.get("targetType") == null ? null : String.valueOf(body.get("targetType"));
        Integer targetId = toInt(body.get("targetId"));
        return thumbService.toggle(userId, targetType, targetId);
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
