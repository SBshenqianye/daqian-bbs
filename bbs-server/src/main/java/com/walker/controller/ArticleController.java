package com.walker.controller;


import com.walker.pojo.Article;
import com.walker.utils.ConstantUtil;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.InformationVO;
import com.walker.vo.ResultBean;
import com.walker.service.ArticleService;
import com.walker.vo.param.ArticleParam;
import com.walker.vo.param.ArticleStatisticParam;
import com.walker.vo.param.PointsRankParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

    @Value("${storage.path}")
    private String basePath;


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

        // 文件保存的路径
        String path = basePath + "User/" + "id_" + id + "/article/" + day + "/" + time + "_." + pType;

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
            imageUrl = "/files/User/" + "id_" + id + "/article/" + day + "/" + time + "_." + pType;

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

        // 文件保存的路径
        String path = basePath + "User/" + "id_" + id + "/article/" + day + "/cover/" + time + "_." + pType;

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

            pathDB = "/files/User/" + "id_" + id + "/article/" + day + "/cover/" + time + "_." + pType;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pathDB;

    }


    @ApiOperation(value = "发布文章")
    @PostMapping("/article/publish")
    public ResultBean publish(@RequestBody ArticleParam articleParam) {

        return articleService.publish(articleParam);

    }


    @ApiOperation(value = "获取顶部的五条推荐文章")
    @GetMapping("/common/article/getHeaderRecommend")
    public List<Article> getHeaderRecommend() {

        return SensitiveWordUtil.desensitizeArticles(articleService.queryHeaderRecommend());
    }


    @ApiOperation(value = "获取推荐文章")
    @GetMapping("/common/article/getRecommend")
    public List<Article> getRecommend() {

        return articleService.queryRecommend();
    }


    @ApiOperation(value = "获取最新文章")
    @GetMapping("/common/article/getNewest")
    public List<Article> getNewest() {

        return articleService.queryNewest();
    }


    @ApiOperation(value = "获取热榜文章(10条记录)")
    @GetMapping("/common/article/getHot")
    public List<Article> getHot() {

        return SensitiveWordUtil.desensitizeArticles(articleService.queryHot());
    }


    @ApiOperation(value = "通过文章Id查询文章")
    @PostMapping("/common/article/getArticleById/articleId/{articleId}")
    public Article getArticleById(@PathVariable("articleId") Integer articleId) {

        return SensitiveWordUtil.desensitizeArticle(articleService.queryArticleById(articleId));

    }


    @ApiOperation(value = "获取文章列表")
    @GetMapping("/common/article/getArticle")
    public List<Article> getArticle(@RequestParam("keywords") String keywords) {

        return SensitiveWordUtil.desensitizeArticles(articleService.queryAllArticleList(keywords));
    }

    @ApiOperation(value = "获取社区文章 时间排序")
    @GetMapping("/article/getArticleByCommunityId/{communityId}")
    public List<Article> getArticleByCommunityId(@PathVariable("communityId") Integer communityId) {
        return articleService.queryArticleByCommunityId(communityId);
    }

    @ApiOperation(value = "获取社区文章 浏览量排序")
    @GetMapping("/article/getArticleByHotAndOrderByDesc/{communityId}")
    public List<Article> getArticleByHotAndOrderByDesc(@PathVariable("communityId") Integer communityId) {
        return articleService.getArticleByHotAndOrderByDesc(communityId);
    }


    @ApiOperation(value = "通过关键词搜索文章(搜索框)")
    @GetMapping("/article/getArticleByKeywords")
    public List<Article> getArticleByKeywords(@RequestParam("keywords") String keywords) {
        return SensitiveWordUtil.desensitizeArticles(articleService.getArticleByKeywords(keywords));
    }


    @ApiOperation(value = "通过用户id查询文章")
    @GetMapping("/article/getArticleByUserId")
    public List<Article> getArticleByUserId(@RequestParam Integer userId) {
        return SensitiveWordUtil.desensitizeArticles(articleService.getArticleByUserId(userId));
    }

    @ApiOperation(value = "获取用户 消息")
    @GetMapping("/article/getMyInformation")
    public List<InformationVO> getMyInformation(@RequestParam Integer userId) {

        return articleService.getMyInformation(userId);
    }

    @ApiOperation(value = "查询所有的已审核的文章")
    @GetMapping("/admin/getAliveArticles/all")
    public ResultBean getAliveArticles(){
        return articleService.getAliveArticles();
    }

    @ApiOperation(value = "查询所有的未审核的文章")
    @GetMapping("/admin/getNotAliveArticles/all")
    public ResultBean getNotAliveArticles(){
        return articleService.getNotAliveArticles();
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
            return articleService.getArticleByArticle(articleId);
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

}
