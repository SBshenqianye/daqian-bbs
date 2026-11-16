package com.walker.vo.param;

import lombok.Data;

/**
 * 标签热榜参数。
 */
@Data
public class ArticleHotParam {
    private Integer labelId;
    /** views / comments / favorites / likes / shares */
    private String metric = "views";
    /** 兼容字段：默认全部时间；显式传 7d/30d 时才按时间窗过滤 */
    private String range = "all";
    private Integer limit = 10;
}
