package com.walker.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.pojo.*;
import com.walker.service.*;
import com.walker.service.ArticleLabelService;
import com.walker.config.audit.AuditContext;
import com.walker.config.security.SelfApprovalException;
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
import com.walker.vo.TestAccountPurgeUserVO;
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
    private RankingCategoryService rankingCategoryService;

    @Autowired
    private SaOrgService saOrgService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private LabelPointsPolicyService labelPointsPolicyService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private LevelService levelService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private FeaturedRecommendationMapper featuredRecommendationMapper;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private SensitiveHitService sensitiveHitService;

    @Autowired
    private SelfApprovalPolicy selfApprovalPolicy;

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
            if (Integer.valueOf(1).equals(articleParam.getIsAnonymous())
                    && !Integer.valueOf(1).equals(label.getAllowAnonymous())) {
                return ResultBean.error("该标签不支持匿名发帖");
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

        // ── 内容质量检测：垃圾内容不再硬拒绝，照常发布但不计分，由版主复核 ──
        // 2026-09 生产故障的教训是"静默不可见"，不是"拒绝"：允许发布 + 明确告知才安全。
        ContentQualityUtil.QualityResult quality = ContentQualityUtil.checkContent(
                articleParam.getArticleTitle(), articleParam.getArticleContent());
        boolean spamContent = quality.isSpam();

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
        article.setFavoriteNum(0);
        article.setShareNum(0);
        article.setCommentNumCount(0);
        article.setIsAnonymous(Integer.valueOf(1).equals(articleParam.getIsAnonymous()) ? 1 : 0);
        article.setLastActivityTime(day);
        // 审核模式：默认 AUTO（与运营方案自动计分+抽查一致）；PRE_REVIEW 时进入待审核
        String publishMode = dictService.getValueByKey(ConstantUtil.ARTICLE_PUBLISH_MODE);
        boolean preReview = "PRE_REVIEW".equalsIgnoreCase(publishMode);
        // 命中敏感词超阈值：该用户的新内容临时进审核（需通知作者与版主/管理员）
        int recentHits = sensitiveHitService.countRecentHits(articleParam.getUserId(), 24);
        boolean hitReview = recentHits >= sensitiveReviewThreshold();
        article.setEnable(preReview || hitReview ? 0 : 1);
        article.setCreateTime(day);

        this.save(article);
        // 如果 文章附件不为空，则添加文章和附件的绑定关系
        if (articleParam.getFiles() != null && articleParam.getFiles().length > 0) {
            articleFileService.updateArticleFile(articleParam.getFiles(), article.getArticleId());
        }

        // 采集命中记录（内部吞异常，绝不影响发布）
        sensitiveHitService.record("article", article.getArticleId(), articleParam.getUserId(), labelId,
                title + "\n" + articleParam.getArticleContent());

        if (hitReview) {
            notifyContentReview(article, recentHits);
        }

        // 发帖积分：通过质量检测且标签允许计分、帖子已可见时才计分
        if (!spamContent && article.getEnable() != null && article.getEnable() == 1
                && labelPointsPolicyService.shouldAward(labelId, "publish")) {
            int postPoints = 2; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_POST);
                if (val != null) postPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(articleParam.getUserId(), postPoints, "发帖积分",
                    "article", article.getArticleId(), null);
        }
        if (article.getEnable() != null && article.getEnable() == 1) {
            achievementService.evaluateUserQuietly(articleParam.getUserId());
        }

        if (spamContent) {
            return ResultBean.success("发布成功，但内容被标记为低质量（" + quality.getDetail()
                    + "），未获得积分，版主会复核");
        }
        if (hitReview) {
            return ResultBean.success("发布成功，因近期多次命中敏感词，该内容需审核后展示");
        }

        return ResultBean.success("发布成功！");
    }

    /**
     * 命中敏感词进审核的阈值（24 小时内命中内容条数），可通过字典 sensitive_review_threshold 调整。
     */
    private int sensitiveReviewThreshold() {
        try {
            String val = dictService.getValueByKey("sensitive_review_threshold");
            if (StringUtils.isNotBlank(val)) {
                int parsed = Integer.parseInt(val.trim());
                return parsed > 0 ? parsed : 3;
            }
        } catch (Exception e) {
            // 配置异常时用默认值
        }
        return 3;
    }

    /**
     * 内容进审核：通知作者 + 通知该板块版主/全局管理员（无版主时按权限上推）。
     */
    private void notifyContentReview(Article article, int recentHits) {
        try {
            notificationService.createNotification(article.getUserId(), null, "content_review",
                    "您的帖子因近期多次命中敏感词，正在审核中，通过后会正常展示",
                    "article", article.getArticleId());

            List<Integer> receivers = new ArrayList<>();
            for (Integer reviewerId : authorizationService.getUserIdsWithPermission(
                    com.walker.config.security.Permission.ARTICLE_REVIEW)) {
                if (authorizationService.canModerateBoard(reviewerId, article.getArticleLabelId())) {
                    receivers.add(reviewerId);
                }
            }
            // 该板块没有版主时按权限上推：交给所有具备审核权限的管理员
            if (receivers.isEmpty()) {
                receivers = authorizationService.getUserIdsWithPermission(
                        com.walker.config.security.Permission.ARTICLE_REVIEW);
            }
            for (Integer reviewerId : receivers) {
                notificationService.createNotification(reviewerId, article.getUserId(), "content_review",
                        "有内容命中敏感词进入审核：" + article.getArticleTitle(),
                        "article", article.getArticleId());
            }
        } catch (Exception e) {
            // 通知失败不影响发布
        }
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

    @Override
    public ResultBean queryArticlePage(com.walker.vo.param.ArticleListParam param) {
        if (param == null) {
            param = new com.walker.vo.param.ArticleListParam();
        }
        int pageNo = param.getPage() == null || param.getPage() < 1 ? 1 : param.getPage();
        int size = param.getSize() == null || param.getSize() < 1 ? 20 : Math.min(param.getSize(), 50);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("enable", 1);
        if (StringUtils.isNotBlank(param.getKeywords())) {
            String kw = param.getKeywords().trim();
            wrapper.and(w -> w.like("article_title", kw)
                    .or().like("article_content", kw)
                    .or().like("article_author", kw));
        }
        if (param.getLabelId() != null && param.getLabelId() > 0) {
            wrapper.eq("article_label_id", param.getLabelId());
        }
        if (param.getAuthorId() != null) {
            wrapper.eq("user_id", param.getAuthorId());
        }
        if (StringUtils.isNotBlank(param.getOrgNo())) {
            wrapper.apply("user_id IN (SELECT id FROM bbs_user WHERE is_delete = 0 AND org_no LIKE CONCAT({0}, '%'))",
                    param.getOrgNo());
        }
        if (param.getFeatured() != null) {
            wrapper.eq("is_featured", param.getFeatured());
        }
        if (param.getHasAttachment() != null && param.getHasAttachment() == 1) {
            wrapper.apply("EXISTS (SELECT 1 FROM bbs_article_file f WHERE f.article_id = bbs_article.article_id)");
        }
        if (StringUtils.isNotBlank(param.getStartTime())) {
            wrapper.ge("create_time", param.getStartTime() + " 00:00:00");
        }
        if (StringUtils.isNotBlank(param.getEndTime())) {
            wrapper.le("create_time", param.getEndTime() + " 23:59:59");
        }
        String sortColumn = resolveSortColumn(param.getSortBy());
        if ("asc".equalsIgnoreCase(param.getSortOrder())) {
            wrapper.orderByAsc(sortColumn);
        } else {
            wrapper.orderByDesc(sortColumn);
        }
        wrapper.orderByDesc("article_id");

        Page<Article> page = articleMapper.selectPage(new Page<>(pageNo, size), wrapper);
        List<Article> list = page.getRecords();
        for (Article article : list) {
            article.setCommentNum(article.getCommentNumCount() == null ? 0 : article.getCommentNumCount());
        }
        enrichWithUserInfo(list);
        maskAnonymousArticles(list);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", page.getTotal());
        data.put("page", pageNo);
        data.put("size", size);
        data.put("pages", page.getPages());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean queryHotByLabel(com.walker.vo.param.ArticleHotParam param) {
        if (param == null) {
            param = new com.walker.vo.param.ArticleHotParam();
        }
        int limit = param.getLimit() == null || param.getLimit() < 1 ? 10 : Math.min(param.getLimit(), 50);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("article_id", "article_title", "create_time", "article_view_num",
                "comment_num", "favorite_num", "article_good_num", "share_num");
        wrapper.eq("enable", 1);
        if (param.getLabelId() != null && param.getLabelId() > 0) {
            wrapper.eq("article_label_id", param.getLabelId());
        }
        String range = param.getRange() == null ? "all" : param.getRange();
        // 热榜口径：默认按全部时间累计排序，不再按 7/30 天截断
        // （保留 range 入参兼容老前端，仅显式传 7d/30d 时才按时间过滤）
        if ("7d".equals(range) || "30d".equals(range)) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, "7d".equals(range) ? -7 : -30);
            wrapper.ge("create_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime()));
        }
        String metric = param.getMetric() == null ? "views" : param.getMetric();
        String column;
        switch (metric) {
            case "comments": column = "comment_num"; break;
            case "favorites": column = "favorite_num"; break;
            case "likes": column = "article_good_num"; break;
            case "shares": column = "share_num"; break;
            default: column = "article_view_num"; break;
        }
        wrapper.orderByDesc(column);
        wrapper.orderByDesc("article_id");
        wrapper.last("LIMIT " + limit);
        return ResultBean.success("查询成功", articleMapper.selectList(wrapper));
    }

    @Override
    public ResultBean queryFavoritePage(Integer userId, Integer page, Integer size) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);

        Page<ArticleUser> favPage = articleUserService.page(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<ArticleUser>()
                        .eq(ArticleUser::getUserId, userId)
                        .orderByDesc(ArticleUser::getCreateTime)
                        .orderByDesc(ArticleUser::getId));

        List<Integer> articleIds = favPage.getRecords().stream()
                .map(ArticleUser::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Article> list = new ArrayList<>();
        if (!articleIds.isEmpty()) {
            List<Article> found = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                    .in(Article::getArticleId, articleIds)
                    .eq(Article::getEnable, 1)
                    .eq(Article::getIsDelete, 0));
            Map<Integer, Article> byId = found.stream()
                    .collect(Collectors.toMap(Article::getArticleId, a -> a, (a, b) -> a));
            for (Integer id : articleIds) {
                Article a = byId.get(id);
                if (a == null) continue;
                a.setCommentNum(a.getCommentNumCount() == null ? 0 : a.getCommentNumCount());
                list.add(a);
            }
            enrichWithUserInfo(list);
            maskAnonymousArticles(list);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", favPage.getTotal());
        data.put("page", pageNo);
        data.put("size", pageSize);
        data.put("pages", favPage.getPages());
        return ResultBean.success("查询成功", data);
    }

    private String resolveSortColumn(String sortBy) {        if (sortBy == null) return "create_time";
        switch (sortBy) {
            case "lastReplyTime": return "last_reply_time";
            case "lastActivityTime": return "last_activity_time";
            case "views": return "article_view_num";
            case "comments": return "comment_num";
            case "likes": return "article_good_num";
            case "favorites": return "favorite_num";
            case "shares": return "share_num";
            case "featuredTime": return "featured_time";
            default: return "create_time";
        }
    }

    private void maskAnonymousArticles(List<Article> articles) {
        if (CollectionUtils.isEmpty(articles)) return;
        for (Article article : articles) {
            if (article.getIsAnonymous() != null && article.getIsAnonymous() == 1) {
                article.setArticleAuthor("匿名用户");
                article.setUserId(null);
                article.setPortrait(null);
                article.setAuthorOrgName(null);
                article.setAuthorOrgNameFull(null);
                article.setAuthorDeptName(null);
            }
        }
    }

    /**
     * 通过文章Id查询文章
     * @return
     */
    @Override
    public Article queryArticleById(Integer articleId) {
        return queryArticleById(articleId, true);
    }

    @Override
    public Article queryArticleById(Integer articleId, boolean countView) {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 1)
                        .eq(Article::getArticleId, articleId)
        );
        // 文章不存在（已被删除或未启用）时直接返回 null，避免 NPE
        if (article == null) {
            return null;
        }
        // 更新浏览量（验收/预览可传 countView=false 跳过，避免污染真实数据）
        if (countView) {
            article.setArticleViewNum(article.getArticleViewNum() + 1);
            articleMapper.updateById(article);
        }
        try {
            if (article.getUserId() != null) {
                Integer levelIndex = levelService.getLevelIndex(article.getUserId());
                article.setAuthorLevelIndex(levelIndex);
                article.setAuthorIconSvg(levelService.getIconSvg(levelIndex));
            }
        } catch (Exception ignore) {
            // 等级查询失败不影响帖子正文展示
        }
        return article;
        // return this.getById(articleId);
    }

    /**
     * 查询所有的文章列表
     * 关键词命中范围：标题 / 正文 / 附件名 / 人员（作者昵称快照、当前昵称、账号、人员编号）
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
            List<Integer> distinctArticleIds = CollectionUtils.isEmpty(articleFileList)
                    ? new ArrayList<>()
                    : articleFileList.stream()
                        .map(ArticleFile::getArticleId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

            Map<String, Object> params = new HashMap<>();
            params.put("keywords", keywords);
            params.put("fileArticleIds", distinctArticleIds);
            // 单条 SQL 完成标题/正文/人员/附件的 OR 匹配，避免条件优先级错乱
            articles = articleMapper.selectArticleListByKeywords(params);
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

        Map<Integer, Integer> levelMap = levelService.getLevelIndexMap(userIds);
        Map<Integer, String> iconMap = levelService.getIconSvgMap();

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
            if (a.getUserId() != null) {
                a.setAuthorLevelIndex(levelMap.get(a.getUserId()));
                a.setAuthorIconSvg(iconMap.get(levelMap.get(a.getUserId())));
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
        // 找出用途类型为 suggestion 的帖子ID（按 label_type 判断，与标签名解耦）
        List<Integer> suggestionArticleIds = articles.stream()
                .filter(a -> a.getArticleId() != null && "suggestion".equals(a.getArticleLabelType()))
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
        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null && boardIds.isEmpty()) {
            return ResultBean.success("查询成功！", new ArrayList<Article>());
        }
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 1);
        if (boardIds != null) {
            wrapper.in(Article::getArticleLabelId, boardIds);
        }
        List<Article> articles = articleMapper.selectList(wrapper);
        return ResultBean.success("查询成功！", SensitiveWordUtil.desensitizeArticles(articles));
    }

    @Override
    public ResultBean getNotAliveArticles() {
        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null && boardIds.isEmpty()) {
            return ResultBean.success("查询成功！", new ArrayList<Article>());
        }
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 0);
        if (boardIds != null) {
            wrapper.in(Article::getArticleLabelId, boardIds);
        }
        List<Article> articles = articleMapper.selectList(wrapper);
        return ResultBean.success("查询成功！",SensitiveWordUtil.desensitizeArticles(articles));
    }

    @Override
    public ResultBean adminDeleteArticleByArticleId(Integer articleId) {
        articleMapper.deleteById(articleId);
        return ResultBean.success("删除成功！");
    }

    @Override
    public ResultBean auditArticleByArticleId(Integer articleId) {
        Article existing = articleMapper.selectById(articleId);
        Article article = new Article();
        article.setArticleId(articleId)
                .setEnable(1);
        articleMapper.updateById(article);
        // 前置审核通过后补发发帖积分（每个帖子只发一次；不计分标签跳过）
        if (existing != null && existing.getUserId() != null) {
            if (labelPointsPolicyService.shouldAward(existing.getArticleLabelId(), "publish")) {
                long awarded = pointsLogService.count(new LambdaQueryWrapper<PointsLog>()
                        .eq(PointsLog::getUserId, existing.getUserId())
                        .eq(PointsLog::getRelatedType, "article")
                        .eq(PointsLog::getRelatedId, articleId)
                        .eq(PointsLog::getReason, "发帖积分")
                        .gt(PointsLog::getPointsChange, 0)
                        .and(w -> w.isNull(PointsLog::getIsReversed).or().eq(PointsLog::getIsReversed, 0)));
                if (awarded == 0) {
                    int postPoints = 2;
                    try {
                        String val = dictService.getValueByKey(ConstantUtil.MANA_POST);
                        if (val != null) postPoints = Integer.parseInt(val);
                    } catch (Exception e) { /* use default */ }
                    pointsLogService.adjustUserPoints(existing.getUserId(), postPoints, "发帖积分",
                            "article", articleId, null);
                }
            }
            achievementService.evaluateUserQuietly(existing.getUserId());
        }
        return ResultBean.success("修改成功！");
    }

    @Override
    public ResultBean purgeTestAccountArticles() {
        List<Integer> articleIds = articleMapper.selectTestAccountArticleIds();
        int affected = 0;
        if (articleIds != null && !articleIds.isEmpty()) {
            affected = articleMapper.deleteBatchIds(articleIds);
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("scope", "test_account");
        after.put("affected", affected);
        AuditContext.setAfter(after);
        return ResultBean.success("已删除 " + affected + " 篇测试账号发布的帖子", affected);
    }

    @Override
    public ResultBean testAccountPurgePreview() {
        List<TestAccountPurgeUserVO> rows = articleMapper.selectTestAccountArticleSummary();
        List<TestAccountPurgeUserVO> users = rows == null ? new ArrayList<>() : rows;
        long articleCount = 0;
        for (TestAccountPurgeUserVO row : users) {
            if (row != null && row.getArticleCount() != null) {
                articleCount += row.getArticleCount();
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", users.size());
        data.put("articleCount", articleCount);
        data.put("users", users);
        return ResultBean.success("查询成功", data);
    }

    @Override
    public void handleBatchDeleteArticlesByAlive() {
        long affected = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getEnable, 1));
        articleMapper.delete(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 1)
        );
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("scope", "all");
        after.put("affected", affected);
        AuditContext.setAfter(after);
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
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("scope", "all");
        after.put("affected", articles.size());
        AuditContext.setAfter(after);
    }

    @Override
    public ResultBean getArticleByArticle(Integer articleId) {

        Article article = articleMapper.selectById(articleId);
        ResultBean denial = boardScopeDenial(article != null ? article.getArticleLabelId() : null);
        if (denial != null) {
            return denial;
        }
        return ResultBean.success("成功查询文章！",SensitiveWordUtil.desensitizeArticle(article));
    }

    @Override
    public Article getArticleByIdRaw(Integer articleId) {
        return articleMapper.selectByIdRaw(articleId);
    }

    @Override
    public String getArticleStatus(Integer articleId) {
        // 绕过逻辑删除读取原始行，以区分"已删除/不可见"与"记录不存在"
        Article article = articleMapper.selectByIdRaw(articleId);
        if (article == null) {
            return "NOT_EXIST";
        }
        // 逻辑删除 或 未审核/已下架（enable != 1）→ 对外不可见
        if (Integer.valueOf(1).equals(article.getIsDelete()) || !Integer.valueOf(1).equals(article.getEnable())) {
            return "DELETED";
        }
        return "NORMAL";
    }

    @Override
    public ResultBean getArticleCount() {

        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null && boardIds.isEmpty()) {
            return ResultBean.success("获取成功！", 0L);
        }
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (boardIds != null) {
            wrapper.in(Article::getArticleLabelId, boardIds);
        }
        Long count = articleMapper.selectCount(wrapper);
        return ResultBean.success("获取成功！",count);

    }


    @Override
    public ResultBean getArticleCountWithNotPass() {

        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null && boardIds.isEmpty()) {
            return ResultBean.success("获取成功！", 0L);
        }
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 0);
        if (boardIds != null) {
            wrapper.in(Article::getArticleLabelId, boardIds);
        }
        Long count = articleMapper.selectCount(wrapper);
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
                    "article", articleId, null, true);

            // 如果是精华帖，额外扣回精华加分
            if (article.getIsFeatured() != null && article.getIsFeatured() == 1) {
                int featuredPoints = 10; // default
                try {
                    String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                    if (val != null) featuredPoints = Integer.parseInt(val);
                } catch (Exception e) { /* use default */ }
                pointsLogService.adjustUserPoints(article.getUserId(), -featuredPoints, "删除精华帖扣回加分",
                        "article", articleId, null, true);
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
        if (Integer.valueOf(1).equals(articleParam.getIsAnonymous())
                && !labelPointsPolicyService.allowsAnonymous(articleParam.getArticleLabelId())) {
            return ResultBean.error("该标签不支持匿名发帖");
        }

        // 归属校验：POST_UPDATE_OWN 只说明"能改自己的帖子"，不说明"能改任意 ID"。
        // 缺这一步时任何登录用户都能凭 articleId 改别人的帖子（2026-11-23 修复）。
        if (articleParam.getArticleId() == null) {
            return ResultBean.error("文章id不能为空");
        }
        User editUser = currentUserService.getCurrentUser();
        if (editUser == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        Article existing = articleMapper.selectById(articleParam.getArticleId());
        if (existing == null) {
            return ResultBean.error("文章不存在或已删除");
        }
        if (existing.getUserId() == null || !existing.getUserId().equals(editUser.getId())) {
            return ResultBean.forbidden("只能编辑自己发布的帖子");
        }

        Article article = new Article();

        article.setArticleId(articleParam.getArticleId())
                .setArticleTitle(title)
                .setArticleContent(articleParam.getArticleContent())
                .setArticleContentHtml(articleParam.getArticleContentHtml())
                .setArticleSummary(articleParam.getArticleSummary())
                .setArticleTypeId(articleParam.getArticleTypeId())
                .setArticleLabelId(articleParam.getArticleLabelId())
                .setIsAnonymous(Integer.valueOf(1).equals(articleParam.getIsAnonymous()) ? 1 : 0);
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

        // 排名单位过滤：优先按榜单分类成员口径；未传分类时兼容旧“参与排名”勾选
        if (!CollectionUtils.isEmpty(resultList)) {
            Set<String> allowedOrgNos;
            if (pointsRankParam.getCategoryId() != null) {
                allowedOrgNos = new HashSet<>(rankingCategoryService.listAllowedOrgNos(pointsRankParam.getCategoryId()));
            } else {
                List<SaOrg> rankingOrgs = saOrgMapper.selectList(
                        new LambdaQueryWrapper<SaOrg>()
                                .eq(SaOrg::getIsRankingSelected, 1)
                                .eq(SaOrg::getIsDelete, 0)
                );
                allowedOrgNos = rankingOrgs.stream()
                        .map(SaOrg::getOrgNo)
                        .collect(Collectors.toSet());
            }
            resultList = resultList.stream()
                    .filter(item -> allowedOrgNos.contains(item.getOrgNo()))
                    .collect(Collectors.toList());
            int[] rank = {1};
            resultList.forEach(item -> item.setRankNum(rank[0]++));
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
        // 默认时间：未显式传区间时回落到当月
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

        // 批量补齐等级序号，列表直接展示等级图标（一次查询，避免 N+1）
        if (!CollectionUtils.isEmpty(list)) {
            try {
                Map<Integer, Integer> levelMap = levelService.getLevelIndexMap(
                        list.stream().map(PersonalPointsRankVO::getUserId).collect(Collectors.toList()));
                Map<Integer, String> iconMap = levelService.getIconSvgMap();
                list.forEach(item -> {
                    item.setLevelIndex(levelMap.get(item.getUserId()));
                    item.setIconSvg(iconMap.get(item.getLevelIndex()));
                });
            } catch (Exception ignore) {
                // 等级计算失败不影响排名主流程
            }
        }

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
        // 当前用户等级（未上榜时也要显示）
        if (currentUser != null && currentUser.getLevelIndex() == null) {
            try {
                currentUser.setLevelIndex(levelService.getLevelIndex(currentUser.getUserId()));
            } catch (Exception ignore) {
                // 等级计算失败不影响排名主流程
            }
        }
        if (currentUser != null && currentUser.getLevelIndex() != null) {
            try {
                currentUser.setIconSvg(levelService.getIconSvg(currentUser.getLevelIndex()));
            } catch (Exception ignore) {
                // 图标缺失按自动图标兜底
            }
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
    public ResultBean getAdminArticleList(String keywords, Integer labelId, String startTime, String endTime,
                                          Integer enable, Integer deleted, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null) {
            if (boardIds.isEmpty()) {
                return emptyAdminArticlePage(page, size);
            }
            if (labelId != null && !boardIds.contains(labelId)) {
                return emptyAdminArticlePage(page, size);
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("keywords", keywords);
        params.put("labelId", labelId);
        params.put("labelIds", boardIds);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        params.put("enable", enable);
        // deleted=1 → 只查逻辑删除的帖子（管理端「已删除」tab）；默认只查未删除
        params.put("deleted", deleted != null && deleted == 1 ? 1 : 0);

        int total = articleMapper.countAdminArticleList(params);
        int offset = (page - 1) * size;
        params.put("offset", offset);
        params.put("size", size);

        List<Article> list = articleMapper.selectAdminArticleList(params);
        if (list == null) list = new ArrayList<>();

        enrichWithUserInfo(list);
        enrichWithCommentCounts(list);
        if (deleted == null || deleted != 1) {
            // 已删除帖子不再展示采纳状态
            enrichWithSuggestionAdopted(list);
        }

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
        // 操作人以登录态身份为准：用于"不能自设精华"校验与通知（2026-10-12 需求）
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        Integer operatorId = currentUser.getId();
        // 查询文章，获取当前精华状态和作者
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return ResultBean.error("文章不存在");
        }
        ResultBean boardDenial = boardScopeDenial(article.getArticleLabelId());
        if (boardDenial != null) {
            return boardDenial;
        }
        Integer oldFeatured = article.getIsFeatured() != null ? article.getIsFeatured() : 0;
        int newFeatured = isFeatured != null ? isFeatured : 0;
        // 权限校验：除超级管理员外，不得将自己的帖子设为精华（取消精华不受限，便于管理员纠错）
        if (newFeatured == 1 && oldFeatured != 1) {
            try {
                selfApprovalPolicy.checkSetFeatured(currentUser, article);
            } catch (SelfApprovalException e) {
                return ResultBean.error(e.getMessage());
            }
        }
        // 更新精华状态
        Article update = new Article();
        update.setArticleId(articleId).setIsFeatured(isFeatured);
        if (newFeatured == 1) {
            update.setFeaturedTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        articleMapper.updateById(update);

        // 积分变动：设为精华+10，取消精华-10
        if (newFeatured == 1 && oldFeatured != 1) {
            // 设为精华 → 加分
            int featuredPoints = 10; // default
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                if (val != null) featuredPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(article.getUserId(), featuredPoints, "精华帖奖励积分",
                    "article", articleId, operatorId, true);
            achievementService.evaluateUserQuietly(article.getUserId());
        } else if (newFeatured != 1 && oldFeatured == 1) {
            // 取消精华 → 扣回
            int featuredPoints = 10;
            try {
                String val = dictService.getValueByKey(ConstantUtil.MANA_FEATURED);
                if (val != null) featuredPoints = Integer.parseInt(val);
            } catch (Exception e) { /* use default */ }
            pointsLogService.adjustUserPoints(article.getUserId(), -featuredPoints, "取消精华帖扣回积分",
                    "article", articleId, operatorId, true);
        }
        // 通知作者：设为精华 / 取消精华
        if (newFeatured == 1 && oldFeatured != 1) {
            String titleSuffix = article.getArticleTitle() != null ? "（《" + article.getArticleTitle() + "》）" : "";
            notificationService.createNotification(article.getUserId(), operatorId,
                    "featured_granted", "恭喜！您的帖子被设为精华帖，已获得精华积分" + titleSuffix,
                    "article", articleId);
            // 直接设精华也进入公示卡，避免绕过“精华公示”
            try {
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                boolean publicEnabled = !"0".equals(dictService.getValueByKey("featured_public_enabled"));
                int publicDays = 5;
                try {
                    String days = dictService.getValueByKey("featured_public_days");
                    if (days != null && !days.trim().isEmpty()) publicDays = Integer.parseInt(days.trim());
                } catch (Exception ignore) { }
                FeaturedRecommendation rec = new FeaturedRecommendation();
                rec.setArticleId(articleId);
                rec.setRecommenderId(operatorId);
                rec.setLabelId(article.getArticleLabelId());
                rec.setStatus("approved");
                rec.setReviewerId(operatorId);
                rec.setReviewRemark("管理员直接设置精华");
                rec.setReviewTime(now);
                rec.setCreateTime(now);
                rec.setFeaturedReason("管理员直接设置精华");
                rec.setPublicStartTime(now);
                if (publicEnabled) {
                    rec.setPublicStatus("PUBLICIZING");
                    rec.setPublicEndTime(addBusinessDays(new Date(), Math.max(1, publicDays)));
                } else {
                    rec.setPublicStatus("PUBLISHED");
                    rec.setPublicEndTime(now);
                }
                featuredRecommendationMapper.insert(rec);
            } catch (Exception ignore) {
                // 公示卡写入失败不影响设精华主流程
            }
        } else if (newFeatured != 1 && oldFeatured == 1) {
            String titleSuffix = article.getArticleTitle() != null ? "（《" + article.getArticleTitle() + "》）" : "";
            notificationService.createNotification(article.getUserId(), operatorId,
                    "featured_revoked", "您的帖子" + titleSuffix + "被取消精华，对应积分已扣回",
                    "article", articleId);
        }
        return ResultBean.success(isFeatured == 1 ? "已设为精华帖" : "已取消精华帖");
    }

    @Override
    public ResultBean getFeaturedList(String keywords, Integer labelId, String startTime, String endTime, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null) {
            if (boardIds.isEmpty()) {
                return emptyAdminArticlePage(page, size);
            }
            if (labelId != null && !boardIds.contains(labelId)) {
                return emptyAdminArticlePage(page, size);
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("keywords", keywords);
        params.put("labelId", labelId);
        params.put("labelIds", boardIds);
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
    public ResultBean getArticleTabCounts() {
        List<Integer> boardIds = visibleBoardIds();
        if (boardIds != null && boardIds.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("done", 0L);
            empty.put("pending", 0L);
            empty.put("featured", 0L);
            empty.put("deleted", 0L);
            return ResultBean.success("查询成功", empty);
        }

        // Article 带 @TableLogic isDelete，selectCount 自动追加 is_delete = 0
        LambdaQueryWrapper<Article> doneWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 1);
        LambdaQueryWrapper<Article> pendingWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getEnable, 0);
        LambdaQueryWrapper<Article> featuredWrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getIsFeatured, 1);
        if (boardIds != null) {
            doneWrapper.in(Article::getArticleLabelId, boardIds);
            pendingWrapper.in(Article::getArticleLabelId, boardIds);
            featuredWrapper.in(Article::getArticleLabelId, boardIds);
        }
        long done = articleMapper.selectCount(doneWrapper);
        long pending = articleMapper.selectCount(pendingWrapper);
        long featured = articleMapper.selectCount(featuredWrapper);
        // 已删除需绕过 @TableLogic 直接统计 is_delete = 1
        long deleted = boardIds == null
                ? articleMapper.countDeletedArticles()
                : articleMapper.countDeletedArticlesByLabelIds(boardIds);
        Map<String, Object> result = new HashMap<>();
        result.put("done", done);
        result.put("pending", pending);
        result.put("featured", featured);
        result.put("deleted", deleted);
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

    /** 当前用户可见板块；null=全局管理员，空列表=版主但无板块 */
    private List<Integer> visibleBoardIds() {
        return authorizationService.getVisibleBoardIds(currentUserService.getCurrentUser());
    }

    /** 非 null 表示无权访问该板块，直接返回 403 文案 */
    private ResultBean boardScopeDenial(Integer labelId) {
        String message = authorizationService.getBoardScopeDenialMessage(
                currentUserService.getCurrentUser(), labelId);
        return message == null ? null : ResultBean.forbidden(message);
    }

    /** 公示结束日：按工作日顺延（跳过周六/周日）。 */
    private String addBusinessDays(Date start, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        int added = 0;
        while (added < days) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) added++;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
    }

    private ResultBean emptyAdminArticlePage(int page, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<Article>());
        result.put("total", 0);
        result.put("page", page);
        result.put("size", size);
        result.put("pages", 0);
        return ResultBean.success("查询成功", result);
    }

}
