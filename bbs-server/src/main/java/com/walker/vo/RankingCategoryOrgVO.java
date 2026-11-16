package com.walker.vo;

import lombok.Data;

/**
 * 排行榜分类成员组织展示对象。
 */
@Data
public class RankingCategoryOrgVO {
    private String orgNo;
    private String orgName;
    private Integer includeDescendants;
    private Integer sortOrder;
}
