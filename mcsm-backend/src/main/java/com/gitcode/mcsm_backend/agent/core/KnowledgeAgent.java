package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.LlmClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Knowledge Agent - 知识提取和建库，异步触发
 */
@Slf4j
public class KnowledgeAgent extends SubAgent {

    private final LlmClient llmClient;
    private final String operationData;

    public KnowledgeAgent(LlmClient llmClient, String operationData) {
        super(AgentType.KNOWLEDGE, ModelTier.FLASH, AgentPromptRegistry.KNOWLEDGE_AGENT_PROMPT);
        this.llmClient = llmClient;
        this.operationData = operationData;
    }

    @Override
    public AgentResult execute() {
        emitAgentStart("知识提取");

        try {
            String dynamicPrompt = systemPrompt + "\n\n--- 操作数据 ---\n" + operationData;
            String result = llmClient.chat(dynamicPrompt, List.of(), modelTier);

            if (result == null || result.isBlank()) {
                emitAgentError("知识提取失败：模型无响应");
                return AgentResult.fail(agentId, type, "模型无响应");
            }

            emitAgentDone(result);
            return AgentResult.ok(agentId, type, result);
        } catch (Exception e) {
            log.error("[KnowledgeAgent] 执行失败: {}", e.getMessage(), e);
            emitAgentError(e.getMessage());
            return AgentResult.fail(agentId, type, e.getMessage());
        }
    }
}
