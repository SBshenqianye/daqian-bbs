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

    /**
     * 分页查询用户积分变动记录
     */
    ResultBean getUserPointsLog(Integer userId, Integer page, Integer size);

    /**
     * 统计用户在指定主题帖下已获得的回帖积分次数（评论+回复）
     * 同一篇主题帖下同一用户最多累计获得3次回帖积分
     * @param userId 用户ID
     * @param articleId 主题帖ID
     * @return 已获得的回帖积分次数
     */
    int countReplyPointsForArticle(Integer userId, Integer articleId);

    /**
     * 统计用户在指定主题帖下已获得的最佳解答采纳积分次数
     * 同一篇主题帖下同一用户最多获得1次最佳解答积分
     * @param userId 用户ID
     * @param articleId 主题帖ID
     * @return 已获得的采纳积分次数
     */
    int countAdoptPointsForArticle(Integer userId, Integer articleId);

    /**
     * 查询指定文章是否已被采纳为建议
     * @param articleId 文章ID
     * @return 已采纳记录数（0=未采纳，>0=已采纳）
     */
    int countSuggestionAdoptForArticle(Integer articleId);
}
