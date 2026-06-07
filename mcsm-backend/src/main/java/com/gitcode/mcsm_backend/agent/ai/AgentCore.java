package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.memory.KnowledgeInjector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Agent 智能体核心 — 两阶段执行模式
 *
 * 借鉴 Claude Code 的工作方式：
 * 阶段1: 规划 — 不带工具，让模型理解任务并输出计划
 * 阶段2: 执行 — 带工具，逐步执行计划
 *
 * 危险操作（stop/restart/delete）需要用户确认后才执行。
 */
@Slf4j
public class AgentCore {

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ChatHistory chatHistory;
    private final ObjectMapper objectMapper;
    private KnowledgeInjector knowledgeInjector;
    private AgentEventListener eventListener;
    private boolean isLlmError = false;

    private static final Set<String> DANGEROUS_TOOLS = Set.of(
            "stop_server", "restart_server", "delete_file"
    );

    public AgentCore(LlmClient llmClient,
                     ToolExecutor toolExecutor,
                     ToolRegistry toolRegistry,
                     ChatHistory chatHistory) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        this.chatHistory = chatHistory;
        this.objectMapper = new ObjectMapper();
    }

    public void setKnowledgeInjector(KnowledgeInjector knowledgeInjector) {
        this.knowledgeInjector = knowledgeInjector;
    }

    public void setEventListener(AgentEventListener eventListener) {
        this.eventListener = eventListener;
    }

    // ==================== 公开 API ====================

    public String chat(String sessionId, String userMessage) {
        return chat(sessionId, userMessage, null);
    }

    public String chat(String sessionId, String userMessage, String model) {
        // 如果有知识注入器，注入相关知识到系统提示词
        if (knowledgeInjector != null) {
            String basePrompt = chatHistory.getBaseSystemPrompt();
            String enhancedPrompt = knowledgeInjector.inject(basePrompt, userMessage, "chat", null);
            chatHistory.getOrCreate(sessionId, enhancedPrompt);
        }

        chatHistory.addMessage(sessionId, "user", userMessage);

        // 直接进入执行阶段（带工具），让模型自己决定是否调用工具
        log.info("[AgentCore] === 执行阶段 ===");
        return executionPhase(sessionId, model);
    }

    /**
     * 带确认参数的执行（用于危险操作确认后的继续执行）
     */
    public String continueWithConfirmation(String sessionId, boolean confirmed, String model) {
        if (!confirmed) {
            String reply = "操作已取消。";
            chatHistory.addMessage(sessionId, "assistant", reply);
            return reply;
        }
        // 用户已确认，告诉模型继续执行
        chatHistory.addMessage(sessionId, "user", "我确认，请继续执行。");
        return executionPhase(sessionId, model);
    }

    // ==================== 阶段1: 规划 ====================

    /**
     * 不带工具调用 LLM，让模型：
     * 1. 理解任务
     * 2. 反问确认（如果信息不足）
     * 3. 输出计划文本（如果信息充分）
     */
    private String planningPhase(String sessionId, String model) {
        List<Map<String, Object>> messages = chatHistory.getOrCreate(sessionId);

        // 关键：传入 null 作为 tools，强制模型只用文本回复
        LlmResponse response = llmClient.chat(messages, null, model);

        if (response.isError()) {
            String error = response.getContent();
            log.error("[AgentCore] LLM 调用失败: {}", error);
            chatHistory.addMessage(sessionId, "assistant", error);
            isLlmError = true;
            emitError(error);
            return error;
        }

        String content = response.getContent();
        if (content == null || content.isEmpty()) {
            content = "收到，让我想想…";
        }

        chatHistory.addMessage(sessionId, "assistant", content);
        log.info("[AgentCore] 规划输出: {}", content.substring(0, Math.min(200, content.length())));
        return content;
    }

    /**
     * 判断规划输出是否需要进入执行阶段
     */
    private boolean isSimpleReply(String planText) {
        if (planText == null || planText.isEmpty()) return true;

        if (planText.contains("【计划】") || planText.contains("计划】")
                || planText.contains("步骤") || planText.contains("接下来")) {
            if (planText.contains("？") && !planText.contains("【计划】")) {
                return true;
            }
            return false;
        }

        return true;
    }

    // ==================== 阶段2: 执行 ====================

    /**
     * 带工具执行 ReAct 循环
     */
    private static final int MAX_EXEC_TURNS = 15;

    private String executionPhase(String sessionId, String model) {
        List<Map<String, Object>> allTools = toolRegistry.getAllTools();

        int turn = 0;
        while (turn < MAX_EXEC_TURNS) {
            turn++;
            log.info("[AgentCore] 执行第 {} 轮", turn);

            List<Map<String, Object>> messages = chatHistory.getOrCreate(sessionId);

            LlmResponse response = llmClient.chat(messages, allTools, model);

            // LLM 调用失败（基础设施错误，不可控）
            if (response.isError()) {
                String error = response.getContent();
                log.error("[AgentCore] LLM 调用失败: {}", error);
                chatHistory.addMessage(sessionId, "assistant", error);
                emitError(error);
                return error;
            }

            // 模型决定回复文本（不再需要工具）→ 结束
            if (!response.isToolCall()) {
                String content = response.getContent();
                if (content == null || content.isBlank()) {
                    // 不报错，注入提示让 LLM 重试
                    log.info("[AgentCore] content 为空，引导 LLM 重试");
                    chatHistory.addMessage(sessionId, "assistant", "");
                    chatHistory.addMessage(sessionId, "user", "你没有回复任何内容。请直接使用工具执行操作，或用中文回复用户。");
                    continue;
                }
                chatHistory.addMessage(sessionId, "assistant", content);
                emitReplyDone(content);
                return content;
            }

            // 有工具调用时，先发射思考事件（模型可能在调用工具前有文本输出）
            if (response.getContent() != null && !response.getContent().isBlank()) {
                emitThinking(response.getContent());
            }

            // 处理工具调用
            List<LlmResponse.ToolCall> toolCalls = response.getToolCalls();
            log.info("[AgentCore] 调用工具数: {}", toolCalls.size());

            chatHistory.addToolCallMessage(sessionId, toolCalls);

            for (LlmResponse.ToolCall tc : toolCalls) {
                String toolName = tc.getFunctionName();
                Map<String, Object> args = parseArguments(tc.getFunctionArguments());

                log.info("[AgentCore] 执行: {}({})", toolName, args);

                // 发射工具调用事件
                emitToolCall(toolName, args);

                // 检查危险操作
                if (DANGEROUS_TOOLS.contains(toolName)) {
                    String confirmMsg = "即将执行危险操作: " + toolName
                            + "\n参数: " + args
                            + "\n请确认是否继续？（回复 确认 或 取消）";
                    chatHistory.addMessage(sessionId, "assistant", confirmMsg);
                    return "[CONFIRM_REQUIRED]" + confirmMsg;
                }

                String result;
                try {
                    result = toolExecutor.execute(toolName, args);
                } catch (Exception e) {
                    log.error("[AgentCore] 工具异常: {}", e.getMessage());
                    result = "工具执行异常: " + e.getMessage();
                }

                // 发射工具结果事件
                boolean success = !result.contains("错误") && !result.contains("失败") && !result.contains("异常");
                emitToolResult(toolName, success, result);

                chatHistory.addToolResult(sessionId, tc.getId(), result);
            }
        }
        // 超过最大轮次，让 LLM 总结
        chatHistory.addMessage(sessionId, "user", "你已执行多轮操作。请简短总结已完成的工作和当前状态。");
        try {
            List<Map<String, Object>> messages = chatHistory.getOrCreate(sessionId);
            LlmResponse summaryResp = llmClient.chat(messages, null, model);
            String summary = summaryResp.getContent();
            if (summary != null && !summary.isBlank()) {
                emitReplyDone(summary);
                return summary;
            }
        } catch (Exception ignored) {}
        String fallback = "操作已执行，请查看上方的工具调用结果。";
        emitReplyDone(fallback);
        return fallback;
    }

    // ==================== 事件发射 ====================

    private void emitThinking(String content) {
        if (eventListener != null) {
            try { eventListener.onThinking(content); } catch (Exception e) { /* ignore */ }
        }
    }

    private void emitToolCall(String toolName, Map<String, Object> arguments) {
        if (eventListener != null) {
            try { eventListener.onToolCall(toolName, arguments); } catch (Exception e) { /* ignore */ }
        }
    }

    private void emitToolResult(String toolName, boolean success, String result) {
        if (eventListener != null) {
            try { eventListener.onToolResult(toolName, success, result); } catch (Exception e) { /* ignore */ }
        }
    }

    private void emitReplyDone(String content) {
        if (eventListener != null) {
            try { eventListener.onReplyDone(content); } catch (Exception e) { /* ignore */ }
        }
    }

    private void emitError(String error) {
        if (eventListener != null) {
            try { eventListener.onError(error); } catch (Exception e) { /* ignore */ }
        }
    }

    // ==================== 工具方法 ====================

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson,
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("[AgentCore] 解析参数失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public void destroySession(String sessionId) {
        chatHistory.destroy(sessionId);
    }

    public int getActiveSessions() {
        return chatHistory.getActiveSessionCount();
    }
}
