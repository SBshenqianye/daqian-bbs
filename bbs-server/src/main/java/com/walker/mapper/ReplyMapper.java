package com.walker.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.walker.pojo.Reply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author walker
 * @since 2022/05/24 11:10
 */
@Mapper
public interface ReplyMapper extends BaseMapper<Reply> {

    /** 绕过 @TableLogic 查询（用于已删除内容的板块归属解析） */
    @Select("SELECT * FROM bbs_reply WHERE reply_id = #{replyId}")
    Reply selectByIdRaw(@Param("replyId") Integer replyId);
}
