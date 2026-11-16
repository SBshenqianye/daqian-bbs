package com.walker.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 个人中心各 Tab 的“最新动态时间”，用于前端红点提示。
 */
@Mapper
public interface AccountBadgeMapper {

    @Select("SELECT MAX(create_time) FROM bbs_points_log WHERE user_id = #{userId}")
    String pointsLatest(@Param("userId") Integer userId);

    @Select("SELECT MAX(create_time) FROM bbs_violation WHERE user_id = #{userId}")
    String violationsLatest(@Param("userId") Integer userId);

    @Select("SELECT MAX(create_time) FROM bbs_article_user WHERE user_id = #{userId}")
    String favoritesLatest(@Param("userId") Integer userId);

    @Select("SELECT MAX(create_time) FROM bbs_appeal WHERE user_id = #{userId}")
    String appealsLatest(@Param("userId") Integer userId);

    @Select("SELECT MAX(create_time) FROM bbs_moderator_complaint WHERE user_id = #{userId}")
    String complaintsLatest(@Param("userId") Integer userId);

    /** 未读互动通知（回复/评论我）的最新时间 */
    @Select("SELECT MAX(create_time) FROM bbs_notification "
            + "WHERE user_id = #{userId} AND category = 'interaction' AND (is_read = 0 OR is_read IS NULL)")
    String repliesLatest(@Param("userId") Integer userId);

    @Select("SELECT MAX(create_time) FROM bbs_report WHERE reporter_id = #{userId}")
    String reportsLatest(@Param("userId") Integer userId);
}
