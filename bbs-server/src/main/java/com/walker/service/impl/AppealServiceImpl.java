package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.AppealMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.service.AppealService;
import com.walker.service.NotificationService;
import com.walker.service.UserService;
import com.walker.service.ViolationService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppealServiceImpl extends ServiceImpl<AppealMapper, Appeal> implements AppealService {

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ViolationService violationService;

    /**
     * 从登录态（JWT）获取当前操作人 id。/admin/** 已由 Spring Security 强制认证。
     * 审核人以"服务端可信身份"为准，不信任前端传入的 reviewerId（#2/#14 同一模式）。
     */
    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    @Override
    @Transactional
    public ResultBean submitAppeal(Integer userId, String appealType, Integer relatedId, String content) {
        if (userId == null || appealType == null || content == null || content.isEmpty()) {
            return ResultBean.error("参数不完整");
        }

        // 违规申诉必须关联违规记录
        if ("violation".equals(appealType) && relatedId == null) {
            return ResultBean.error("请选择要申诉的违规记录");
        }

        // 检查是否有待审核的申诉
        long count = this.count(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getUserId, userId)
                .eq(Appeal::getAppealType, appealType)
                .eq(relatedId != null, Appeal::getRelatedId, relatedId)
                .eq(Appeal::getStatus, "pending"));
        if (count > 0) {
            return ResultBean.error("您已有待审核的申诉，请等待处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setAppealType(appealType);
        appeal.setRelatedId(relatedId);
        appeal.setContent(content);
        appeal.setStatus("pending");
        appeal.setCreateTime(fmt.format(now));
        this.save(appeal);

        return ResultBean.success("申诉已提交，等待审核");
    }

    @Override
    @Transactional
    public ResultBean reviewAppeal(Integer appealId, Integer reviewerId, String status, String remark) {
        if (appealId == null || status == null) {
            return ResultBean.error("参数不完整");
        }
        // 审核人以登录态身份为准：前端传入的 reviewerId 必须与当前登录用户一致，防伪造
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        if (reviewerId != null && !currentUserId.equals(reviewerId)) {
            return ResultBean.error("审核人身份校验失败，请重新登录后操作");
        }
        // 后续落库与通知均以登录态 currentUserId 为准
        reviewerId = currentUserId;

        Appeal appeal = this.getById(appealId);
        if (appeal == null) {
            return ResultBean.error("申诉记录不存在");
        }
        if (!"pending".equals(appeal.getStatus())) {
            return ResultBean.error("该申诉已处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        appeal.setStatus(status);
        appeal.setReviewerId(reviewerId);
        appeal.setReviewTime(fmt.format(now));
        appeal.setReviewRemark(remark);
        this.updateById(appeal);

        // 通知申诉人
        String title = "accepted".equals(status) ? "您的申诉已通过" : "您的申诉已被驳回";
        notificationService.createNotification(appeal.getUserId(), reviewerId,
                "appeal_review", title, "appeal", appealId);

        // 口径②：违规申诉通过时自动取消关联违规——回滚该次扣分并恢复被隐藏/删除的内容可见性。
        if ("accepted".equals(status) && "violation".equals(appeal.getAppealType()) && appeal.getRelatedId() != null) {
            violationService.autoCancelByAppeal(appeal.getRelatedId(), reviewerId, remark);
        }

        return ResultBean.success("审核完成");
    }

    @Override
    public ResultBean listAppeals(String status, Integer page, Integer size) {
        Page<Appeal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }
        wrapper.orderByDesc(Appeal::getCreateTime);
        Page<Appeal> result = this.page(pageParam, wrapper);

        // 批量查询用户昵称
        Set<Integer> userIds = result.getRecords().stream()
                .map(Appeal::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listUsersWithOrgInfo(userIds);
            for (User u : users) {
                nicknameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
        }

        // 批量查询关联违规记录
        Set<Integer> violationIds = result.getRecords().stream()
                .filter(a -> "violation".equals(a.getAppealType()) && a.getRelatedId() != null)
                .map(Appeal::getRelatedId)
                .collect(Collectors.toSet());
        Map<Integer, Violation> violationMap = new HashMap<>();
        if (!violationIds.isEmpty()) {
            List<Violation> violations = violationService.listByIds(violationIds);
            for (Violation v : violations) {
                violationMap.put(v.getId(), v);
            }
        }

        // 违规类型中文映射
        Map<String, String> violationTypeLabel = new HashMap<>();
        violationTypeLabel.put("illegal", "违法违规内容");
        violationTypeLabel.put("attack", "人身攻击");
        violationTypeLabel.put("spam", "恶意灌水");
        violationTypeLabel.put("plagiarism", "抄袭剽窃");
        violationTypeLabel.put("false_report", "虚假举报");
        violationTypeLabel.put("leak", "泄露秘密");

        // 组装带昵称和违规信息的结果
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Appeal appeal : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", appeal.getId());
            map.put("userId", appeal.getUserId());
            map.put("nickname", nicknameMap.getOrDefault(appeal.getUserId(), "未知用户"));
            map.put("appealType", appeal.getAppealType());
            map.put("relatedId", appeal.getRelatedId());
            map.put("content", appeal.getContent());
            map.put("status", appeal.getStatus());
            map.put("createTime", appeal.getCreateTime());
            map.put("reviewRemark", appeal.getReviewRemark());
            map.put("reviewTime", appeal.getReviewTime());

            // 违规关联详情
            if ("violation".equals(appeal.getAppealType()) && appeal.getRelatedId() != null) {
                Violation v = violationMap.get(appeal.getRelatedId());
                if (v != null) {
                    Map<String, Object> violationInfo = new LinkedHashMap<>();
                    violationInfo.put("id", v.getId());
                    violationInfo.put("violationType", v.getViolationType());
                    violationInfo.put("violationLabel", violationTypeLabel.getOrDefault(v.getViolationType(), v.getViolationType()));
                    violationInfo.put("pointsDeducted", v.getPointsDeducted());
                    violationInfo.put("remark", v.getRemark());
                    violationInfo.put("relatedType", v.getRelatedType());
                    violationInfo.put("relatedId", v.getRelatedId());
                    map.put("violation", violationInfo);
                }
            }

            enriched.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", enriched);
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyAppeals(Integer userId, Integer page, Integer size) {
        Page<Appeal> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appeal::getUserId, userId);
        wrapper.orderByDesc(Appeal::getCreateTime);
        Page<Appeal> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }
}
