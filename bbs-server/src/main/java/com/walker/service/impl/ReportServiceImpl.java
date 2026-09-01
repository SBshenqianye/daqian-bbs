package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ReportMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.Report;
import com.walker.pojo.User;
import com.walker.service.ArticleService;
import com.walker.service.CommentService;
import com.walker.service.NotificationService;
import com.walker.service.PointsLogService;
import com.walker.service.ReplyService;
import com.walker.service.ReportService;
import com.walker.service.UserService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    /** 单人单日举报上限（兜底防"驳回→再报"循环刷量） */
    private static final int MAX_REPORTS_PER_DAY = 10;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private PointsLogService pointsLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public ResultBean submitReport(Integer reporterId, String targetType, Integer targetId, String violationType, String reason) {
        if (reporterId == null || targetType == null || targetId == null) {
            return ResultBean.error("参数不完整");
        }

        // 不能举报自己发布的内容（实名举报面向他人内容，同时杜绝自产自销刷分）
        Integer ownerId = queryTargetOwnerId(targetType, targetId);
        if (ownerId != null && ownerId.equals(reporterId)) {
            return ResultBean.error("不能举报自己发布的内容");
        }

        // 该内容已被核实处理过（存在 confirmed 记录）则不再接受任何新举报：首报有效，防确认后跟报刷分
        long confirmedCount = this.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "confirmed"));
        if (confirmedCount > 0) {
            return ResultBean.error("该内容已被核实处理，无需重复举报");
        }

        // 单人单日频率限制：兜底防"驳回→再报"无限循环刷量
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        long todayCount = this.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .ge(Report::getCreateTime, today));
        if (todayCount >= MAX_REPORTS_PER_DAY) {
            return ResultBean.error("举报过于频繁，请明天再试");
        }

        // 检查是否已举报过：存在未驳回的记录即拒绝（pending 期间防重复，confirmed 后防同一内容反复计分；
        // rejected 后允许再报，便于补充证据，由单日上限兜底）
        long count = this.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .ne(Report::getStatus, "rejected"));
        if (count > 0) {
            return ResultBean.error("您已举报过该内容，请等待审核");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setViolationType(violationType);
        report.setReason(reason);
        report.setStatus("pending");
        report.setPointsAwarded(0);
        report.setCreateTime(fmt.format(now));
        this.save(report);

        // 通知超级管理员（bbs_user id=1，与 adopt_pending 待审批通知同模式）有新举报待审核。
        // 仅当本条是该目标第一条待审举报时才通知：同一内容多人跟报不重复轰炸超管；
        // 该目标被处理后（无 pending）有人再报，会重新触发通知（新一轮待办）。
        // 举报人即管理员本人时 createNotification 内部会跳过自身通知。
        long pendingCount = this.count(new LambdaQueryWrapper<Report>()
                .eq(Report::getTargetType, targetType)
                .eq(Report::getTargetId, targetId)
                .eq(Report::getStatus, "pending"));
        if (pendingCount == 1) {
            notificationService.createNotification(1, reporterId, "report_pending",
                    "有新的实名举报待审核：" + targetTypeLabel(targetType) + " #" + targetId,
                    "report", report.getId());
        }

        return ResultBean.success("举报已提交，等待审核");
    }

    /**
     * 查询举报目标内容的作者 id（用于禁止自举报）；目标不存在或类型未知返回 null
     */
    private Integer queryTargetOwnerId(String targetType, Integer targetId) {
        if ("article".equals(targetType)) {
            Article article = articleService.getById(targetId);
            return article == null ? null : article.getUserId();
        }
        if ("comment".equals(targetType)) {
            Comment comment = commentService.getById(targetId);
            return comment == null ? null : comment.getCommentUserId();
        }
        if ("reply".equals(targetType)) {
            Reply reply = replyService.getById(targetId);
            return reply == null ? null : reply.getReplyUserId();
        }
        return null;
    }

    /** 举报对象类型的中文展示名（通知文案用），与前端 getTargetTypeLabel 口径一致 */
    private String targetTypeLabel(String targetType) {
        if (targetType == null) {
            return "内容";
        }
        switch (targetType) {
            case "article": return "文章";
            case "comment": return "评论";
            case "reply":   return "回复";
            default:        return targetType;
        }
    }

    @Override
    @Transactional
    public ResultBean reviewReport(Integer reportId, Integer reviewerId, String status, String remark) {
        if (reportId == null || reviewerId == null || status == null) {
            return ResultBean.error("参数不完整");
        }

        Report report = this.getById(reportId);
        if (report == null) {
            return ResultBean.error("举报记录不存在");
        }
        if (!"pending".equals(report.getStatus())) {
            return ResultBean.error("该举报已处理");
        }

        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        report.setStatus(status);
        report.setReviewerId(reviewerId);
        report.setReviewTime(fmt.format(now));
        report.setReviewRemark(remark);

        // 确认举报属实：给该举报人加2分
        if ("confirmed".equals(status)) {
            report.setPointsAwarded(1);
            this.updateById(report);

            pointsLogService.adjustUserPoints(report.getReporterId(), 2, "举报属实奖励",
                    "report", reportId, reviewerId);

            // 通知举报人
            notificationService.createNotification(report.getReporterId(), reviewerId,
                    "report_confirmed", "您的举报已核实", "report", reportId);

            // 一次批准、全组计分：同一内容其余待审举报一并确认，各自 +2 分并通知。
            // 计分截止于本次核实——之后的跟报在提交时即被拒（见 submitReport 的 confirmed 拦截），不再有后续参与者。
            List<Report> others = this.list(new LambdaQueryWrapper<Report>()
                    .eq(Report::getTargetType, report.getTargetType())
                    .eq(Report::getTargetId, report.getTargetId())
                    .eq(Report::getStatus, "pending")
                    .ne(Report::getId, reportId));
            for (Report other : others) {
                other.setStatus("confirmed");
                other.setPointsAwarded(1);
                other.setReviewerId(reviewerId);
                other.setReviewTime(fmt.format(now));
                other.setReviewRemark("同一内容举报核实，一并确认加分");
                this.updateById(other);

                pointsLogService.adjustUserPoints(other.getReporterId(), 2, "举报属实奖励",
                        "report", other.getId(), reviewerId);

                notificationService.createNotification(other.getReporterId(), reviewerId,
                        "report_confirmed", "您举报的内容已核实", "report", other.getId());
            }
        } else {
            this.updateById(report);
        }

        return ResultBean.success("审核完成");
    }

    @Override
    public ResultBean listReports(String status, Integer page, Integer size) {
        Page<Report> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);
        Page<Report> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listReportsGrouped(String status, Integer page, Integer size) {
        // 举报表数据量有限（社区场景），一次性取过滤后记录在内存分组，换取组完整性（分页不拆组）
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByAsc(Report::getCreateTime);
        List<Report> all = this.list(wrapper);

        // 批量查询举报人昵称
        Set<Integer> reporterIds = new HashSet<>();
        for (Report r : all) {
            if (r.getReporterId() != null) reporterIds.add(r.getReporterId());
        }
        Map<Integer, String> reporterNameMap = new HashMap<>();
        if (!reporterIds.isEmpty()) {
            List<User> users = userService.listByIds(reporterIds);
            for (User u : users) {
                reporterNameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
        }

        // 批量查询被举报内容的作者和正文预览（用于"确认并扣分"自动填入 + 内容预览）
        Map<String, Integer> targetAuthorMap = new HashMap<>(); // "type|id" → userId
        Map<String, String> targetTitleMap = new HashMap<>();   // "type|id" → 标题（仅文章）
        Map<String, String> targetContentMap = new HashMap<>(); // "type|id" → 正文预览（前200字）
        Map<String, String> targetContentHtmlMap = new HashMap<>(); // "type|id" → 正文 HTML（仅文章）
        Set<Integer> articleIds = new HashSet<>();
        Set<Integer> commentIds = new HashSet<>();
        Set<Integer> replyIds = new HashSet<>();
        for (Report r : all) {
            String key = r.getTargetType() + "|" + r.getTargetId();
            if (targetAuthorMap.containsKey(key)) continue;
            if ("article".equals(r.getTargetType())) articleIds.add(r.getTargetId());
            else if ("comment".equals(r.getTargetType())) commentIds.add(r.getTargetId());
            else if ("reply".equals(r.getTargetType())) replyIds.add(r.getTargetId());
        }
        if (!articleIds.isEmpty()) {
            for (Integer artId : articleIds) {
                // 使用 selectByIdRaw 绕过 @TableLogic，确保已删除文章也能被查到
                Article a = articleService.getArticleByIdRaw(artId);
                if (a != null) {
                    String key = "article|" + a.getArticleId();
                    targetAuthorMap.put(key, a.getUserId());
                    targetTitleMap.put(key, a.getArticleTitle());
                    targetContentMap.put(key, truncate(a.getArticleContent(), 200));
                    targetContentHtmlMap.put(key, a.getArticleContentHtml());
                }
            }
        }
        // 评论/回复 → 所属文章 ID 映射（用于前端加载完整文章上下文 + 高亮定位）
        Map<String, Integer> targetArticleIdMap = new HashMap<>();
        if (!commentIds.isEmpty()) {
            for (Comment c : commentService.listByIds(commentIds)) {
                String key = "comment|" + c.getCommentId();
                targetAuthorMap.put(key, c.getCommentUserId());
                targetContentMap.put(key, truncate(c.getCommentContent(), 200));
                targetArticleIdMap.put(key, c.getCommentArticleId());
            }
        }
        if (!replyIds.isEmpty()) {
            for (Reply rp : replyService.listByIds(replyIds)) {
                String key = "reply|" + rp.getReplyId();
                targetAuthorMap.put(key, rp.getReplyUserId());
                targetContentMap.put(key, truncate(rp.getReplyContent(), 200));
                // reply → comment → article
                if (rp.getCommentId() != null) {
                    Comment parentComment = commentService.getById(rp.getCommentId());
                    if (parentComment != null) {
                        targetArticleIdMap.put(key, parentComment.getCommentArticleId());
                    }
                }
            }
        }
        // 批量查询作者昵称
        Set<Integer> authorIds = new HashSet<>(targetAuthorMap.values());
        Map<Integer, String> authorNameMap = new HashMap<>();
        if (!authorIds.isEmpty()) {
            List<User> authors = userService.listByIds(authorIds);
            for (User u : authors) {
                authorNameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
            }
        }

        // 按 targetType|targetId 分组，LinkedHashMap 保持最早举报在前
        Map<String, List<Report>> groups = new LinkedHashMap<>();
        for (Report r : all) {
            groups.computeIfAbsent(r.getTargetType() + "|" + r.getTargetId(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> records = new ArrayList<>();
        int total = groups.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(from + size, total);
        List<List<Report>> groupLists = new ArrayList<>(groups.values());
        for (int i = from; i < to; i++) {
            List<Report> members = groupLists.get(i);
            // 代表记录：优先首条待审（管理员最需要操作的），否则最早一条（首报）
            Report representative = members.get(0);
            for (Report m : members) {
                if ("pending".equals(m.getStatus())) {
                    representative = m;
                    break;
                }
            }

            // 将 Report 转为 Map 并附加 reporterName + 目标作者 + 内容预览
            Map<String, Object> repMap = reportToMap(representative, reporterNameMap);
            String targetKey = representative.getTargetType() + "|" + representative.getTargetId();
            Integer authorId = targetAuthorMap.get(targetKey);
            repMap.put("targetAuthorId", authorId);
            repMap.put("targetAuthorName", authorId != null ? authorNameMap.getOrDefault(authorId, "用户" + authorId) : null);
            repMap.put("targetTitle", targetTitleMap.get(targetKey));
            repMap.put("targetContent", targetContentMap.get(targetKey));
            repMap.put("targetContentHtml", targetContentHtmlMap.get(targetKey));
            repMap.put("targetArticleId", targetArticleIdMap.get(targetKey));

            List<Map<String, Object>> memberMaps = new ArrayList<>();
            for (Report m : members) {
                memberMaps.add(reportToMap(m, reporterNameMap));
            }

            Map<String, Object> group = new HashMap<>();
            group.put("representative", repMap);
            group.put("members", memberMaps);
            group.put("totalCount", members.size());
            group.put("targetType", representative.getTargetType());
            group.put("targetId", representative.getTargetId());
            records.add(group);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean listMyReports(Integer reporterId, Integer page, Integer size) {
        Page<Report> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, reporterId);
        wrapper.orderByDesc(Report::getCreateTime);
        Page<Report> result = this.page(pageParam, wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return ResultBean.success("查询成功", data);
    }

    /**
     * 截断文本并清理 Markdown 语法，用于内容预览
     */
    private String truncate(String text, int maxLen) {
        if (text == null || text.isEmpty()) return null;
        // 去掉常见 Markdown 标记，只保留纯文本预览
        String plain = text
                .replaceAll("!\\[.*?]\\(.*?\\)", "")       // 图片
                .replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1")  // 链接保留文字
                .replaceAll("```[\\s\\S]*?```", "[代码]")   // 代码块
                .replaceAll("`([^`]+)`", "$1")              // 行内代码
                .replaceAll("#{1,6}\\s*", "")               // 标题
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")     // 粗体
                .replaceAll("\\*(.+?)\\*", "$1")            // 斜体
                .replaceAll("\\n+", " ")                    // 换行变空格
                .trim();
        return plain.length() > maxLen ? plain.substring(0, maxLen) + "..." : plain;
    }

    /**
     * 将 Report 转为 Map 并附加 reporterName
     */
    private Map<String, Object> reportToMap(Report r, Map<Integer, String> nameMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("reporterId", r.getReporterId());
        map.put("reporterName", nameMap.getOrDefault(r.getReporterId(), "用户" + r.getReporterId()));
        map.put("targetType", r.getTargetType());
        map.put("targetId", r.getTargetId());
        map.put("violationType", r.getViolationType());
        map.put("reason", r.getReason());
        map.put("status", r.getStatus());
        map.put("reviewerId", r.getReviewerId());
        map.put("reviewRemark", r.getReviewRemark());
        map.put("reviewTime", r.getReviewTime());
        map.put("createTime", r.getCreateTime());
        map.put("pointsAwarded", r.getPointsAwarded());
        return map;
    }
}
