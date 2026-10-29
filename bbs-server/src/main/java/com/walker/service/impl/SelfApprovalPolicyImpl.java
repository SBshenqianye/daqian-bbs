package com.walker.service.impl;

import com.walker.config.security.SelfApprovalException;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.Report;
import com.walker.pojo.User;
import com.walker.service.AuthorizationService;
import com.walker.service.SelfApprovalPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 自审批策略实现：统一使用 AuthorizationService.isSuperAdmin 做豁免。
 */
@Service
public class SelfApprovalPolicyImpl implements SelfApprovalPolicy {

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public void checkAdoptSuggestion(User operator, Article article) {
        if (isSelf(operator, article != null ? article.getUserId() : null)
                && !authorizationService.isSuperAdmin(operator)) {
            throw new SelfApprovalException("不能采纳自己的建议");
        }
    }

    @Override
    public void checkReviewReport(User reviewer, Report report) {
        if (isSelf(reviewer, report != null ? report.getReporterId() : null)
                && !authorizationService.isSuperAdmin(reviewer)) {
            throw new SelfApprovalException("不能审核自己提交的举报");
        }
    }

    @Override
    public void checkReviewAppeal(User reviewer, Appeal appeal) {
        if (isSelf(reviewer, appeal != null ? appeal.getUserId() : null)
                && !authorizationService.isSuperAdmin(reviewer)) {
            throw new SelfApprovalException("不能审核自己的申诉");
        }
    }

    @Override
    public void checkReviewComplaint(User reviewer, ModeratorComplaint complaint) {
        if (isSelf(reviewer, complaint != null ? complaint.getReporterId() : null)
                && !authorizationService.isSuperAdmin(reviewer)) {
            throw new SelfApprovalException("不能审核自己的投诉");
        }
    }

    @Override
    public void checkReviewFeatured(User reviewer, FeaturedRecommendation recommendation) {
        if (isSelf(reviewer, recommendation != null ? recommendation.getRecommenderId() : null)
                && !authorizationService.isSuperAdmin(reviewer)) {
            throw new SelfApprovalException("不能审核自己的推荐");
        }
    }

    @Override
    public void checkSetFeatured(User operator, Article article) {
        if (isSelf(operator, article != null ? article.getUserId() : null)
                && !authorizationService.isSuperAdmin(operator)) {
            throw new SelfApprovalException("不能将自己的帖子设为精华");
        }
    }

    @Override
    public void checkApproveAdopt(User operator, Article article) {
        // 当前产品规则：任何人（含超管）都不能审批自己提问的最佳解答
        if (isSelf(operator, article != null ? article.getUserId() : null)) {
            throw new SelfApprovalException("不能审批自己提问的最佳解答");
        }
    }

    private boolean isSelf(User operator, Integer ownerId) {
        return operator != null && ownerId != null && ownerId.equals(operator.getId());
    }
}
