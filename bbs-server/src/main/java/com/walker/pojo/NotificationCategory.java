package com.walker.pojo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知分类注册表
 *
 * <p>通知的细粒度 {@code type}（reply/adopt/violation...）归组到少数几个展示分类
 * {@code category}（interaction/system），前台按分类展示独立红点与未读计数。
 *
 * <p><b>可复用约定</b>：新增通知类型时只需在 {@link #TYPE_CATEGORY} 注册一行
 * type → category，未读分组计数、分类标记已读、列表过滤即自动生效；
 * 未注册的类型默认归入 {@link #SYSTEM}。
 */
public final class NotificationCategory {

    /** 互动消息：与我发布/评论内容直接相关的社交互动（对应"回复我的"页面） */
    public static final String INTERACTION = "interaction";

    /** 系统通知：采纳、审批、举报、违规、积分奖励等系统行为通知（对应"消息通知"页面） */
    public static final String SYSTEM = "system";

    /** type → category 注册表。新增通知类型在此注册一行即可。 */
    private static final Map<String, String> TYPE_CATEGORY;

    static {
        Map<String, String> m = new HashMap<>();
        // ── 互动消息 ──
        m.put("reply", INTERACTION);            // 有人回复了你的评论/回复
        m.put("comment", INTERACTION);          // 有人评论了你的帖子
        // ── 系统通知 ──
        m.put("adopt", SYSTEM);                 // 回复/评论被采纳
        m.put("adopt_rejected", SYSTEM);        // 采纳被拒绝
        m.put("adopt_pending", SYSTEM);         // 待版主/管理员审批
        m.put("appeal_review", SYSTEM);         // 申诉审核结果
        m.put("hot_bonus", SYSTEM);             // 帖子热度奖励
        m.put("suggestion_adopted", SYSTEM);    // 建议被采纳
        m.put("report_confirmed", SYSTEM);      // 举报核实
        m.put("report_pending", SYSTEM);        // 实名举报待审核（通知超级管理员）
        m.put("violation", SYSTEM);             // 违规记录
        m.put("post_restricted", SYSTEM);       // 发帖权限限制
        m.put("moderator_reward", SYSTEM);      // 版主月度履职奖励
        m.put("moderator_complaint", SYSTEM);    // 版主投诉（通知超管）
        m.put("complaint_review", SYSTEM);       // 投诉审核结果（通知投诉人）
        m.put("featured_recommend", SYSTEM);      // 精华帖推荐（通知超管）
        m.put("featured_review", SYSTEM);         // 精华推荐审核结果（通知推荐人）
        TYPE_CATEGORY = Collections.unmodifiableMap(m);
    }

    private NotificationCategory() {
    }

    /**
     * 查询通知类型所属分类；未注册的类型默认归入系统通知。
     */
    public static String categoryOf(String type) {
        if (type == null) {
            return SYSTEM;
        }
        return TYPE_CATEGORY.getOrDefault(type, SYSTEM);
    }

    /**
     * 判断给定值是否为合法分类。
     */
    public static boolean isValid(String category) {
        return INTERACTION.equals(category) || SYSTEM.equals(category);
    }
}
