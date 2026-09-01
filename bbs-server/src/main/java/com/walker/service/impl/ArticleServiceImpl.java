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
import com.walker.utils.ContentQualityUtil;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.InformationVO;
import com.walker.vo.PointsRankVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.ArticleStatisticVO;
import com.walker.vo.param.PointsRankParam;
import com.walker.vo.param.PersonalPointsRankParam;
import com.walker.vo.PersonalPointsRankVO;
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
    private SaOrgService saOrgService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PointsLogService pointsLogService;

    /**
     * 发布文章
     * @param articleParam
     * @return
     */
    @Override
    public ResultBean publish(ArticleParam articleParam) {

        // ── 发帖权限校验 ──
        Integer userId = articleParam.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null && user.getPostRestricted() != null && user.getPostRestricted() == 1) {
                // 检查限制是否已过期
                if (user.getPostRestrictedUntil() != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date until = sdf.parse(user.getPostRestrictedUntil());
                        if (new Date().before(until)) {
                            return ResultBean.error("您的账号已被限制发帖，请联系管理员");
                        }
                        // 已过期，自动解除限制（LambdaUpdateWrapper 强制置空 post_restricted_until）
                        user.setPostRestricted(0);
                        userService.update(user, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                                .eq(User::getId, userId)
                                .set(User::getPostRestricted, 0)
                                .set(User::getPostRestrictedUntil, null));
                    } catch (Exception e) {
                        // 解析失败视为永久限制
                        return ResultBean.error("您的账号已被限制发帖，请联系管理员");
                    }
                } else {
                    // postRestrictedUntil 为空 = 永久限制（leak 类型）
                    return ResultBean.error("您的账号已被限制发帖，请联系管理员");
                }
            }
        }

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
        // 标题校验：不能为空、不能包含图片/富文本语法（严格拒绝）
        String title = articleParam.getArticleTitle();
        if (title == null || title.trim().isEmpty()) {
            return ResultBean.error("标题不能为空");
        }
        if (title.matches(".*(<[^>]*>|!\\[[^\\]]*\\]\\([^)]*\\)).*")) {
            return ResultBean.error("标题不允许包含图片或富文本内容");
        }
        title = sanitizeTitle(title);
        // 内容不能为空（纯图片内容为 ![图片](url) 文本，非空，可正常通过）
        if (articleParam.getArticleContent() == null || articleParam.getArticleContent().trim().isEmpty()) {
            return ResultBean.error("内容不能为空");
        }

        // ── 内容质量检测：垃圾内容标记为不可见，不计入积分 ──
        ContentQualityUtil.QualityResult quality = ContentQualityUtil.checkContent(
                articleParam.getArticleTitle(), articleParam.getArticleContent());

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String day = format.format(date);

        Article article = new Article();
        article.setArticleLabelId(labelId);
        article.setArticleAuthor(articleParam.getArticleAuthor());
        article.setArticleTitle(title);
        article.setArticleSummary(articleParam.getArticleSummary());
        article.setArticleTypeId(articleParam.getArticleTypeId());
        article.setArticleContent(articleParam.getArticleContent());
        article.setArticleContentHtml(articleParam.getArticleContentHtml());
        article.setArticleImage(articleParam.getArticleImage());
        article.setUserId(articleParam.getUserId());
        article.setArticleGoodNum(0);
        article.setArticleViewNum(0);
        // 根据内容质量检测结果决定是否通过审核：垃圾内容标记为不可见（不计积分）
        article.setEnable(quality.isPassed() ? 1 : 0);
        article.setArticleCommunityId(articleParam.getArticleCommunityId());
        article.setCreateTime(day);

        this.save(article);
        // 如果 文章附件不为空，则添加文章和附件的绑定关系
        if (articleParam.getFiles() != null && articleParam.getFiles().length > 0) {
            articleFileService.updateArticleFile(articleParam.getFiles(), article.getArticleId());
        }

        // 发帖积分：只有通过质量检测的帖子才计分
        if (quality.isPassed()) {
            int postPoints = 2; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_POST);
                if (val != null) postPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(articleParam.getUserId(), postPoints, "发帖积分",
                    "article", article.getArticleId(), null);
        }

        // 垃圾内容提示用户
        if (quality.isSpam()) {
            return ResultBean.success("发布成功，但内容被判定为低质量，暂不展示且不计入积分");
        }
        return ResultBean.success("发布成功！");
    }

    /**
     * 标题净化：压缩空白，按码点截断为 30 字
     * 码点截断避免拆散 emoji 等代理对，且与 DB varchar(30) 按字符计数的语义一致
     * 返回 null 表示净化后为空（标题无效）
     */
    private String sanitizeTitle(String title) {
        if (title == null) return null;
        String cleaned = title.trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) return null;
        if (cleaned.codePointCount(0, cleaned.length()) > 30) {
            int end = cleaned.offsetByCodePoints(0, 30);
            cleaned = cleaned.substring(0, end);
        }
        return cleaned;
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

        // 批量解析显示层级名称（用户组织被隐藏时显示上一可见级）
        Set<String> orgNos = users.stream()
                .map(User::getOrgNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, String> orgNameMap = saOrgService.resolveDisplayOrgNames(orgNos);

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

    /**
     * 为"建议反馈"标签的帖子补充采纳状态（isSuggestionAdopted）
     */
    private void enrichWithSuggestionAdopted(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) return;
        // 找出标签名为"建议反馈"的帖子ID
        List<Integer> suggestionArticleIds = articles.stream()
                .filter(a -> a.getArticleId() != null && a.getArticleLabelName() != null
                        && "建议反馈".equals(a.getArticleLabelName()))
                .map(Article::getArticleId)
                .collect(Collectors.toList());
        if (suggestionArticleIds.isEmpty()) return;
        // 批量查询哪些已被采纳
        for (Article a : articles) {
            if (suggestionArticleIds.contains(a.getArticleId())) {
                int count = pointsLogService.countSuggestionAdoptForArticle(a.getArticleId());
                a.setIsSuggestionAdopted(count > 0);
            }
        }
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
    public Article getArticleByIdRaw(Integer articleId) {
        return articleMapper.selectByIdRaw(articleId);
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
        // 删除前先获取文章信息，用于扣回积分
        Article article = articleMapper.selectById(articleId);
        if (article != null && article.getEnable() != null && article.getEnable() == 1) {
            int postPoints = 2; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_POST);
                if (val != null) postPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(article.getUserId(), -postPoints, "删除帖子扣回积分",
                    "article", articleId, null);

            // 如果是精华帖，额外扣回精华加分
            if (article.getIsFeatured() != null && article.getIsFeatured() == 1) {
                int featuredPoints = 10; // default
                try {
                    String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                    if (val != null) featuredPoints = Integer.parseInt(val);
                } catch (Exception e) { /* use default */ }
                pointsLogService.adjustUserPoints(article.getUserId(), -featuredPoints, "删除精华帖扣回加分",
                        "article", articleId, null);
            }
        }
        articleMapper.deleteById(articleId);
        return ResultBean.success("删除成功！");
    }

    @Override
    public ResultBean editArticle(ArticleParam articleParam) {
        // 标题校验：不能为空、不能包含图片/富文本语法（严格拒绝）
        String title = articleParam.getArticleTitle();
        if (title == null || title.trim().isEmpty()) {
            return ResultBean.error("标题不能为空");
        }
        if (title.matches(".*(<[^>]*>|!\\[[^\\]]*\\]\\([^)]*\\)).*")) {
            return ResultBean.error("标题不允许包含图片或富文本内容");
        }
        title = sanitizeTitle(title);
        // 内容不能为空
        if (articleParam.getArticleContent() == null || articleParam.getArticleContent().trim().isEmpty()) {
            return ResultBean.error("内容不能为空");
        }

        Article article = new Article();

        article.setArticleId(articleParam.getArticleId())
                .setArticleTitle(title)
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
        // 按单位统计查询当前层级和下级
        pointsRankParam.setOrgLength(pointsRankParam.getOrgNo().length()+2);
        // 01：本月，02：累计，获取配置的开始和结束日期
        // 如果前端已传 startTime 和 endTime，直接使用；否则按 rankType 计算默认值
        if (StringUtils.isEmpty(pointsRankParam.getStartTime()) || StringUtils.isEmpty(pointsRankParam.getEndTime())) {
            if (ConstantUtil.MANA_ZERO_ONE.equals(pointsRankParam.getRankType())) {
                pointsRankParam.setStartTime(DateUtil.formatDate(DateUtil.beginOfMonth(new Date())));
                pointsRankParam.setEndTime(DateUtil.formatDate(DateUtil.endOfMonth(new Date())));
            } else {
                // 累计排名：优先从配置读取，没有则覆盖全部历史
                String startTime = dictService.getValueByKey(ConstantUtil.MANA_POINTS_START_TIME);
                String endTime = dictService.getValueByKey(ConstantUtil.MANA_POINTS_END_TIME);
                if (startTime != null && endTime != null) {
                    pointsRankParam.setStartTime(startTime);
                    pointsRankParam.setEndTime(endTime);
                } else {
                    // 未配置 → 覆盖全部历史
                    pointsRankParam.setStartTime("2000-01-01");
                    pointsRankParam.setEndTime(DateUtil.formatDate(new Date()));
                }
            }
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

        // 排名单位过滤：只显示在 admin-ui 中勾选了"参与排名"的单位
        if (!CollectionUtils.isEmpty(resultList)) {
            List<SaOrg> rankingOrgs = saOrgMapper.selectList(
                    new LambdaQueryWrapper<SaOrg>()
                            .eq(SaOrg::getIsRankingSelected, 1)
                            .eq(SaOrg::getIsDelete, 0)
            );
            if (CollectionUtils.isEmpty(rankingOrgs)) {
                // 没有勾选任何排名单位 → 返回空列表
                resultList = new ArrayList<>();
            } else {
                // 有勾选的 → 只显示勾选的那些
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

    /**
     * 获取积分配置（发帖/回帖/精华帖分值）
     */
    private int[] getPointsConfig() {
        int post = 3, reply = 1, featured = 10;
        try { String v = dictService.getValueByKey(ConstantUtil.MANA_POST); if (v != null) post = Integer.parseInt(v); } catch (Exception e) {}
        try { String v = dictService.getValueByKey(ConstantUtil.MANA_REPLY); if (v != null) reply = Integer.parseInt(v); } catch (Exception e) {}
        try { String v = dictService.getValueByKey(ConstantUtil.MANA_FEATURED); if (v != null) featured = Integer.parseInt(v); } catch (Exception e) {}
        return new int[]{post, reply, featured};
    }

    /**
     * 从 bbs_system_config 读取 JSON 数组格式的排除列表（点8/9共用）
     * @param configKey 配置键名（org_statistics_exclude / org_display_exclude）
     * @return 排除的 orgNo 列表，无配置或解析失败返回空列表
     */
    @Override
    public ResultBean personalPointsRank(PersonalPointsRankParam param) {
        // 默认时间：当月
        if (StringUtils.isEmpty(param.getStartTime()) || StringUtils.isEmpty(param.getEndTime())) {
            param.setStartTime(DateUtil.formatDate(DateUtil.beginOfMonth(new Date())));
            param.setEndTime(DateUtil.formatDate(DateUtil.endOfMonth(new Date())));
        }
        // 默认 size = 20
        if (param.getSize() == null || param.getSize() < 1) {
            param.setSize(20);
        }

        // 查询 Top N（积分完全来自 bbs_points_log，无需传入 dict 配置）
        List<PersonalPointsRankVO> list = articleMapper.personalPointsRank(param);

        // 分配排名序号
        if (!CollectionUtils.isEmpty(list)) {
            int[] rank = {1};
            list.forEach(item -> item.setRankNum(rank[0]++));
        }

        // 按显示层级解析组织标签（用户组织被隐藏时显示上一可见级）
        applyDisplayOrgToRank(list);

        // 查询当前用户排名
        PersonalPointsRankVO currentUser = null;
        if (param.getCurrentUserId() != null) {
            param.setUserId(param.getCurrentUserId());
            currentUser = articleMapper.getUserPersonalRank(param);

            // 如果用户 0 积分（不在排名中），也返回基础信息卡
            if (currentUser == null) {
                User user = userService.getById(param.getCurrentUserId());
                if (user != null) {
                    currentUser = new PersonalPointsRankVO();
                    currentUser.setUserId(user.getId());
                    currentUser.setNickName(user.getNickname());
                    currentUser.setPortrait(user.getPortrait());
                    currentUser.setOrgNo(user.getOrgNo());
                    if (user.getOrgNo() != null) {
                        SaOrg org = saOrgMapper.selectOne(
                            new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, user.getOrgNo())
                        );
                        if (org != null) {
                            // 同样按显示层级过滤（原始名 org.getOrgName() 作为兜底）
                            currentUser.setOrgName(saOrgService.resolveDisplayOrgName(user.getOrgNo(), org.getOrgName()));
                        }
                    }
                    currentUser.setPosts(0);
                    currentUser.setReplies(0);
                    currentUser.setPoints(0);
                    currentUser.setRankNum(null); // 未上榜
                }
            }
        }

        // 当前用户排名卡的 orgName 同样按显示层级过滤
        if (currentUser != null && currentUser.getOrgNo() != null) {
            currentUser.setOrgName(saOrgService.resolveDisplayOrgName(currentUser.getOrgNo(), currentUser.getOrgName()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("currentUser", currentUser);
        return ResultBean.success("查询成功", result);
    }

    /**
     * 按显示层级批量过滤个人排名的组织标签（与文章/评论的 orgName 保持一致的过滤规则）
     */
    private void applyDisplayOrgToRank(List<PersonalPointsRankVO> list) {
        if (CollectionUtils.isEmpty(list)) return;
        Set<String> orgNos = list.stream()
                .map(PersonalPointsRankVO::getOrgNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (orgNos.isEmpty()) return;
        Map<String, String> orgNameMap = saOrgService.resolveDisplayOrgNames(orgNos);
        list.forEach(item -> {
            String resolved = orgNameMap.get(item.getOrgNo());
            if (resolved != null) item.setOrgName(resolved);
        });
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
        enrichWithSuggestionAdopted(list);

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
        // 查询文章，获取当前精华状态和作者
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return ResultBean.error("文章不存在");
        }
        Integer oldFeatured = article.getIsFeatured() != null ? article.getIsFeatured() : 0;
        // 更新精华状态
        Article update = new Article();
        update.setArticleId(articleId).setIsFeatured(isFeatured);
        articleMapper.updateById(update);

        // 积分变动：设为精华+10，取消精华-10
        int newFeatured = isFeatured != null ? isFeatured : 0;
        if (newFeatured == 1 && oldFeatured != 1) {
            // 设为精华 → 加分
            int featuredPoints = 10; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                if (val != null) featuredPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(article.getUserId(), featuredPoints, "精华帖奖励积分",
                    "article", articleId, null);
        } else if (newFeatured != 1 && oldFeatured == 1) {
            // 取消精华 → 扣回
            int featuredPoints = 10;
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                if (val != null) featuredPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(article.getUserId(), -featuredPoints, "取消精华帖扣回积分",
                    "article", articleId, null);
        }
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
