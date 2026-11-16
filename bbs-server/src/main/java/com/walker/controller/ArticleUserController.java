package com.walker.controller;


import com.walker.pojo.Article;
import com.walker.service.ArticleService;
import com.walker.service.ArticleUserService;
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
 * @since 2022/12/10 18:03
 */

@Api(tags = "ArticleUserController")
@RestController
public class ArticleUserController {

    @Autowired
    private ArticleUserService articleUserService;

    @Autowired
    private ArticleService articleService;


    @ApiOperation(value = "用户收藏文章")
    @PostMapping("/saveUserCollection")
    public ResultBean saveUserCollection(@RequestParam Integer userId, @RequestParam Integer articleId){

        return articleUserService.saveUserCollection(userId, articleId);
    }

    @ApiOperation(value = "获取用户收藏的文章")
    @GetMapping("/getMyCollection")
    public List<Article> getMyCollection(@RequestParam Integer userId){
        return articleUserService.getMyCollection(userId);
    }

    @ApiOperation(value = "我的收藏（分页，含作者信息与等级）")
    @GetMapping("/common/user/favorites")
    public ResultBean favorites(@RequestParam Integer userId,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        return articleService.queryFavoritePage(userId, page, size);
    }

}
