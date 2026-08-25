package com.walker.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.walker.pojo.Notification;
import com.walker.vo.ResultBean;

/**
 * 通知服务接口
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 获取用户未读通知数量
     */
    int getUnreadCount(Integer userId);

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
