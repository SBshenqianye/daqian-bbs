package com.walker.vo.param;

import lombok.Data;
import lombok.NonNull;

/**
 * @Author chengQing
 * @Date 2026/3/9 14:36
 * @PackageName:com.walker.vo.param
 * @ClassName: PointsRankParam
 * @Description: 积分排名参数实体类
 */
@Data
public class PointsRankParam {
    /**
     * 排名类型（01：当月排名，02累计排名）
     */
    private String rankType;
    /**
     * 单位编号
     */
    private String orgNo;
    /**
     * 统计开始日期（yyyy-mm-dd）
     */
    private String startTime;
    /**
     * 统计结束日期（yyyy-mm-dd）
     */
    private String endTime;
    /**
     * 查询单位层级长度（市县所：5-7-9）
     */
    private Integer orgLength;
    /**
     * 发帖积分
     */
    private Integer post;
    /**
     * 回帖积分
     */
    private Integer reply;

    /**
     * 精华帖加分
     */
    private Integer featured;
}
