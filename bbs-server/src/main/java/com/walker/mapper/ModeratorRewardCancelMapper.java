package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.ModeratorRewardCancel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ModeratorRewardCancelMapper extends BaseMapper<ModeratorRewardCancel> {

    @Select("SELECT * FROM bbs_moderator_reward_cancel WHERE year_month = #{yearMonth}")
    List<ModeratorRewardCancel> findByYearMonth(String yearMonth);

    @Select("SELECT user_id FROM bbs_moderator_reward_cancel WHERE year_month = #{yearMonth}")
    List<Integer> findCancelledUserIds(String yearMonth);
}
