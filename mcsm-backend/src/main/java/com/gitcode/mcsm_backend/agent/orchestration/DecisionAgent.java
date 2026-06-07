package com.gitcode.mcsm_backend.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.communication.*;
import com.gitcode.mcsm_backend.agent.core.*;
import com.gitcode.mcsm_backend.agent.memory.KnowledgeInjector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 决策智能体 — 中枢调度
 * Pro 模型。ReAct 循环：LLM 自主决定调用什么工具，直到输出最终回复。
 */
@Slf4j
public class DecisionAgent {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final AgentEventStream eventStream;
    private final InteractionQueue interactionQueue;
    private final int maxTurns;
    private final Map<String, List<ChatMessage>> sessionHistories = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionLastAccess = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private KnowledgeInjector knowledgeInjector;
    private ContextManager contextManager;

    public DecisionAgent(LlmClient llmClient, ToolRegistry toolRegistry, ToolExecutor toolExecutor,
                          AgentEventStream eventStream, InteractionQueue interactionQueue, int maxTurns) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.eventStream = eventStream;
        this.interactionQueue = interactionQueue;
        this.maxTurns = maxTurns;
    }

    public void setKnowledgeInjector(KnowledgeInjector knowledgeInjector) {
        this.knowledgeInjector = knowledgeInjector;
    }

    public void setContextManager(ContextManager contextManager) {
        this.contextManager = contextManager;
    }

    /**
     * 处理用户消息 — ReAct 循环
     */
    public void handleUserMessage(String userId, String sessionId, String message) {
        List<ChatMessage> history = sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());
        sessionLastAccess.put(sessionId, System.currentTimeMillis());
        synchronized (history) {
            history.add(ChatMessage.user(message));

            if (history.size() > 20) {
                int keepStart = history.size() - 20;
                // 从截断点向前找安全边界，避免切断 tool_call/tool_result 配对
                while (keepStart > 0 && keepStart < history.size()
                        && !"user".equals(history.get(keepStart).getRole())) {
                    keepStart++;
                }
                if (keepStart >= history.size()) keepStart = history.size() - 20;
                List<ChatMessage> compressed = new ArrayList<>(history.subList(keepStart, history.size()));
                sessionHistories.put(sessionId, compressed);
                history = compressed;
            }
        }

        List<Map<String, Object>> toolDefs = toolRegistry.getAllTools();

        String systemPrompt = ChatHistory.SYSTEM_PROMPT;
        if (knowledgeInjector != null) {
            systemPrompt = knowledgeInjector.inject(systemPrompt, message, "chat", null);
        }

        // 发送初始上下文信息
        if (contextManager != null && eventStream != null) {
            eventStream.emitContextInfo(userId, contextManager.getUsagePercent(history));
        }

        for (int turn = 0; turn < maxTurns; turn++) {
            // LLM 调用前检查是否需要压缩
            if (contextManager != null) {
                synchronized (history) {
                    if (contextManager.needsCompression(history)) {
                        List<ChatMessage> compressed = contextManager.compress(history);
                        sessionHistories.put(sessionId, compressed);
                        history = compressed;
                    }
                }
                // 压缩后重新获取最新引用，防止其他线程已替换
                history = sessionHistories.get(sessionId);
            }

            LlmResponse resp;
            try {
                resp = llmClient.chatWithToolsStream(
                        systemPrompt, history, toolDefs, ModelTier.PRO,
                        // 文本 chunk → 流式回复
                        chunk -> {
                            if (eventStream != null) eventStream.emitReplyChunk(userId, chunk);
                        },
                        // reasoning chunk → 实时思考（过滤工具 JSON，压缩多余换行）
                        reasoning -> {
                            if (eventStream != null && !isToolCallJson(reasoning)) {
                                String cleaned = reasoning.replaceAll("\\n{3,}", "\n\n");
                                if (!cleaned.isBlank()) {
                                    eventStream.emitThinking(userId, cleaned);
                                }
                            }
                        });
            } catch (LlmClient.LlmException e) {
                log.error("[DecisionAgent] LLM 调用失败: {}", e.getMessage(), e);
                if (eventStream != null) eventStream.emitError(userId, e.getMessage());
                return;
            }

            // 调试：每轮 LLM 响应状态
            log.info("[DecisionAgent] 第 {}轮 - toolCall={}, content长度={}, reasoning长度={}",
                    turn + 1, resp.isToolCall(),
                    resp.getContent() != null ? resp.getContent().length() : "null",
                    resp.getReasoningContent() != null ? resp.getReasoningContent().length() : "null");

            if (!resp.isToolCall()) {
                String content = resp.getContent();
                String reasoning = resp.getReasoningContent();

                // 处理 "AI 未返回有效响应" 但 reasoning 中有工具调用的情况
                if (content != null && content.contains("AI 未返回有效响应") && reasoning != null && !reasoning.isBlank()) {
                    log.info("[DecisionAgent] content 为 fallback，尝试从 reasoning 提取工具调用");
                    List<LlmResponse.ToolCall> extracted = extractToolCallsFromReasoning(reasoning);
                    if (!extracted.isEmpty()) {
                        log.info("[DecisionAgent] 从 reasoning 提取到 {} 个工具调用，继续执行", extracted.size());
                        resp = LlmResponse.toolCalls("", reasoning, extracted);
                        // 继续到下面的工具调用处理逻辑
                    } else {
                        // reasoning 中没有工具调用，尝试用 reasoning 中的纯文本作为回复
                        String cleanReasoning = stripJsonFromReasoning(reasoning);
                        if (cleanReasoning != null && !cleanReasoning.isBlank()) {
                            log.info("[DecisionAgent] 使用 reasoning 中的纯文本作为回复");
                            if (eventStream != null) eventStream.emitReplyDone(userId, cleanReasoning);
                            return;
                        }
                        // 不直接放弃，注入提示让 LLM 重试
                        log.info("[DecisionAgent] reasoning 中无有效内容，引导 LLM 重试");
                        synchronized (history) {
                            history.add(ChatMessage.assistant(""));
                            history.add(ChatMessage.user("你没有正确回复。请直接调用工具执行用户的请求，不要输出空内容。"));
                        }
                        continue;
                    }
                }

                if (!resp.isToolCall()) {
                    if (content == null || content.isBlank()) {
                        log.info("[DecisionAgent] content 为空，尝试引导 LLM 使用工具");
                        // 不直接放弃，注入提示让 LLM 重试
                        synchronized (history) {
                            history.add(ChatMessage.assistant(""));
                            history.add(ChatMessage.user("你没有回复任何内容。请直接使用工具执行操作，不要只思考不行动。如果用户要求启动服务器，请调用 start_server 工具。"));
                        }
                        continue; // 回到循环重试
                    }

                    synchronized (history) {
                        history.add(ChatMessage.assistant(content));
                    }

                    String reply = extractReply(content);
                    if (eventStream != null) eventStream.emitReplyDone(userId, reply);
                    return;
                }
            }

            // 思考内容：reasoning_content 已通过回调实时推送
            // 这里处理 content 中可能存在的思考文本（兼容不支持 reasoning_content 的模型）
            if (eventStream != null) {
                String thinkingContent = resp.getContent();
                if (thinkingContent != null && !thinkingContent.isBlank()) {
                    String trimmed = thinkingContent.trim();
                    if (!(trimmed.startsWith("{") && trimmed.contains("\"tool_calls\""))) {
                        eventStream.emitThinking(userId, trimmed);
                    }
                }
            }

            synchronized (history) {
                ChatMessage assistantMsg = ChatMessage.assistant(resp.getContent() != null ? resp.getContent() : "");
                assistantMsg.setToolCalls(buildToolCallsList(resp));
                history.add(assistantMsg);
            }

            // 检测当轮是否调用了 think_more
            boolean hasThinkMore = resp.getToolCalls().stream()
                    .anyMatch(tc -> "think_more".equals(tc.getFunctionName()));

            AgentToolProvider.setCurrentContext(
                    new AgentToolProvider.ExecutionContext(eventStream, userId, "decision"));

            try {
                for (LlmResponse.ToolCall tc : resp.getToolCalls()) {
                    // think_more 是元工具，不真正执行，只返回 "ok" 满足 API 格式要求
                    if ("think_more".equals(tc.getFunctionName())) {
                        synchronized (history) {
                            history.add(ChatMessage.tool(tc.getId(), "ok"));
                        }
                        continue;
                    }

                    if (eventStream != null) eventStream.emitToolCall(userId, "decision", tc.getFunctionName(), tc.getFunctionArguments());

                    String result;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> args = objectMapper.readValue(tc.getFunctionArguments(), Map.class);
                        result = toolExecutor.execute(tc.getFunctionName(), args);
                    } catch (Exception e) {
                        result = "工具执行异常: " + java.util.Objects.toString(e.getMessage(), "未知错误");
                    }

                    if (eventStream != null) eventStream.emitToolResult(userId, "decision", tc.getFunctionName(), true,
                            result.length() > 200 ? result.substring(0, 200) + "..." : result);

                    synchronized (history) {
                        history.add(ChatMessage.tool(tc.getId(), result));
                    }
                }
            } finally {
                AgentToolProvider.clearCurrentContext();
            }

            // 每轮工具调用后发送上下文信息
            if (contextManager != null && eventStream != null) {
                eventStream.emitContextInfo(userId, contextManager.getUsagePercent(history));
            }

            // 调用了 think_more → 继续下一轮
            if (hasThinkMore) {
                if (eventStream != null) {
                    eventStream.emitAgentStart(userId, "decision", "DECISION", "继续执行...", null);
                }
                continue;
            }
        }

        // 超过最大轮次，让 LLM 总结已完成的工作而不是报错
        synchronized (history) {
            history.add(ChatMessage.user("你已经执行了多轮操作。请用简短中文总结你已经完成了哪些操作、当前状态如何、还有什么未完成。"));
        }
        try {
            String summary = llmClient.chat(ChatHistory.SYSTEM_PROMPT, history, ModelTier.FLASH);
            if (summary != null && !summary.isBlank()) {
                if (eventStream != null) eventStream.emitReplyDone(userId, summary);
            } else {
                if (eventStream != null) eventStream.emitReplyDone(userId, "操作已执行，请查看上方的工具调用结果。");
            }
        } catch (Exception e) {
            if (eventStream != null) eventStream.emitReplyDone(userId, "操作已执行，请查看上方的工具调用结果。");
        }
    }

    private String extractReply(String content) {
        if (content == null || content.isBlank()) return content;
        String trimmed = content.trim();

        // 如果是 Markdown 格式（非 JSON），直接返回
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }

        try {
            String jsonStr = extractJson(trimmed);
            Map<String, Object> parsed = objectMapper.readValue(jsonStr, Map.class);

            // 优先取 reply 字段
            String reply = (String) parsed.get("reply");
            if (reply != null && !reply.isEmpty()) {
                return reply;
            }

            // 其次取 content 字段（处理 {"role":"assistant","content":"..."} 格式）
            Object contentObj = parsed.get("content");
            if (contentObj instanceof String c && !c.isEmpty()) {
                return c;
            }
        } catch (Exception ignored) {}
        return trimmed;
    }

    private boolean isToolCallJson(String chunk) {
        if (chunk == null) return false;
        String trimmed = chunk.trim();
        if (trimmed.isEmpty()) return false;
        char first = trimmed.charAt(0);
        // JSON 结构字符（{ } [ ]）
        if (first == '{' || first == '[' || first == '}' || first == ']') return true;
        // JSON 字段名模式：必须以 "开头" 且至少有两对引号（如 "name":"value"）
        if (first == '"') {
            long quoteCount = trimmed.chars().filter(c -> c == '"').count();
            if (quoteCount >= 3) return true; // "key":" 至少3个引号
            // 单独的 " 或双引号包裹的短文本，可能是中文引号，不过滤
        }
        // JSON 关键字（带引号，出现在任何位置）
        if (trimmed.contains("\"tool_calls\"")) return true;
        if (trimmed.contains("\"function\"")) return true;
        if (trimmed.contains("\"arguments\"")) return true;
        if (trimmed.contains("\"name\"")) return true;
        if (trimmed.contains("\"type\"")) return true;
        // JSON 裸关键字（带冒号，表示 JSON key-value）
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("tool_calls") || lower.startsWith("function:")
                || lower.startsWith("arguments:") || lower.startsWith("type:")) return true;
        // 纯 JSON 值字符（数字、true、false、null）
        if (trimmed.matches("[0-9,]+")) return true;
        if (trimmed.equals("true") || trimmed.equals("false") || trimmed.equals("null")) return true;
        return false;
    }

    public void cleanupStaleSessions() {
        long cutoff = System.currentTimeMillis() - 30 * 60 * 1000L;
        sessionLastAccess.entrySet().removeIf(e -> {
            if (e.getValue() < cutoff) {
                sessionHistories.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    private String extractJson(String text) {
        int jsonStart = text.indexOf("```json");
        if (jsonStart != -1) {
            int jsonEnd = text.indexOf("```", jsonStart + 7);
            if (jsonEnd != -1) return text.substring(jsonStart + 7, jsonEnd).trim();
        }
        int codeStart = text.indexOf("```");
        if (codeStart != -1) {
            int codeEnd = text.indexOf("```", codeStart + 3);
            if (codeEnd != -1) return text.substring(codeStart + 3, codeEnd).trim();
        }
        int braceStart = text.indexOf("{");
        int braceEnd = text.lastIndexOf("}");
        if (braceStart != -1 && braceEnd > braceStart) {
            return text.substring(braceStart, braceEnd + 1);
        }
        return text.trim();
    }

    /**
     * 从 reasoning 文本中尝试提取工具调用 JSON
     */
    private List<LlmResponse.ToolCall> extractToolCallsFromReasoning(String reasoning) {
        List<LlmResponse.ToolCall> result = new ArrayList<>();
        try {
            int toolCallsIdx = reasoning.indexOf("\"tool_calls\"");
            if (toolCallsIdx == -1) toolCallsIdx = reasoning.indexOf("tool_calls");
            if (toolCallsIdx == -1) return result;

            int arrayStart = reasoning.indexOf('[', toolCallsIdx);
            if (arrayStart == -1) return result;

            // 用大括号+方括号联合深度匹配，正确处理嵌套
            int depth = 0;
            int arrayEnd = -1;
            boolean inString = false;
            boolean escaped = false;
            for (int i = arrayStart; i < reasoning.length(); i++) {
                char c = reasoning.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (c == '\\') { escaped = true; continue; }
                if (c == '"') { inString = !inString; continue; }
                if (inString) continue;
                if (c == '[' || c == '{') depth++;
                else if (c == ']' || c == '}') depth--;
                if (depth == 0) { arrayEnd = i; break; }
            }
            if (arrayEnd == -1) return result;

            String arrayJson = reasoning.substring(arrayStart, arrayEnd + 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCallsRaw = objectMapper.readValue(arrayJson, List.class);
            for (Map<String, Object> tc : toolCallsRaw) {
                String id = (String) tc.getOrDefault("id", "call_" + System.currentTimeMillis());
                @SuppressWarnings("unchecked")
                Map<String, String> func = (Map<String, String>) tc.get("function");
                if (func != null) {
                    String name = func.get("name");
                    String arguments = func.get("arguments");
                    if (name != null) {
                        result.add(new LlmResponse.ToolCall(id, name, arguments != null ? arguments : "{}"));
                    }
                }
            }
        } catch (Exception e) {
            log.info("[DecisionAgent] 从 reasoning 提取工具调用失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 从 reasoning 中去除 JSON 工具调用片段，保留自然语言思考
     */
    private String stripJsonFromReasoning(String reasoning) {
        if (reasoning == null || reasoning.isEmpty()) return "";
        // 查找明确的工具调用 JSON 起始位置
        int idx = reasoning.indexOf("{\"tool_calls\"");
        if (idx == -1) idx = reasoning.indexOf("{\"function\"");
        if (idx == -1) idx = reasoning.indexOf("\"tool_calls\"");
        if (idx > 0) return reasoning.substring(0, idx).trim();
        if (idx == 0) return "";
        // 没找到明确的工具调用关键字，检查整个文本是否主要是 JSON
        String trimmed = reasoning.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return "";
        }
        // 混合文本：去除明显的 JSON 片段（{...} 和 [...] 结构）
        return stripJsonFragments(trimmed);
    }

    /**
     * 从混合文本中去除 JSON 片段，保留自然语言
     */
    private String stripJsonFragments(String text) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                // 找到 JSON 结构起点，跳过整个结构
                int depth = 0;
                boolean inStr = false;
                boolean esc = false;
                int start = i;
                for (int j = i; j < text.length(); j++) {
                    char ch = text.charAt(j);
                    if (esc) { esc = false; continue; }
                    if (ch == '\\') { esc = true; continue; }
                    if (ch == '"') { inStr = !inStr; continue; }
                    if (inStr) continue;
                    if (ch == '{' || ch == '[') depth++;
                    else if (ch == '}' || ch == ']') depth--;
                    if (depth == 0) { i = j + 1; break; }
                }
                if (i == start) i++; // 防止死循环
                // JSON 块前后加换行分隔
                if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                    result.append('\n');
                }
            } else {
                result.append(c);
                i++;
            }
        }
        String cleaned = result.toString().trim();
        // 去除多余空行
        return cleaned.replaceAll("\\n{3,}", "\n\n");
    }

    private Map<String, Object> buildAssistantMessage(LlmResponse resp) {
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("role", "assistant");
        msg.put("content", resp.getContent() != null ? resp.getContent() : "");
        msg.put("tool_calls", buildToolCallsList(resp));
        return msg;
    }

    private List<Map<String, Object>> buildToolCallsList(LlmResponse resp) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (LlmResponse.ToolCall tc : resp.getToolCalls()) {
            toolCalls.add(Map.of(
                    "id", tc.getId(),
                    "type", "function",
                    "function", Map.of(
                            "name", tc.getFunctionName(),
                            "arguments", tc.getFunctionArguments())));
        }
        return toolCalls;
    }
}
