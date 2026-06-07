package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.Notification;
import com.gitcode.mcsm_backend.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

/**
 * 通知服务 - 创建、查询、已读、删除用户通知消息
 */
@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    /**
     * 添加通知
     */
    public void addNotification(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        this.save(notification);
    }

    /**
     * 获取用户的通知列表（分页）
     */
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Page<Notification> pageParam = new Page<>(page, size);
        return this.page(pageParam,
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
    }

    /**
     * 获取未读数量
     */
    public Long getUnreadCount(Long userId) {
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    /**
     * 标记单条为已读
     */
    public void markAsRead(Long notificationId, Long userId) {
        this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    /**
     * 标记全部为已读
     */
    public void markAllAsRead(Long userId) {
        this.update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }
}