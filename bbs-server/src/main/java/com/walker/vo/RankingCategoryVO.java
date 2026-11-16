package com.walker.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜分类展示对象。legacy=true 表示“内江供电公司本部”，沿用原有排名逻辑，不参与拖拽配置。
 */
@Data
public class RankingCategoryVO {
    private Integer id;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer sortOrder;
    private Integer enabled;
    private Boolean legacy;
    private Integer orgCount;
    private List<RankingCategoryOrgVO> members = new ArrayList<>();
}
