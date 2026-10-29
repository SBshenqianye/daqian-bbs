package com.walker.service;

import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.Report;
import com.walker.pojo.User;

/**
 * 自审批策略：判断“是否允许对自己操作”，与权限点分离。
 *
 * <p>除“审批自己提问的最佳解答”对所有人拦截外，其余规则超级管理员豁免。</p>
 */
public interface SelfApprovalPolicy {

    /** 采纳自己的建议（超管豁免） */
    void checkAdoptSuggestion(User operator, Article article);

    /** 审核自己提交的举报（超管豁免） */
    void checkReviewReport(User reviewer, Report report);

    /** 审核自己的申诉（超管豁免） */
    void checkReviewAppeal(User reviewer, Appeal appeal);

    /** 审核自己的版主投诉（超管豁免） */
    void checkReviewComplaint(User reviewer, ModeratorComplaint complaint);

    /** 审核自己的精华推荐（超管豁免） */
    void checkReviewFeatured(User reviewer, FeaturedRecommendation recommendation);

    /** 将自己的帖子设为精华（超管豁免；取消精华不调用本规则） */
    void checkSetFeatured(User operator, Article article);

    /** 审批自己提问的最佳解答（全员拦截，不设超管豁免） */
    void checkApproveAdopt(User operator, Article article);
}
