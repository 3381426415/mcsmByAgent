package com.gitcode.mcsm_backend.agent.ai;

import com.gitcode.mcsm_backend.agent.core.ChatMessage;
import com.gitcode.mcsm_backend.agent.core.ModelTier;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 上下文窗口管理器 — 基于 token 估算的智能历史管理
 *
 * 当历史消息 token 总量超过预算的 80% 时，自动将最旧的一半消息压缩为摘要。
 */
@Slf4j
public class ContextManager {

    private final int maxTokens;
    private final int compressThreshold;
    private final TokenEstimator estimator;
    private final LlmClient llmClient;

    public ContextManager(int maxTokens, TokenEstimator estimator, LlmClient llmClient) {
        this.maxTokens = maxTokens;
        this.compressThreshold = (int) (maxTokens * 0.8);
        this.estimator = estimator;
        this.llmClient = llmClient;
    }

    /**
     * 检查是否需要压缩
     */
    public boolean needsCompression(List<ChatMessage> messages) {
        return estimator.estimateMessages(messages) > compressThreshold;
    }

    /**
     * 获取当前 token 用量
     */
    public int getCurrentTokens(List<ChatMessage> messages) {
        return estimator.estimateMessages(messages);
    }

    /**
     * 获取上下文使用百分比
     */
    public int getUsagePercent(List<ChatMessage> messages) {
        int tokens = estimator.estimateMessages(messages);
        if (tokens == 0) return 0;
        return Math.min(100, Math.max(1, (int) Math.round((tokens * 100.0) / maxTokens)));
    }

    /**
     * 执行压缩：取最旧一半非系统消息 → 调 flash 模型摘要 → 替换
     */
    public List<ChatMessage> compress(List<ChatMessage> messages) {
        if (messages == null || messages.size() <= 2) return messages;

        // 分离系统消息和非系统消息
        List<ChatMessage> systemMessages = new ArrayList<>();
        List<ChatMessage> nonSystemMessages = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemMessages.add(msg);
            } else {
                nonSystemMessages.add(msg);
            }
        }

        if (nonSystemMessages.size() <= 2) return messages;

        // 取最旧的一半做摘要
        int halfSize = nonSystemMessages.size() / 2;
        List<ChatMessage> toCompress = nonSystemMessages.subList(0, halfSize);
        List<ChatMessage> toKeep = nonSystemMessages.subList(halfSize, nonSystemMessages.size());

        // 拼接待压缩消息为文本
        StringBuilder conversationText = new StringBuilder();
        for (ChatMessage msg : toCompress) {
            String role = switch (msg.getRole()) {
                case "user" -> "用户";
                case "assistant" -> "助手";
                case "tool" -> "工具结果";
                default -> msg.getRole();
            };
            conversationText.append("[").append(role).append("] ");
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                // 截断过长的工具结果
                String content = msg.getContent();
                if (content.length() > 2000) {
                    content = content.substring(0, 2000) + "...(已截断)";
                }
                conversationText.append(content);
            }
            if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                conversationText.append(" [调用了工具]");
            }
            conversationText.append("\n");
        }

        // 调 flash 模型生成摘要
        String summary;
        try {
            summary = llmClient.chatWithSystemPrompt(
                    "你是一个对话历史压缩器。请将以下对话历史压缩为简洁的中文摘要，保留：用户的意图和需求、关键操作和工具调用结果、重要的配置变更或发现。丢弃中间过程和重复信息。直接输出摘要，不要加前缀。",
                    List.of(Map.of("role", "user", "content", conversationText.toString())),
                    ModelTier.FLASH.name()
            );
        } catch (Exception e) {
            log.error("[ContextManager] 摘要生成失败，直接截断: {}", e.getMessage());
            // 降级：直接丢弃旧消息，不做摘要
            List<ChatMessage> result = new ArrayList<>(systemMessages);
            result.addAll(toKeep);
            return result;
        }

        if (summary == null || summary.isBlank()) {
            summary = "（摘要生成失败，早期对话已省略）";
        }

        // 构造压缩后的消息列表：系统消息 + 摘要 + 保留的消息
        List<ChatMessage> result = new ArrayList<>(systemMessages);
        result.add(ChatMessage.user("[历史摘要]\n" + summary));
        result.addAll(toKeep);

        int beforeTokens = estimator.estimateMessages(messages);
        int afterTokens = estimator.estimateMessages(result);
        log.info("[ContextManager] 压缩完成: {}条 → {}条, token: {} → {} ({}%)", messages.size(), result.size(),
                beforeTokens, afterTokens, afterTokens * 100 / maxTokens);

        return result;
    }
}
