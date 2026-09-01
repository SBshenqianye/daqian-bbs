package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.vo.ResultBean;

/**
 * 精华帖推荐审批服务接口
 */
public interface FeaturedRecommendationService extends IService<FeaturedRecommendation> {

    /**
     * 版主推荐帖子为精华（初审）
     */
    ResultBean recommend(Integer articleId, Integer recommenderId, Integer labelId);

    /**
     * 总运营审核推荐（终审）
     * 批准时将帖子设为精华并加分，拒绝时通知推荐人
     */
    ResultBean review(Integer recommendationId, String status, String remark, Integer reviewerId);

    /**
     * 查询待审核推荐列表（管理端）
     */
    ResultBean listRecommendations(Integer page, Integer size, String status);
}
