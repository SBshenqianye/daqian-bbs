package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Article;
import com.walker.vo.PointsRankVO;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.ArticleStatisticVO;
import com.walker.vo.param.PointsRankParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author walker
 * @since 2022/05/20 14:26
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    /**
     * 方法描述 按单位统计文章数
     * @author chengQing
     * @date 2026/3/6 10:42
     * @param articleStatisticParam 查询条件实体类
     * @return List<ArticleStatisticVO>
     */
    List<ArticleStatisticVO> articleStatisticByOrg(ArticleStatisticParam articleStatisticParam);

    /**
     * 方法描述 获取积分排名
     * @author chengQing
     * @date 2026/3/9 15:02
     * @param pointsRankParam 查询条件实
     * @return List<PointsRankVO> 返回结果集
     */
    List<PointsRankVO> pointsRank(PointsRankParam pointsRankParam);

    /**
     * 管理员查询全部文章列表（支持搜索过滤分页，含标签名）
     */
    List<Article> selectAdminArticleList(Map<String, Object> params);

    /**
     * 管理员查询文章列表总数
     */
    int countAdminArticleList(Map<String, Object> params);

    /**
     * 查询精华帖列表（管理端，支持搜索过滤）
     */
    List<Article> selectFeaturedList(Map<String, Object> params);

    /**
     * 查询精华帖总数（管理端）
     */
    int countFeaturedList(Map<String, Object> params);

    /**
     * 查询全部精华帖（用户端，分页）
     */
    List<Article> selectFeaturedByPage(Map<String, Object> params);

    /**
     * 查询精华帖总数（用户端）
     */
    int countFeaturedByPage(Map<String, Object> params);

    /**
     * 查询最新 N 条精华帖（置顶用）
     */
    List<Article> selectFeaturedTop(@Param("limit") int limit);
}
