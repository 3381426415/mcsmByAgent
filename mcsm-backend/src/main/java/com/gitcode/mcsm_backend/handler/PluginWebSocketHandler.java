package com.gitcode.mcsm_backend.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.service.PluginConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

/**
 * 插件 WebSocket 事件处理器
 * 处理插件的连接、消息、断开事件
 */
@Slf4j
@Component
public class PluginWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PluginConnectionManager connectionManager;

    @Value("${plugin.secret:}")
    private String pluginSecret;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        Map<String, String> params = parseQueryParams(uri.getQuery());
        String secret = params.get("secret");
        String serverId = params.get("serverId");

        // 验证 secret
        if (pluginSecret != null && !pluginSecret.isEmpty()) {
            if (secret == null || !secret.equals(pluginSecret)) {
                log.error("[PluginWS] 认证失败，secret 不匹配");
                session.close(new CloseStatus(4001, "认证失败"));
                return;
            }
        }

        if (serverId == null || serverId.isBlank()) {
            log.error("[PluginWS] 缺少 serverId 参数");
            session.close(new CloseStatus(4002, "缺少 serverId"));
            return;
        }

        // 暂存 serverId，等 register 消息确认版本后再正式注册
        session.getAttributes().put("serverId", serverId);
        log.info("[PluginWS] 插件连接: {}", serverId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        String payload = textMessage.getPayload();
        Map<String, Object> message;
        try {
            message = objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("[PluginWS] 消息解析失败: {}", payload);
            return;
        }

        String type = (String) message.get("type");
        if (type == null) return;

        switch (type) {
            case "register" -> handleRegister(session, message);
            case "response" -> handleResponse(message);
            case "heartbeat" -> handleHeartbeat(session);
            default -> log.info("[PluginWS] 未知消息类型: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String serverId = (String) session.getAttributes().get("serverId");
        if (serverId != null) {
            connectionManager.unregister(serverId);
            log.info("[PluginWS] 插件断开: {} ({})", serverId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String serverId = (String) session.getAttributes().get("serverId");
        log.error("[PluginWS] 传输错误 [{}]: {}", serverId, exception.getMessage());
    }

    // ==================== 消息处理 ====================

    private void handleRegister(WebSocketSession session, Map<String, Object> message) {
        String serverId = (String) session.getAttributes().get("serverId");
        String version = (String) message.getOrDefault("version", "unknown");

        if (serverId == null) {
            log.error("[PluginWS] 注册失败：缺少 serverId");
            return;
        }

        // 如果同一个 serverId 已有旧连接，关闭旧的
        WebSocketSession oldSession = connectionManager.getSession(serverId);
        if (oldSession != null && oldSession.isOpen() && !oldSession.equals(session)) {
            try {
                oldSession.close(new CloseStatus(4003, "被新连接替换"));
            } catch (Exception ignored) {}
        }

        connectionManager.register(serverId, session, version);

        // 回复注册成功
        try {
            String ack = objectMapper.writeValueAsString(Map.of(
                    "type", "register_ack",
                    "serverId", serverId,
                    "msg", "注册成功"
            ));
            session.sendMessage(new TextMessage(ack));
        } catch (Exception e) {
            log.error("[PluginWS] 发送注册确认失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleResponse(Map<String, Object> message) {
        String requestId = (String) message.get("requestId");
        if (requestId == null) return;
        connectionManager.handleResponse(requestId, message);
    }

    private void handleHeartbeat(WebSocketSession session) {
        try {
            String ack = objectMapper.writeValueAsString(Map.of("type", "heartbeat_ack"));
            session.sendMessage(new TextMessage(ack));
        } catch (Exception ignored) {}
    }

    // ==================== 工具 ====================

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }
}
