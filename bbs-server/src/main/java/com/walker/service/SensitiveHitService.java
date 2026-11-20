package com.walker.service;

import com.walker.pojo.SensitiveHit;
import com.walker.vo.ResultBean;

import java.util.List;
import java.util.Map;

/**
 * 敏感词命中记录与风险画像。
 *
 * <p>只做"记录与呈现"，不做处置：写库时采集命中，读接口按权限与板块范围展示。</p>
 */
public interface SensitiveHitService {

    /**
     * 采集一条内容的命中记录（同一内容只保留一行，编辑时更新）。
     * 内部吞掉所有异常，绝不因采集失败影响发布。
     *
     * @param targetType 内容类型 article/comment/reply
     * @param targetId   内容 id
     * @param userId     作者 id
     * @param labelId    所属板块 id（可为空）
     * @param text       用于匹配的原文
     */
    void record(String targetType, Integer targetId, Integer userId, Integer labelId, String text);

    /**
     * 指定用户最近 N 小时命中的内容条数（用于"超阈值进审核"）。
     */
    int countRecentHits(Integer userId, int hours);

    /**
     * 热力方阵：全量用户 + 累计命中数；版主只统计自己板块内的命中。
     */
    Map<String, Object> grid();

    /**
     * 风险排行榜（含近 12 周热力条）。
     */
    ResultBean rank(int page, int size, int days);

    /**
     * 单个风险用户的详情：基本统计 + 命中内容列表。
     */
    ResultBean userDetail(Integer userId, int page, int size);

    /**
     * 清理一条内容的命中记录（内容被删除时同步）。
     */
    void remove(String targetType, Integer targetId);

    /**
     * 查询一条内容的命中词（审核详情用）。
     */
    List<SensitiveHit> findByTarget(String targetType, Integer targetId);

    /**
     * 命中详情：命中词 + 原文上下文片段（受 sensitive:read 与板块范围约束）。
     */
    ResultBean hitDetail(String targetType, Integer targetId);
}
