package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.FeaturedRecommendation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FeaturedRecommendationMapper extends BaseMapper<FeaturedRecommendation> {

    @Select("SELECT COUNT(*) FROM bbs_featured_recommendation WHERE article_id = #{articleId} AND status = 'pending'")
    int countPendingByArticle(Integer articleId);
}
