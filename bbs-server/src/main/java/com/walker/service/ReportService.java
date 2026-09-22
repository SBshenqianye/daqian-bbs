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
    ResultBean submitReport(Integer reporterId, String targetType, Integer targetId, String violationType, String reason);

    /**
     * 管理员审核举报
     */
    ResultBean reviewReport(Integer reportId, Integer reviewerId, String status, String remark);

    /**
     * 管理员审核举报（含恶意举报扣分参数）。
     * 仅 status=rejected 时 malicious/deductPoints 生效：
     * 勾选恶意举报后对举报人扣 deductPoints 分（正数，分）并通知、记积分日志；
     * 普通驳回仅置 rejected 并通知举报人结果。
     *
     * @param malicious     是否认定为恶意/虚假举报（仅驳回时有效）
     * @param deductPoints  恶意举报扣分分值（正整数，分）；malicious=true 时必填
     */
    ResultBean reviewReport(Integer reportId, Integer reviewerId, String status, String remark,
                            Boolean malicious, Integer deductPoints);

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

    /**
     * 前置查询：当前用户对某目标是否已有未驳回的举报（pending/confirmed）。
     * 供前端"点举报"时即时提醒，口径与 submitReport 的重复/已核实拦截一致。
     * 返回 {reported: bool, status: 'pending'|'confirmed'|null}。
     */
    ResultBean checkReported(Integer reporterId, String targetType, Integer targetId);
}
