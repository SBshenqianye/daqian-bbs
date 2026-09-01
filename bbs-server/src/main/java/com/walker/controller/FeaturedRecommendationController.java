package com.walker.controller;

import com.walker.service.FeaturedRecommendationService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 精华帖推荐审批控制器
 */
@Api(tags = "FeaturedRecommendationController")
@RestController
public class FeaturedRecommendationController {

    @Autowired
    private FeaturedRecommendationService recommendationService;

    @ApiOperation(value = "版主推荐帖子为精华（初审）")
    @PostMapping("/article/recommendFeatured")
    public ResultBean recommend(@RequestBody Map<String, Object> params) {
        Integer articleId = params.get("articleId") != null ? Integer.parseInt(params.get("articleId").toString()) : null;
        Integer recommenderId = params.get("recommenderId") != null ? Integer.parseInt(params.get("recommenderId").toString()) : null;
        Integer labelId = params.get("labelId") != null ? Integer.parseInt(params.get("labelId").toString()) : null;
        return recommendationService.recommend(articleId, recommenderId, labelId);
    }

    @ApiOperation(value = "总运营审核精华推荐（终审）")
    @PostMapping("/admin/featured/review")
    public ResultBean review(@RequestBody Map<String, Object> params) {
        Integer recommendationId = params.get("recommendationId") != null ? Integer.parseInt(params.get("recommendationId").toString()) : null;
        String status = (String) params.get("status");
        String remark = (String) params.get("remark");
        Integer reviewerId = params.get("reviewerId") != null ? Integer.parseInt(params.get("reviewerId").toString()) : null;
        return recommendationService.review(recommendationId, status, remark, reviewerId);
    }

    @ApiOperation(value = "查询精华推荐列表（管理端）")
    @PostMapping("/admin/featured/recommendList")
    public ResultBean listRecommendations(@RequestBody Map<String, Object> params) {
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        String status = (String) params.get("status");
        return recommendationService.listRecommendations(page, size, status);
    }
}
