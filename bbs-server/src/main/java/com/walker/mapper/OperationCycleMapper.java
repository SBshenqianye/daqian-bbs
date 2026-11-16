package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.OperationCycle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OperationCycleMapper extends BaseMapper<OperationCycle> {

    @Select("SELECT config_value FROM bbs_cycle_config c JOIN bbs_operation_cycle o ON o.id=c.cycle_id "
            + "WHERE o.status=1 AND c.config_key=#{key} LIMIT 1")
    String selectActiveCycleConfig(@Param("key") String key);

    @Select("SELECT id FROM bbs_operation_cycle WHERE status=1 LIMIT 1")
    Integer selectActiveCycleId();
}
