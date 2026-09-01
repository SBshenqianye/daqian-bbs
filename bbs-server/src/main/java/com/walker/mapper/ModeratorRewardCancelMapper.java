package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.ModeratorRewardCancel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModeratorRewardCancelMapper extends BaseMapper<ModeratorRewardCancel> {

    // 注意：year_month 是 MySQL 8.0 保留字，必须用反引号；PostgreSQL 不支持反引号，
    // 若切换数据库需在此处适配或改用 rename column 方案。
    @Select("SELECT * FROM bbs_moderator_reward_cancel WHERE `year_month` = #{yearMonth}")
    List<ModeratorRewardCancel> findByYearMonth(String yearMonth);

    @Select("SELECT user_id FROM bbs_moderator_reward_cancel WHERE `year_month` = #{yearMonth}")
    List<Integer> findCancelledUserIds(String yearMonth);
}
