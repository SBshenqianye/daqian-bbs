package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.config.security.Permission;
import com.walker.mapper.SensitiveHitMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.SensitiveHit;
import com.walker.pojo.User;
import com.walker.service.AuthorizationService;
import com.walker.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 高风险用户每日聚合提醒。
 *
 * <p>目标：让版主把精力集中在"反复命中敏感词的用户"上，而不是逐条内容。
 * 每天扫描一次近 24 小时的命中记录，命中的用户按板块找版主；板块没有版主时按权限上推给管理员。</p>
 */
@Service
public class SensitiveRiskReminderJob {

    private static final Logger log = LoggerFactory.getLogger(SensitiveRiskReminderJob.class);

    /** 近 24 小时命中内容条数达到该值才提醒 */
    private static final int THRESHOLD = 3;

    @Autowired
    private SensitiveHitMapper hitMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private NotificationService notificationService;

    /** 每天 09:00 扫描一次 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scan() {
        try {
            runOnce();
        } catch (Exception e) {
            log.warn("sensitive risk reminder failed: {}", e.getMessage());
        }
    }

    /** 供手动触发/测试调用 */
    public int runOnce() {
        String since = hoursAgo(24);
        List<SensitiveHit> recent = hitMapper.selectList(new LambdaQueryWrapper<SensitiveHit>()
                .select(SensitiveHit::getUserId, SensitiveHit::getCreateTime, SensitiveHit::getLabelId)
                .ge(SensitiveHit::getCreateTime, since));
        if (recent == null || recent.isEmpty()) {
            return 0;
        }
        // 内存聚合，避免 SQL 别名在 PostgreSQL 下被小写化
        Map<Integer, Integer> countByUser = new LinkedHashMap<>();
        for (SensitiveHit hit : recent) {
            if (hit.getUserId() != null) {
                countByUser.merge(hit.getUserId(), 1, Integer::sum);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : countByUser.entrySet()) {
            if (entry.getValue() >= THRESHOLD) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", entry.getKey());
                row.put("cnt", entry.getValue());
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return 0;
        }

        List<Integer> reviewers = authorizationService.getUserIdsWithPermission(Permission.ARTICLE_REVIEW);
        Map<Integer, User> users = loadUsers(rows);
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        int sent = 0;
        for (Map<String, Object> row : rows) {
            Integer userId = ((Number) row.get("userId")).intValue();
            int count = ((Number) row.get("cnt")).intValue();
            Set<Integer> labels = recentLabels(userId, since);
            List<Integer> receivers = pickReceivers(reviewers, labels);

            User author = users.get(userId);
            String nickname = displayName(author, userId);
            for (Integer receiver : receivers) {
                if (receiver == null || receiver.equals(userId)) {
                    continue;
                }
                if (alreadyNotified(receiver, userId, today)) {
                    continue;
                }
                notificationService.createNotification(receiver, userId, "sensitive_user",
                        "用户「" + nickname + "」近 24 小时有 " + count + " 条内容命中敏感词，建议重点关注",
                        "sensitive_user", userId);
                sent++;
            }
        }
        return sent;
    }

    /** 有权限审核该用户内容的人；板块无人任职时上推给全部审核人员 */
    private List<Integer> pickReceivers(List<Integer> reviewers, Set<Integer> labels) {
        List<Integer> matched = new ArrayList<>();
        for (Integer reviewer : reviewers) {
            for (Integer label : labels) {
                if (label != null && authorizationService.canModerateBoard(reviewer, label)) {
                    matched.add(reviewer);
                    break;
                }
            }
        }
        if (!matched.isEmpty()) {
            return matched;
        }
        // 无版主/无法解析板块：按权限上推
        return new ArrayList<>(new LinkedHashSet<>(reviewers));
    }

    private Set<Integer> recentLabels(Integer userId, String since) {
        Set<Integer> labels = new HashSet<>();
        List<SensitiveHit> hits = hitMapper.selectList(new LambdaQueryWrapper<SensitiveHit>()
                .eq(SensitiveHit::getUserId, userId)
                .ge(SensitiveHit::getCreateTime, since)
                .select(SensitiveHit::getLabelId));
        if (hits != null) {
            for (SensitiveHit hit : hits) {
                if (hit.getLabelId() != null) {
                    labels.add(hit.getLabelId());
                }
            }
        }
        return labels;
    }

    private boolean alreadyNotified(Integer receiverId, Integer riskUserId, String today) {
        Long exists = null;
        try {
            exists = notificationCount(receiverId, riskUserId, today);
        } catch (Exception e) {
            return false;
        }
        return exists != null && exists > 0;
    }

    /** 同日同用户只提醒一次（避免重启补跑重复轰炸） */
    private Long notificationCount(Integer receiverId, Integer riskUserId, String today) {
        return (long) notificationService.count(new LambdaQueryWrapper<com.walker.pojo.Notification>()
                .eq(com.walker.pojo.Notification::getUserId, receiverId)
                .eq(com.walker.pojo.Notification::getType, "sensitive_user")
                .eq(com.walker.pojo.Notification::getRelatedId, riskUserId)
                .ge(com.walker.pojo.Notification::getCreateTime, today + " 00:00:00"));
    }

    private Map<Integer, User> loadUsers(List<Map<String, Object>> rows) {
        Map<Integer, User> map = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get("userId");
            if (value != null) {
                ids.add(((Number) value).intValue());
            }
        }
        if (ids.isEmpty()) {
            return map;
        }
        List<User> users = userMapper.selectBatchIds(ids);
        if (users != null) {
            for (User user : users) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private String displayName(User user, Integer userId) {
        if (user == null) {
            return "用户" + userId;
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        return user.getUsername() == null ? ("用户" + userId) : user.getUsername();
    }

    private String hoursAgo(int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -hours);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
    }
}
