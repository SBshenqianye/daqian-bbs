package com.walker.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 组织榜单统计（按组织/组织子树聚合，排除测试账号）。
 */
@Mapper
public interface RankingStatsMapper {

    Integer countMembers(@Param("orgNo") String orgNo,
                         @Param("includeDescendants") boolean includeDescendants);

    Integer sumPoints(@Param("orgNo") String orgNo,
                      @Param("includeDescendants") boolean includeDescendants,
                      @Param("startTime") String startTime,
                      @Param("endTime") String endTime);

    Integer countPosts(@Param("orgNo") String orgNo,
                       @Param("includeDescendants") boolean includeDescendants,
                       @Param("startTime") String startTime,
                       @Param("endTime") String endTime);

    Integer countReplies(@Param("orgNo") String orgNo,
                         @Param("includeDescendants") boolean includeDescendants,
                         @Param("startTime") String startTime,
                         @Param("endTime") String endTime);
}
