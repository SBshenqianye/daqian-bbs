package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.pojo.ModeratorRewardCancel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ModeratorRewardCancelMapper extends BaseMapper<ModeratorRewardCancel> {

    // 使用 LambdaQueryWrapper 避免 MySQL/PostgreSQL SQL 方言差异
    default List<ModeratorRewardCancel> findByYearMonth(String yearMonth) {
        return selectList(new LambdaQueryWrapper<ModeratorRewardCancel>()
                .eq(ModeratorRewardCancel::getYearMonth, yearMonth));
    }

    default List<Integer> findCancelledUserIds(String yearMonth) {
        return selectList(new LambdaQueryWrapper<ModeratorRewardCancel>()
                .eq(ModeratorRewardCancel::getYearMonth, yearMonth)
                .select(ModeratorRewardCancel::getUserId))
                .stream()
                .map(ModeratorRewardCancel::getUserId)
                .collect(java.util.stream.Collectors.toList());
    }
}
