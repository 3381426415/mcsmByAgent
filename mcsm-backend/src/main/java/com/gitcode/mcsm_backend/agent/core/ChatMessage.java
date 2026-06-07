package com.gitcode.mcsm_backend.agent.core;

import java.util.List;
import java.util.Map;

/**
 * 聊天消息 - 封装单条对话消息的角色、内容、时间戳和工具调用 ID
 */
public class ChatMessage {
    private String role;
    private String content;
    private long timestamp;
    private String toolCallId;
    private List<Map<String, Object>> toolCalls;

    public ChatMessage() {}

    public ChatMessage(String role, String content, long timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, System.currentTimeMillis());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, System.currentTimeMillis());
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, System.currentTimeMillis());
    }

    public static ChatMessage tool(String toolCallId, String content) {
        ChatMessage msg = new ChatMessage("tool", content, System.currentTimeMillis());
        msg.setToolCallId(toolCallId);
        return msg;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }
    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<Map<String, Object>> toolCalls) { this.toolCalls = toolCalls; }
}
