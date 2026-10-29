package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.BoardModerator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BoardModeratorMapper extends BaseMapper<BoardModerator> {

    @Select("SELECT * FROM bbs_board_moderator WHERE user_id = #{userId} AND label_id = #{labelId} AND status = 1")
    BoardModerator findByUserAndLabel(@Param("userId") Integer userId, @Param("labelId") Integer labelId);

    /**
     * 查询同用户同板块的历史记录（含已撤销），用于撤销后重新任命时复用记录。
     */
    @Select("SELECT * FROM bbs_board_moderator WHERE user_id = #{userId} AND label_id = #{labelId} ORDER BY id DESC LIMIT 1")
    BoardModerator findAnyByUserAndLabel(@Param("userId") Integer userId, @Param("labelId") Integer labelId);
}
