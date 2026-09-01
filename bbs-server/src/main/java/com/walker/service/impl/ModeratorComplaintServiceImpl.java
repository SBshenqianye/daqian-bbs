package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.User;
import com.walker.service.BoardModeratorService;
import com.walker.service.ModeratorComplaintService;
import com.walker.service.NotificationService;
import com.walker.service.UserService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ModeratorComplaintServiceImpl extends ServiceImpl<ModeratorComplaintMapper, ModeratorComplaint>
        implements ModeratorComplaintService {

    @Autowired
    private ModeratorComplaintMapper complaintMapper;

    @Autowired
    private BoardModeratorService boardModeratorService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Override
    @Transactional
    public ResultBean submit(Integer reporterId, Integer moderatorId, Integer labelId, String content) {
        if (reporterId == null || moderatorId == null || content == null || content.trim().isEmpty()) {
            return ResultBean.error("参数不完整");
        }
        if (reporterId.equals(moderatorId)) {
            return ResultBean.error("不能投诉自己");
        }

        // 检查是否已有相同版主的待审投诉
        int pendingCount = complaintMapper.countPendingByReporterAndModerator(reporterId, moderatorId);
        if (pendingCount > 0) {
            return ResultBean.error("您已对该版主提交过投诉，请等待审核结果");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setReporterId(reporterId);
        complaint.setModeratorId(moderatorId);
        complaint.setLabelId(labelId);
        complaint.setContent(content.trim());
        complaint.setStatus("pending");
        complaint.setCreateTime(fmt.format(new Date()));
        this.save(complaint);

        // 通知超级管理员（bbs_user id=1）
        User reporter = userService.getById(reporterId);
        String reporterName = reporter != null ? reporter.getNickname() : "用户#" + reporterId;
        notificationService.createNotification(1, reporterId, "moderator_complaint",
                "收到新的版主投诉：「" + reporterName + "」投诉版主 #" + moderatorId,
                "moderator_complaint", complaint.getId());

        return ResultBean.success("投诉已提交，将在5个工作日内处理");
    }

    @Override
    @Transactional
    public ResultBean review(Integer complaintId, String status, String remark, Integer reviewerId) {
        if (complaintId == null || status == null) {
            return ResultBean.error("参数不完整");
        }
        if (!"accepted".equals(status) && !"rejected".equals(status)) {
            return ResultBean.error("无效的审核状态");
        }

        ModeratorComplaint complaint = this.getById(complaintId);
        if (complaint == null) {
            return ResultBean.error("投诉记录不存在");
        }
        if (!"pending".equals(complaint.getStatus())) {
            return ResultBean.error("该投诉已处理");
        }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        complaint.setStatus(status);
        complaint.setReviewerId(reviewerId);
        complaint.setReviewRemark(remark);
        complaint.setReviewTime(fmt.format(new Date()));
        this.updateById(complaint);

        // 如果接受投诉，撤销版主身份
        if ("accepted".equals(status) && complaint.getLabelId() != null) {
            boardModeratorService.dismiss(complaint.getModeratorId(), complaint.getLabelId());
        }

        // 通知投诉人
        String statusLabel = "accepted".equals(status) ? "已采纳" : "已驳回";
        notificationService.createNotification(complaint.getReporterId(), reviewerId, "complaint_review",
                "您的版主投诉已审核：" + statusLabel + (remark != null ? "（" + remark + "）" : ""),
                "moderator_complaint", complaint.getId());

        return ResultBean.success("审核完成" + ("accepted".equals(status) ? "，版主已被撤销" : ""));
    }

    @Override
    public ResultBean listComplaints(Integer page, Integer size, String status) {
        Page<ModeratorComplaint> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ModeratorComplaint> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ModeratorComplaint::getStatus, status);
        }
        wrapper.orderByDesc(ModeratorComplaint::getCreateTime);
        Page<ModeratorComplaint> result = this.page(pageParam, wrapper);

        // 填充用户信息
        List<Map<String, Object>> records = new ArrayList<>();
        Set<Integer> userIds = new HashSet<>();
        for (ModeratorComplaint c : result.getRecords()) {
            userIds.add(c.getReporterId());
            userIds.add(c.getModeratorId());
        }
        Map<Integer, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(userIds);
            for (User u : users) userMap.put(u.getId(), u);
        }

        for (ModeratorComplaint c : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("reporterId", c.getReporterId());
            User reporter = userMap.get(c.getReporterId());
            map.put("reporterName", reporter != null ? reporter.getNickname() : "");
            map.put("moderatorId", c.getModeratorId());
            User moderator = userMap.get(c.getModeratorId());
            map.put("moderatorName", moderator != null ? moderator.getNickname() : "");
            map.put("labelId", c.getLabelId());
            map.put("content", c.getContent());
            map.put("status", c.getStatus());
            map.put("reviewRemark", c.getReviewRemark());
            map.put("reviewTime", c.getReviewTime());
            map.put("createTime", c.getCreateTime());
            records.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyComplaints(Integer reporterId) {
        LambdaQueryWrapper<ModeratorComplaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModeratorComplaint::getReporterId, reporterId);
        wrapper.orderByDesc(ModeratorComplaint::getCreateTime);
        List<ModeratorComplaint> list = this.list(wrapper);

        // 填充版主信息
        Set<Integer> modIds = new HashSet<>();
        for (ModeratorComplaint c : list) modIds.add(c.getModeratorId());
        Map<Integer, User> userMap = new HashMap<>();
        if (!modIds.isEmpty()) {
            List<User> users = userService.listByIds(modIds);
            for (User u : users) userMap.put(u.getId(), u);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (ModeratorComplaint c : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("moderatorId", c.getModeratorId());
            User moderator = userMap.get(c.getModeratorId());
            map.put("moderatorName", moderator != null ? moderator.getNickname() : "");
            map.put("content", c.getContent());
            map.put("status", c.getStatus());
            map.put("reviewRemark", c.getReviewRemark());
            map.put("createTime", c.getCreateTime());
            records.add(map);
        }

        return ResultBean.success("查询成功", records);
    }
}
