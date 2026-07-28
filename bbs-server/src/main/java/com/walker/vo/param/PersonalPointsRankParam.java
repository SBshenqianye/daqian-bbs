package com.walker.vo.param;

import lombok.Data;

/**
 * 个人积分排名请求参数
 */
@Data
public class PersonalPointsRankParam {
    /**
     * 排名类型（01：当月，02：累计）
     */
    private String rankType;

    /**
     * 统计开始日期（yyyy-MM-dd）
     */
    private String startTime;

    /**
     * 统计结束日期（yyyy-MM-dd）
     */
    private String endTime;

    /**
     * 当前用户ID（用于底部展示登录人信息）
     */
    private Integer currentUserId;

    /**
     * 查询目标用户ID（getUserPersonalRank 用）
     */
    private Integer userId;

    /**
     * 返回条数（默认20）
     */
    private Integer size = 20;

    /**
     * 发帖积分（由 Service 设置）
     */
    private Integer post;

    /**
     * 回帖积分（由 Service 设置）
     */
    private Integer reply;

    /**
     * 精华帖加分（由 Service 设置）
     */
    private Integer featured;
}
