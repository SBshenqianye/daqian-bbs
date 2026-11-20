package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.SensitiveHit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词命中记录持久层。
 */
@Mapper
public interface SensitiveHitMapper extends BaseMapper<SensitiveHit> {
}
