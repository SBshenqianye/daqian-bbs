package com.walker.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.walker.mapper.NotificationMapper;
import com.walker.pojo.Notification;
import com.walker.pojo.NotificationCategory;
import com.walker.service.NotificationService;
import com.walker.vo.ResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Object> getUnreadSummary(Integer userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        Map<String, Integer> byType = new LinkedHashMap<>();
        if (userId == null) {
            summary.put("total", 0);
            summary.put("byCategory", byCategory);
            summary.put("byType", byType);
            return summary;
        }

        // 分分类未读计数（红点/角标数据源，相互独立且可加）
        List<Map<String, Object>> catRows = notificationMapper.countUnreadGroupByCategory(userId);
        int categorySum = 0;
        for (Map<String, Object> row : catRows) {
            String category = String.valueOf(row.get("category"));
            int cnt = ((Number) row.get("cnt")).intValue();
            byCategory.put(category, cnt);
            categorySum += cnt;
        }

        // 分类型未读计数（细粒度数据源，供未来细分展示）
        List<Map<String, Object>> typeRows = notificationMapper.countUnreadGroupByType(userId);
        for (Map<String, Object> row : typeRows) {
            String type = String.valueOf(row.get("type"));
            int cnt = ((Number) row.get("cnt")).intValue();
            byType.put(type, cnt);
        }

        // total 直接 COUNT 全表未读，保证与分类计数天然可加
        int total = notificationMapper.countUnreadByUserId(userId);
        // 防御：历史数据 category 为空等极端情况下，以分类之和为准
        if (total != categorySum) {
            total = categorySum;
        }

        summary.put("total", total);
        summary.put("byCategory", byCategory);
        summary.put("byType", byType);
        return summary;
    }

    @Override
    public ResultBean markRead(Integer userId, String type, String category) {
        if (userId == null) {
            return ResultBean.error("用户ID不能为空");
        }
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Notification::getIsRead, 1)
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0);
        if (category != null && !category.isEmpty()) {
            // 按分类标记：只清该分类，不影响其他分类的未读状态
            updateWrapper.eq(Notification::getCategory, category);
        } else if (type != null && !type.isEmpty()) {
            // 按单一类型标记（兼容旧接口）
            updateWrapper.eq(Notification::getType, type);
        }
        this.update(updateWrapper);
        return ResultBean.success("标记已读成功");
    }

    @Override
    public ResultBean markRead(Integer userId, String type) {
        return markRead(userId, type, null);
    }

    @Override
    public ResultBean markAllRead(Integer userId) {
        return markRead(userId, null, null);
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
        // 按注册表归组分类，未注册类型兜底为系统通知
        notification.setCategory(NotificationCategory.categoryOf(type));
        notification.setTitle(title);
        notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(format.format(date));

        this.save(notification);
        return ResultBean.success("通知已创建");
    }
}
