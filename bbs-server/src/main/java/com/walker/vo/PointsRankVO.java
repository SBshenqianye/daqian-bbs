package com.walker.vo;

import lombok.Data;

/**
 * @Author chengQing
 * @Date 2026/3/9 14:39
 * @PackageName:com.walker.vo
 * @ClassName: PointsRankVO
 * @Description: 积分排名返回实体类
 */
@Data
public class PointsRankVO {
    /**
     * 单位编号
     */
    private String orgNo;
    /**
     * 单位名称
     */
    private String orgName;
    /**
     * 发帖数
     */
    private Integer posts;
    /**
     * 回帖数
     */
    private Integer replies;

    /**
     * 精华帖数
     */
    private Integer featuredPosts;
    /**
     * 积分
     */
    private Integer points;
    /**
     * 排序序号
     */
    private Integer rankNum;
    /**
     * 是否本级（0：否，1：是）
     */
    private Integer isSelf;
}
