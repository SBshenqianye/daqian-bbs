package com.walker.vo.param;

import lombok.Data;

/**
 * 分类组织榜查询参数。
 */
@Data
public class RankingOrgRankParam {
    /** 0 或 null = 内江供电公司本部（沿用旧逻辑） */
    private Integer categoryId;
    private String startTime;
    private String endTime;
}
