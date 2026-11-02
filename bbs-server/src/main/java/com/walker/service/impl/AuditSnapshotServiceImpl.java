package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.walker.mapper.AppealMapper;
import com.walker.mapper.ArticleLabelMapper;
import com.walker.mapper.ArticleMapper;
import com.walker.mapper.BoardModeratorMapper;
import com.walker.mapper.CommentMapper;
import com.walker.mapper.DictMapper;
import com.walker.mapper.FeaturedRecommendationMapper;
import com.walker.mapper.ModeratorComplaintMapper;
import com.walker.mapper.ReplyMapper;
import com.walker.mapper.ReportMapper;
import com.walker.mapper.SaOrgMapper;
import com.walker.mapper.SensitiveWordMapper;
import com.walker.mapper.SlideshowMapper;
import com.walker.mapper.SystemConfigMapper;
import com.walker.mapper.UserMapper;
import com.walker.mapper.ViolationMapper;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.ArticleLabel;
import com.walker.pojo.BoardModerator;
import com.walker.pojo.Comment;
import com.walker.pojo.Dict;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.Reply;
import com.walker.pojo.Report;
import com.walker.pojo.SaOrg;
import com.walker.pojo.SensitiveWord;
import com.walker.pojo.Slideshow;
import com.walker.pojo.SystemConfig;
import com.walker.pojo.User;
import com.walker.pojo.Violation;
import com.walker.service.AuditSnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计快照实现。
 *
 * <p>只读 mapper，不触发业务逻辑；只输出白名单字段，避免把正文、密码等写入审计表。</p>
 */
