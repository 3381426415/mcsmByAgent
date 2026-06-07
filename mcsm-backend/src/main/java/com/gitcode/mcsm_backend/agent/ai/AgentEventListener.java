package com.gitcode.mcsm_backend.agent.ai;

import java.util.Map;

/**
 * Agent 执行过程事件监听器
 * 用于将 AgentCore 内部的工具调用、思考等中间事件推送到前端
 */
public interface AgentEventListener {

    void onThinking(String content);

    void onToolCall(String toolName, Map<String, Object> arguments);

    void onToolResult(String toolName, boolean success, String result);

    void onReplyChunk(String content);

    void onReplyDone(String content);

    void onError(String error);
}
