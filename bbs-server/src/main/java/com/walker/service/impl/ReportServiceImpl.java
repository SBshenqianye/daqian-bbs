package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ReportMapper;
import com.walker.pojo.Report;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.ReportService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public ResultBean submitReport(Integer reporterId, String targetType, Integer targetId, String reason) {
        if (reporterId == null || targetType == null || targetId == null) {
            return ResultBean.error("参数不完整");
        }

        // 检查是否已举报过
        long count = this.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .ne(Report::getStatus, "rejected"));
        if (count > 0) {
            return ResultBean.error("您已举报过该内容，请等待审核");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus("pending");
        report.setPointsAwarded(0);
        report.setCreateTime(fmt.format(now));
        this.save(report);

        return ResultBean.success("举报已提交，等待审核");
    }

    @Override
    @Transactional
    public ResultBean reviewReport(Integer reportId, Integer reviewerId, String status, String remark) {
        if (reportId == null || reviewerId == null || status == null) {
            return ResultBean.error("参数不完整");
        }

        Report report = this.getById(reportId);
        if (report == null) {
            return ResultBean.error("举报记录不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            return ResultBean.error("该举报已处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        report.setStatus(status);
        report.setReviewerId(reviewerId);
        report.setReviewTime(fmt.format(now));
        report.setReviewRemark(remark);

        // 确认举报属实：给举报人加2分
        if ("confirmed".equals(status)) {
            report.setPointsAwarded(1);
            this.updateById(report);

            pointsLogService.adjustUserPoints(report.getReporterId(), 2, "举报属实奖励",
                    "report", reportId, reviewerId);

            // 通知举报人
            notificationService.createNotification(report.getReporterId(), reviewerId,
                    "report_confirmed", "您的举报已核实", "report", reportId);
        } else {
            this.updateById(report);
        }

        return ResultBean.success("审核完成");
    }

    @Override
    public ResultBean listReports(String status, Integer page, Integer size) {
        Page<Report> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);
        Page<Report> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyReports(Integer reporterId, Integer page, Integer size) {
        Page<Report> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId);
        wrapper.orderByDesc(Report::getCreateTime);
        Page<Report> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }
}
