package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.PointsLog;
import com.walker.vo.ResultBean;

/**
 * 积分调整日志服务接口
 */
public interface PointsLogService extends IService<PointsLog> {

    /**
     * 新增积分调整记录
     */
    ResultBean addPointsLog(PointsLog pointsLog);

    /**
     * 查询用户手动积分调整总和
     */
    Integer getPointsAdjustment(Integer userId);

    /**
     * 调整用户积分（新增记录）
     * @param userId 被调整用户ID
     * @param pointsChange 积分变动（正数加分，负数扣分）
     * @param reason 调整原因
     * @param relatedType 关联类型
     * @param relatedId 关联ID
     * @param operatorId 操作人ID
     */
    ResultBean adjustUserPoints(Integer userId, Integer pointsChange, String reason,
                                String relatedType, Integer relatedId, Integer operatorId);

    /**
     * 撤销一条积分调整记录
     * @param logId 要撤销的记录ID
     * @param operatorId 操作人ID
     */
    ResultBean undoPointsLog(Integer logId, Integer operatorId);
}
