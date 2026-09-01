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

    /**
     * 发放本月版主履职奖励（每月一次性15积分）
     * 运营方案：圆满完成月度版务工作，每月给予一次性15积分履职奖励
     * @param operatorId 操作人ID（管理员触发）
     * @return 发放结果
     */
    ResultBean monthlyReward(Integer operatorId);
}
