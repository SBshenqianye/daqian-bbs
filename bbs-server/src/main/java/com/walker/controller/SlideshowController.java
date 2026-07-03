package com.walker.controller;


import com.walker.pojo.Slideshow;
import com.walker.service.SlideshowService;
import com.walker.utils.ImageUtil;
import com.walker.vo.ResultBean;
import com.walker.vo.param.SlideshowParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Api(tags = "SlideshowController")
@RestController
//@RequestMapping("/slideshow")
public class SlideshowController {

    @Autowired
    private SlideshowService slideshowService;

    @Value("${storage.path}")
    private String basePath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @ApiOperation(value = "获取首页轮播图")
    @GetMapping("/common/getSlideshow")
    public List<Slideshow> getAllSlideshow(){
        return slideshowService.queryAllSlideshow();
    }

    @ApiOperation(value = "获取轮播图")
    @GetMapping("/admin/getSlideshow")
    public ResultBean getAllSlideshowByAdmin(){
        return slideshowService.getAllSlideshowByAdmin();
    }

    @ApiOperation(value = "保存轮播图返回地址给前端")
    @PostMapping("/admin/addSlideshowReturnImageUrl")
    public String addSlideshowReturnUrl(@RequestParam("image") String image) throws Exception {
        // 图片后缀
        String suffix="jpg";

        Long time = System.currentTimeMillis();

        // 文件保存的路径
        String path = basePath + "Admin/slidesshow/" + time + "_." + suffix;

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
            ImageUtil.generateImage(image, path);


            pathDB = contextPath + "/files/Admin/slidesshow/" + time + "_." + suffix;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return pathDB;
    }


    @ApiOperation(value = "添加轮播图")
    @PostMapping("/admin/addSlideshow")
    public ResultBean addSlideshow(@RequestBody SlideshowParam slideshowParam){
        return slideshowService.saveSlideshow(slideshowParam.getSuccessive(),slideshowParam.getImageUrl());
    }


    @ApiOperation(value = "修改优先级")
    @PutMapping("/admin/editSuccessive/{slideshowId}/{successive}")
    public ResultBean updateSuccessive(@PathVariable Integer slideshowId, @PathVariable Integer successive){
        return slideshowService.updateSuccessive(slideshowId,successive);
    }
}
