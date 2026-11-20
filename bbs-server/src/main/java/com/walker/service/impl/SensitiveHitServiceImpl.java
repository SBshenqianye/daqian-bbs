package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.SensitiveHitMapper;
import com.walker.mapper.UserMapper;
import com.walker.pojo.Article;
import com.walker.pojo.Comment;
import com.walker.pojo.Reply;
import com.walker.pojo.SensitiveHit;
import com.walker.pojo.User;
import com.walker.service.AuthorizationService;
import com.walker.service.CurrentUserService;
import com.walker.service.SensitiveHitService;
import com.walker.utils.SensitiveWordUtil;
import com.walker.vo.ResultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 敏感词命中记录与风险画像实现。
 */
@Service
public class SensitiveHitServiceImpl extends ServiceImpl<SensitiveHitMapper, SensitiveHit>
        implements SensitiveHitService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveHitServiceImpl.class);

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 热力条覆盖的周数（近 12 周） */
    private static final int HEAT_WEEKS = 12;

    @Autowired
    private SensitiveHitMapper hitMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private CurrentUserService currentUserService;

    // ==================== 采集 ====================

    @Override
    public void record(String targetType, Integer targetId, Integer userId, Integer labelId, String text) {
        if (targetType == null || targetId == null || userId == null || text == null || text.isEmpty()) {
            return;
        }
        try {
            List<String> words = SensitiveWordUtil.match(text, 10);
            SensitiveHit existing = this.getOne(new LambdaQueryWrapper<SensitiveHit>()
                    .eq(SensitiveHit::getTargetType, targetType)
                    .eq(SensitiveHit::getTargetId, targetId)
                    .last("LIMIT 1"), false);
            if (words.isEmpty()) {
                // 编辑后不再命中：清掉旧记录
                if (existing != null) {
                    this.removeById(existing.getId());
                }
                return;
            }
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            if (existing != null) {
                // 编辑后命中词可能变化：整行覆盖，不重复计数
                existing.setUserId(userId);
                existing.setLabelId(labelId);
                existing.setHitWords(String.join(",", words));
                existing.setHitCount(words.size());
                existing.setCreateTime(now);
                this.updateById(existing);
                return;
            }
            SensitiveHit hit = new SensitiveHit();
            hit.setUserId(userId);
            hit.setTargetType(targetType);
            hit.setTargetId(targetId);
            hit.setLabelId(labelId);
            hit.setHitWords(String.join(",", words));
            hit.setHitCount(words.size());
            hit.setCreateTime(now);
            this.save(hit);
        } catch (Exception e) {
            // 采集失败绝不影响发布主流程
            log.warn("record sensitive hit failed: {} {} - {}", targetType, targetId, e.getMessage());
        }
    }

    @Override
    public int countRecentHits(Integer userId, int hours) {
        if (userId == null) {
            return 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -Math.max(1, hours));
        String since = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
        Long count = hitMapper.selectCount(new LambdaQueryWrapper<SensitiveHit>()
                .eq(SensitiveHit::getUserId, userId)
                .ge(SensitiveHit::getCreateTime, since));
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void remove(String targetType, Integer targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        try {
            hitMapper.delete(new LambdaQueryWrapper<SensitiveHit>()
                    .eq(SensitiveHit::getTargetType, targetType)
                    .eq(SensitiveHit::getTargetId, targetId));
        } catch (Exception e) {
            log.warn("remove sensitive hit failed: {} {} - {}", targetType, targetId, e.getMessage());
        }
    }

    @Override
    public List<SensitiveHit> findByTarget(String targetType, Integer targetId) {
        if (targetType == null || targetId == null) {
            return Collections.emptyList();
        }
        return hitMapper.selectList(new LambdaQueryWrapper<SensitiveHit>()
                .eq(SensitiveHit::getTargetType, targetType)
                .eq(SensitiveHit::getTargetId, targetId));
    }

    // ==================== 画像 ====================

    @Override
    public ResultBean hitDetail(String targetType, Integer targetId) {
        if (targetType == null || targetId == null) {
            return ResultBean.error("参数不完整");
        }
        List<SensitiveHit> hits = findByTarget(targetType, targetId);
        if (hits.isEmpty()) {
            return ResultBean.error("该内容没有命中记录");
        }
        SensitiveHit hit = hits.get(0);
        List<Integer> scope = currentBoardScope();
        if (scope != null && (scope.isEmpty()
                || hit.getLabelId() == null || !scope.contains(hit.getLabelId()))) {
            return ResultBean.forbidden("无权查看该板块的命中详情");
        }

        String rawText = rawText(targetType, targetId);
        List<String> words = new ArrayList<>();
        List<Map<String, Object>> snippets = new ArrayList<>();
        if (hit.getHitWords() != null && !hit.getHitWords().isEmpty()) {
            for (String word : hit.getHitWords().split(",")) {
                if (word.trim().isEmpty()) {
                    continue;
                }
                words.add(word);
                Map<String, Object> snippet = new LinkedHashMap<>();
                snippet.put("word", word);
                snippet.put("context", context(rawText, word, 30));
                snippets.add(snippet);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("targetType", targetType);
        data.put("targetId", targetId);
        data.put("labelId", hit.getLabelId());
        data.put("authorId", hit.getUserId());
        data.put("createTime", hit.getCreateTime());
        data.put("hitWords", words);
        data.put("snippets", snippets);
        return ResultBean.success("查询成功", data);
    }

    /** 读取原文（不做脱敏），仅命中详情与作者编辑接口使用 */
    private String rawText(String targetType, Integer targetId) {
        try {
            if ("article".equals(targetType)) {
                Article article = articleMapper.selectById(targetId);
                if (article == null) {
                    return null;
                }
                return (article.getArticleTitle() == null ? "" : article.getArticleTitle() + "\n")
                        + (article.getArticleContent() == null ? "" : article.getArticleContent());
            }
            if ("comment".equals(targetType)) {
                Comment comment = commentMapper.selectById(targetId);
                return comment == null ? null : comment.getCommentContent();
            }
            if ("reply".equals(targetType)) {
                Reply reply = replyMapper.selectById(targetId);
                return reply == null ? null : reply.getReplyContent();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /** 命中词前后 N 字的上下文片段 */
    private String context(String text, String word, int radius) {
        if (text == null || word == null) {
            return null;
        }
        int index = text.indexOf(word);
        if (index < 0) {
            return null;
        }
        int start = Math.max(0, index - radius);
        int end = Math.min(text.length(), index + word.length() + radius);
        String prefix = start > 0 ? "..." : "";
        String suffix = end < text.length() ? "..." : "";
        return prefix + text.substring(start, end).replaceAll("\\s+", " ") + suffix;
    }

    @Override
    public Map<String, Object> grid() {
        List<Integer> scope = currentBoardScope();
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> cells = new ArrayList<>();
        List<User> users = listVisibleUsers();
        Map<Integer, Map<String, Object>> statByUser = statsByUser(scope, null);

        for (User user : users) {
            Map<String, Object> stat = statByUser.get(user.getId());
            Map<String, Object> cell = new LinkedHashMap<>();
            cell.put("userId", user.getId());
            cell.put("nickname", displayName(user));
            cell.put("portrait", user.getPortrait());
            cell.put("hits", stat == null ? 0 : ((Number) stat.get("cnt")).intValue());
            cell.put("lastHit", stat == null ? null : stat.get("lastHit"));
            cells.add(cell);
        }

        int total = cells.size();
        int columns = total <= 0 ? 1 : (int) Math.ceil(Math.sqrt(total));
        result.put("generatedAt", LocalDateTime.now().format(DATE_TIME));
        result.put("total", total);
        result.put("columns", columns);
        result.put("scoped", scope != null);
        result.put("cells", cells);
        return result;
    }

    @Override
    public ResultBean rank(int page, int size, int days) {
        List<Integer> scope = currentBoardScope();
        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(100, size));
        int dayRange = Math.max(1, Math.min(365, days));

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -dayRange);
        String since = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());

        Map<Integer, Map<String, Object>> statByUser = statsByUser(scope, since);
        List<Map<String, Object>> rows = new ArrayList<>(statByUser.values());
        rows.sort((a, b) -> Integer.compare(
                ((Number) b.get("cnt")).intValue(), ((Number) a.get("cnt")).intValue()));

        int total = rows.size();
        int from = Math.min((pageNo - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);

        List<Map<String, Object>> records = new ArrayList<>();
        if (from < to) {
            List<Integer> userIds = new ArrayList<>();
            for (int i = from; i < to; i++) {
                userIds.add(((Number) rows.get(i).get("userId")).intValue());
            }
            Map<Integer, User> userMap = loadUsers(userIds);
            Map<Integer, int[]> weekly = weeklyHeat(userIds);
            Map<Integer, Integer> published = publishedCounts(userIds);

            for (int i = from; i < to; i++) {
                Map<String, Object> stat = rows.get(i);
                Integer userId = ((Number) stat.get("userId")).intValue();
                User user = userMap.get(userId);
                int hits = ((Number) stat.get("cnt")).intValue();
                int totalPublished = published.getOrDefault(userId, 0);
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("userId", userId);
                record.put("nickname", user == null ? "用户" + userId : displayName(user));
                record.put("portrait", user == null ? null : user.getPortrait());
                record.put("hits", hits);
                record.put("published", totalPublished);
                record.put("rate", totalPublished <= 0 ? 0D
                        : Math.round(hits * 1000D / totalPublished) / 10D);
                record.put("lastHit", stat.get("lastHit"));
                record.put("weekly", weekly.getOrDefault(userId, new int[HEAT_WEEKS]));
                records.add(record);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("page", pageNo);
        data.put("size", pageSize);
        data.put("days", dayRange);
        data.put("weeks", HEAT_WEEKS);
        data.put("scoped", scope != null);
        return ResultBean.success("查询成功", data);
    }

    @Override
    public ResultBean userDetail(Integer userId, int page, int size) {
        if (userId == null) {
            return ResultBean.error("缺少用户ID");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResultBean.error("用户不存在");
        }
        List<Integer> scope = currentBoardScope();
        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(100, size));

        LambdaQueryWrapper<SensitiveHit> wrapper = new LambdaQueryWrapper<SensitiveHit>()
                .eq(SensitiveHit::getUserId, userId)
                .orderByDesc(SensitiveHit::getCreateTime);
        if (scope != null) {
            if (scope.isEmpty()) {
                wrapper.eq(SensitiveHit::getLabelId, -1);
            } else {
                wrapper.in(SensitiveHit::getLabelId, scope);
            }
        }
        List<SensitiveHit> all = hitMapper.selectList(wrapper);

        List<Map<String, Object>> records = new ArrayList<>();
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        for (int i = from; i < to; i++) {
            SensitiveHit hit = all.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("targetType", hit.getTargetType());
            row.put("targetId", hit.getTargetId());
            row.put("labelId", hit.getLabelId());
            row.put("hitWords", hit.getHitWords());
            row.put("hitCount", hit.getHitCount());
            row.put("createTime", hit.getCreateTime());
            row.put("title", contentTitle(hit.getTargetType(), hit.getTargetId()));
            records.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userProfile(user));
        data.put("records", records);
        data.put("total", all.size());
        data.put("page", pageNo);
        data.put("size", pageSize);
        data.put("published", publishedCounts(Collections.singletonList(userId)).getOrDefault(userId, 0));
        return ResultBean.success("查询成功", data);
    }

    // ==================== 内部工具 ====================

    /**
     * 当前用户的板块范围：null=全局（不限），空列表=无可见板块。
     */
    private List<Integer> currentBoardScope() {
        User current = currentUserService.getCurrentUser();
        if (current == null) {
            return Collections.emptyList();
        }
        if (authorizationService.isGlobalAdmin(current)) {
            return null;
        }
        List<Integer> boards = authorizationService.getVisibleBoardIds(current);
        return boards == null ? Collections.emptyList() : boards;
    }

    /** 未删除用户（热力方阵与排行榜的分母） */
    private List<User> listVisibleUsers() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("id", "nickname", "username", "portrait", "create_time")
                .eq("is_delete", 0)
                .orderByAsc("id");
        List<User> users = userMapper.selectList(wrapper);
        return users == null ? Collections.emptyList() : users;
    }

    /**
     * 按用户聚合命中：key=userId, value={userId, cnt, lastHit}。
     * 在内存里聚合，避免依赖 SQL 别名大小写（MySQL 与 PostgreSQL 行为不一致）。
     * scope 为 null 表示不限板块；空列表直接返回空。
     */
    private Map<Integer, Map<String, Object>> statsByUser(List<Integer> scope, String since) {
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        if (scope != null && scope.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<SensitiveHit> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SensitiveHit::getUserId, SensitiveHit::getCreateTime);
        if (scope != null) {
            wrapper.in(SensitiveHit::getLabelId, scope);
        }
        if (since != null) {
            wrapper.ge(SensitiveHit::getCreateTime, since);
        }
        List<SensitiveHit> hits = hitMapper.selectList(wrapper);
        if (hits == null) {
            return result;
        }
        for (SensitiveHit hit : hits) {
            Integer userId = hit.getUserId();
            if (userId == null) {
                continue;
            }
            Map<String, Object> stat = result.get(userId);
            if (stat == null) {
                stat = new LinkedHashMap<>();
                stat.put("userId", userId);
                stat.put("cnt", 0);
                stat.put("lastHit", null);
                result.put(userId, stat);
            }
            stat.put("cnt", ((Integer) stat.get("cnt")) + 1);
            String time = hit.getCreateTime();
            String last = (String) stat.get("lastHit");
            if (time != null && (last == null || time.compareTo(last) > 0)) {
                stat.put("lastHit", time);
            }
        }
        return result;
    }

    private Map<Integer, User> loadUsers(List<Integer> userIds) {
        Map<Integer, User> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        if (users != null) {
            for (User user : users) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    /** 近 12 周按周计数，索引 0 = 最早一周，11 = 本周 */
    private Map<Integer, int[]> weeklyHeat(List<Integer> userIds) {
        Map<Integer, int[]> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        LocalDate today = LocalDate.now();
        LocalDate earliest = today.minusWeeks(HEAT_WEEKS - 1).with(java.time.DayOfWeek.MONDAY);
        String since = earliest.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        List<SensitiveHit> hits = hitMapper.selectList(new LambdaQueryWrapper<SensitiveHit>()
                .in(SensitiveHit::getUserId, userIds)
                .ge(SensitiveHit::getCreateTime, since)
                .select(SensitiveHit::getUserId, SensitiveHit::getCreateTime));
        if (hits == null) {
            return result;
        }
        for (SensitiveHit hit : hits) {
            int[] buckets = result.computeIfAbsent(hit.getUserId(), key -> new int[HEAT_WEEKS]);
            LocalDateTime time = parse(hit.getCreateTime());
            if (time == null) {
                continue;
            }
            LocalDate weekStart = time.toLocalDate().with(java.time.DayOfWeek.MONDAY);
            long weeks = ChronoUnit.WEEKS.between(weekStart, today.with(java.time.DayOfWeek.MONDAY));
            int index = HEAT_WEEKS - 1 - (int) weeks;
            if (index >= 0 && index < HEAT_WEEKS) {
                buckets[index]++;
            }
        }
        return result;
    }

    private LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    /** 各用户累计发布量（帖子+评论+回复），用于命中率 */
    private Map<Integer, Integer> publishedCounts(List<Integer> userIds) {
        Map<Integer, Integer> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        // 只取主键/作者列，在内存聚合，避免跨库 SQL 别名差异
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getUserId)
                .in(Article::getUserId, userIds));
        if (articles != null) {
            for (Article article : articles) {
                if (article.getUserId() != null) {
                    result.merge(article.getUserId(), 1, Integer::sum);
                }
            }
        }
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .select(Comment::getCommentUserId)
                .in(Comment::getCommentUserId, userIds));
        if (comments != null) {
            for (Comment comment : comments) {
                if (comment.getCommentUserId() != null) {
                    result.merge(comment.getCommentUserId(), 1, Integer::sum);
                }
            }
        }
        List<Reply> replies = replyMapper.selectList(new LambdaQueryWrapper<Reply>()
                .select(Reply::getReplyUserId)
                .in(Reply::getReplyUserId, userIds));
        if (replies != null) {
            for (Reply reply : replies) {
                if (reply.getReplyUserId() != null) {
                    result.merge(reply.getReplyUserId(), 1, Integer::sum);
                }
            }
        }
        return result;
    }

    private String contentTitle(String targetType, Integer targetId) {
        try {
            if ("article".equals(targetType)) {
                Article article = articleMapper.selectById(targetId);
                return article == null ? null : article.getArticleTitle();
            }
            if ("comment".equals(targetType)) {
                Comment comment = commentMapper.selectById(targetId);
                return comment == null ? null : truncate(comment.getCommentContent());
            }
            if ("reply".equals(targetType)) {
                Reply reply = replyMapper.selectById(targetId);
                return reply == null ? null : truncate(reply.getReplyContent());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        String plain = text.replaceAll("\\s+", " ").trim();
        return plain.length() > 60 ? plain.substring(0, 60) + "..." : plain;
    }

    private Map<String, Object> userProfile(User user) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getId());
        profile.put("nickname", displayName(user));
        profile.put("portrait", user.getPortrait());
        profile.put("orgName", user.getOrgName());
        profile.put("createTime", user.getCreateTime());
        profile.put("postRestricted", user.getPostRestricted());
        return profile;
    }

    private String displayName(User user) {
        if (user == null) {
            return "未知用户";
        }
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        return user.getUsername() == null ? ("用户" + user.getId()) : user.getUsername();
    }
}
