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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String batchKey = new SimpleDateFormat("yyyyMMddHH").format(new Date());
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (ReminderRule rule : rules) {
            int pending = countPending(rule.getEventType());
            if (pending <= 0) continue;
            Permission permission = Permission.fromCode(rule.getPermissionCode());
            if (permission == null) continue;
            List<Integer> users = authorizationService.getUserIdsWithPermission(permission);
            for (Integer userId : users) {
                Long exists = logMapper.selectCount(new LambdaQueryWrapper<ReminderLog>()
                        .eq(ReminderLog::getRuleId, rule.getId())
                        .eq(ReminderLog::getUserId, userId)
                        .eq(ReminderLog::getBatchKey, batchKey));
                if (exists != null && exists > 0) continue;
                ReminderLog logRow = new ReminderLog();
                logRow.setRuleId(rule.getId());
                logRow.setTargetType("AGG");
                logRow.setTargetId(0);
                logRow.setUserId(userId);
                logRow.setBatchKey(batchKey);
                logRow.setSentAt(now);
                try {
                    logMapper.insert(logRow);
                    String title = rule.getTitleTemplate() != null && !rule.getTitleTemplate().isEmpty()
                            ? rule.getTitleTemplate().replace("{count}", String.valueOf(pending))
                            : "您有 " + pending + " 条待处理事项（" + rule.getRuleName() + "）";
                    notificationService.createNotification(userId, null, "reminder_pending", title, "reminder", rule.getId());
                } catch (Exception e) {
                    log.debug("reminder skipped: {}", e.getMessage());
                }
            }
        }
    }

    private int countPending(String eventType) {
        if (eventType == null) return 0;
        try {
            switch (eventType) {
                case "article_pending":
                    return safeCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getEnable, 0)));
                case "report_pending":
                    return safeCount(reportMapper.selectCount(new LambdaQueryWrapper<Report>().eq(Report::getStatus, "pending")));
                case "appeal_pending":
                    return safeCount(appealMapper.selectCount(new LambdaQueryWrapper<Appeal>().eq(Appeal::getStatus, "pending")));
                case "featured_pending":
                    return safeCount(featuredMapper.selectCount(new LambdaQueryWrapper<FeaturedRecommendation>().eq(FeaturedRecommendation::getStatus, "pending")));
                case "adopt_pending":
                    return safeCount(replyMapper.selectCount(new LambdaQueryWrapper<Reply>().eq(Reply::getAdoptStatus, 1)))
                            + safeCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getAdoptStatus, 1)));
                case "complaint_pending":
                    return safeCount(complaintMapper.selectCount(new LambdaQueryWrapper<ModeratorComplaint>().eq(ModeratorComplaint::getStatus, "pending")));
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeCount(Long value) { return value == null ? 0 : value.intValue(); }
}
