package com.walker.vo;

import lombok.Data;

/**
 * 个人积分排名返回VO
 */
@Data
public class PersonalPointsRankVO {
    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String portrait;

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
     * 积分
     */
    private Integer points;

    /**
     * 排名序号
     */
    private Integer rankNum;

    /**
     * 等级序号（1..24，按累计积分换算；用于列表展示等级图标）
     */
    private Integer levelIndex;
}
