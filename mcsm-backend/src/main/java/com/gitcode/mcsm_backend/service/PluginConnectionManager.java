package com.gitcode.mcsm_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 插件 WebSocket 连接管理器
 * 维护 serverId → WebSocketSession 映射，支持命令发送与响应等待
 */
@Slf4j
@Service
public class PluginConnectionManager {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** serverId → WebSocket session */
    private final Map<String, WebSocketSession> connections = new ConcurrentHashMap<>();

    /** serverId → 插件信息 */
    private final Map<String, PluginInfo> pluginInfos = new ConcurrentHashMap<>();

    /** requestId → 待响应的 Future */
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();

    // ==================== 注册 / 注销 ====================

    public void register(String serverId, WebSocketSession session, String version) {
        connections.put(serverId, session);
        pluginInfos.put(serverId, new PluginInfo(serverId, version, System.currentTimeMillis()));
        log.info("[PluginWS] 插件已注册: {} (v{})", serverId, version);
    }

    public void unregister(String serverId) {
        connections.remove(serverId);
        pluginInfos.remove(serverId);
        log.info("[PluginWS] 插件已断开: {}", serverId);
    }

    // ==================== 查询 ====================

    public WebSocketSession getSession(String serverId) {
        return connections.get(serverId);
    }

    public boolean isConnected(String serverId) {
        WebSocketSession session = connections.get(serverId);
        return session != null && session.isOpen();
    }

    public Set<String> getConnectedServerIds() {
        return Collections.unmodifiableSet(connections.keySet());
    }

    public List<Map<String, Object>> listConnected() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (PluginInfo info : pluginInfos.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("serverId", info.serverId);
            map.put("version", info.version);
            map.put("connectedAt", info.connectedAt);
            map.put("online", isConnected(info.serverId));
            result.add(map);
        }
        return result;
    }

    // ==================== 命令发送 ====================

    /**
     * 向指定插件发送命令并同步等待响应
     * @param serverId 目标服务器 ID
     * @param action   命令动作（如 "getServerStatus"）
     * @param params   命令参数
     * @return 插件返回的结果
     */
    @SuppressWarnings("unchecked")
    public Result sendCommand(String serverId, String action, Map<String, Object> params) {
        WebSocketSession session = connections.get(serverId);
        if (session == null || !session.isOpen()) {
            return Result.error("插件未连接: " + serverId);
        }

        String requestId = UUID.randomUUID().toString();

        // 构造命令消息
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "command");
        message.put("requestId", requestId);
        message.put("action", action);
        message.put("params", params != null ? params : Map.of());

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }

            // 等待响应，超时 5 秒
            Map<String, Object> response = future.get(5, TimeUnit.SECONDS);

            int code = (int) response.getOrDefault("code", 3000);
            String msg = (String) response.getOrDefault("msg", "");
            Object data = response.get("data");

            if (code == 2000) {
                return Result.success(msg, data);
            } else {
                return Result.error(msg);
            }

        } catch (TimeoutException e) {
            return Result.error("插件响应超时: " + serverId);
        } catch (Exception e) {
            return Result.error("通信失败: " + e.getMessage());
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /**
     * 处理插件返回的响应（由 WebSocket Handler 调用）
     */
    public void handleResponse(String requestId, Map<String, Object> response) {
        CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response);
        }
    }

    /**
     * 向插件发送文本消息
     */
    public void sendMessage(String serverId, String message) throws IOException {
        WebSocketSession session = connections.get(serverId);
        if (session != null && session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    // ==================== 内部类 ====================

    public static class PluginInfo {
        public final String serverId;
        public final String version;
        public final long connectedAt;

        public PluginInfo(String serverId, String version, long connectedAt) {
            this.serverId = serverId;
            this.version = version;
            this.connectedAt = connectedAt;
        }
    }
}
