package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.Notification;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口 - 用户查看、已读、删除通知消息
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/list")
    public Result<Page<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Page<Notification> data = notificationService.getUserNotifications(currentUser.getId(), page, size);
        return Result.success("获取成功", data);
    }

    /**
     * 获取未读数量
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long count = notificationService.getUnreadCount(currentUser.getId());
        return Result.success("获取成功", count);
    }

    /**
     * 标记单条为已读
     */
    @PostMapping("/read/{id}")
    public Result<String> markAsRead(@PathVariable Long id) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        notificationService.markAsRead(id, currentUser.getId());
        return Result.successMsg("已标记为已读");
    }

    /**
     * 标记全部为已读
     */
    @PostMapping("/read-all")
    public Result<String> markAllAsRead() {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        notificationService.markAllAsRead(currentUser.getId());
        return Result.successMsg("全部已读");
    }
}