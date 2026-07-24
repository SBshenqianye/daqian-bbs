package com.walker.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.*;
import com.walker.service.*;
import com.walker.service.ArticleLabelService;
import com.walker.utils.ConstantUtil;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.InformationVO;
import com.walker.vo.PointsRankVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.ArticleStatisticVO;
import com.walker.vo.param.PointsRankParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author walker
 * @since 2022/05/20 14:26
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleUserService articleUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private DictService dictService;

    @Autowired
    private ArticleFileService articleFileService;

    @Autowired
    private ArticleLabelService articleLabelService;

    @Autowired
    private SaOrgMapper saOrgMapper;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 发布文章
     * @param articleParam
     * @return
     */
    @Override
    public ResultBean publish(ArticleParam articleParam) {

        // 校验标签是否存在且未被禁用
        Integer labelId = articleParam.getArticleLabelId();
        if (labelId != null && labelId > 0) {
            ArticleLabel label = articleLabelService.getById(labelId);
            if (label == null) {
                return ResultBean.error("所选标签不存在");
            }
            if (label.getEnabled() == null || label.getEnabled() != 1) {
                return ResultBean.error("所选标签已被禁用，请重新选择");
            }
        }
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String day = format.format(date);

        Article article = new Article();
        article.setArticleLabelId(labelId);
        article.setArticleAuthor(articleParam.getArticleAuthor());
        article.setArticleTitle(articleParam.getArticleTitle());
        article.setArticleSummary(articleParam.getArticleSummary());
        article.setArticleTypeId(articleParam.getArticleTypeId());
        article.setArticleContent(articleParam.getArticleContent());
        article.setArticleContentHtml(articleParam.getArticleContentHtml());
        article.setArticleImage(articleParam.getArticleImage());
        article.setUserId(articleParam.getUserId());
        article.setArticleGoodNum(0);
        article.setArticleViewNum(0);
        // 默认发布后就显示，默认审核通过，管理后台取消审核功能
        article.setEnable(1);
        article.setArticleCommunityId(articleParam.getArticleCommunityId());
        article.setCreateTime(day);

        this.save(article);
        // 如果 文章附件不为空，则添加文章和附件的绑定关系
        if (articleParam.getFiles() != null && articleParam.getFiles().length > 0) {
            articleFileService.updateArticleFile(articleParam.getFiles(), article.getArticleId());
        }

        return ResultBean.success("发布成功！");
    }

    /**
     * 获取顶部五条推荐文章的信息
     * @return
     */
    @Override
    public List<Article> queryHeaderRecommend() {

        return articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        // 查询recommend >= 1的记录
                        .ge(Article::getRecommend,1)
                        .select(Article::getArticleId,Article::getArticleTitle,Article::getCreateTime)
                        //只查询 5 条数据
                        .last("limit 5")

        );

    }

    /**
     * 获取推荐文章
     * @return
     */
    @Override
    public List<Article> queryRecommend() {

        return articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        //按照点赞数量推荐
                        .orderByDesc(Article::getArticleGoodNum)
                        .select(Article::getArticleId,Article::getArticleTitle,Article::getArticleAuthor)
                        //只查询 3 条数据
                        .last("limit 3")

        );
    }

    /**
     * 获取最新文章
     * @return
     */
    @Override
    public List<Article> queryNewest() {

        return articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        .orderByDesc(Article::getCreateTime)
                        .select(Article::getArticleId,Article::getArticleTitle,Article::getArticleAuthor)
                        //只查询 3 条数据
                        .last("limit 3")

        );
    }

    /**
     * 获取热榜
     * @return
     */
    @Override
    public List<Article> queryHot() {

        return articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        .orderByDesc(Article::getArticleViewNum)
                        .select(Article::getArticleId,Article::getArticleTitle)
                        //只查询 3 条数据
                        .last("limit 10")

        );
    }

    /**
     * 通过文章Id查询文章
     * @return
     */
    @Override
    public Article queryArticleById(Integer articleId) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 1)
                        .eq(Article::getArticleId, articleId)
        );
        // 更新浏览量
        article.setArticleViewNum(article.getArticleViewNum() + 1);
        articleMapper.updateById(article);
        return article;
        // return this.getById(articleId);
    }

    /**
     * 查询所有的文章列表
     * @return
     */
    @Override
    public List<Article> queryAllArticleList(String keywords) {
        List<Article> articles;
        if (StringUtils.isEmpty(keywords)) {
            articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                    .eq(Article::getEnable,1)
                    .orderByDesc(Article::getCreateTime)
            );
        } else {
            // 先模糊查询附件名称，查询出附件名称能匹配的文章Id
            List<ArticleFile> articleFileList = articleFileService.getArticleFileByKeywords(keywords);
            if (!CollectionUtils.isEmpty(articleFileList)) {
                // 拿到文章Id
                List<Integer> distinctArticleIds = articleFileList.stream()
                        .map(ArticleFile::getArticleId)
                        .distinct()
                        .collect(Collectors.toList());

                articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        .like(Article::getArticleContent, keywords)
                        .or()
                        .like(Article::getArticleTitle, keywords)
                        .or()
                        .in(Article::getArticleId, distinctArticleIds)
                        .orderByDesc(Article::getCreateTime)
                );
            } else {
                articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable,1)
                        .like(Article::getArticleContent, keywords)
                        .or()
                        .like(Article::getArticleTitle, keywords)
                        .orderByDesc(Article::getCreateTime)
                );
            }
        }
        // 批量填充用户头像和组织信息
        enrichWithUserInfo(articles);
        // 批量填充评论数量
        enrichWithCommentCounts(articles);
        return articles;
    }

    private void enrichWithUserInfo(List<Article> articles) {
        Set<Integer> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;

        // 批量查询用户并解析组织信息
        List<User> users = userService.listUsersWithOrgInfo(userIds);
        Map<Integer, User> userMap = users.stream()
                .filter(u -> u.getPortrait() != null || u.getOrgName() != null)
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 批量查询组织表，构建 orgNo → orgName 映射（用于 display 过滤后的完整名称）
        Map<String, String> orgNameMap = new HashMap<>();
        Set<String> orgNos = users.stream()
                .map(User::getOrgNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!orgNos.isEmpty()) {
            List<SaOrg> orgs = saOrgMapper.selectList(
                    new LambdaQueryWrapper<SaOrg>()
                            .in(SaOrg::getOrgNo, orgNos)
                            .eq(SaOrg::getIsDelete, 0)
            );
            for (SaOrg org : orgs) {
                orgNameMap.put(org.getOrgNo(), org.getOrgName());
            }
        }

        articles.forEach(a -> {
            if (a.getUserId() != null && userMap.containsKey(a.getUserId())) {
                User u = userMap.get(a.getUserId());
                if (u.getPortrait() != null) {
                    a.setPortrait(u.getPortrait());
                }
                a.setAuthorOrgName(u.getOrgName());
                String fullOrgName = orgNameMap.get(u.getOrgNo());
                a.setAuthorOrgNameFull(fullOrgName != null ? fullOrgName : u.getOrgName());
                a.setAuthorDeptName(u.getDeptName());
            }
        });
    }

    private void enrichWithCommentCounts(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) return;
        List<Integer> articleIds = articles.stream()
                .map(Article::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (articleIds.isEmpty()) return;
        List<Map<String, Object>> counts = commentMapper.countByArticleIds(articleIds);
        Map<Integer, Integer> countMap = counts.stream()
                .filter(m -> m.get("articleId") != null)
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("articleId")).intValue(),
                        m -> {
                            Object count = m.get("commentCount");
                            if (count == null) return 0;
                            return ((Number) count).intValue();
                        },
                        Integer::sum
                ));
        articles.forEach(a -> {
            if (a.getArticleId() != null && countMap.containsKey(a.getArticleId())) {
                a.setCommentNum(countMap.get(a.getArticleId()));
            }
        });
    }

    @Override
    public List<Article> queryArticleByCommunityId(Integer communityId) {

        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Article::getEnable,1)
                .eq(Article::getArticleCommunityId, communityId)
                .orderByDesc(Article::getCreateTime);
        return articleMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public List<Article> getArticleByHotAndOrderByDesc(Integer communityId) {
        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Article::getEnable,1)
                .eq(Article::getArticleCommunityId, communityId)
                .orderByDesc(Article::getArticleViewNum);
        return articleMapper.selectList(lambdaQueryWrapper);

    }

    @Override
    public List<Article> getArticleByKeywords(String keywords) {
        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Article::getEnable,1)
                .like(Article::getArticleContent, keywords)
                .or()
                .like(Article::getArticleTitle, keywords);
        return articleMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public void articleGoodNumPlusOne(Integer articleId) {
        Article article = articleMapper.selectById(articleId);

        LambdaUpdateWrapper<Article> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(Article::getArticleId,articleId).set(Article::getArticleGoodNum, article.getArticleGoodNum() + 1);
        articleMapper.update(null,lambdaUpdateWrapper);
    }

    @Override
    public List<Article> getArticlesByIds(List<Integer> articleIds) {

        return articleMapper.selectBatchIds(articleIds);

    }

    @Override
    public List<Article> getArticleByUserId(Integer userId) {
        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper
                .eq(Article::getEnable,1)
                .eq(Article::getUserId, userId)
                .orderByDesc(Article::getCreateTime);
        List<Article> articles = articleMapper.selectList(lambdaQueryWrapper);
        enrichWithUserInfo(articles);
        enrichWithCommentCounts(articles);
        return articles;
    }

    @Override
    public List<InformationVO> getMyInformation(Integer userId) {

        // 通过我的id查询我的文章id
        LambdaQueryWrapper<Article> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Article::getUserId, userId);
        List<Article> articles = articleMapper.selectList(lambdaQueryWrapper);

        if (articles.size() > 0){
            List<Integer> allArticleIds = articles.stream().map(Article::getArticleId).collect(Collectors.toList());

            // 通过我的文章id查询 用户与我的文章直接的关系
            List<ArticleUser> articleUsers = articleUserService.getArticleUserByArticleIds(allArticleIds);

            if (articleUsers.size() > 0){
                ArrayList<InformationVO> list = new ArrayList<>();
                for (ArticleUser articleUser: articleUsers) {
                    InformationVO informationVO = new InformationVO();

                    // 用户
                    User user = userService.getUserInfoByUserId(articleUser.getUserId());
                    // 文章
                    Article article = articleMapper.selectById(articleUser.getArticleId());
                    // 时间
                    String time = articleUser.getCreateTime();

                    informationVO.setArticleId(article.getArticleId())
                            .setArticleName(article.getArticleTitle())
                            .setUserId(user.getId())
                            .setNickName(user.getNickname())
                            .setPortrait(user.getPortrait())
                            .setTime(time);
                    list.add(informationVO);

                }
                return list;
            }
            return null;
        }
        return null;
    }

    @Override
    public ResultBean getAliveArticles() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 1)
        );
        return ResultBean.success("查询成功！", SensitiveWordUtil.desensitizeArticles(articles));
    }

    @Override
    public ResultBean getNotAliveArticles() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 0)
        );
        return ResultBean.success("查询成功！",SensitiveWordUtil.desensitizeArticles(articles));
    }

    @Override
    public ResultBean adminDeleteArticleByArticleId(Integer articleId) {
        articleMapper.deleteById(articleId);
        return ResultBean.success("删除成功！");
    }

    @Override
    public ResultBean auditArticleByArticleId(Integer articleId) {
        Article article = new Article();
        article.setArticleId(articleId)
                .setEnable(1);
        articleMapper.updateById(article);
        return ResultBean.success("修改成功！");
    }

    @Override
    public void handleBatchDeleteArticlesByAlive() {
        articleMapper.delete(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 1)
        );
    }

    @Override
    public void batchAudit() {
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 0)
        );
        articles.forEach(article -> {
            Article newArticle = new Article();
            newArticle.setArticleId(article.getArticleId())
                    .setEnable(1);
            articleMapper.updateById(newArticle);
        });

    }

    @Override
    public ResultBean getArticleByArticle(Integer articleId) {

        Article article = articleMapper.selectById(articleId);
        return ResultBean.success("成功查询文章！",SensitiveWordUtil.desensitizeArticle(article));
    }

    @Override
    public ResultBean getArticleCount() {

        Long count = articleMapper.selectCount(null);
        return ResultBean.success("获取成功！",count);

    }


    @Override
    public ResultBean getArticleCountWithNotPass() {

        Long count = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 0)
        );
        return ResultBean.success("获取成功！",count);
    }

    @Override
    public ResultBean deleteArticleByArticleId(Integer articleId) {
        articleMapper.deleteById(articleId);
        return ResultBean.success("删除成功！");
    }

    @Override
    public ResultBean editArticle(ArticleParam articleParam) {
        Article article = new Article();

        article.setArticleId(articleParam.getArticleId())
                .setArticleTitle(articleParam.getArticleTitle())
                .setArticleContent(articleParam.getArticleContent())
                .setArticleContentHtml(articleParam.getArticleContentHtml())
                .setArticleSummary(articleParam.getArticleSummary())
                .setArticleTypeId(articleParam.getArticleTypeId())
                .setArticleCommunityId(articleParam.getArticleCommunityId())
                .setArticleLabelId(articleParam.getArticleLabelId());
        // System.out.println("articleParam.getArticleImage() = " + articleParam.getArticleImage());
        if (articleParam.getArticleImage() != null){
            article.setArticleImage(articleParam.getArticleImage());
        }

        articleMapper.updateById(article);
        // 如果 文章附件不为空，则添加文章和附件的绑定关系
        if (articleParam.getFiles() != null && articleParam.getFiles().length > 0) {
            // 先解绑再绑定
            articleFileService.unBindArticleFile(article.getArticleId());
            // 绑定文章和附件关系
            articleFileService.updateArticleFile(articleParam.getFiles(), article.getArticleId());
        }
        return ResultBean.success("发布成功！");
    }

    @Override
    public ResultBean articleStatisticByOrg(ArticleStatisticParam articleStatisticParam) {
        List<ArticleStatisticVO> resultList = articleMapper.articleStatisticByOrg(articleStatisticParam);
        if (CollectionUtils.isEmpty(resultList)) {
            resultList = Arrays.asList();
        } else {
            resultList.stream().forEach(item -> {
                if (articleStatisticParam.getOrgNo().equals(item.getOrgNo()) || item.getOrgNo().length() == ConstantUtil.MANA_NINE) {
                    item.setIsSelf(0);
                } else {
                    item.setIsSelf(1);
                }
            });
        }
        return ResultBean.success("查询成功", resultList);
    }

    @Override
    public ResultBean pointsRank(PointsRankParam pointsRankParam) {
        // 如果单位为空就默认填充 内江单位编号
        if (StringUtils.isEmpty(pointsRankParam.getOrgNo())){
            pointsRankParam.setOrgNo(ConstantUtil.ORG_NEI_JIANG);
        }
        // 按单位统计查询当前层级和下级 发布文章数
        pointsRankParam.setOrgLength(pointsRankParam.getOrgNo().length()+2);
        // 查询配置中-发帖和回帖积分
        List<Dict> postList = dictService.listDictByType(ConstantUtil.MANA_POST);
        List<Dict> replyList = dictService.listDictByType(ConstantUtil.MANA_REPLY);
        if (CollectionUtils.isEmpty(postList) || CollectionUtils.isEmpty(replyList)) {
            return ResultBean.error("积分计算发帖回帖配置不全，请联系管理员");
        }
        pointsRankParam.setPost(Integer.parseInt(postList.get(0).getDictValue()));
        pointsRankParam.setReply(Integer.parseInt(replyList.get(0).getDictValue()));
        // 查询配置中-精华帖积分
        List<Dict> featuredList = dictService.listDictByType(ConstantUtil.MANA_FEATURED);
        if (!CollectionUtils.isEmpty(featuredList)) {
            pointsRankParam.setFeatured(Integer.parseInt(featuredList.get(0).getDictValue()));
        } else {
            pointsRankParam.setFeatured(0);
        }
        // 01：本月，02：累计，获取配置的开始和结束日期
        if (ConstantUtil.MANA_ZERO_ONE.equals(pointsRankParam.getRankType())) {
            pointsRankParam.setStartTime(DateUtil.formatDate(DateUtil.beginOfMonth(new Date())));
            pointsRankParam.setEndTime(DateUtil.formatDate(DateUtil.endOfMonth(new Date())));
        } else {
            // 查询累计排名配置的开始和结束时间
            List<Dict> startTimeList = dictService.listDictByType(ConstantUtil.MANA_POINTS_START_TIME);
            List<Dict> endTimeList = dictService.listDictByType(ConstantUtil.MANA_POINTS_END_TIME);
            if (CollectionUtils.isEmpty(startTimeList) || CollectionUtils.isEmpty(endTimeList) || CollectionUtils.isEmpty(postList) || CollectionUtils.isEmpty(replyList)) {
                return ResultBean.error("累积排名日期配置不全，请联系管理员");
            }
            pointsRankParam.setStartTime(startTimeList.get(0).getDictValue());
            pointsRankParam.setEndTime(endTimeList.get(0).getDictValue());
        }

        List<PointsRankVO> resultList = articleMapper.pointsRank(pointsRankParam);
        if (CollectionUtils.isEmpty(resultList)) {
            resultList = Arrays.asList();
        } else {
            resultList.stream().forEach(item -> {
                if (pointsRankParam.getOrgNo().equals(item.getOrgNo()) || item.getOrgNo().length() == ConstantUtil.MANA_NINE) {
                    item.setIsSelf(0);
                } else {
                    item.setIsSelf(1);
                }
            });
        }

        // 排名单位过滤：如果 bbs_sa_org 中有勾选了参与排名的单位，只显示这些单位
        if (!CollectionUtils.isEmpty(resultList)) {
            List<SaOrg> rankingOrgs = saOrgMapper.selectList(
                    new LambdaQueryWrapper<SaOrg>()
                            .eq(SaOrg::getIsRankingSelected, 1)
                            .eq(SaOrg::getIsDelete, 0)
            );
            if (!CollectionUtils.isEmpty(rankingOrgs)) {
                List<String> allowedOrgNos = rankingOrgs.stream()
                        .map(SaOrg::getOrgNo)
                        .collect(Collectors.toList());
                resultList = resultList.stream()
                        .filter(item -> allowedOrgNos.contains(item.getOrgNo()))
                        .collect(Collectors.toList());
                int[] rank = {1};
                resultList.forEach(item -> item.setRankNum(rank[0]++));
            }
        }

        return ResultBean.success("查询成功", resultList);
    }

    @Override
    public ResultBean getAdminArticleList(String keywords, String labelId, String startTime, String endTime, Integer enable, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Map<String, Object> params = new HashMap<>();
        params.put("keywords", keywords);
        params.put("labelId", labelId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("enable", enable);

        int total = articleMapper.countAdminArticleList(params);
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("size", size);

        List<Article> list = articleMapper.selectAdminArticleList(params);
        if (list == null) list = new ArrayList<>();

        enrichWithUserInfo(list);
        enrichWithCommentCounts(list);

        // 脱敏处理
        SensitiveWordUtil.desensitizeArticles(list);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        return ResultBean.success("查询成功", result);
    }

    @Override
    public ResultBean setFeatured(Integer articleId, Integer isFeatured) {
        if (articleId == null) {
            return ResultBean.error("文章ID不能为空");
        }
        Article article = new Article();
        article.setArticleId(articleId).setIsFeatured(isFeatured);
        articleMapper.updateById(article);
        return ResultBean.success(isFeatured == 1 ? "已设为精华帖" : "已取消精华帖");
    }

    @Override
    public ResultBean getFeaturedList(String keywords, String labelId, String startTime, String endTime, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Map<String, Object> params = new HashMap<>();
        params.put("keywords", keywords);
        params.put("labelId", labelId);
        params.put("startTime", startTime);
        params.put("endTime", endTime);

        int total = articleMapper.countFeaturedList(params);
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("size", size);

        List<Article> list = articleMapper.selectFeaturedList(params);
        if (list == null) list = new ArrayList<>();

        // 补充用户头像
        enrichWithUserInfo(list);
        // 补充评论数
        enrichWithCommentCounts(list);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return ResultBean.success("查询成功", result);
    }

    @Override
    public ResultBean getFeaturedByPage(Integer page, Integer size, Integer labelId) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        Map<String, Object> params = new HashMap<>();
        if (labelId != null && labelId > 0) {
            params.put("labelId", labelId);
        }
        int total = articleMapper.countFeaturedByPage(params);
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("size", size);

        List<Article> list = articleMapper.selectFeaturedByPage(params);
        if (list == null) list = new ArrayList<>();

        enrichWithUserInfo(list);
        enrichWithCommentCounts(list);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) total / size));
        return ResultBean.success("查询成功", result);
    }

    @Override
    public ResultBean getFeaturedTop(int limit) {
        if (limit <= 0) limit = 3;
        List<Article> list = articleMapper.selectFeaturedTop(limit);
        if (list == null) list = new ArrayList<>();
        enrichWithUserInfo(list);
        enrichWithCommentCounts(list);
        return ResultBean.success("查询成功", list);
    }

}
