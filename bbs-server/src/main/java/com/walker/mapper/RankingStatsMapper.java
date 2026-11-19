package com.walker.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织榜单统计（按组织列表聚合，排除测试账号）。
 */
@Mapper
public interface RankingStatsMapper {

    Integer countMembersByOrgs(@Param("orgNos") List<String> orgNos);

    Integer sumPointsByOrgs(@Param("orgNos") List<String> orgNos,
                            @Param("startTime") String startTime,
                            @Param("endTime") String endTime);

    Integer countPostsByOrgs(@Param("orgNos") List<String> orgNos,
                             @Param("startTime") String startTime,
                             @Param("endTime") String endTime);

    Integer countRepliesByOrgs(@Param("orgNos") List<String> orgNos,
                               @Param("startTime") String startTime,
                               @Param("endTime") String endTime);
}
