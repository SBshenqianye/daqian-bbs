package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.DictMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.Article;
import com.walker.pojo.PointsLog;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.pojo.Appeal;
import com.walker.service.CommentService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.ReplyService;
import com.walker.service.UserService;
import com.walker.service.ViolationService;
import com.walker.service.AppealService;
import com.walker.utils.ConstantUtil;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private AppealService appealService;

    // 违规类型 → 默认扣分映射（兜底，字典表无数据时使用）
    private static final Map<String, Integer> DEFAULT_VIOLATION_POINTS = new HashMap<>();
    static {
        DEFAULT_VIOLATION_POINTS.put("illegal", 15);
        DEFAULT_VIOLATION_POINTS.put("attack", 10);
        DEFAULT_VIOLATION_POINTS.put("spam", 4);
        DEFAULT_VIOLATION_POINTS.put("plagiarism", 12);
        DEFAULT_VIOLATION_POINTS.put("false_report", 3);
        DEFAULT_VIOLATION_POINTS.put("leak", 20);
    }

    /**
     * 从数据字典获取违规扣分（dict_key=类型标识, dict_value=扣分值）
     */
    private int getViolationPoints(String violationType) {
        try {
            String val = dictMapper.selectValueByKey(violationType);
            if (val != null) return Integer.parseInt(val);
        } catch (Exception e) { /* fallback to default */ }
        Integer fallback = DEFAULT_VIOLATION_POINTS.get(violationType);
        return fallback != null ? fallback : 0;
    }

    @Override
    @Transactional
    public ResultBean addViolation(Integer userId, String violationType, String relatedType,
                                   Integer relatedId, Integer operatorId, String remark) {
        if (userId == null || violationType == null || operatorId == null) {
            return ResultBean.error("参数不完整");
        }
        // 操作人以登录态身份为准：前端传入的 operatorId 必须与当前登录用户一致，防伪造
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }
        if (!currentUserId.equals(operatorId)) {
            return ResultBean.error("操作人身份校验失败，请重新登录后操作");
        }
        // 后续落库、扣分与通知均以登录态 currentUserId 为准
        operatorId = currentUserId;

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");

        // ── 1. 计算扣分 ──
        int points;
        if ("plagiarism".equals(violationType) && "article".equals(relatedType) && relatedId != null) {
            // 抄袭：撤销原帖全部所得积分（替代固定-12分）
            points = calculateArticleEarnedPoints(relatedId);
        } else {
            points = getViolationPoints(violationType);
            if (points <= 0) {
                return ResultBean.error("未知的违规类型: " + violationType);
            }
        }

        // ── 2. 记录违规 ──
        Violation violation = new Violation();
        violation.setUserId(userId);
        violation.setViolationType(violationType);
        violation.setPointsDeducted(points);
        violation.setRelatedType(relatedType);
        violation.setRelatedId(relatedId);
        violation.setOperatorId(operatorId);
        violation.setRemark(remark);
        violation.setStatus("active");
        violation.setCreateTime(fmt.format(now));
        this.save(violation);

        // ── 3. 删除关联内容（在扣分之前，避免重复扣分） ──
        if (relatedId != null && relatedType != null) {
            deleteRelatedContent(relatedType, relatedId, violationType);
        }

        // ── 4. 扣分 ──
        String reason = "违规扣分 - " + getViolationLabel(violationType);
        pointsLogService.adjustUserPoints(userId, -points, reason,
                "violation", violation.getId(), operatorId);

        // ── 5. 通知用户 ──
        notificationService.createNotification(userId, operatorId,
                "violation", "您有新的违规记录: " + getViolationLabel(violationType),
                "violation", violation.getId());

        // ── 6. leak 类型：立即限制发帖（不走月度累计逻辑） ──
        if ("leak".equals(violationType)) {
            User user = userService.getById(userId);
            if (user != null && (user.getPostRestricted() == null || user.getPostRestricted() == 0)) {
                user.setPostRestricted(1);
                // leak 类型永久限制：LambdaUpdateWrapper 强制置空 post_restricted_until（updateById 会跳过 null）
                userService.update(user, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getPostRestricted, 1)
                        .set(User::getPostRestrictedUntil, null));

                notificationService.createNotification(userId, operatorId,
                        "post_restricted", "因泄露企业秘密/个人隐私，您的账号已被暂停发帖权限",
                        "user", userId);
            }
        } else {
            // ── 7. 其他类型：检查月度累计扣分，超过20分自动限制发帖7天 ──
            String monthStr = monthFmt.format(now);
            String monthStart = monthStr + "-01 00:00:00";
            String monthEnd = monthStr + "-32 00:00:00";
            Integer monthlyTotal = violationMapper.sumMonthlyDeductions(userId, monthStart, monthEnd);

            if (monthlyTotal > 20) {
                User user = userService.getById(userId);
                if (user != null && (user.getPostRestricted() == null || user.getPostRestricted() == 0)) {
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
        }

        return ResultBean.success("违规记录已添加，扣" + points + "分");
    }

    /**
     * 删除违规关联的内容（帖子/评论/回复）
     * 抄袭类型不在此处删除帖子（由调用方单独处理积分撤销后删除）
     */
    private void deleteRelatedContent(String relatedType, Integer relatedId, String violationType) {
        try {
            switch (relatedType) {
                case "article":
                    // 抄袭类型：在 addViolation 中已计算积分，此处只删除记录不扣积分
                    if ("plagiarism".equals(violationType)) {
                        articleMapper.deleteById(relatedId);
                    } else {
                        // 其他类型：手动执行删除逻辑（与 ArticleServiceImpl.deleteArticleByArticleId 一致）
                        Article article = articleMapper.selectById(relatedId);
                        if (article != null) {
                            if (article.getEnable() != null && article.getEnable() == 1) {
                                int postPoints = getDictValueByKey(ConstantUtil.MANA_POST, 2);
                                pointsLogService.adjustUserPoints(article.getUserId(), -postPoints,
                                        "违规删除帖子扣回积分", "article", relatedId, null);
                                if (article.getIsFeatured() != null && article.getIsFeatured() == 1) {
                                    int featuredPoints = getDictValueByKey(ConstantUtil.MANA_FEATURED, 10);
                                    pointsLogService.adjustUserPoints(article.getUserId(), -featuredPoints,
                                            "违规删除精华帖扣回积分", "article", relatedId, null);
                                }
                            }
                            articleMapper.deleteById(relatedId);
                        }
                    }
                    break;
                case "comment":
                    commentService.deleteCommentById(relatedId);
                    break;
                case "reply":
                    replyService.deleteReplyById(relatedId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            // 内容删除失败不阻塞违规记录，仅打印日志
            System.err.println("[Violation] 删除关联内容失败: " + relatedType + "#" + relatedId + " - " + e.getMessage());
        }
    }

    /**
     * 计算帖子已获得的全部积分（发帖积分 + 精华加分）
     */
    private int calculateArticleEarnedPoints(Integer articleId) {
        // 查询该帖子关联的所有正向积分记录
        List<PointsLog> logs = pointsLogService.list(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getRelatedType, "article")
                .eq(PointsLog::getRelatedId, articleId)
                .gt(PointsLog::getPointsChange, 0)
                .eq(PointsLog::getIsReversed, 0));

        int total = 0;
        for (PointsLog log : logs) {
            total += log.getPointsChange();
        }
        return total > 0 ? total : 1; // 至少扣1分
    }

    /**
     * 从数据字典获取配置值（统一入口，按 dict_key 查询）
     */
    private int getDictValueByKey(String key, int defaultValue) {
        try {
            String val = dictMapper.selectValueByKey(key);
            if (val != null) return Integer.parseInt(val);
        } catch (Exception e) { /* use default */ }
        return defaultValue;
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

        // 批量查询用户昵称
        Set<Integer> userIds = new HashSet<>();
        for (Violation v : result.getRecords()) {
            if (v.getUserId() != null) userIds.add(v.getUserId());
        }
        Map<Integer, String> nicknameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listUsersWithOrgInfo(userIds);
            for (User u : users) {
                nicknameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
        }

        // 批量查询申诉状态（每条违规最多一条申诉）
        Set<Integer> violationIds = new HashSet<>();
        for (Violation v : result.getRecords()) {
            if (v.getId() != null) violationIds.add(v.getId());
        }
        // violationId → 最新申诉状态（pending/accepted/rejected）
        Map<Integer, String> appealStatusMap = new HashMap<>();
        if (!violationIds.isEmpty()) {
            List<Appeal> appeals = appealService.list(new LambdaQueryWrapper<Appeal>()
                    .eq(Appeal::getAppealType, "violation")
                    .in(Appeal::getRelatedId, violationIds)
                    .orderByDesc(Appeal::getCreateTime));
            for (Appeal a : appeals) {
                // 只保留最新一条的状态
                if (a.getRelatedId() != null && !appealStatusMap.containsKey(a.getRelatedId())) {
                    appealStatusMap.put(a.getRelatedId(), a.getStatus());
                }
            }
        }

        // 批量反查评论/回复所属文章 id（#11 内容预览定位用；原生 SQL 可查已删除内容）
        Set<Integer> commentIds = new HashSet<>();
        Set<Integer> replyIds = new HashSet<>();
        for (Violation v : result.getRecords()) {
            if ("comment".equals(v.getRelatedType()) && v.getRelatedId() != null) commentIds.add(v.getRelatedId());
            if ("reply".equals(v.getRelatedType()) && v.getRelatedId() != null) replyIds.add(v.getRelatedId());
        }
        Map<Integer, Integer> commentArticleMap = new HashMap<>(); // commentId -> articleId
        for (Map<String, Object> row : queryCommentArticleIds(commentIds)) {
            commentArticleMap.put(toInt(row.get("commentId")), toInt(row.get("articleId")));
        }
        Map<Integer, Integer> replyCommentMap = new HashMap<>(); // replyId -> commentId
        for (Map<String, Object> row : queryReplyCommentIds(replyIds)) {
            replyCommentMap.put(toInt(row.get("replyId")), toInt(row.get("commentId")));
        }
        // 回复 -> 评论 -> 文章：二次补查评论所属文章
        Set<Integer> replyCommentIds = new HashSet<>(replyCommentMap.values());
        replyCommentIds.removeAll(commentArticleMap.keySet());
        for (Map<String, Object> row : queryCommentArticleIds(replyCommentIds)) {
            commentArticleMap.put(toInt(row.get("commentId")), toInt(row.get("articleId")));
        }

        // 组装结果
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Violation v : result.getRecords()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", v.getId());
            map.put("userId", v.getUserId());
            map.put("nickname", nicknameMap.getOrDefault(v.getUserId(), "未知用户"));
            map.put("violationType", v.getViolationType());
            map.put("violationLabel", getViolationLabel(v.getViolationType()));
            map.put("pointsDeducted", v.getPointsDeducted());
            map.put("relatedType", v.getRelatedType());
            map.put("relatedId", v.getRelatedId());
            map.put("remark", v.getRemark());
            map.put("createTime", v.getCreateTime());
            map.put("appealStatus", appealStatusMap.get(v.getId()));
            map.put("status", v.getStatus() == null ? "active" : v.getStatus());
            map.put("cancelReason", v.getCancelReason());
            map.put("cancelTime", v.getCancelTime());
            // #11 内容预览用：评论/回复所属文章 id
            Integer relatedArticleId = null;
            if ("article".equals(v.getRelatedType())) {
                relatedArticleId = v.getRelatedId();
            } else if ("comment".equals(v.getRelatedType())) {
                relatedArticleId = commentArticleMap.get(v.getRelatedId());
            } else if ("reply".equals(v.getRelatedType())) {
                Integer commentId = replyCommentMap.get(v.getRelatedId());
                relatedArticleId = commentId == null ? null : commentArticleMap.get(commentId);
            }
            map.put("relatedArticleId", relatedArticleId);
            enriched.add(map);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", enriched);
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

    // ==================== #14 取消违规 ====================

    /**
     * 从登录态（JWT）获取当前操作人 id。/admin/** 已由 Spring Security 强制认证。
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
    public ResultBean cancelViolation(Integer violationId, String reason) {
        if (violationId == null) {
            return ResultBean.error("参数不完整");
        }
        Integer operatorId = getCurrentUserId();
        if (operatorId == null) {
            return ResultBean.error("未获取到登录用户信息，请重新登录");
        }

        Violation v = this.getById(violationId);
        if (v == null) {
            return ResultBean.error("违规记录不存在");
        }
        if ("cancelled".equals(v.getStatus())) {
            return ResultBean.error("该违规已取消，不可重复操作");
        }
        if (reason == null || reason.trim().isEmpty()) {
            return ResultBean.error("请填写取消原因");
        }

        // 幂等保护：存在审核中的申诉时拒绝直接取消（避免与申诉流程竞态）
        long pending = appealService.count(new LambdaQueryWrapper<Appeal>()
                .eq(Appeal::getAppealType, "violation")
                .eq(Appeal::getRelatedId, violationId)
                .eq(Appeal::getStatus, "pending"));
        if (pending > 0) {
            return ResultBean.error("该违规存在审核中的申诉，请先处理该申诉后再取消");
        }

        doCancel(v, reason.trim(), operatorId);
        return ResultBean.success("违规已取消，扣分已回滚，关联内容已恢复");
    }

    @Override
    @Transactional
    public void autoCancelByAppeal(Integer violationId, Integer reviewerId, String reviewRemark) {
        if (violationId == null || reviewerId == null) return;
        Violation v = this.getById(violationId);
        if (v == null || "cancelled".equals(v.getStatus())) return;
        String reason = "申诉通过自动取消" + (reviewRemark != null && !reviewRemark.trim().isEmpty() ? "：" + reviewRemark.trim() : "");
        doCancel(v, reason, reviewerId);
    }

    /**
     * 取消违规的公共核心：回滚扣分日志 → 恢复内容可见性 → 状态置已取消留痕 → 通知用户。
     */
    private void doCancel(Violation v, String reason, Integer operatorId) {
        // 1. 回滚本次违规主扣分日志（负分、未撤销）
        List<PointsLog> mainLogs = pointsLogService.list(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getRelatedType, "violation")
                .eq(PointsLog::getRelatedId, v.getId())
                .lt(PointsLog::getPointsChange, 0)
                .eq(PointsLog::getIsReversed, 0));
        for (PointsLog log : mainLogs) {
            pointsLogService.undoPointsLog(log.getId(), operatorId);
        }

        // 2. 回滚内容删除引发的积分回滚日志，并恢复内容可见性
        restoreRelatedContent(v, operatorId);

        // 3. 状态置已取消并留痕
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        v.setStatus("cancelled");
        v.setCancelReason(reason);
        v.setCancelOperatorId(operatorId);
        v.setCancelTime(fmt.format(now));
        this.updateById(v);

        // 4. 通知用户
        notificationService.createNotification(v.getUserId(), operatorId,
                "violation", "您的违规记录已取消：" + getViolationLabel(v.getViolationType()),
                "violation", v.getId());
    }

    /** 恢复被违规删除的关联内容可见性，并回滚删除内容时扣回的积分 */
    private void restoreRelatedContent(Violation v, Integer operatorId) {
        String relatedType = v.getRelatedType();
        Integer relatedId = v.getRelatedId();
        if (relatedId == null || relatedType == null) return;
        try {
            switch (relatedType) {
                case "article":
                    undoDeductedContentLogs("article", relatedId, operatorId);
                    violationMapper.restoreArticleById(relatedId);
                    break;
                case "comment":
                    undoDeductedContentLogs("comment", relatedId, operatorId);
                    violationMapper.restoreCommentById(relatedId);
                    break;
                case "reply":
                    undoDeductedContentLogs("reply", relatedId, operatorId);
                    violationMapper.restoreReplyById(relatedId);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("[Violation] 取消违规恢复内容失败: " + relatedType + "#" + relatedId + " - " + e.getMessage());
        }
    }

    /** 撤销因删除内容而扣回的积分日志（对应内容类型、负分、未撤销） */
    private void undoDeductedContentLogs(String relatedType, Integer relatedId, Integer operatorId) {
        List<PointsLog> logs = pointsLogService.list(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getRelatedType, relatedType)
                .eq(PointsLog::getRelatedId, relatedId)
                .lt(PointsLog::getPointsChange, 0)
                .eq(PointsLog::getIsReversed, 0));
        for (PointsLog log : logs) {
            pointsLogService.undoPointsLog(log.getId(), operatorId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryCommentArticleIds(Set<Integer> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) return Collections.emptyList();
        return violationMapper.selectArticleIdByCommentIds(new ArrayList<>(commentIds));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryReplyCommentIds(Set<Integer> replyIds) {
        if (replyIds == null || replyIds.isEmpty()) return Collections.emptyList();
        return violationMapper.selectCommentIdByReplyIds(new ArrayList<>(replyIds));
    }

    private Integer toInt(Object o) {
        return o == null ? null : ((Number) o).intValue();
    }
}
