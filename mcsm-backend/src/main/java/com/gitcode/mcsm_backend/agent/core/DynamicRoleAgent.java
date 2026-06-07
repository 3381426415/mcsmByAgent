package com.gitcode.mcsm_backend.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.ai.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 动态 Role Agent — 配置分析/诊断
 * 内部 ReAct 循环：LLM 自主决定调用什么工具来完成分析
 */
@Slf4j
public class DynamicRoleAgent extends SubAgent {

    private final LlmClient llmClient;
    private final String taskDescription;
    private final ToolExecutor toolExecutor;
    private final List<Map<String, Object>> toolDefs;
    private final int maxTurns;

    public DynamicRoleAgent(LlmClient llmClient, String taskDescription,
                             ToolRegistry toolRegistry, ToolExecutor toolExecutor, int maxTurns) {
        super(AgentType.ROLE, ModelTier.FLASH, AgentPromptRegistry.ROLE_AGENT_BASE_PROMPT);
        this.llmClient = llmClient;
        this.taskDescription = taskDescription;
        this.toolExecutor = toolExecutor;
        this.toolDefs = toolRegistry.getAllTools();
        this.maxTurns = maxTurns;
    }

    @Override
    public AgentResult execute() {
        emitAgentStart("配置分析: " + taskDescription);

        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", taskDescription));

            for (int turn = 0; turn < maxTurns; turn++) {
                LlmResponse resp = llmClient.chat(messages, toolDefs, modelTier.name());

                if (resp.isToolCall()) {
                    messages.add(buildAssistantMessage(resp));

                    for (LlmResponse.ToolCall tc : resp.getToolCalls()) {
                        emitToolCall(tc.getFunctionName(), tc.getFunctionArguments());

                        Map<String, Object> args = new ObjectMapper()
                                .readValue(tc.getFunctionArguments(), Map.class);
                        String result = toolExecutor.execute(tc.getFunctionName(), args);

                        emitToolResult(tc.getFunctionName(), true,
                                result.length() > 200 ? result.substring(0, 200) + "..." : result);

                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", tc.getId(),
                                "content", result));
                    }
                } else {
                    String output = resp.getContent();
                    emitAgentDone(output);
                    return AgentResult.ok(agentId, type, output);
                }
            }

            String msg = "超过最大推理轮次 (" + maxTurns + ")";
            emitAgentError(msg);
            return AgentResult.fail(agentId, type, msg);
        } catch (Exception e) {
            log.error("[DynamicRoleAgent] 执行失败: {}", e.getMessage());
            emitAgentError(e.getMessage());
            return AgentResult.fail(agentId, type, e.getMessage());
        }
    }

    private Map<String, Object> buildAssistantMessage(LlmResponse resp) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", resp.getContent() != null ? resp.getContent() : "");
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (LlmResponse.ToolCall tc : resp.getToolCalls()) {
            toolCalls.add(Map.of(
                    "id", tc.getId(),
                    "type", "function",
                    "function", Map.of(
                            "name", tc.getFunctionName(),
                            "arguments", tc.getFunctionArguments())));
        }
        msg.put("tool_calls", toolCalls);
        return msg;
    }
}
