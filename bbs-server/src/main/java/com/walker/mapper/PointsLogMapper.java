package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.PointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 积分调整日志 Mapper 接口
 */
@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {

    /**
     * 查询指定用户的手动积分调整总和
     * @param userId 用户ID
     * @return 积分调整总和（正数加分总额 - 负数扣分总额）
     */
    Integer sumPointsChangeByUserId(@Param("userId") Integer userId);

    /**
     * 统计用户在指定主题帖下已获得的回帖积分次数（评论+回复）
     * 同一篇主题帖下同一用户最多累计获得3次回帖积分
     * @param userId 用户ID
     * @param articleId 主题帖ID
     * @return 已获得的回帖积分次数
     */
    Integer countReplyPointsForArticle(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 统计用户在指定主题帖下已获得的最佳解答采纳积分次数
     * 同一篇主题帖下同一用户最多获得1次最佳解答积分
     * @param userId 用户ID
     * @param articleId 主题帖ID
     * @return 已获得的采纳积分次数
     */
    Integer countAdoptPointsForArticle(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 查询指定文章是否已被采纳为建议（通过积分日志中"建议被采纳积分"记录判断）
     * @param articleId 文章ID
     * @return 已采纳记录数
     */
    Integer countSuggestionAdoptForArticle(@Param("articleId") Integer articleId);
}
