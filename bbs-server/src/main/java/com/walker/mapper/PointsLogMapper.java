package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.PointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 积分调整日志 Mapper 接口
 */
@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {

    /**
     * 查询指定用户的手动积分调整总和
     * @param userId 用户ID
     * @return 积分调整总和（正数加分总额 - 负数扣分总额）
     */
    Integer sumPointsChangeByUserId(@Param("userId") Integer userId);
}
