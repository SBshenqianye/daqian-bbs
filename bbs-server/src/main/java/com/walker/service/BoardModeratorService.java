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

    /**
     * 取消指定版主下月履职奖励
     * @param userId 被取消的版主用户ID
     * @param operatorId 操作人ID
     * @param remark 取消原因
     * @return 操作结果
     */
    ResultBean cancelReward(Integer userId, Integer operatorId, String remark);

    /**
     * 恢复指定版主下月履职奖励（取消取消）
     * @param userId 被恢复的版主用户ID
     * @return 操作结果
     */
    ResultBean restoreReward(Integer userId);

    /**
     * 查询当前月取消列表
     * @return 取消记录列表
     */
    ResultBean listCancelledRewards();

    /**
     * 自动发放版主履职奖励（定时任务调用）
     * 检查当前日期是否为发放日，若是则自动发放
     * @return 发放结果
     */
    ResultBean autoMonthlyReward();
}
