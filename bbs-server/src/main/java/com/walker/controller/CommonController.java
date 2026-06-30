package com.walker.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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
        // 1. Normalize file extension
        String contentType = file.getContentType();
        String ext = contentType != null ? contentType.substring(contentType.indexOf("/") + 1) : "png";
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }

        // 2. Generate date-based path and unique filename
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String day = sdf.format(date);
        long timestamp = System.currentTimeMillis();

        // 3. Build paths
        String relativePath = "common/upload/" + day + "/" + timestamp + "_." + ext;
        String absolutePath = basePath + relativePath;

        // 4. Ensure parent directory exists
        File outFile = new File(absolutePath);
        if (outFile.getParentFile() != null && !outFile.getParentFile().isDirectory()) {
            outFile.getParentFile().mkdirs();
        }

        // 5. Persist file
        file.transferTo(outFile);

        // 6. Return URL string for frontend consumption
        return "/files/" + relativePath;
    }
}
