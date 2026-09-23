package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Violation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {

    @Select("SELECT COALESCE(SUM(points_deducted), 0) FROM bbs_violation WHERE user_id = #{userId} AND create_time >= #{monthStart} AND create_time < #{monthEnd}")
    Integer sumMonthlyDeductions(@Param("userId") Integer userId, @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd);

    @Select("SELECT * FROM bbs_violation WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Violation> findByUserId(@Param("userId") Integer userId);

    // ── #14 取消违规：恢复被逻辑删除的关联内容（原生 UPDATE 绕过 @TableLogic 过滤） ──

    @Update("UPDATE bbs_article SET is_delete = 0 WHERE article_id = #{id}")
    int restoreArticleById(@Param("id") Integer id);

    @Update("UPDATE bbs_comment SET is_delete = 0 WHERE comment_id = #{id}")
    int restoreCommentById(@Param("id") Integer id);

    @Update("UPDATE bbs_reply SET is_delete = 0 WHERE reply_id = #{id}")
    int restoreReplyById(@Param("id") Integer id);

    /** 评论 id → 所属文章 id（原生 SQL，不受 @TableLogic 影响，可查到已删除评论） */
    @Select({"<script>",
            "SELECT comment_id AS commentId, comment_article_id AS articleId FROM bbs_comment WHERE comment_id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<Map<String, Object>> selectArticleIdByCommentIds(@Param("ids") List<Integer> ids);

    /** 回复 id → 所属评论 id（原生 SQL，不受 @TableLogic 影响，可查到已删除回复） */
    @Select({"<script>",
            "SELECT reply_id AS replyId, comment_id AS commentId FROM bbs_reply WHERE reply_id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<Map<String, Object>> selectCommentIdByReplyIds(@Param("ids") List<Integer> ids);
}
