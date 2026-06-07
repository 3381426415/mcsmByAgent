package com.gitcode.mcsm_backend.agent.communication;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Agent 广播机制 - 一个 Agent 的发现，所有相关 Agent 立即收到
 */
@Slf4j
public class AgentBroadcast {

    private final Map<String, List<BiConsumer<String, BroadcastMessage>>> listeners = new ConcurrentHashMap<>();

    public enum BroadcastType {
        CONFLICT_FOUND,
        DEPENDENCY_MISSING,
        KNOWLEDGE_UPDATE,
        USER_DIRECTIVE_CHANGE
    }

    public void subscribe(String agentId, BiConsumer<String, BroadcastMessage> handler) {
        listeners.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void unsubscribe(String agentId) {
        listeners.remove(agentId);
    }

    public void emit(String senderId, BroadcastType type, String message) {
        BroadcastMessage broadcastMessage = new BroadcastMessage(senderId, type, message, System.currentTimeMillis());
        log.info("[Broadcast] {} -> {}: {}", senderId, type, message);

        for (Map.Entry<String, List<BiConsumer<String, BroadcastMessage>>> entry : listeners.entrySet()) {
            if (!entry.getKey().equals(senderId)) {
                for (BiConsumer<String, BroadcastMessage> handler : entry.getValue()) {
                    try {
                        handler.accept(senderId, broadcastMessage);
                    } catch (Exception e) {
                        log.error("[Broadcast] 处理广播失败: agentId={}", entry.getKey(), e);
                    }
                }
            }
        }
    }

    public record BroadcastMessage(String senderId, BroadcastType type, String message, long timestamp) {}
}
