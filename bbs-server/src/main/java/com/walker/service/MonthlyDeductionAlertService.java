package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.PointsAlertMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.PointsAlert;
import com.walker.utils.ConstantUtil;
import com.walker.utils.MapRowUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 当月累计扣分达到阈值时的站内提醒。
 *
 * <p>统计口径：bbs_violation 中 status=active 的记录（已取消违规不计入），按自然月累计。
 * 同一用户、同一月份、同一阈值只提醒一次。</p>
 */
@Service
public class MonthlyDeductionAlertService {

    private static final Logger log = LoggerFactory.getLogger(MonthlyDeductionAlertService.class);
    private static final String ALERT_TYPE = "MONTHLY_DEDUCTION";
    private static final int DEFAULT_THRESHOLD = 10;

    @Autowired
    private PointsAlertMapper pointsAlertMapper;

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private DictService dictService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 违规落库后即时检查。
     */
    public void checkAndNotify(Integer userId, Integer monthlyDeduction, String month) {
        if (userId == null || monthlyDeduction == null || monthlyDeduction <= 0) {
            return;
        }
        if (!"1".equals(configValue(ConstantUtil.MONTHLY_DEDUCTION_ALERT_ENABLED, "1"))) {
            return;
        }
        int threshold = intConfig(ConstantUtil.MONTHLY_DEDUCTION_ALERT_THRESHOLD, DEFAULT_THRESHOLD);
        if (monthlyDeduction < threshold) {
            return;
        }
        String period = month != null ? month : new SimpleDateFormat("yyyy-MM").format(new Date());
        Long exists = pointsAlertMapper.selectCount(new LambdaQueryWrapper<PointsAlert>()
                .eq(PointsAlert::getUserId, userId)
                .eq(PointsAlert::getAlertType, ALERT_TYPE)
                .eq(PointsAlert::getPeriod, period)
                .eq(PointsAlert::getThreshold, threshold));
        if (exists != null && exists > 0) {
            return;
        }
        PointsAlert alert = new PointsAlert();
        alert.setUserId(userId);
        alert.setAlertType(ALERT_TYPE);
        alert.setPeriod(period);
        alert.setThreshold(threshold);
        alert.setPoints(monthlyDeduction);
        alert.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        try {
            pointsAlertMapper.insert(alert);
            notificationService.createNotification(userId, null, "monthly_deduction_alert",
                    "您本月累计扣分已达 " + monthlyDeduction + " 分（提醒阈值 " + threshold
                            + " 分），请注意内容规范，可在“我的违规/积分明细”查看详情。",
                    "user", userId);
        } catch (Exception e) {
            // 并发下唯一键冲突视为已提醒，不阻断主流程
            log.debug("monthly deduction alert skipped: {}", e.getMessage());
        }
    }

    /**
     * 每日补扫：部署后已有用户可能已达标但尚未收到提醒。
     */
    @Scheduled(cron = "0 30 9 * * ?")
    public void scanCurrentMonth() {
        if (!"1".equals(configValue(ConstantUtil.MONTHLY_DEDUCTION_ALERT_ENABLED, "1"))) {
            return;
        }
        int threshold = intConfig(ConstantUtil.MONTHLY_DEDUCTION_ALERT_THRESHOLD, DEFAULT_THRESHOLD);
        String month = new SimpleDateFormat("yyyy-MM").format(new Date());
        String monthStart = month + "-01 00:00:00";
        String monthEnd = month + "-32 00:00:00";
        List<Map<String, Object>> rows = violationMapper.findUsersReachingMonthlyThreshold(
                monthStart, monthEnd, threshold);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Map<String, Object> row : rows) {
            // PG 会把别名 userId/points 折叠成小写，用不敏感取值
            Integer userId = toInt(MapRowUtil.get(row, "userId"));
            Integer points = toInt(MapRowUtil.get(row, "points"));
            if (userId != null && points != null) {
                checkAndNotify(userId, points, month);
            }
        }
    }

    private String configValue(String key, String fallback) {
        try {
            String value = dictService.getValueByKey(key);
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        } catch (Exception e) {
            return fallback;
        }
    }

    private int intConfig(String key, int fallback) {
        try {
            String value = dictService.getValueByKey(key);
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
