package com.gitcode.mcsm_backend.event;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
/**
 * 通知事件 - Spring 事件载体，用于异步创建通知并推送给前端
 */
public class NotificationEvent {
    private Long userId;
    private String type;
    private String title;
    private String content;
}