package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Violation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ViolationMapper extends BaseMapper<Violation> {

    @Select("SELECT COALESCE(SUM(points_deducted), 0) FROM bbs_violation WHERE user_id = #{userId} AND create_time >= #{monthStart} AND create_time < #{monthEnd}")
    Integer sumMonthlyDeductions(@Param("userId") Integer userId, @Param("monthStart") String monthStart, @Param("monthEnd") String monthEnd);

    @Select("SELECT * FROM bbs_violation WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Violation> findByUserId(@Param("userId") Integer userId);
}
