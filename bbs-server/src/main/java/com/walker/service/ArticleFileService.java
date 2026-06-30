package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.ArticleFile;

import java.util.List;

/**
 * @Author chengQing
 * @Date 2026/3/12 9:47
 * @PackageName:com.walker.service
 * @ClassName: ArticleFileService
 * @Description: 文章附件接口层
 */
public interface ArticleFileService extends IService<ArticleFile> {

    /**
     * 方法描述 保存文章附件
     * @author chengQing
     * @date 2026/3/12 10:17
     * @param articleFile 文章附件保存实体类
     * @return int
     */
    int addArticleFile(ArticleFile articleFile);

    /**
     * 方法描述 更新附件与文章的绑定关系
     * @author chengQing
     * @date 2026/3/12 10:49
     * @param fileIds 附件id集合
     * @param articleId 文章id
     * @return int 返回数据影响行数
     */
    int updateArticleFile(Integer [] fileIds, Integer articleId);

    /**
     * 方法描述 获取文章附件信息
     * @author chengQing
     * @date 2026/3/12 11:45
     * @param articleId 文章id
     * @return List<ArticleFile> 返回附件信息集合
     */
    List<ArticleFile> getArticleFileByArticleId(Integer articleId);

    /**
     * 方法描述 解除文章绑定的附件
     * @author chengQing
     * @date 2026/3/12 14:18
     * @param articleId 文章id
     * @return int 受影响行数
     */
    int unBindArticleFile(Integer articleId);

    /**
     * 方法描述 根据关键词搜索文章附件
     * @author chengQing
     * @date 2026/3/12 14:18
     * @param keywords 关键词
     * @return List<ArticleFile> 搜索结果
     */
    List<ArticleFile> getArticleFileByKeywords(String keywords);
}
