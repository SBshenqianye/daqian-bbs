package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.ModeratorComplaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ModeratorComplaintMapper extends BaseMapper<ModeratorComplaint> {

    @Select("SELECT COUNT(*) FROM bbs_moderator_complaint WHERE reporter_id = #{reporterId} AND moderator_id = #{moderatorId} AND status = 'pending'")
    int countPendingByReporterAndModerator(Integer reporterId, Integer moderatorId);
}
