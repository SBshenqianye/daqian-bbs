package com.walker.service;

import com.walker.vo.ResultBean;

/**
 * 管理端仪表盘统计服务（#7）
 */
public interface DashboardService {

    /**
     * 仪表盘概览聚合统计：用户/帖子/评论/举报待审/违规/申诉待审等待办与存量计数。
     * 口径与各管理列表一致（已逻辑删除的内容由 MyBatis-Plus @TableLogic 自动过滤）。
     */
    ResultBean getCounts();
}
