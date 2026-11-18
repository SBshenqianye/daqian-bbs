package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.config.security.Permission;
import com.walker.mapper.AppealMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.mapper.ReminderLogMapper;
import com.walker.mapper.ReminderRuleMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ReportMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.ReminderLog;
import com.walker.pojo.ReminderRule;
import com.walker.pojo.Reply;
import com.walker.pojo.Report;
import com.walker.vo.ResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 待办提醒：按规则扫描待处理事项，向有权限的用户发送聚合站内提醒。
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    @Autowired private ReminderRuleMapper ruleMapper;
    @Autowired private ReminderLogMapper logMapper;
    @Autowired private ArticleMapper articleMapper;
    @Autowired private ReportMapper reportMapper;
    @Autowired private AppealMapper appealMapper;
    @Autowired private FeaturedRecommendationMapper featuredMapper;
    @Autowired private ReplyMapper replyMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private ModeratorComplaintMapper complaintMapper;
    @Autowired private AuthorizationService authorizationService;
    @Autowired private NotificationService notificationService;
    @Autowired private ContentBoardResolver contentBoardResolver;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter HOUR_KEY = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final DateTimeFormatter DAY_START = DateTimeFormatter.ofPattern("yyyy-MM-dd 00:00:00");
    private static final DateTimeFormatter TIME_HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public ResultBean listRules() {
        return ResultBean.success("查询成功", ruleMapper.selectList(new LambdaQueryWrapper<ReminderRule>()
                .orderByAsc(ReminderRule::getSortOrder).orderByAsc(ReminderRule::getId)));
    }

    /** 权限点枚举（供提醒配置下拉选择，避免手填出错） */
    public ResultBean listPermissions() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Permission p : Permission.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", p.getCode());
            row.put("label", PERMISSION_LABELS.getOrDefault(p.getCode(), prettify(p.getCode())));
            row.put("adminDefault", p.isGrantedTo(com.walker.config.security.AccountRole.ADMIN));
            list.add(row);
        }
        return ResultBean.success("查询成功", list);
    }

    private static final Map<String, String> PERMISSION_LABELS = new java.util.HashMap<>();

    /** 未登记中文名时，至少给出可读的兜底文案，避免整列显示英文 code */
    private static String prettify(String code) {
        if (code == null) return "";
        return code.replace(':', ' ').replace('_', ' ').trim();
    }
    static {
        PERMISSION_LABELS.put("admin:access", "管理端访问");
        PERMISSION_LABELS.put("user:read", "用户查看");
        PERMISSION_LABELS.put("user:create", "用户新增");
        PERMISSION_LABELS.put("user:update", "用户编辑");
        PERMISSION_LABELS.put("user:delete", "用户删除");
        PERMISSION_LABELS.put("user:grant:admin", "授予管理员");
        PERMISSION_LABELS.put("config:read", "配置查看");
        PERMISSION_LABELS.put("config:write", "配置修改");
        PERMISSION_LABELS.put("permission:manage", "权限管理");
        PERMISSION_LABELS.put("audit:read", "审计日志");
        PERMISSION_LABELS.put("article:read", "帖子查看");
        PERMISSION_LABELS.put("article:review", "帖子审核");
        PERMISSION_LABELS.put("article:delete", "帖子删除");
        PERMISSION_LABELS.put("article:feature", "设置精华");
        PERMISSION_LABELS.put("article:suggestion:adopt", "建议采纳");
        PERMISSION_LABELS.put("report:review", "举报审核");
        PERMISSION_LABELS.put("appeal:review", "申诉审核");
        PERMISSION_LABELS.put("violation:review", "违规处理");
        PERMISSION_LABELS.put("violation:add", "新增违规");
        PERMISSION_LABELS.put("violation:cancel", "撤销违规");
        PERMISSION_LABELS.put("moderator:manage", "版主任免");
        PERMISSION_LABELS.put("moderator:complaint:review", "版主投诉处理");
        PERMISSION_LABELS.put("adopt:review", "采纳审批");
        PERMISSION_LABELS.put("featured:recommend", "精华推荐");
        PERMISSION_LABELS.put("featured:review", "精华终审");
        PERMISSION_LABELS.put("points:read", "积分查看");
        PERMISSION_LABELS.put("points:adjust", "积分调整");
        PERMISSION_LABELS.put("dashboard:read", "看板查看");
        PERMISSION_LABELS.put("post:create", "发帖");
        PERMISSION_LABELS.put("post:update:own", "编辑自己的帖子");
        PERMISSION_LABELS.put("post:delete:own", "删除自己的帖子");
        PERMISSION_LABELS.put("comment:create", "发表评论");
        PERMISSION_LABELS.put("comment:delete:own", "删除自己的评论");
        PERMISSION_LABELS.put("reply:create", "发表回复");
        PERMISSION_LABELS.put("reply:delete:own", "删除自己的回复");
        PERMISSION_LABELS.put("report:submit", "提交举报");
        PERMISSION_LABELS.put("appeal:submit", "提交申诉");
        PERMISSION_LABELS.put("moderator:complain", "投诉版主");
        PERMISSION_LABELS.put("upload:file", "上传附件");
        PERMISSION_LABELS.put("profile:update", "修改个人资料");
        PERMISSION_LABELS.put("password:update", "修改密码");
    }

    public ResultBean saveRule(ReminderRule rule) {
        if (rule == null || rule.getRuleName() == null || rule.getRuleName().trim().isEmpty()) {
            return ResultBean.error("规则名称不能为空");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (rule.getRuleCode() == null || rule.getRuleCode().trim().isEmpty()) {
            rule.setRuleCode("RULE" + System.currentTimeMillis());
        }
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getScopeType() == null) rule.setScopeType("ALL");
        if (rule.getDelayMinutes() == null) rule.setDelayMinutes(10);
        if (rule.getRepeatMinutes() == null) rule.setRepeatMinutes(60);
        if (rule.getMaxPerDay() == null) rule.setMaxPerDay(8);
        if (rule.getEscalateMinutes() == null) rule.setEscalateMinutes(0);
        if (rule.getSortOrder() == null) rule.setSortOrder(0);
        if (rule.getId() == null) {
            rule.setCreateTime(now); rule.setUpdateTime(now); ruleMapper.insert(rule);
        } else {
            rule.setUpdateTime(now); ruleMapper.updateById(rule);
        }
        return ResultBean.success("保存成功");
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void scan() {
        List<ReminderRule> rules;
        try {
            rules = ruleMapper.selectList(new LambdaQueryWrapper<ReminderRule>().eq(ReminderRule::getEnabled, 1));
        } catch (Exception e) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String nowText = DATE_TIME.format(now);
        String batchKey = "R" + HOUR_KEY.format(now);
        for (ReminderRule rule : rules) {
            try {
                scanRule(rule, now, nowText, batchKey);
            } catch (Exception e) {
                log.warn("reminder scan failed for rule {}: {}", rule.getRuleCode(), e.getMessage());
            }
        }
    }

    private void scanRule(ReminderRule rule, LocalDateTime now, String nowText, String batchKey) {
        if (inQuietHours(rule.getQuietStart(), rule.getQuietEnd(), now.toLocalTime())) {
            return;
        }
        List<PendingItem> pending = loadPending(rule.getEventType());
        if (pending.isEmpty()) return;
        int delay = rule.getDelayMinutes() == null ? 0 : Math.max(0, rule.getDelayMinutes());
        List<PendingItem> matured = new ArrayList<>();
        for (PendingItem item : pending) {
            if (isMatured(item.getCreateTime(), delay, now)) matured.add(item);
        }
        if (matured.isEmpty()) return;

        Permission permission = Permission.fromCode(rule.getPermissionCode());
        if (permission == null) return;

        int repeatMinutes = rule.getRepeatMinutes() == null ? 60 : Math.max(1, rule.getRepeatMinutes());
        int maxPerDay = rule.getMaxPerDay() == null ? 8 : Math.max(1, rule.getMaxPerDay());
        Set<Integer> notified = new HashSet<>();
        for (Integer userId : authorizationService.getUserIdsWithPermission(permission)) {
            List<PendingItem> visible = visibleItems(rule, userId, matured);
            if (visible.isEmpty()) continue;
            if (!canSendNow(rule.getId(), userId, repeatMinutes, maxPerDay, now, true)) continue;
            if (send(rule, userId, visible.size(), batchKey, nowText, null)) {
                notified.add(userId);
            }
        }

        // 升级提醒：待办超过阈值仍未处理时，发给更高权限人员（同一小时去重）
        if (rule.getEscalateMinutes() != null && rule.getEscalateMinutes() > 0
                && rule.getEscalatePermissionCode() != null && !rule.getEscalatePermissionCode().trim().isEmpty()) {
            LocalDateTime oldest = oldestCreateTime(matured);
            if (oldest == null || oldest.plusMinutes(rule.getEscalateMinutes()).isAfter(now)) return;
            Permission escalatePermission = Permission.fromCode(rule.getEscalatePermissionCode().trim());
            if (escalatePermission == null) return;
            String escalateKey = "E" + HOUR_KEY.format(now);
            for (Integer userId : authorizationService.getUserIdsWithPermission(escalatePermission)) {
                if (notified.contains(userId)) continue;
                List<PendingItem> visible = visibleItems(rule, userId, matured);
                if (visible.isEmpty()) continue;
                // 升级提醒不受普通提醒的重复间隔影响，只受每日上限约束
                if (!canSendNow(rule.getId(), userId, repeatMinutes, maxPerDay, now, false)) continue;
                send(rule, userId, visible.size(), escalateKey, nowText, "【升级】");
            }
        }
    }

    /** 发送一条聚合提醒；同规则/同用户/同批次已发送时返回 false。 */
    private boolean send(ReminderRule rule, Integer userId, int count, String batchKey,
                         String nowText, String titlePrefix) {
        Long exists = logMapper.selectCount(new LambdaQueryWrapper<ReminderLog>()
                .eq(ReminderLog::getRuleId, rule.getId())
                .eq(ReminderLog::getUserId, userId)
                .eq(ReminderLog::getBatchKey, batchKey));
        if (exists != null && exists > 0) return false;
        ReminderLog logRow = new ReminderLog();
        logRow.setRuleId(rule.getId());
        logRow.setTargetType("AGG");
        logRow.setTargetId(0);
        logRow.setUserId(userId);
        logRow.setBatchKey(batchKey);
        logRow.setSentAt(nowText);
        try {
            logMapper.insert(logRow);
            String title = rule.getTitleTemplate() != null && !rule.getTitleTemplate().isEmpty()
                    ? rule.getTitleTemplate().replace("{count}", String.valueOf(count))
                    : "您有 " + count + " 条待处理事项（" + rule.getRuleName() + "）";
            if (titlePrefix != null) title = titlePrefix + title;
            notificationService.createNotification(userId, null, "reminder_pending", title, "reminder", rule.getId());
            return true;
        } catch (Exception e) {
            log.debug("reminder skipped: {}", e.getMessage());
            return false;
        }
    }

    /** 规则范围：BOARD 只提醒能管理该板块的人；无法解析板块的待办不发给版主。 */
    private List<PendingItem> visibleItems(ReminderRule rule, Integer userId, List<PendingItem> items) {
        if (!"BOARD".equalsIgnoreCase(rule.getScopeType())) return items;
        List<PendingItem> result = new ArrayList<>();
        for (PendingItem item : items) {
            if (item.getLabelId() != null && authorizationService.canModerateBoard(userId, item.getLabelId())) {
                result.add(item);
            }
        }
        return result;
    }

    /** 重复间隔与每日上限都满足时才发送。 */
    private boolean canSendNow(Integer ruleId, Integer userId, int repeatMinutes, int maxPerDay,
                               LocalDateTime now, boolean checkRepeat) {
        String dayStart = DAY_START.format(now);
        Long today = logMapper.selectCount(new LambdaQueryWrapper<ReminderLog>()
                .eq(ReminderLog::getRuleId, ruleId)
                .eq(ReminderLog::getUserId, userId)
                .eq(ReminderLog::getTargetType, "AGG")
                .ge(ReminderLog::getSentAt, dayStart));
        if (today != null && today >= maxPerDay) return false;
        if (!checkRepeat) return true;
        ReminderLog last = logMapper.selectOne(new LambdaQueryWrapper<ReminderLog>()
                .eq(ReminderLog::getRuleId, ruleId)
                .eq(ReminderLog::getUserId, userId)
                .eq(ReminderLog::getTargetType, "AGG")
                .orderByDesc(ReminderLog::getSentAt)
                .last("LIMIT 1"));
        if (last == null || last.getSentAt() == null) return true;
        LocalDateTime lastTime = parseDateTime(last.getSentAt());
        return lastTime == null || !lastTime.plusMinutes(repeatMinutes).isAfter(now);
    }

    static boolean inQuietHours(String start, String end, LocalTime now) {
        if (start == null || end == null || start.trim().isEmpty() || end.trim().isEmpty()) return false;
        try {
            LocalTime s = LocalTime.parse(start.trim(), TIME_HHMM);
            LocalTime e = LocalTime.parse(end.trim(), TIME_HHMM);
            if (s.equals(e)) return false;
            if (s.isBefore(e)) return !now.isBefore(s) && now.isBefore(e);
            // 跨午夜，例如 22:00 - 08:00
            return !now.isBefore(s) || now.isBefore(e);
        } catch (Exception ex) {
            return false;
        }
    }

    static boolean isMatured(String createTime, int delayMinutes, LocalDateTime now) {
        LocalDateTime created = parseDateTime(createTime);
        if (created == null) return true;
        return !created.plusMinutes(delayMinutes).isAfter(now);
    }

    private static LocalDateTime oldestCreateTime(List<PendingItem> items) {
        LocalDateTime oldest = null;
        for (PendingItem item : items) {
            LocalDateTime created = parseDateTime(item.getCreateTime());
            if (created != null && (oldest == null || created.isBefore(oldest))) oldest = created;
        }
        return oldest;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private List<PendingItem> loadPending(String eventType) {
        List<PendingItem> result = new ArrayList<>();
        if (eventType == null) return result;
        switch (eventType) {
            case "article_pending":
                for (Article a : articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getEnable, 0).orderByAsc(Article::getCreateTime).last("LIMIT 500"))) {
                    result.add(new PendingItem(a.getArticleId(), a.getArticleLabelId(), a.getCreateTime()));
                }
                break;
            case "report_pending":
                for (Report r : reportMapper.selectList(new LambdaQueryWrapper<Report>()
                        .eq(Report::getStatus, "pending").orderByAsc(Report::getCreateTime).last("LIMIT 500"))) {
                    Integer labelId = contentBoardResolver.resolveLabelId(r.getTargetType(), r.getTargetId());
                    result.add(new PendingItem(r.getId(), labelId, r.getCreateTime()));
                }
                break;
            case "appeal_pending":
                for (Appeal a : appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
                        .eq(Appeal::getStatus, "pending").orderByAsc(Appeal::getCreateTime).last("LIMIT 500"))) {
                    result.add(new PendingItem(a.getId(), null, a.getCreateTime()));
                }
                break;
            case "featured_pending":
                for (FeaturedRecommendation f : featuredMapper.selectList(
                        new LambdaQueryWrapper<FeaturedRecommendation>()
                                .eq(FeaturedRecommendation::getStatus, "pending")
                                .orderByAsc(FeaturedRecommendation::getCreateTime).last("LIMIT 500"))) {
                    result.add(new PendingItem(f.getId(), f.getLabelId(), f.getCreateTime()));
                }
                break;
            case "adopt_pending":
                for (Reply r : replyMapper.selectList(new LambdaQueryWrapper<Reply>()
                        .eq(Reply::getAdoptStatus, 1).orderByAsc(Reply::getReplyTime).last("LIMIT 500"))) {
                    Integer labelId = contentBoardResolver.resolveLabelId("reply", r.getReplyId());
                    result.add(new PendingItem(r.getReplyId(), labelId, r.getReplyTime()));
                }
                for (Comment c : commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getAdoptStatus, 1).orderByAsc(Comment::getCommentTime).last("LIMIT 500"))) {
                    Integer labelId = contentBoardResolver.resolveLabelId("comment", c.getCommentId());
                    result.add(new PendingItem(c.getCommentId(), labelId, c.getCommentTime()));
                }
                break;
            case "complaint_pending":
                for (ModeratorComplaint c : complaintMapper.selectList(
                        new LambdaQueryWrapper<ModeratorComplaint>()
                                .eq(ModeratorComplaint::getStatus, "pending")
                                .orderByAsc(ModeratorComplaint::getCreateTime).last("LIMIT 500"))) {
                    result.add(new PendingItem(c.getId(), null, c.getCreateTime()));
                }
                break;
            default:
                break;
        }
        return result;
    }

    private static final class PendingItem {
        private final Integer targetId;
        private final Integer labelId;
        private final String createTime;

        private PendingItem(Integer targetId, Integer labelId, String createTime) {
            this.targetId = targetId;
            this.labelId = labelId;
            this.createTime = createTime;
        }

        private Integer getTargetId() { return targetId; }
        private Integer getLabelId() { return labelId; }
        private String getCreateTime() { return createTime; }
    }
}
