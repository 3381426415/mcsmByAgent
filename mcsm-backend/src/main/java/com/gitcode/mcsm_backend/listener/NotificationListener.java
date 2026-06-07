package com.gitcode.mcsm_backend.listener;

import com.gitcode.mcsm_backend.event.NotificationEvent;
import com.gitcode.mcsm_backend.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知事件监听器 - 监听 NotificationEvent，持久化通知并通过 WebSocket 推送
 */
@Component
@Slf4j
public class NotificationListener {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpUserRegistry userRegistry;

    @EventListener
    @Async
    public void handleNotification(NotificationEvent event) {
        // 1. 写数据库
        try {
            notificationService.addNotification(
                    event.getUserId(),
                    event.getType(),
                    event.getTitle(),
                    event.getContent()
            );
        } catch (Exception e) {
            log.error("通知写入数据库失败，userId={}, type={}",
                    event.getUserId(), event.getType(), e);
            return;  // 数据库都没写进去，停止后续处理
        }

        // 2. 判断用户是否在线
        String userId = event.getUserId().toString();
        if (!isUserOnline(userId)) {
            log.debug("用户不在线，跳过WebSocket推送，userId={}", userId);
            return;
        }

        // 3. 在线则推送
        try {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notification",
                    Map.of(
                            "type", "NEW_NOTIFICATION",
                            "title", event.getTitle(),
                            "content", event.getContent()
                    )
            );
            log.debug("WebSocket推送成功，userId={}", userId);
        } catch (Exception e) {
            log.warn("WebSocket推送异常（用户在线但发送失败），userId={}", userId, e);
        }
    }

    private boolean isUserOnline(String userId) {
        SimpUser user = userRegistry.getUser(userId);
        return user != null && user.hasSessions();
    }
}