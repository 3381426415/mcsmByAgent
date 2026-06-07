package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.LlmClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Planner Agent - 全局规划，输出变更方案
 */
@Slf4j
public class PlannerAgent extends SubAgent {

    private final LlmClient llmClient;
    private final String taskDescription;
    private final List<String> serverInfo;

    public PlannerAgent(LlmClient llmClient, String taskDescription, List<String> serverInfo) {
        super(AgentType.PLANNER, ModelTier.PRO, AgentPromptRegistry.PLANNER_AGENT_PROMPT);
        this.llmClient = llmClient;
        this.taskDescription = taskDescription;
        this.serverInfo = serverInfo;
    }

    @Override
    public AgentResult execute() {
        emitAgentStart("全局规划: " + taskDescription);

        try {
            String dynamicPrompt = systemPrompt + "\n\n--- 动态注入 ---\n"
                    + "## 用户请求\n" + taskDescription + "\n"
                    + "## 服务器信息\n" + String.join("\n", serverInfo);

            String result = llmClient.chat(dynamicPrompt, List.of(), modelTier);

            if (result == null || result.isBlank()) {
                emitAgentError("规划失败：模型无响应");
                return AgentResult.fail(agentId, type, "模型无响应");
            }

            emitAgentDone(result);
            return AgentResult.ok(agentId, type, result);
        } catch (Exception e) {
            log.error("[PlannerAgent] 执行失败: {}", e.getMessage());
            emitAgentError(e.getMessage());
            return AgentResult.fail(agentId, type, e.getMessage());
        }
    }
}
