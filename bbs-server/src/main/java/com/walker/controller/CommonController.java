package com.walker.controller;

import com.walker.utils.FileValidationUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author walker
 * @PackageName:com.walker.controller
 * @ClassName: CommonController
 * @Description: 通用功能（图片上传等）
 */
@Api(tags = "CommonController")
@RestController
public class CommonController {

    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>();
    static {
        ALLOWED_IMAGE_EXTENSIONS.add("jpg");
        ALLOWED_IMAGE_EXTENSIONS.add("jpeg");
        ALLOWED_IMAGE_EXTENSIONS.add("png");
        ALLOWED_IMAGE_EXTENSIONS.add("gif");
        ALLOWED_IMAGE_EXTENSIONS.add("bmp");
        ALLOWED_IMAGE_EXTENSIONS.add("webp");
    }

    @Value("${storage.path}")
    private String basePath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @ApiOperation(value = "上传图片（通用图片上传）")
    @PostMapping("/common/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        // 1. 登录校验
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            log.warn("未登录用户尝试上传文件");
            return "请先登录";
        }

        // 2. 文件校验（类型 + 大小 + 魔术字节）
        String err = FileValidationUtil.validateImage(file, ALLOWED_IMAGE_EXTENSIONS, FileValidationUtil.IMAGE_MAX_SIZE);
        if (err != null) {
            log.warn("图片上传校验不通过: {}", err);
            return err;
        }

        // 3. 确定扩展名（优先用魔术字节推断，fallback 到 Content-Type）
        String ext = detectImageExt(file);

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String day = format.format(date);
        Long time = System.currentTimeMillis();

        String url = "common/upload/" + day + "/" + time + "_." + ext;
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
            imageUrl = contextPath + "/files/" + url;
        } catch (Exception e) {
            log.error("图片保存失败", e);
        }
        return imageUrl;
    }

    /**
     * 通过魔术字节推断图片扩展名，fallback 到 Content-Type
     */
    private String detectImageExt(MultipartFile file) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (java.io.InputStream is = file.getInputStream()) {
            read = is.read(header, 0, header.length);
        }
        if (read >= 3) {
            if (header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) return "jpg";
            if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return "png";
            if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46) return "gif";
            if (header[0] == 0x42 && header[1] == 0x4D) return "bmp";
            if (read >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) return "webp";
        }
        // fallback: Content-Type
        String ct = file.getContentType();
        if (ct != null && ct.contains("/")) {
            String t = ct.substring(ct.indexOf("/") + 1).toLowerCase();
            if ("jpeg".equals(t)) return "jpg";
            if (ALLOWED_IMAGE_EXTENSIONS.contains(t)) return t;
        }
        return "jpg"; // 兜底
    }
}
