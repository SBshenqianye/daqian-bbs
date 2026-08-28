package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Report;
import com.walker.vo.ResultBean;

import java.util.Map;

/**
 * 实名举报服务接口
 */
public interface ReportService extends IService<Report> {

    /**
     * 用户提交举报
     */
    ResultBean submitReport(Integer reporterId, String targetType, Integer targetId, String reason);

    /**
     * 管理员审核举报
     */
    ResultBean reviewReport(Integer reportId, Integer reviewerId, String status, String remark);

    /**
     * 分页查询举报列表（管理员端）
     */
    ResultBean listReports(String status, Integer page, Integer size);

    /**
     * 分页查询举报列表——按举报目标（targetType+targetId）分组折叠（管理员端）。
     * 每组返回 representative（代表记录：优先首条 pending，否则最早一条）、members（组内全部记录）、totalCount。
     * 分页以组为单位，避免同组记录被拆到两页。
     */
    ResultBean listReportsGrouped(String status, Integer page, Integer size);

    /**
     * 用户查看自己的举报记录
     */
    ResultBean listMyReports(Integer reporterId, Integer page, Integer size);
}
