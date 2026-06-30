package com.walker.controller;


import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.walker.pojo.ArticleLabel;
import com.walker.service.ArticleLabelService;
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
        boolean ok = articleLabelService.removeById(articleLabel.getLabelId());
        return ok ? ResultBean.success("删除成功！") : ResultBean.error("删除失败！");
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
