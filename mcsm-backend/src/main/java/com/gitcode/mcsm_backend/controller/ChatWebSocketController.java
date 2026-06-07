package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.service.AgentChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 聊天 WebSocket 控制器 - 接收前端聊天消息，直接调用本地 AgentCore 处理
 */
@Controller
public class ChatWebSocketController {

    private final AgentChatService agentChatService;

    public ChatWebSocketController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @MessageMapping("/chat.send")
    public void send(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.sendAsync(body, principal.getName());
    }

    @MessageMapping("/chat.confirm")
    public void confirm(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.confirmAsync(body, principal.getName());
    }

    @MessageMapping("/chat.destroy")
    public void destroy(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.destroyAsync(body, principal.getName());
    }

    // ==================== Agent 聊天端点（兼容旧路径） ====================

    @MessageMapping("/agent.send")
    public void agentSend(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.sendAsync(body, principal.getName());
    }

    @MessageMapping("/agent.confirm")
    public void agentConfirm(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.confirmAsync(body, principal.getName());
    }

    @MessageMapping("/agent.destroy")
    public void agentDestroy(Map<String, Object> body, Principal principal) {
        if (principal == null) return;
        agentChatService.destroyAsync(body, principal.getName());
    }
}
