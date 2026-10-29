package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 批量统计各文章的评论数量
     * @param articleIds 文章ID列表
     * @return 每行包含 articleId 和 commentCount 的 Map 列表
     */
    List<Map<String, Object>> countByArticleIds(@Param("list") List<Integer> articleIds);

    /** 绕过 @TableLogic 查询（用于已删除内容的板块归属解析） */
    @Select("SELECT * FROM bbs_comment WHERE comment_id = #{commentId}")
    Comment selectByIdRaw(@Param("commentId") Integer commentId);
}
