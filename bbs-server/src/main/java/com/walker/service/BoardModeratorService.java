package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.BoardModerator;
import com.walker.vo.ResultBean;

/**
 * 版块管理员服务接口
 */
public interface BoardModeratorService extends IService<BoardModerator> {

    /**
     * 任命版主
     */
    ResultBean appoint(Integer userId, Integer labelId, Integer operatorId);

    /**
     * 撤销版主
     */
    ResultBean dismiss(Integer userId, Integer labelId);

    /**
     * 版主列表
     */
    ResultBean listModerators(Integer page, Integer size);

    /**
     * 检查用户是否为某标签版主
     */
    boolean isModerator(Integer userId, Integer labelId);
}
