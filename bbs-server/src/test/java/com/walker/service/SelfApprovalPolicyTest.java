package com.walker.service;

import com.walker.config.security.SelfApprovalException;
import com.walker.pojo.Appeal;
import com.walker.pojo.Article;
import com.walker.pojo.FeaturedRecommendation;
import com.walker.pojo.ModeratorComplaint;
import com.walker.pojo.Report;
import com.walker.pojo.User;
import com.walker.service.impl.SelfApprovalPolicyImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 自审批策略测试（设计规划 3.4）。
 */
@ExtendWith(MockitoExtension.class)
class SelfApprovalPolicyTest {

    @InjectMocks
    private SelfApprovalPolicyImpl policy;

    @Mock
    private AuthorizationService authorizationService;

    private User user(int id, String userType) {
        User user = new User();
        user.setId(id);
        user.setUserType(userType);
        return user;
    }

    @BeforeEach
    void setUp() {
        lenient().when(authorizationService.isSuperAdmin(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    return u != null && "3".equals(u.getUserType());
                });
    }

    @Test
    @DisplayName("普通管理员采纳自己的建议 → 拒绝")
    void adoptSuggestion_normalAdmin_denied() {
        Article article = new Article();
        article.setUserId(5);
        assertThrows(SelfApprovalException.class,
                () -> policy.checkAdoptSuggestion(user(5, "2"), article));
    }

    @Test
    @DisplayName("超管采纳自己的建议 → 放行")
    void adoptSuggestion_superAdmin_allowed() {
        Article article = new Article();
        article.setUserId(1);
        assertDoesNotThrow(() -> policy.checkAdoptSuggestion(user(1, "3"), article));
    }

    @Test
    @DisplayName("普通管理员审核自己的举报/申诉/投诉/推荐 → 全部拒绝")
    void reviewSelf_normalAdmin_denied() {
        User admin = user(5, "2");

        Report report = new Report();
        report.setReporterId(5);
        assertThrows(SelfApprovalException.class, () -> policy.checkReviewReport(admin, report));

        Appeal appeal = new Appeal();
        appeal.setUserId(5);
        assertThrows(SelfApprovalException.class, () -> policy.checkReviewAppeal(admin, appeal));

        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setReporterId(5);
        assertThrows(SelfApprovalException.class, () -> policy.checkReviewComplaint(admin, complaint));

        FeaturedRecommendation recommendation = new FeaturedRecommendation();
        recommendation.setRecommenderId(5);
        assertThrows(SelfApprovalException.class, () -> policy.checkReviewFeatured(admin, recommendation));
    }

    @Test
    @DisplayName("超管审核自己的举报/申诉/投诉/推荐 → 全部放行")
    void reviewSelf_superAdmin_allowed() {
        User superAdmin = user(1, "3");

        Report report = new Report();
        report.setReporterId(1);
        assertDoesNotThrow(() -> policy.checkReviewReport(superAdmin, report));

        Appeal appeal = new Appeal();
        appeal.setUserId(1);
        assertDoesNotThrow(() -> policy.checkReviewAppeal(superAdmin, appeal));

        ModeratorComplaint complaint = new ModeratorComplaint();
        complaint.setReporterId(1);
        assertDoesNotThrow(() -> policy.checkReviewComplaint(superAdmin, complaint));

        FeaturedRecommendation recommendation = new FeaturedRecommendation();
        recommendation.setRecommenderId(1);
        assertDoesNotThrow(() -> policy.checkReviewFeatured(superAdmin, recommendation));
    }

    @Test
    @DisplayName("设置自己的帖子为精华：普通管理员拒绝，超管放行")
    void setFeatured_self() {
        Article article = new Article();
        article.setUserId(5);
        assertThrows(SelfApprovalException.class, () -> policy.checkSetFeatured(user(5, "2"), article));
        assertDoesNotThrow(() -> policy.checkSetFeatured(user(1, "3"), article));
    }

    @Test
    @DisplayName("审批自己提问的最佳解答：超管也拒绝")
    void approveAdopt_self_alwaysDenied() {
        Article article = new Article();
        article.setUserId(1);
        assertThrows(SelfApprovalException.class, () -> policy.checkApproveAdopt(user(1, "3"), article));
    }
}
