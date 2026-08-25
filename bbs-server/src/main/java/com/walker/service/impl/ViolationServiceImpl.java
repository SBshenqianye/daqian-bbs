package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.DictMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.UserService;
import com.walker.service.ViolationService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ViolationServiceImpl extends ServiceImpl<ViolationMapper, Violation> implements ViolationService {

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    // 违规类型 → 默认扣分映射
    private static final Map<String, Integer> VIOLATION_POINTS = new HashMap<>();
    static {
        VIOLATION_POINTS.put("illegal", 15);
        VIOLATION_POINTS.put("attack", 10);
        VIOLATION_POINTS.put("spam", 4);
        VIOLATION_POINTS.put("plagiarism", 12);
        VIOLATION_POINTS.put("false_report", 3);
        VIOLATION_POINTS.put("leak", 20);
    }

    @Override
    @Transactional
    public ResultBean addViolation(Integer userId, String violationType, String relatedType,
                                   Integer relatedId, Integer operatorId, String remark) {
        if (userId == null || violationType == null || operatorId == null) {
            return ResultBean.error("参数不完整");
        }

        Integer points = VIOLATION_POINTS.get(violationType);
        if (points == null) {
            return ResultBean.error("未知的违规类型: " + violationType);
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");

        // 记录违规
        Violation violation = new Violation();
        violation.setUserId(userId);
        violation.setViolationType(violationType);
        violation.setPointsDeducted(points);
        violation.setRelatedType(relatedType);
        violation.setRelatedId(relatedId);
        violation.setOperatorId(operatorId);
        violation.setRemark(remark);
        violation.setCreateTime(fmt.format(now));
        this.save(violation);

        // 扣分
        String reason = "违规扣分 - " + getViolationLabel(violationType);
        pointsLogService.adjustUserPoints(userId, -points, reason,
                "violation", violation.getId(), operatorId);

        // 通知用户
        notificationService.createNotification(userId, operatorId,
                "violation", "您有新的违规记录: " + getViolationLabel(violationType),
                "violation", violation.getId());

        // 检查月度累计扣分，超过20分自动限制发帖7天
        String monthStr = monthFmt.format(now);
        String monthStart = monthStr + "-01 00:00:00";
        String monthEnd = monthStr + "-32 00:00:00"; // 简化处理
        Integer monthlyTotal = violationMapper.sumMonthlyDeductions(userId, monthStart, monthEnd);

        if (monthlyTotal > 20) {
            User user = userService.getById(userId);
            if (user != null && (user.getPostRestricted() == null || user.getPostRestricted() == 0)) {
                // 计算7天后日期
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, 7);
                String until = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());

                user.setPostRestricted(1);
                user.setPostRestrictedUntil(until);
                userService.updateById(user);

                notificationService.createNotification(userId, operatorId,
                        "post_restricted", "因月度违规累计扣分超过20分，您已被限制发帖7天",
                        "user", userId);
            }
        }

        return ResultBean.success("违规记录已添加，扣" + points + "分");
    }

    @Override
    public ResultBean listViolations(Integer userId, Integer page, Integer size) {
        Page<Violation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Violation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Violation::getUserId, userId);
        }
        wrapper.orderByDesc(Violation::getCreateTime);
        Page<Violation> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyViolations(Integer userId, Integer page, Integer size) {
        return listViolations(userId, page, size);
    }

    private String getViolationLabel(String type) {
        switch (type) {
            case "illegal": return "违法违规内容";
            case "attack": return "人身攻击/争吵引战";
            case "spam": return "恶意灌水/刷屏";
            case "plagiarism": return "抄袭剽窃";
            case "false_report": return "虚假恶意举报";
            case "leak": return "泄露企业秘密";
            default: return type;
        }
    }
}
