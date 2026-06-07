package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.LlmClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Summarizer Agent - 检查点验证 + 结果压缩
 */
@Slf4j
public class SummarizerAgent extends SubAgent {

    private final LlmClient llmClient;
    private final String stageResults;

    public SummarizerAgent(LlmClient llmClient, String stageResults) {
        super(AgentType.SUMMARIZER, ModelTier.FLASH, AgentPromptRegistry.SUMMARIZER_AGENT_PROMPT);
        this.llmClient = llmClient;
        this.stageResults = stageResults;
    }

    @Override
    public AgentResult execute() {
        emitAgentStart("结果压缩");

        try {
            String dynamicPrompt = systemPrompt + "\n\n--- 阶段结果 ---\n" + stageResults;
            String result = llmClient.chat(dynamicPrompt, List.of(), modelTier);

            if (result == null || result.isBlank()) {
                emitAgentError("压缩失败：模型无响应");
                return AgentResult.fail(agentId, type, "模型无响应");
            }

            emitAgentDone(result);
            return AgentResult.ok(agentId, type, result);
        } catch (Exception e) {
            log.error("[SummarizerAgent] 执行失败: {}", e.getMessage());
            emitAgentError(e.getMessage());
            return AgentResult.fail(agentId, type, e.getMessage());
        }
    }
}
