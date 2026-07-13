package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Article;
import com.walker.vo.InformationVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.param.PointsRankParam;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author walker
 * @since 2022/05/20 14:26
 */
public interface ArticleService extends IService<Article> {


    /**
     * 发布文章
     * @param articleParam
     * @return
     */
    ResultBean publish(ArticleParam articleParam);

    /**
     * 获取顶部五条推荐文章的信息
     * @return
     */
    List<Article> queryHeaderRecommend();

    /**
     * 获取推荐文章
     * @return
     */
    List<Article> queryRecommend();

    /**
     * 获取最新文章
     * @return
     */
    List<Article> queryNewest();


    /**
     * 获取热榜
     * @return
     */
    List<Article> queryHot();

    /**
     * 通过文章Id查询文章
     * @return
     */
    Article queryArticleById(Integer articleId);

    /**
     * 查询所有的文章列表
     * @param keywords 关键词
     * @return
     */
    List<Article> queryAllArticleList(String keywords);

    /**
     * 获取社区文章按照发布时间排序
     * @param communityId
     * @return
     */
    List<Article> queryArticleByCommunityId(Integer communityId);


    /**
     * 获取社区的文章按照浏览量降序排列
     * @param communityId
     * @return
     */
    List<Article> getArticleByHotAndOrderByDesc(Integer communityId);

    /**
     * 通过关键词搜索文章
     * @param keywords
     * @return
     */
    List<Article> getArticleByKeywords(String keywords);

    /**
     * 文章表收藏数量加 1
     * @param articleId
     */
    void articleGoodNumPlusOne(Integer articleId);

    /**
     * 通过文章的id批量查询文章
     * @param articleIds
     * @return
     */
    List<Article> getArticlesByIds(List<Integer> articleIds);

    /**
     * 通过用户id查询文章
     * @param userId
     * @return
     */
    List<Article> getArticleByUserId(Integer userId);

    /**
     * 获取用户的消息
     * @param userId
     * @return
     */
    List<InformationVO> getMyInformation(Integer userId);


    /**
     * 获取所有的已经审核通过的文章
     * @return
     */
    ResultBean getAliveArticles();


    /**
     * 查询所有未审核的文章
     * @return
     */
    ResultBean getNotAliveArticles();


    /**
     * 通过文章id删除文章
     * @param articleId
     * @return
     */
    ResultBean adminDeleteArticleByArticleId(Integer articleId);

    /**
     * 修改文章状态值 isAlive
     * @param articleId
     * @return
     */
    ResultBean auditArticleByArticleId(Integer articleId);


    /**
     * 删除所有通过审核的文章
     */
    void handleBatchDeleteArticlesByAlive();

    /**
     * 批量审核通过文章
     */
    void batchAudit();

    /**
     * 通过文章id获取文章信息
     * @param articleId
     * @return
     */
    ResultBean getArticleByArticle(Integer articleId);


    /**
     * 查询文章总数
     * @return
     */
    ResultBean getArticleCount();


    /**
     * 获取未通过审核的文章数量
     * @return
     */
    ResultBean getArticleCountWithNotPass();

    /**
     * 通过文章id删除文章
     * @param articleId
     * @return
     */
    ResultBean deleteArticleByArticleId(Integer articleId);


    /**
     * 编辑文章
     * @param articleParam
     * @return
     */
    ResultBean editArticle(ArticleParam articleParam);

    /**
     * 方法描述 按单位统计文章数
     * @author chengQing
     * @date 2026/3/6 10:37
     * @param articleStatisticParam 查询条件实体类
     * @return ResultBean 返回结果集
     */
    ResultBean articleStatisticByOrg(ArticleStatisticParam articleStatisticParam);

    /**
     * 方法描述 获取积分排名
     * @author chengQing
     * @date 2026/3/9 14:50
     * @param pointsRankParam 查询条件
     * @return ResultBean 返回结果
     */
    ResultBean pointsRank(PointsRankParam pointsRankParam);

    /**
     * 管理员获取文章列表（支持搜索过滤分页，含标签名）
     */
    ResultBean getAdminArticleList(String keywords, String labelId, String startTime, String endTime, Integer enable, Integer page, Integer size);

    /**
     * 设置/取消精华帖
     */
    ResultBean setFeatured(Integer articleId, Integer isFeatured);

    /**
     * 查询精华帖列表（管理端，支持搜索过滤分页）
     */
    ResultBean getFeaturedList(String keywords, String labelId, String startTime, String endTime, Integer page, Integer size);

    /**
     * 查询精华帖列表（用户端，分页）
     */
    ResultBean getFeaturedByPage(Integer page, Integer size, Integer labelId);

    /**
     * 获取最新精华帖（置顶用）
     */
    ResultBean getFeaturedTop(int limit);
}
