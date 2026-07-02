package com.walker.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author walker
 * @PackageName:com.walker.controller
 * @ClassName: CommonController
 * @Description: 通用功能（mavon-editor 图片上传等）
 */
@Api(tags = "CommonController")
@RestController
public class CommonController {

    @Value("${storage.path}")
    private String basePath;

    @ApiOperation(value = "上传图片（mavon-editor 通用图片上传）")
    @PostMapping("/common/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        String pType = file.getContentType();
        pType = pType.substring(pType.indexOf("/") + 1);
        if ("jpeg".equals(pType)) {
            pType = "jpg";
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String day = format.format(date);
        Long time = System.currentTimeMillis();

        String url = "common/upload/" + day + "/" + time + "_." + pType;
        String path = basePath + url;

        String imageUrl = "";
        File outFile = new File(path);
        File parentDir = outFile.getParentFile();
        if (parentDir != null && !parentDir.isDirectory()) {
            if (!parentDir.mkdirs() && !parentDir.exists()) {
                throw new IOException("无法创建上传目录: " + parentDir.getAbsolutePath());
            }
        }
        try {
            file.transferTo(new File(path));
            imageUrl = "/files/" + url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageUrl;
    }
}
