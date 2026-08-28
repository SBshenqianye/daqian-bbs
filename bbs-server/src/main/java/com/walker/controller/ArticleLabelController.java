package com.walker.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.Article;
import com.walker.pojo.Reply;
import com.walker.service.ArticleLabelService;
import com.walker.service.ArticleService;
import com.walker.service.ReplyService;
import com.walker.service.CommentService;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/20 22:07
 */
@Api(tags = "ArticleLabelController")
@RestController
//@RequestMapping("/article-label")
public class ArticleLabelController {


    @Autowired
    private ArticleLabelService articleLabelService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentService commentService;

    @ApiOperation(value = "获取文章的标签")
    @GetMapping("/common/getArticleLabel")
    public List<ArticleLabel> getArticleLabel(){

        return articleLabelService.queryAllArticleLabel();

    }

    @ApiOperation(value = "新增文章标签")
    @PostMapping("/admin/addArticleLabel")
    public ResultBean addArticleLabel(@RequestBody ArticleLabel articleLabel) {
        if (articleLabel == null || articleLabel.getLabelName() == null || articleLabel.getLabelName().trim().isEmpty()) {
            return ResultBean.error("标签名称不能为空！");
        }
        String labelName = articleLabel.getLabelName().trim();
        if (articleLabelService.existsByLabelName(labelName)) {
            return ResultBean.error("标签已存在，请勿重复添加");
        }
        if (articleLabel.getEnabled() == null) {
            articleLabel.setEnabled(0);
        }
        articleLabel.setLabelName(labelName);
        boolean ok = articleLabelService.save(articleLabel);
        return ok ? ResultBean.success("新增成功！") : ResultBean.error("新增失败！");
    }

    @ApiOperation(value = "修改文章标签")
    @PostMapping("/admin/updArticleLabel")
    public ResultBean updateArticleLabel(@RequestBody ArticleLabel articleLabel) {
        if (articleLabel == null || articleLabel.getLabelId() == null) {
            return ResultBean.error("标签ID不能为空！");
        }
        if (articleLabel.getLabelName() != null) {
            String labelName = articleLabel.getLabelName().trim();
            if (labelName.isEmpty()) {
                return ResultBean.error("标签名称不能为空！");
            }
            if (articleLabelService.existsByLabelNameExcludeId(labelName, articleLabel.getLabelId())) {
                return ResultBean.error("标签已存在，请勿重复添加");
            }
            // 检查该标签下是否有待审批的最佳解答采纳
            int pendingCount = countPendingAdoptsByLabelId(articleLabel.getLabelId());
            if (pendingCount > 0) {
                return ResultBean.error("该标签下有 " + pendingCount + " 条待审批的最佳解答采纳，请先处理后再修改");
            }
            articleLabel.setLabelName(labelName);
        }
        boolean ok = articleLabelService.updateById(articleLabel);
        return ok ? ResultBean.success("修改成功！") : ResultBean.error("修改失败！");
    }

    @ApiOperation(value = "删除文章标签")
    @PostMapping("/admin/delArticleLabel")
    public ResultBean deleteArticleLabel(@RequestBody ArticleLabel articleLabel) {
        if (articleLabel == null || articleLabel.getLabelId() == null) {
            return ResultBean.error("标签ID不能为空！");
        }
        // 检查该标签下是否有待审批的最佳解答采纳
        int pendingCount = countPendingAdoptsByLabelId(articleLabel.getLabelId());
        if (pendingCount > 0) {
            return ResultBean.error("该标签下有 " + pendingCount + " 条待审批的最佳解答采纳，请先处理后再删除");
        }
        boolean ok = articleLabelService.removeById(articleLabel.getLabelId());
        return ok ? ResultBean.success("删除成功！") : ResultBean.error("删除失败！");
    }

    /**
     * 统计指定标签下待审批(adopt_status=1)的采纳数量
     * 查询路径：reply → comment → article → articleLabelId
     */
    private int countPendingAdoptsByLabelId(Integer labelId) {
        if (labelId == null) return 0;
        // 查出该标签下的所有文章ID
        LambdaQueryWrapper<Article> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(Article::getArticleLabelId, labelId).select(Article::getArticleId);
        List<Article> articles = articleService.list(articleWrapper);
        if (articles.isEmpty()) return 0;

        List<Integer> articleIds = articles.stream().map(Article::getArticleId).collect(java.util.stream.Collectors.toList());

        // 查出这些文章对应的所有评论ID
        LambdaQueryWrapper<com.walker.pojo.Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.in(com.walker.pojo.Comment::getCommentArticleId, articleIds)
                     .select(com.walker.pojo.Comment::getCommentId);
        List<com.walker.pojo.Comment> comments = commentService.list(commentWrapper);
        if (comments.isEmpty()) return 0;

        List<Integer> commentIds = comments.stream().map(com.walker.pojo.Comment::getCommentId).collect(java.util.stream.Collectors.toList());

        // 查出这些评论下 adopt_status=1 的回复数
        LambdaQueryWrapper<Reply> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.in(Reply::getCommentId, commentIds)
                   .eq(Reply::getAdoptStatus, 1);
        return (int) replyService.count(replyWrapper);
    }

    @ApiOperation(value = "分页查询文章标签（可按标签名模糊查询）")
    @PostMapping("/admin/pageArticleLabel")
    public ResultBean pageArticleLabel(@RequestBody JSONObject jsonObject) {
        int pageIndex = jsonObject.getIntValue("pageIndex");
        int pageSize = jsonObject.getIntValue("pageSize");
        String searchInfo = jsonObject.getString("searchInfo");
        PageInfo<ArticleLabel> pageInfo = articleLabelService.getAllArticleLabelByPageAndSearch(pageIndex,pageSize,searchInfo);
        return ResultBean.success("成功获取！",pageInfo);
    }
}
