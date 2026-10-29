package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.User;
import com.walker.service.BoardModeratorService;
import com.walker.service.AuthorizationService;
import com.walker.service.CurrentUserService;
import com.walker.service.ModeratorComplaintService;
import com.walker.service.NotificationService;
import com.walker.service.SelfApprovalPolicy;
import com.walker.service.UserService;
import com.walker.config.security.Permission;
import com.walker.config.security.SelfApprovalException;
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

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SelfApprovalPolicy selfApprovalPolicy;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    @Transactional
    public ResultBean submit(Integer reporterId, Integer moderatorId, Integer labelId, String content) {
        if (reporterId == null || content == null || content.trim().isEmpty()) {
            return ResultBean.error("请填写投诉内容");
        }
        if (reporterId.equals(moderatorId)) {
            return ResultBean.error("不能投诉自己");
        }

        // 如果指定了版主，检查是否已有相同版主的待审投诉
        if (moderatorId != null) {
            int pendingCount = complaintMapper.countPendingByReporterAndModerator(reporterId, moderatorId);
            if (pendingCount > 0) {
                return ResultBean.error("您已对该版主提交过投诉，请等待审核结果");
            }
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

        // 按权限广播：拥有 moderator:complaint:review 的管理员/超管全部接收
        User reporter = userService.getById(reporterId);
        String reporterName = reporter != null ? reporter.getNickname() : "用户#" + reporterId;
        String targetDesc = moderatorId != null ? "版主 #" + moderatorId : "（未指定具体版主）";
        for (Integer reviewerId : authorizationService.getUserIdsWithPermission(Permission.MODERATOR_COMPLAINT_REVIEW)) {
            notificationService.createNotification(reviewerId, reporterId, "moderator_complaint",
                    "收到新的版主投诉：「" + reporterName + "」投诉" + targetDesc,
                    "moderator_complaint", complaint.getId());
        }

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
        // 审核人以登录态身份为准：前端传入的 reviewerId 必须与当前登录用户一致，防伪造
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        Integer currentUserId = currentUser.getId();
        if (reviewerId != null && !currentUserId.equals(reviewerId)) {
            return ResultBean.error("审核人身份校验失败，请重新登录后操作");
        }
        // 后续落库与通知均以登录态 currentUserId 为准
        reviewerId = currentUserId;

        ModeratorComplaint complaint = this.getById(complaintId);
        if (complaint == null) {
            return ResultBean.error("投诉记录不存在");
        }
        // 权限校验：除超级管理员外，审核人不得审核自己的投诉（2026-10-12 需求）
        try {
            selfApprovalPolicy.checkReviewComplaint(currentUser, complaint);
        } catch (SelfApprovalException e) {
            return ResultBean.error(e.getMessage());
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

        // 如果接受投诉，撤销版主身份（需要同时有 moderatorId 和 labelId）
        if ("accepted".equals(status) && complaint.getModeratorId() != null && complaint.getLabelId() != null) {
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
