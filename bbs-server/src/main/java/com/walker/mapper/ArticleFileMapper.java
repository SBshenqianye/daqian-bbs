package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.ArticleFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Author chengQing
 * @Date 2026/3/12 9:48
 * @PackageName:com.walker.mapper
 * @ClassName: ArticleFileMapper
 * @Description: 文章附件持久层
 */
@Mapper
public interface ArticleFileMapper extends BaseMapper<ArticleFile> {

    /**
     * 方法描述 更新附件与文章的绑定关系
     * @author chengQing
     * @date 2026/3/12 10:49
     * @param fileIds 附件id集合
     * @param articleId 文章id
     * @return int 返回数据影响行数
     */
    int updateArticleFile(@Param("fileIds") Integer[] fileIds, @Param("articleId")Integer articleId);

    /**
     * 方法描述 解除文章绑定的附件
     * @author chengQing
     * @date 2026/3/12 14:18
     * @param articleId 文章id
     * @return int 受影响行数
     */
    int unBindArticleFile(Integer articleId);
}
