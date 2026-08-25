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
     * 用户查看自己的举报记录
     */
    ResultBean listMyReports(Integer reporterId, Integer page, Integer size);
}
