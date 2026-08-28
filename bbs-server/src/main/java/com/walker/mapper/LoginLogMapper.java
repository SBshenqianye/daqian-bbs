package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    @Select("SELECT * FROM bbs_login_log WHERE user_id = #{userId} AND login_date = #{loginDate}")
    LoginLog findByUserAndDate(@Param("userId") Integer userId, @Param("loginDate") String loginDate);
}
