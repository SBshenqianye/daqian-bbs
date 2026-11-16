package com.walker.vo.param;

import lombok.Data;

/**
 * 用户端帖子列表筛选/排序参数。
 */
@Data
public class ArticleListParam {
    private String keywords;
    private Integer labelId;
    private Integer authorId;
    private String orgNo;
    private Integer featured;
    private Integer hasAttachment;
    private String startTime;
    private String endTime;
    /** publishTime / lastReplyTime / lastActivityTime / views / comments / likes / favorites / shares / featuredTime */
    private String sortBy = "publishTime";
    /** asc / desc */
    private String sortOrder = "desc";
    private Integer page = 1;
    private Integer size = 20;
}
