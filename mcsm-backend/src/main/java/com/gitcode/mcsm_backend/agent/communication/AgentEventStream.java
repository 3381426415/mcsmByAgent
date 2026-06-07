package com.gitcode.mcsm_backend.agent.communication;

import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 事件流 - 将执行过程推送到前端
 * 直接通过 STOMP 推送，带 seq 序号支持 ACK 确认和断线恢复
 */
public class AgentEventStream {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionEventBuffer eventBuffer;
    private final ConcurrentHashMap<String, AtomicInteger> sessionSeqCounters = new ConcurrentHashMap<>();

    public AgentEventStream(SimpMessagingTemplate messagingTemplate, SessionEventBuffer eventBuffer) {
        this.messagingTemplate = messagingTemplate;
        this.eventBuffer = eventBuffer;
    }

    public void emitAgentStart(String userId, String agentId, String agentType, String task, String parentAgentId) {
        Map<String, Object> data = new HashMap<>();
        data.put("agentId", agentId);
        data.put("agentType", agentType);
        data.put("task", task);
        if (parentAgentId != null) {
            data.put("parentAgentId", parentAgentId);
        }
        emit(userId, "AGENT_START", data);
    }

    public void emitToolCall(String userId, String agentId, String toolName, String args) {
        emit(userId, "TOOL_CALL", Map.of(
                "agentId", agentId, "toolName", toolName, "args", args));
    }

    public void emitToolResult(String userId, String agentId, String toolName,
                                boolean success, String summary) {
        emit(userId, "TOOL_RESULT", Map.of(
                "agentId", agentId, "toolName", toolName,
                "success", success, "summary", summary));
    }

    public void emitAgentDone(String userId, String agentId, String result) {
        emit(userId, "AGENT_DONE", Map.of("agentId", agentId, "result", result));
    }

    public void emitAgentError(String userId, String agentId, String error) {
        emit(userId, "AGENT_ERROR", Map.of("agentId", agentId, "error", error));
    }

    public void emitThinking(String userId, String content) {
        emit(userId, "THINKING", Map.of("content", content));
    }

    public void emitReplyChunk(String userId, String content) {
        emit(userId, "REPLY_CHUNK", Map.of("content", content));
    }

    public void emitReplyDone(String userId, String content) {
        emit(userId, "REPLY_DONE", Map.of("content", content));
    }

    public void emitStatus(String userId, String message) {
        emit(userId, "STATUS", Map.of("message", message));
    }

    public void emitError(String userId, String message) {
        emit(userId, "ERROR", Map.of("message", message));
    }

    public void emitChoice(String userId, String choiceId, String title,
                            List<ChoiceOption> options) {
        Map<String, Object> data = new HashMap<>();
        data.put("choiceId", choiceId);
        data.put("title", title);
        data.put("options", options);
        emit(userId, "CHOICE", data);
    }

    public void emitContextInfo(String userId, int percent) {
        emit(userId, "CONTEXT_INFO", Map.of("percent", percent));
    }

    public void emit(String userId, String type, Object data) {
        if (messagingTemplate != null) {
            int seq = nextSeq(userId);

            // 构造带 seq 的 wrapped data
            Map<String, Object> wrappedData = new HashMap<>();
            if (data instanceof Map) {
                wrappedData.putAll((Map<String, Object>) data);
            }
            wrappedData.put("seq", seq);

            // 缓存带 seq 的数据，replay 时才有 seq 字段
            if (eventBuffer != null) {
                eventBuffer.buffer(userId, seq, type, wrappedData);
            }

            Map<String, Object> message = Map.of("type", type, "data", wrappedData);
            messagingTemplate.convertAndSend("/user/" + userId + "/queue/agent", message);
        }
    }

    private int nextSeq(String sessionId) {
        AtomicInteger counter = sessionSeqCounters.computeIfAbsent(sessionId, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }
}
