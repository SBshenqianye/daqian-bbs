package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.NotificationMapper;
import com.walker.pojo.Notification;
import com.walker.service.NotificationService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 通知服务实现类
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public int getUnreadCount(Integer userId) {
        if (userId == null) return 0;
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public ResultBean markRead(Integer userId, String type) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Notification::getIsRead, 1)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        if (type != null && !type.isEmpty()) {
            updateWrapper.eq(Notification::getType, type);
        }
        this.update(updateWrapper);
        return ResultBean.success("标记已读成功");
    }

    @Override
    public ResultBean markAllRead(Integer userId) {
        return markRead(userId, null);
    }

    @Override
    public ResultBean createNotification(Integer userId, Integer fromUserId, String type,
                                          String title, String relatedType, Integer relatedId) {
        if (userId == null || type == null) {
            return ResultBean.error("参数不能为空");
        }
        // 不给自己发通知
        if (userId.equals(fromUserId)) {
            return ResultBean.success("跳过自身通知");
        }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setFromUserId(fromUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(format.format(date));

        this.save(notification);
        return ResultBean.success("通知已创建");
    }
}
