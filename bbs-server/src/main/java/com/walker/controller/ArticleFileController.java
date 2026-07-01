package com.walker.controller;

import com.walker.pojo.ArticleFile;
import com.walker.service.ArticleFileService;
import com.walker.vo.CommentReplyVO;
import com.walker.vo.ResultBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/12 9:42
 * @PackageName:com.walker.controller
 * @ClassName: ArticleFileController
 * @Description: 文章附件控制层
 */
@Api(tags = "ArticleFileController")
@RestController
public class ArticleFileController {
    @Autowired
    private ArticleFileService articleFileService;

    @Value("${storage.path}")
    private String basePath;
    /**
     * 方法描述 保存文章中的附件
     * @author chengQing
     * @date 2026/3/11 17:37
     * @param id 当前登录人id
     * @param file 附件
     * @return ResultBean 返回结果集
     */
    @ApiOperation(value = "保存文章中的附件并返回附件id")
    @PostMapping("/articleFile/upload")
    public ResultBean articleFile(@RequestParam("userId") Integer id, @RequestParam("file") MultipartFile file) throws Exception {
        // 附件完整名称，如：xxx.txt
        String fileName = file.getOriginalFilename();
        // 防止路径遍历：过滤掉文件名中的目录分隔符
        if (fileName != null) {
            fileName = new File(fileName).getName();
        }
        // 附件完整类型，如："image/jpeg" 或 "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        String contentType = file.getContentType();
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String day = format.format(date);
        Long time = System.currentTimeMillis();
        String url = "User/" + "id_" + id + "/file/" + day + "/" + time + "/" + fileName;
        // 文件保存的路径
        String path = basePath + url;
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
            file.transferTo(new File(path));
            imageUrl = "/files/"+url;
            // 将附件基本信息 保存到数据库并返回 数据库file信息
            ArticleFile articleFile = new ArticleFile();
            articleFile.setFileName(fileName);
            articleFile.setFileType(contentType);
            articleFile.setFilePath(imageUrl);
            int result = articleFileService.addArticleFile(articleFile);
            if (result > 0) {
                return  ResultBean.success(articleFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultBean.error("上传失败");
    }

    /**
     * 方法描述 获取文章附件信息
     * @author chengQing
     * @date 2026/3/12 11:43
     * @param articleId 文章id
     * @return List<ArticleFile> 返回附件集合
     */
    @ApiOperation(value = "获取文章附件信息")
    @PostMapping("/common/getArticleFileByArticleId/{articleId}")
    public List<ArticleFile> getArticleFileByArticleId(@PathVariable("articleId") Integer articleId){
        return articleFileService.getArticleFileByArticleId(articleId);
    }

}
