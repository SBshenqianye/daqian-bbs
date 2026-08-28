package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Notification;
import com.walker.vo.ResultBean;

import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 获取用户未读通知数量（全部类型合计）
     */
    int getUnreadCount(Integer userId);

    /**
     * 获取用户未读通知汇总（分类独立计数，可加和为 total）
     *
     * @return { total: N, byCategory: {interaction: n1, system: n2}, byType: {reply: n, ...} }
     */
    Map<String, Object> getUnreadSummary(Integer userId);

    /**
     * 标记通知为已读：优先按分类（category），其次按类型（type），都为空则标记全部
     */
    ResultBean markRead(Integer userId, String type, String category);

    /**
     * 标记某类型通知为已读（或全部）
     */
    ResultBean markRead(Integer userId, String type);

    /**
     * 标记所有通知为已读
     */
    ResultBean markAllRead(Integer userId);

    /**
     * 创建通知
     */
    ResultBean createNotification(Integer userId, Integer fromUserId, String type,
                                   String title, String relatedType, Integer relatedId);
}
