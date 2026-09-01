package com.walker.controller;


import com.alibaba.excel.EasyExcel;
import cn.hutool.core.date.DateUtil;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.PointsLog;
import com.walker.pojo.SaOrg;
import com.walker.pojo.User;
import com.walker.service.*;
import com.walker.utils.ConstantUtil;
import com.walker.utils.FilePathNormalizer;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.InformationVO;
import com.walker.vo.PersonalPointsRankVO;
import com.walker.vo.PointsRankVO;
import com.walker.vo.ResultBean;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.param.PersonalPointsRankParam;
import com.walker.vo.param.PointsRankParam;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/20 14:26
 */
@Api(tags = "ArticleController")
@RestController
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ArticleLabelService articleLabelService;

    @Autowired
    private com.walker.service.DictService dictService;

    @Autowired
    private com.walker.mapper.SaOrgMapper saOrgMapper;

    @Autowired
    private com.walker.mapper.PointsLogMapper pointsLogMapper;

    @Value("${storage.path}")
    private String basePath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 脱敏 + 路径归一化：确保返回前端的路径统一包含 context-path
     */
    private Article normalizeArticle(Article article) {
        if (article == null) return null;
        SensitiveWordUtil.desensitizeArticle(article);
        article.setArticleContent(FilePathNormalizer.normalizeEmbeddedUrls(article.getArticleContent(), contextPath));
        article.setArticleContentHtml(FilePathNormalizer.normalizeEmbeddedUrls(article.getArticleContentHtml(), contextPath));
        article.setArticleImage(FilePathNormalizer.normalizeFieldUrl(article.getArticleImage(), contextPath));
        return article;
    }

    private List<Article> normalizeArticles(List<Article> articles) {
        if (articles == null) return null;
        for (Article a : articles) {
            normalizeArticle(a);
        }
        return articles;
    }


    @ApiOperation(value = "保存文章中的图片并返回地址")
    @PostMapping("/article/articleImg")
    public String articleImg(@RequestParam("userId") Integer id, @RequestParam("image") MultipartFile image) throws Exception {

        String pType = image.getContentType();
        pType = pType.substring(pType.indexOf("/") + 1);
        if ("jpeg".equals(pType)) {
            pType = "jpg";
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String day = format.format(date);

        Long time = System.currentTimeMillis();

        // 文件保存的路径（joinStoragePath 防止 storage.path 无尾斜杠时拼错目录，2026-08 生产故障）
        String path = FilePathNormalizer.joinStoragePath(basePath, "User/" + "id_" + id + "/article/" + day + "/" + time + "_." + pType);

        // 返回给前端的 url 路径
        String imageUrl = "";

        File outFile = new File(path);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.isDirectory()) {
            if (!parentDir.mkdirs() && !parentDir.exists()) {
                throw new IOException("无法创建上传目录: " + parentDir.getAbsolutePath());
            }
        }
        try {
            // 将前端传递的文件保存到本地服务器路径下
            image.transferTo(new File(path));
            imageUrl = contextPath + "/files/User/" + "id_" + id + "/article/" + day + "/" + time + "_." + pType;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageUrl;
    }


    @ApiOperation(value = "保存文章封面并返回地址")
    @PostMapping("/article/coverImg")
    public String coverImg(@RequestParam("userId") Integer id, @RequestParam("image") MultipartFile image) throws Exception {

        String pType = image.getContentType();
        pType = pType.substring(pType.indexOf("/") + 1);
        if ("jpeg".equals(pType)) {
            pType = "jpg";
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String day = format.format(date);

        Long time = System.currentTimeMillis();

        // 文件保存的路径（joinStoragePath 防止 storage.path 无尾斜杠时拼错目录，2026-08 生产故障）
        String path = FilePathNormalizer.joinStoragePath(basePath, "User/" + "id_" + id + "/article/" + day + "/cover/" + time + "_." + pType);

        // 同步到数据库中的路径(返回给前端的地址)
        String pathDB = "";

        File outFile = new File(path);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.isDirectory()) {
            if (!parentDir.mkdirs() && !parentDir.exists()) {
                throw new IOException("无法创建上传目录: " + parentDir.getAbsolutePath());
            }
        }
        try {
            // 将前端传递的文件保存到本地服务器路径下
            image.transferTo(new File(path));

            pathDB = contextPath + "/files/User/" + "id_" + id + "/article/" + day + "/cover/" + time + "_." + pType;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pathDB;

    }


    @ApiOperation(value = "发布文章")
    @PostMapping("/article/publish")
    public ResultBean publish(@RequestBody ArticleParam articleParam) {
        // 发帖限制检查
        if (articleParam.getUserId() != null) {
            User user = userService.getById(articleParam.getUserId());
            if (user != null && user.getPostRestricted() != null && user.getPostRestricted() == 1) {
                // 检查是否过期
                if (user.getPostRestrictedUntil() != null) {
                    try {
                        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        java.util.Date until = fmt.parse(user.getPostRestrictedUntil());
                        if (new java.util.Date().before(until)) {
                            return ResultBean.error("您已被限制发帖，截止时间: " + user.getPostRestrictedUntil());
                        } else {
                            // 已过期，自动解除限制
                            user.setPostRestricted(0);
                            user.setPostRestrictedUntil(null);
                            userService.updateById(user);
                        }
                    } catch (Exception e) {
                        // 解析失败，拒绝发帖
                        return ResultBean.error("发帖限制状态异常，请联系管理员");
                    }
                } else {
                    return ResultBean.error("您已被限制发帖，请联系管理员");
                }
            }
        }
        return articleService.publish(articleParam);
    }


    @ApiOperation(value = "获取顶部的五条推荐文章")
    @GetMapping("/common/article/getHeaderRecommend")
    public List<Article> getHeaderRecommend() {

        return normalizeArticles(articleService.queryHeaderRecommend());
    }


    @ApiOperation(value = "获取推荐文章")
    @GetMapping("/common/article/getRecommend")
    public List<Article> getRecommend() {

        return normalizeArticles(articleService.queryRecommend());
    }


    @ApiOperation(value = "获取最新文章")
    @GetMapping("/common/article/getNewest")
    public List<Article> getNewest() {

        return normalizeArticles(articleService.queryNewest());
    }


    @ApiOperation(value = "获取热榜文章(10条记录)")
    @GetMapping("/common/article/getHot")
    public List<Article> getHot() {

        return normalizeArticles(articleService.queryHot());
    }


    @ApiOperation(value = "通过文章Id查询文章")
    @PostMapping("/common/article/getArticleById/articleId/{articleId}")
    public Article getArticleById(@PathVariable("articleId") Integer articleId) {

        return normalizeArticle(articleService.queryArticleById(articleId));

    }


    @ApiOperation(value = "获取文章列表")
    @GetMapping("/common/article/getArticle")
    public List<Article> getArticle(@RequestParam("keywords") String keywords) {

        return normalizeArticles(articleService.queryAllArticleList(keywords));
    }

    @ApiOperation(value = "获取社区文章 时间排序")
    @GetMapping("/article/getArticleByCommunityId/{communityId}")
    public List<Article> getArticleByCommunityId(@PathVariable("communityId") Integer communityId) {
        return normalizeArticles(articleService.queryArticleByCommunityId(communityId));
    }

    @ApiOperation(value = "获取社区文章 浏览量排序")
    @GetMapping("/article/getArticleByHotAndOrderByDesc/{communityId}")
    public List<Article> getArticleByHotAndOrderByDesc(@PathVariable("communityId") Integer communityId) {
        return normalizeArticles(articleService.getArticleByHotAndOrderByDesc(communityId));
    }


    @ApiOperation(value = "通过关键词搜索文章(搜索框)")
    @GetMapping("/article/getArticleByKeywords")
    public List<Article> getArticleByKeywords(@RequestParam("keywords") String keywords) {
        return normalizeArticles(articleService.getArticleByKeywords(keywords));
    }


    @ApiOperation(value = "通过用户id查询文章")
    @GetMapping("/article/getArticleByUserId")
    public List<Article> getArticleByUserId(@RequestParam Integer userId) {
        return normalizeArticles(articleService.getArticleByUserId(userId));
    }

    @ApiOperation(value = "获取用户 消息")
    @GetMapping("/article/getMyInformation")
    public List<InformationVO> getMyInformation(@RequestParam Integer userId) {

        return articleService.getMyInformation(userId);
    }

    @ApiOperation(value = "查询所有的已审核的文章")
    @GetMapping("/admin/getAliveArticles/all")
    public ResultBean getAliveArticles(){
        ResultBean result = articleService.getAliveArticles();
        if (result != null && result.getObj() instanceof List) {
            for (Object item : (List<?>) result.getObj()) {
                if (item instanceof Article) normalizeArticle((Article) item);
            }
        }
        return result;
    }

    @ApiOperation(value = "查询所有的未审核的文章")
    @GetMapping("/admin/getNotAliveArticles/all")
    public ResultBean getNotAliveArticles(){
        ResultBean result = articleService.getNotAliveArticles();
        if (result != null && result.getObj() instanceof List) {
            for (Object item : (List<?>) result.getObj()) {
                if (item instanceof Article) normalizeArticle((Article) item);
            }
        }
        return result;
    }


    @ApiOperation(value = "管理员通过文章id删除文章")
    @PostMapping("/admin/deleteArticleByArticleId")
    public ResultBean adminDeleteArticleByArticleId(@RequestBody ArticleParam articleParam){
        if (articleParam.getArticleId() != null){
            return articleService.adminDeleteArticleByArticleId(articleParam.getArticleId());
        }
        return ResultBean.error("文章id不能为空！");
    }

    @ApiOperation(value = "通过文章id修改文章状态")
    @PostMapping("/admin/auditArticleByArticleId")
    public ResultBean auditArticleByArticleId(@RequestBody ArticleParam articleParam){
        if (articleParam.getArticleId() != null){
            return articleService.auditArticleByArticleId(articleParam.getArticleId());
        }
        return ResultBean.error("文章id不能为空！");
    }


    @ApiOperation(value = "删除所有的已通过审核的文章")
    @PostMapping("/admin/handleBatchDeleteArticlesByAlive/all")
    public ResultBean batchDeleteArticlesByAlive(){
        articleService.handleBatchDeleteArticlesByAlive();
        return ResultBean.success("成功删除所有文章！");
    }


    @ApiOperation(value = "批量审核文章")
    @PostMapping("/admin/batchAudit")
    public ResultBean batchAudit(){
        articleService.batchAudit();
        return ResultBean.success("审核成功！");
    }


    @ApiOperation(value = "通过文章id获取文章信息")
    @GetMapping("/admin/getArticleByArticleId/{articleId}")
    public ResultBean getArticleByArticle(@PathVariable Integer articleId){
        if (articleId != null){
            ResultBean result = articleService.getArticleByArticle(articleId);
            if (result != null && result.getObj() instanceof Article) {
                normalizeArticle((Article) result.getObj());
            }
            return result;
        }
        return ResultBean.error("查询文章id不能为空！");
    }

    @ApiOperation(value = "通过文章id获取文章信息（含已删除，管理员用）")
    @GetMapping("/admin/getArticleByIdInclDeleted/{articleId}")
    public ResultBean getArticleByIdInclDeleted(@PathVariable Integer articleId){
        if (articleId != null){
            Article article = articleService.getArticleByIdRaw(articleId);
            if (article != null) {
                normalizeArticle(article);
            }
            if (article == null) {
                return ResultBean.error("文章不存在");
            }
            return ResultBean.success("成功查询文章！", article);
        }
        return ResultBean.error("查询文章id不能为空！");
    }


    @ApiOperation(value = "获取文章总数")
    @GetMapping("/admin/getArticleCount/all")
    public ResultBean getArticleCount(){
        return articleService.getArticleCount();
    }


    @ApiOperation(value = "获取未通过审核的文章数量")
    @GetMapping("/admin/getArticleCountWithNotPass/all")
    public ResultBean getArticleCountWithNotPass(){
        return articleService.getArticleCountWithNotPass();
    }

    @ApiOperation(value = "用户通过文章id删除文章")
    @PostMapping("/article/deleteArticleByArticleId")
    public ResultBean deleteArticleByArticleId(@RequestBody ArticleParam articleParam){
        return articleService.deleteArticleByArticleId(articleParam.getArticleId());
    }

    @ApiOperation(value = "编辑文章")
    @PostMapping("/article/editArticle")
    public ResultBean editArticle(@RequestBody ArticleParam articleParam){
        if (articleParam.getArticleId() != null){
            return articleService.editArticle(articleParam);
        }
        return ResultBean.error("参数有误！");
    }

    /**
     * 方法描述 按单位统计文章数
     * @author chengQing
     * @date 2026/3/6 10:18
     * @param articleStatisticParam 查询条件实体类
     * @return ResultBean 返回结果
     */
    @ApiOperation(value = "按单位统计文章数")
    @PostMapping("/admin/articleStatisticByOrg")
    public ResultBean articleStatisticByOrg(@RequestBody ArticleStatisticParam articleStatisticParam){
        if (StringUtils.isEmpty(articleStatisticParam.getOrgNo())){
            articleStatisticParam.setOrgNo(ConstantUtil.ORG_NEI_JIANG);
        }
        // 按单位统计查询当前层级和下级 发布文章数
        articleStatisticParam.setOrgLength(articleStatisticParam.getOrgNo().length()+2);
        return articleService.articleStatisticByOrg(articleStatisticParam);
    }

    /**
     * 方法描述 获取积分排名
     * @author chengQing
     * @date 2026/3/9 14:23
     * @param pointsRankParam 查询条件实体类
     * @return ResultBean 返回结果集
     */
    @ApiOperation(value = "获取积分排名")
    @PostMapping("/common/pointsRank")
    public ResultBean pointsRank(@RequestBody PointsRankParam pointsRankParam){
        return articleService.pointsRank(pointsRankParam);
    }

    @ApiOperation(value = "获取个人积分排名（Top 20 + 当前用户信息）")
    @PostMapping("/common/personalPointsRank")
    public ResultBean personalPointsRank(@RequestBody PersonalPointsRankParam param){
        return articleService.personalPointsRank(param);
    }

    @ApiOperation(value = "管理员获取文章列表（支持搜索过滤分页，含标签名）")
    @PostMapping("/admin/article/list")
    public ResultBean getAdminArticleList(@RequestBody Map<String, Object> params) {
        String keywords = (String) params.getOrDefault("keywords", "");
        String labelId = (String) params.getOrDefault("labelId", "");
        String startTime = (String) params.getOrDefault("startTime", "");
        String endTime = (String) params.getOrDefault("endTime", "");
        Integer enable = params.get("enable") != null ? Integer.parseInt(params.get("enable").toString()) : null;
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return articleService.getAdminArticleList(keywords, labelId, startTime, endTime, enable, page, size);
    }

    // ==================== 精华帖接口 ====================

    @ApiOperation(value = "设置/取消精华帖")
    @PostMapping("/admin/featured/set")
    public ResultBean setFeatured(@RequestBody ArticleParam articleParam) {
        if (articleParam.getArticleId() == null) {
            return ResultBean.error("文章ID不能为空");
        }
        return articleService.setFeatured(articleParam.getArticleId(), articleParam.getIsFeatured());
    }

    @ApiOperation(value = "精华帖管理列表（支持搜索过滤分页）")
    @PostMapping("/admin/featured/list")
    public ResultBean getFeaturedList(@RequestBody Map<String, Object> params) {
        String keywords = (String) params.getOrDefault("keywords", "");
        String labelId = (String) params.getOrDefault("labelId", "");
        String startTime = (String) params.getOrDefault("startTime", "");
        String endTime = (String) params.getOrDefault("endTime", "");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer size = params.get("size") != null ? Integer.parseInt(params.get("size").toString()) : 10;
        return articleService.getFeaturedList(keywords, labelId, startTime, endTime, page, size);
    }

    @ApiOperation(value = "获取最新精华帖（用户端置顶用）")
    @GetMapping("/common/article/getFeaturedTop")
    public ResultBean getFeaturedTop() {
        return articleService.getFeaturedTop(3);
    }

    @ApiOperation(value = "获取精华帖列表（用户端分页，可指定标签）")
    @GetMapping("/common/article/getFeatured")
    public ResultBean getFeatured(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                  @RequestParam(value = "size", defaultValue = "10") Integer size,
                                  @RequestParam(value = "labelId", required = false) Integer labelId) {
        return articleService.getFeaturedByPage(page, size, labelId);
    }

    // ==================== 运营方案V2 新增接口 ====================

    @ApiOperation(value = "管理员采纳建议")
    @PostMapping("/admin/suggestion/adopt")
    public ResultBean adoptSuggestion(@RequestBody Map<String, Object> params) {
        Integer articleId = (Integer) params.get("articleId");
        Integer operatorId = (Integer) params.get("operatorId");

        if (articleId == null || operatorId == null) {
            return ResultBean.error("参数不完整");
        }

        Article article = articleService.queryArticleById(articleId);
        if (article == null) {
            return ResultBean.error("文章不存在");
        }

        // 校验：帖子必须是"建议反馈"标签
        if (article.getArticleLabelId() == null) {
            return ResultBean.error("该帖子不是建议反馈类型，无法采纳");
        }
        ArticleLabel label = articleLabelService.getById(article.getArticleLabelId());
        if (label == null || !"建议反馈".equals(label.getLabelName())) {
            return ResultBean.error("该帖子不是建议反馈类型，无法采纳");
        }

        // 校验：管理员不能采纳自己的建议
        if (operatorId.equals(article.getUserId())) {
            return ResultBean.error("不能采纳自己的建议");
        }

        // 校验：防止重复采纳（查询积分日志中是否已有该文章的建议采纳记录）
        int existCount = pointsLogService.countSuggestionAdoptForArticle(articleId);
        if (existCount > 0) {
            return ResultBean.error("该建议已被采纳");
        }

        // 给作者加5分
        pointsLogService.adjustUserPoints(article.getUserId(), 5, "建议被采纳积分",
                "article", articleId, operatorId);

        // 通知作者
        String titleSuffix = article.getArticleTitle() != null ? "（《" + article.getArticleTitle() + "》）" : "";
        notificationService.createNotification(article.getUserId(), operatorId,
                "suggestion_adopted", "恭喜！您的建议被采纳，获得+5积分" + titleSuffix,
                "article", articleId);

        return ResultBean.success("采纳成功");
    }

    // ==================== 积分排名导出 ====================

    /** EasyExcel 自定义表头行类 */
    public static class RankHeader {
        public String ranking;
        public String orgName;
        public String orgName2; // 个人排名用
        public String nickname;
        public String posts;
        public String replies;
        public String points;
    }

    @ApiOperation(value = "积分排名导出 Excel（单位排名 + 个人排名 + 积分明细）")
    @GetMapping("/admin/points/export")
    public void exportPointsRank(
            @RequestParam(required = false, defaultValue = "01") String rankType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false, defaultValue = "51404") String orgNo,
            HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("积分排名_" + new SimpleDateFormat("yyyyMMdd").format(new Date()), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // ── 1. 准备查询参数（复用已有逻辑） ──
        PointsRankParam rankParam = new PointsRankParam();
        rankParam.setRankType(rankType);
        rankParam.setOrgNo(orgNo);
        rankParam.setStartTime(startTime);
        rankParam.setEndTime(endTime);

        if (StringUtils.isEmpty(rankParam.getStartTime()) || StringUtils.isEmpty(rankParam.getEndTime())) {
            if ("01".equals(rankParam.getRankType())) {
                rankParam.setStartTime(new SimpleDateFormat("yyyy-MM-dd").format(DateUtil.beginOfMonth(new Date())));
                rankParam.setEndTime(new SimpleDateFormat("yyyy-MM-dd").format(DateUtil.endOfMonth(new Date())));
            } else {
                String cfgStart = dictService.getValueByKey(ConstantUtil.MANA_POINTS_START_TIME);
                String cfgEnd = dictService.getValueByKey(ConstantUtil.MANA_POINTS_END_TIME);
                if (cfgStart != null && cfgEnd != null) {
                    rankParam.setStartTime(cfgStart);
                    rankParam.setEndTime(cfgEnd);
                } else {
                    rankParam.setStartTime("2000-01-01");
                    rankParam.setEndTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                }
            }
        }
        rankParam.setOrgLength(rankParam.getOrgNo().length() + 2);

        // ── 2. 查询单位排名 ──
        ResultBean unitResult = articleService.pointsRank(rankParam);
        @SuppressWarnings("unchecked")
        List<PointsRankVO> unitList = (unitResult.getObj() != null)
                ? (List<PointsRankVO>) unitResult.getObj()
                : new ArrayList<>();

        // ── 3. 查询个人排名（全量导出） ──
        PersonalPointsRankParam personalParam = new PersonalPointsRankParam();
        personalParam.setRankType(rankType);
        personalParam.setStartTime(rankParam.getStartTime());
        personalParam.setEndTime(rankParam.getEndTime());
        personalParam.setSize(9999);
        ResultBean personalResult = articleService.personalPointsRank(personalParam);
        @SuppressWarnings("unchecked")
        Map<String, Object> personalData = (Map<String, Object>) personalResult.getObj();
        @SuppressWarnings("unchecked")
        List<PersonalPointsRankVO> personalList = (personalData != null && personalData.get("list") != null)
                ? (List<PersonalPointsRankVO>) personalData.get("list")
                : new ArrayList<>();

        // ── 4. 查询积分明细 ──
        String startFull = rankParam.getStartTime() + " 00:00:00";
        String endFull = rankParam.getEndTime() + " 23:59:59";
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.walker.pojo.PointsLog> logWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        logWrapper.ge(com.walker.pojo.PointsLog::getCreateTime, startFull)
                .le(com.walker.pojo.PointsLog::getCreateTime, endFull)
                .orderByDesc(com.walker.pojo.PointsLog::getCreateTime);
        List<com.walker.pojo.PointsLog> pointsLogs = pointsLogMapper.selectList(logWrapper);

        // ── 5. 写入 Excel（3 个 Sheet） ──
        com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
        try {
            // Sheet1：单位排名
            com.alibaba.excel.write.metadata.WriteSheet sheet1 =
                    EasyExcel.writerSheet("单位排名").head(unitRankHeader()).build();
            excelWriter.write(buildUnitRankData(unitList), sheet1);

            // Sheet2：个人排名
            com.alibaba.excel.write.metadata.WriteSheet sheet2 =
                    EasyExcel.writerSheet("个人排名").head(personalRankHeader()).build();
            excelWriter.write(buildPersonalRankData(personalList), sheet2);

            // Sheet3：积分明细
            com.alibaba.excel.write.metadata.WriteSheet sheet3 =
                    EasyExcel.writerSheet("积分明细").head(pointsLogHeader()).build();
            excelWriter.write(buildPointsLogData(pointsLogs), sheet3);
        } finally {
            excelWriter.finish();
        }
    }

    // ── 导出表头定义 ──

    private List<List<String>> unitRankHeader() {
        List<List<String>> header = new ArrayList<>();
        header.add(Arrays.asList("排名", "单位编号", "单位名称", "发帖数", "回帖数", "积分"));
        return header;
    }

    private List<List<String>> personalRankHeader() {
        List<List<String>> header = new ArrayList<>();
        header.add(Arrays.asList("排名", "用户ID", "昵称", "单位编号", "单位名称", "发帖数", "回帖数", "积分"));
        return header;
    }

    private List<List<String>> pointsLogHeader() {
        List<List<String>> header = new ArrayList<>();
        header.add(Arrays.asList("时间", "用户ID", "昵称", "单位名称", "积分变动", "变动原因", "关联类型", "关联ID"));
        return header;
    }

    // ── 数据转换 ──

    private List<List<Object>> buildUnitRankData(List<PointsRankVO> list) {
        List<List<Object>> data = new ArrayList<>();
        if (list == null) return data;
        for (PointsRankVO item : list) {
            data.add(Arrays.asList(
                    item.getRankNum(),
                    item.getOrgNo(),
                    item.getOrgName(),
                    item.getPosts() != null ? item.getPosts() : 0,
                    item.getReplies() != null ? item.getReplies() : 0,
                    item.getPoints() != null ? item.getPoints() : 0
            ));
        }
        return data;
    }

    private List<List<Object>> buildPersonalRankData(List<PersonalPointsRankVO> list) {
        List<List<Object>> data = new ArrayList<>();
        if (list == null) return data;
        for (PersonalPointsRankVO item : list) {
            data.add(Arrays.asList(
                    item.getRankNum(),
                    item.getUserId(),
                    item.getNickName(),
                    item.getOrgNo(),
                    item.getOrgName(),
                    item.getPosts() != null ? item.getPosts() : 0,
                    item.getReplies() != null ? item.getReplies() : 0,
                    item.getPoints() != null ? item.getPoints() : 0
            ));
        }
        return data;
    }

    private List<List<Object>> buildPointsLogData(List<com.walker.pojo.PointsLog> logs) {
        List<List<Object>> data = new ArrayList<>();
        if (logs == null) return data;
        for (com.walker.pojo.PointsLog log : logs) {
            String userName = "";
            String orgName = "";
            User u = userService.getById(log.getUserId());
            if (u != null) {
                userName = u.getNickname() != null ? u.getNickname() : "";
                if (u.getOrgNo() != null) {
                    SaOrg org = saOrgMapper.selectOne(
                            new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, u.getOrgNo()));
                    orgName = (org != null) ? org.getOrgName() : u.getOrgNo();
                }
            }
            data.add(Arrays.asList(
                    log.getCreateTime(),
                    log.getUserId(),
                    userName,
                    orgName,
                    log.getPointsChange(),
                    log.getReason() != null ? log.getReason() : "",
                    log.getRelatedType() != null ? log.getRelatedType() : "",
                    log.getRelatedId() != null ? log.getRelatedId() : ""
            ));
        }
        return data;
    }
}