@Service
public class AuditSnapshotServiceImpl implements AuditSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AuditSnapshotServiceImpl.class);

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private ViolationMapper violationMapper;

    @Autowired
    private AppealMapper appealMapper;

    @Autowired
    private FeaturedRecommendationMapper featuredRecommendationMapper;

    @Autowired
    private ModeratorComplaintMapper moderatorComplaintMapper;

    @Autowired
    private BoardModeratorMapper boardModeratorMapper;

    @Autowired
    private ReplyMapper replyMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private DictMapper dictMapper;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ArticleLabelMapper articleLabelMapper;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private SlideshowMapper slideshowMapper;

    @Autowired
    private SaOrgMapper saOrgMapper;

    @Override
    public Map<String, Object> snapshot(String targetType, String targetId) {
        if (targetType == null || targetType.trim().isEmpty()
                || targetId == null || targetId.trim().isEmpty()
                || "ALL".equalsIgnoreCase(targetId)) {
            return null;
        }
        try {
            switch (targetType.toUpperCase()) {
                case "ARTICLE":
                    return article(parseInt(targetId));
                case "USER":
                    return user(parseInt(targetId));
                case "REPORT":
                    return report(parseInt(targetId));
                case "VIOLATION":
                    return violation(parseInt(targetId));
                case "APPEAL":
                    return appeal(parseInt(targetId));
                case "FEATURED_RECOMMENDATION":
                    return featuredRecommendation(parseInt(targetId));
                case "MODERATOR_COMPLAINT":
                    return moderatorComplaint(parseInt(targetId));
                case "MODERATOR_BOARD":
                    return moderatorBoard(parseInt(targetId));
                case "ADOPT":
                    return adopt(targetId);
                case "DICT":
                    return dict(parseInt(targetId));
                case "SYSTEM_CONFIG":
                    return systemConfig(parseInt(targetId));
                case "ARTICLE_LABEL":
                    return articleLabel(parseInt(targetId));
                case "SENSITIVE_WORD":
                    return sensitiveWord(parseInt(targetId));
                case "SLIDESHOW":
                    return slideshow(parseInt(targetId));
                case "ORG":
                    return org(targetId);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.debug("[审计日志] 快照解析失败 targetType={}, targetId={}, err={}",
                    targetType, targetId, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> article(Integer id) {
        if (id == null) return null;
        Article a = articleMapper.selectById(id);
        if (a == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("articleId", a.getArticleId());
        map.put("articleTitle", a.getArticleTitle());
        map.put("articleLabelId", a.getArticleLabelId());
        map.put("userId", a.getUserId());
        map.put("enable", a.getEnable());
        map.put("isFeatured", a.getIsFeatured());
        map.put("isDelete", a.getIsDelete());
        return map;
    }

    private Map<String, Object> user(Integer id) {
        if (id == null) return null;
        User u = userMapper.selectById(id);
        if (u == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("nickname", u.getNickname());
        map.put("userType", u.getUserType());
        map.put("isAlive", u.getIsAlive());
        map.put("isDelete", u.getIsDelete());
        map.put("isFirstLogin", u.getIsFirstLogin());
        map.put("orgNo", u.getOrgNo());
        map.put("postRestricted", u.getPostRestricted());
        map.put("postRestrictedUntil", u.getPostRestrictedUntil());
        map.put("phone", u.getPhone());
        map.put("idCard", u.getIdCard());
        return map;
    }

    private Map<String, Object> report(Integer id) {
        if (id == null) return null;
        Report r = reportMapper.selectById(id);
        if (r == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("reporterId", r.getReporterId());
        map.put("targetType", r.getTargetType());
        map.put("targetId", r.getTargetId());
        map.put("status", r.getStatus());
        map.put("reviewerId", r.getReviewerId());
        map.put("reviewRemark", r.getReviewRemark());
        map.put("reviewTime", r.getReviewTime());
        map.put("pointsAwarded", r.getPointsAwarded());
        return map;
    }

    private Map<String, Object> violation(Integer id) {
        if (id == null) return null;
        Violation v = violationMapper.selectById(id);
        if (v == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", v.getId());
        map.put("userId", v.getUserId());
        map.put("violationType", v.getViolationType());
        map.put("pointsDeducted", v.getPointsDeducted());
        map.put("relatedType", v.getRelatedType());
        map.put("relatedId", v.getRelatedId());
        map.put("status", v.getStatus());
        map.put("remark", v.getRemark());
        map.put("cancelReason", v.getCancelReason());
        map.put("cancelOperatorId", v.getCancelOperatorId());
        return map;
    }

    private Map<String, Object> appeal(Integer id) {
        if (id == null) return null;
        Appeal a = appealMapper.selectById(id);
        if (a == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", a.getId());
        map.put("userId", a.getUserId());
        map.put("appealType", a.getAppealType());
        map.put("relatedId", a.getRelatedId());
        map.put("status", a.getStatus());
        map.put("reviewerId", a.getReviewerId());
        map.put("reviewRemark", a.getReviewRemark());
        map.put("reviewTime", a.getReviewTime());
        return map;
    }

    private Map<String, Object> featuredRecommendation(Integer id) {
        if (id == null) return null;
        FeaturedRecommendation r = featuredRecommendationMapper.selectById(id);
        if (r == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("articleId", r.getArticleId());
        map.put("recommenderId", r.getRecommenderId());
        map.put("labelId", r.getLabelId());
        map.put("status", r.getStatus());
        map.put("reviewerId", r.getReviewerId());
        map.put("reviewRemark", r.getReviewRemark());
        map.put("reviewTime", r.getReviewTime());
        return map;
    }

    private Map<String, Object> moderatorComplaint(Integer id) {
        if (id == null) return null;
        ModeratorComplaint c = moderatorComplaintMapper.selectById(id);
        if (c == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("reporterId", c.getReporterId());
        map.put("moderatorId", c.getModeratorId());
        map.put("labelId", c.getLabelId());
        map.put("status", c.getStatus());
        map.put("reviewerId", c.getReviewerId());
        map.put("reviewRemark", c.getReviewRemark());
        map.put("reviewTime", c.getReviewTime());
        return map;
    }

    private Map<String, Object> moderatorBoard(Integer userId) {
        if (userId == null) return null;
        List<BoardModerator> rows = boardModeratorMapper.selectList(
                new LambdaQueryWrapper<BoardModerator>()
                        .eq(BoardModerator::getUserId, userId)
                        .eq(BoardModerator::getStatus, 1));
        List<Integer> labelIds = new ArrayList<>();
        for (BoardModerator row : rows) {
            if (row.getLabelId() != null) labelIds.add(row.getLabelId());
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", userId);
        map.put("labelIds", labelIds);
        return map;
    }

    private Map<String, Object> adopt(String targetId) {
        int idx = targetId.indexOf(':');
        if (idx <= 0) return null;
        String type = targetId.substring(0, idx);
        Integer id = parseInt(targetId.substring(idx + 1));
        if (id == null) return null;
        if ("reply".equalsIgnoreCase(type)) {
            Reply r = replyMapper.selectById(id);
            if (r == null) return null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("replyId", r.getReplyId());
            map.put("commentId", r.getCommentId());
            map.put("replyUserId", r.getReplyUserId());
            map.put("enable", r.getEnable());
            map.put("isDelete", r.getIsDelete());
            map.put("isAdopted", r.getIsAdopted());
            map.put("adoptStatus", r.getAdoptStatus());
            return map;
        }
        if ("comment".equalsIgnoreCase(type)) {
            Comment c = commentMapper.selectById(id);
            if (c == null) return null;
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("commentId", c.getCommentId());
            map.put("commentArticleId", c.getCommentArticleId());
            map.put("commentUserId", c.getCommentUserId());
            map.put("enable", c.getEnable());
            map.put("isDelete", c.getIsDelete());
            map.put("adoptStatus", c.getAdoptStatus());
            return map;
        }
        return null;
    }

    private Map<String, Object> dict(Integer id) {
        if (id == null) return null;
        Dict d = dictMapper.selectById(id);
        if (d == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("dictType", d.getDictType());
        map.put("dictKey", d.getDictKey());
        map.put("dictValue", d.getDictValue());
        map.put("dictLabel", d.getDictLabel());
        map.put("dictSort", d.getDictSort());
        map.put("remark", d.getRemark());
        return map;
    }

    private Map<String, Object> systemConfig(Integer id) {
        if (id == null) return null;
        SystemConfig c = systemConfigMapper.selectById(id);
        if (c == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("configKey", c.getConfigKey());
        map.put("configValue", c.getConfigValue());
        map.put("configLabel", c.getConfigLabel());
        map.put("configGroup", c.getConfigGroup());
        map.put("configType", c.getConfigType());
        map.put("sortOrder", c.getSortOrder());
        map.put("remark", c.getRemark());
        return map;
    }

    private Map<String, Object> articleLabel(Integer id) {
        if (id == null) return null;
        ArticleLabel l = articleLabelMapper.selectById(id);
        if (l == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("labelId", l.getLabelId());
        map.put("labelName", l.getLabelName());
        map.put("enabled", l.getEnabled());
        map.put("icon", l.getIcon());
        map.put("description", l.getDescription());
        map.put("labelType", l.getLabelType());
        return map;
    }

    private Map<String, Object> sensitiveWord(Integer id) {
        if (id == null) return null;
        SensitiveWord w = sensitiveWordMapper.selectById(id);
        if (w == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", w.getId());
        map.put("keyword", w.getKeyword());
        return map;
    }

    private Map<String, Object> slideshow(Integer id) {
        if (id == null) return null;
        Slideshow s = slideshowMapper.selectById(id);
        if (s == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("slideshowId", s.getSlideshowId());
        map.put("imageUrl", s.getImageUrl());
        map.put("successive", s.getSuccessive());
        return map;
    }

    private Map<String, Object> org(String orgNo) {
        if (orgNo == null || orgNo.trim().isEmpty()) return null;
        List<SaOrg> rows = saOrgMapper.selectList(
                new LambdaQueryWrapper<SaOrg>().eq(SaOrg::getOrgNo, orgNo));
        if (rows == null || rows.isEmpty()) return null;
        SaOrg o = rows.get(0);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", o.getId());
        map.put("orgNo", o.getOrgNo());
        map.put("orgName", o.getOrgName());
        map.put("pOrgNo", o.getPOrgNo());
        map.put("isDelete", o.getIsDelete());
        map.put("isRankingSelected", o.getIsRankingSelected());
        map.put("isDisplaySelected", o.getIsDisplaySelected());
        return map;
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
