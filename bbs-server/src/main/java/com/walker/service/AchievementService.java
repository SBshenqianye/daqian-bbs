package com.walker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walker.mapper.AchievementMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.LoginLogMapper;
import com.walker.mapper.PointsLogMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ReportMapper;
import com.walker.mapper.UserAchievementMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.Achievement;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.PointsLog;
import com.walker.pojo.Reply;
import com.walker.pojo.User;
import com.walker.pojo.UserAchievement;
import com.walker.vo.ResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就配置、规则评估与用户成就。
 */
@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private AchievementMapper achievementMapper;
    @Autowired private UserAchievementMapper userAchievementMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ArticleMapper articleMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private ReplyMapper replyMapper;
    @Autowired private LoginLogMapper loginLogMapper;
    @Autowired private ReportMapper reportMapper;
    @Autowired private PointsLogMapper pointsLogMapper;
    @Autowired private PointsLogService pointsLogService;
    @Autowired private LevelService levelService;
    @Autowired private NotificationService notificationService;

    public ResultBean listAdmin() {
        List<Achievement> list = achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .orderByAsc(Achievement::getSortOrder).orderByAsc(Achievement::getId));
        return ResultBean.success("查询成功", list);
    }

    public ResultBean save(Achievement achievement) {
        if (achievement == null || achievement.getAchievementName() == null
                || achievement.getAchievementName().trim().isEmpty()) {
            return ResultBean.error("成就名称不能为空");
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (achievement.getAchievementCode() == null || achievement.getAchievementCode().trim().isEmpty()) {
            achievement.setAchievementCode("ACH" + System.currentTimeMillis());
        }
        if (achievement.getEnabled() == null) achievement.setEnabled(1);
        if (achievement.getRuleLogic() == null) achievement.setRuleLogic("ALL");
        if (achievement.getCategory() == null) achievement.setCategory("成长");
        if (achievement.getSortOrder() == null) achievement.setSortOrder(0);
        if (achievement.getIconSvg() != null && !achievement.getIconSvg().trim().isEmpty()) {
            String sanitized = sanitizeMedal(achievement.getIconSvg());
            if (sanitized == null) {
                return ResultBean.error("勋章 SVG 含不安全内容或格式不支持");
            }
            achievement.setIconSvg(sanitized);
        }
        if (achievement.getId() == null) {
            achievement.setCreateTime(now);
            achievement.setUpdateTime(now);
            achievementMapper.insert(achievement);
        } else {
            achievement.setUpdateTime(now);
            achievementMapper.updateById(achievement);
        }
        return ResultBean.success("保存成功");
    }

    public ResultBean delete(Integer id) {
        if (id == null) return ResultBean.error("缺少成就ID");
        achievementMapper.deleteById(id);
        userAchievementMapper.delete(new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getAchievementId, id));
        return ResultBean.success("删除成功");
    }

    public String medalTemplate() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 256 256\">\n"
                + "  <path d=\"M44 36 H212 V214 L128 170 L44 214 Z\" fill=\"#1f6f5c\" stroke=\"#ffffff\" stroke-width=\"6\" stroke-linejoin=\"round\"/>\n"
                + "  <path d=\"M60 52 H196 V196 L128 160 L60 196 Z\" fill=\"none\" stroke=\"#ffffff\" stroke-opacity=\".55\" stroke-width=\"2\"/>\n"
                + "  <!-- 中心图形安全区：x=80..176, y=72..152 -->\n"
                + "  <circle cx=\"128\" cy=\"112\" r=\"28\" fill=\"none\" stroke=\"#ffffff\" stroke-width=\"4\"/>\n"
                + "</svg>";
    }

    public ResultBean listUserAchievements(Integer userId) {
        if (userId == null) return ResultBean.error("用户ID不能为空");
        evaluateUser(userId);
        List<Achievement> all = achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getEnabled, 1)
                .orderByAsc(Achievement::getSortOrder).orderByAsc(Achievement::getId));
        List<UserAchievement> owned = userAchievementMapper.selectList(new LambdaQueryWrapper<UserAchievement>()
                .eq(UserAchievement::getUserId, userId));
        Map<Integer, UserAchievement> ownedMap = new HashMap<>();
        for (UserAchievement ua : owned) ownedMap.put(ua.getAchievementId(), ua);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Achievement a : all) {
            UserAchievement ua = ownedMap.get(a.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("code", a.getAchievementCode());
            row.put("name", a.getAchievementName());
            row.put("description", a.getDescription());
            row.put("iconSvg", a.getIconSvg());
            row.put("category", a.getCategory());
            row.put("unlocked", ua != null && Integer.valueOf(1).equals(ua.getUnlocked()) ? 1 : 0);
            row.put("unlockedAt", ua == null ? null : ua.getUnlockedAt());
            result.add(row);
        }
        return ResultBean.success("查询成功", result);
    }

    public void evaluateUser(Integer userId) {
        if (userId == null) return;
        Map<String, Integer> metrics = buildMetrics(userId);
        List<Achievement> achievements = achievementMapper.selectList(new LambdaQueryWrapper<Achievement>()
                .eq(Achievement::getEnabled, 1));
        for (Achievement achievement : achievements) {
            if (!matches(achievement, metrics)) continue;
            UserAchievement existing = userAchievementMapper.selectOne(new LambdaQueryWrapper<UserAchievement>()
                    .eq(UserAchievement::getUserId, userId)
                    .eq(UserAchievement::getAchievementId, achievement.getId())
                    .last("LIMIT 1"));
            if (existing != null && Integer.valueOf(1).equals(existing.getUnlocked())) continue;
            if (existing == null) {
                existing = new UserAchievement();
                existing.setUserId(userId);
                existing.setAchievementId(achievement.getId());
                existing.setProgressJson(toJson(metrics));
                existing.setUnlocked(1);
                existing.setUnlockedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                existing.setSource("rule");
                try {
                    userAchievementMapper.insert(existing);
                    notificationService.createNotification(userId, null, "achievement_unlocked",
                            "恭喜解锁成就「" + achievement.getAchievementName() + "」",
                            "achievement", achievement.getId());
                } catch (Exception e) {
                    log.debug("achievement insert skipped: {}", e.getMessage());
                }
            } else {
                existing.setUnlocked(1);
                existing.setUnlockedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                existing.setProgressJson(toJson(metrics));
                userAchievementMapper.updateById(existing);
            }
        }
    }

    @Scheduled(cron = "0 20 2 * * ?")
    public void evaluateAllUsers() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId).eq(User::getIsDelete, 0));
        for (User user : users) {
            try {
                evaluateUser(user.getId());
            } catch (Exception e) {
                log.warn("evaluate achievement failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    public String sanitizeMedal(String svg) {
        if (svg == null) return null;
        String trimmed = svg.trim();
        if (trimmed.startsWith("data:image/png;base64,")) {
            return trimmed.length() <= 200000 ? trimmed : null;
        }
        String lower = trimmed.toLowerCase();
        if (!lower.contains("<svg") || trimmed.length() > 200000) return null;
        if (lower.contains("<script") || lower.contains("<foreignobject")
                || lower.contains("javascript:") || lower.contains("onload=")
                || lower.contains("onerror=") || lower.contains("onclick=")
                || lower.contains("xlink:href=\"http") || lower.contains("href=\"http")) {
            return null;
        }
        return trimmed;
    }

    private boolean matches(Achievement achievement, Map<String, Integer> metrics) {
        String json = achievement.getRuleJson();
        if (json == null || json.trim().isEmpty()) return false;
        try {
            Map<String, Object> rule = objectMapper.readValue(json, Map.class);
            return evalRule(rule, metrics);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evalRule(Map<String, Object> rule, Map<String, Integer> metrics) {
        if (rule == null) return false;
        String logic = String.valueOf(rule.getOrDefault("logic", "ALL")).toUpperCase();
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        List<Map<String, Object>> groups = (List<Map<String, Object>>) rule.get("groups");
        List<Boolean> results = new ArrayList<>();
        if (conditions != null) {
            for (Map<String, Object> c : conditions) results.add(evalCondition(c, metrics));
        }
        if (groups != null) {
            for (Map<String, Object> g : groups) results.add(evalRule(g, metrics));
        }
        if (results.isEmpty()) return false;
        if ("ANY".equals(logic)) {
            return results.stream().anyMatch(Boolean::booleanValue);
        }
        return results.stream().allMatch(Boolean::booleanValue);
    }

    private boolean evalCondition(Map<String, Object> condition, Map<String, Integer> metrics) {
        String metric = String.valueOf(condition.get("metric"));
        String op = String.valueOf(condition.getOrDefault("op", ">="));
        int target = toInt(condition.get("value"));
        int actual = metrics.getOrDefault(metric, 0);
        switch (op) {
            case ">": return actual > target;
            case "=": return actual == target;
            case "<=": return actual <= target;
            case "<": return actual < target;
            case "!=": return actual != target;
            default: return actual >= target;
        }
    }

    private Map<String, Integer> buildMetrics(Integer userId) {
        Map<String, Integer> m = new HashMap<>();
        m.put("article_count", safeCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId).eq(Article::getEnable, 1))));
        m.put("comment_count", safeCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getCommentUserId, userId).eq(Comment::getEnable, 1))));
        m.put("reply_count", safeCount(replyMapper.selectCount(new LambdaQueryWrapper<Reply>()
                .eq(Reply::getReplyUserId, userId).eq(Reply::getEnable, 1))));
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getUserId, userId).eq(Article::getEnable, 1));
        int likes = 0, favorites = 0, views = 0, featured = 0;
        for (Article a : articles) {
            likes += a.getArticleGoodNum() == null ? 0 : a.getArticleGoodNum();
            favorites += a.getFavoriteNum() == null ? 0 : a.getFavoriteNum();
            views += a.getArticleViewNum() == null ? 0 : a.getArticleViewNum();
            if (Integer.valueOf(1).equals(a.getIsFeatured())) featured++;
        }
        m.put("like_received", likes);
        m.put("favorite_received", favorites);
        m.put("view_count", views);
        m.put("featured_count", featured);
        int points = 0;
        try { Integer p = pointsLogService.getPointsAdjustment(userId); points = p == null ? 0 : p; } catch (Exception ignore) {}
        m.put("points_total", points);
        m.put("login_days", safeCount(loginLogMapper.selectCount(new LambdaQueryWrapper<com.walker.pojo.LoginLog>()
                .eq(com.walker.pojo.LoginLog::getUserId, userId))));
        Object level = levelService.getUserLevel(userId).get("levelIndex");
        m.put("level_index", level instanceof Number ? ((Number) level).intValue() : 0);
        int adopted = safeCount(replyMapper.selectCount(new LambdaQueryWrapper<Reply>()
                .eq(Reply::getReplyUserId, userId).eq(Reply::getIsAdopted, 1)))
                + safeCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getCommentUserId, userId).eq(Comment::getAdoptStatus, 2)));
        m.put("adopted_count", adopted);
        m.put("report_confirmed_count", safeCount(reportMapper.selectCount(new LambdaQueryWrapper<com.walker.pojo.Report>()
                .eq(com.walker.pojo.Report::getReporterId, userId)
                .eq(com.walker.pojo.Report::getStatus, "confirmed"))));
        int suggestion = safeCount(pointsLogMapper.selectCount(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, userId)
                .eq(PointsLog::getReason, "建议被采纳积分")));
        m.put("suggestion_adopted_count", suggestion);
        return m;
    }

    private int safeCount(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (Exception e) { return null; }
    }
}
