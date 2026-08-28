package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 通知 Mapper 接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 查询用户未读通知数量（全部类型合计）
     */
    @Select("SELECT COUNT(*) FROM bbs_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(@Param("userId") Integer userId);

    /**
     * 按分类分组查询用户未读通知数量（category → count）
     */
    @Select("SELECT category, COUNT(*) AS cnt FROM bbs_notification WHERE user_id = #{userId} AND is_read = 0 GROUP BY category")
    List<Map<String, Object>> countUnreadGroupByCategory(@Param("userId") Integer userId);

    /**
     * 按通知类型分组查询用户未读数量（type → count）
     */
    @Select("SELECT type, COUNT(*) AS cnt FROM bbs_notification WHERE user_id = #{userId} AND is_read = 0 GROUP BY type")
    List<Map<String, Object>> countUnreadGroupByType(@Param("userId") Integer userId);
}
