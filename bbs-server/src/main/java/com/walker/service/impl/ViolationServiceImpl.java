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
}
