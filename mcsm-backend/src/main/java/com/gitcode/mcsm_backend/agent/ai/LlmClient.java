package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.core.ChatMessage;
import com.gitcode.mcsm_backend.agent.core.ModelTier;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * LLM API 客户端（OpenAI 兼容接口）
 * 支持 OpenAI、智谱AI 等兼容 provider
 */
@Slf4j
public class LlmClient {

    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final MultiModelRouter router;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ProviderFields providerFields;

    /**
     * LLM 调用异常
     */
    public static class LlmException extends RuntimeException {
        public LlmException(String message) {
            super(message);
        }
    }

    public LlmClient(String baseUrl, String apiKey, String defaultModel) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.router = null;
        this.providerFields = new ProviderFields(null, null, null, null, null);
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    public LlmClient(MultiModelRouter router) {
        this.router = router;
        this.baseUrl = router.getBaseUrl();
        this.apiKey = router.getApiKey();
        this.defaultModel = router.getFlashModel();
        this.providerFields = router.getProviderFields();
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(30)).build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 带 system prompt 和模型级别的对话（供 DecisionAgent 等使用）
     * ChatMessage 版本，接受 ModelTier 枚举
     */
    public String chat(String systemPrompt,
                        List<ChatMessage> messages,
                        ModelTier tier) {
        List<Map<String, String>> msgMaps = new ArrayList<>();
        for (ChatMessage cm : messages) {
            msgMaps.add(Map.of("role", cm.getRole(), "content", cm.getContent() != null ? cm.getContent() : ""));
        }
        return chatWithSystemPrompt(systemPrompt, msgMaps, tier.name());
    }

    /**
     * 带 tools 的对话（供 DecisionAgent ReAct 循环使用）
     * 返回 LlmResponse，可能包含 tool_calls
     */
    public LlmResponse chatWithTools(String systemPrompt,
                                      List<ChatMessage> messages,
                                      List<Map<String, Object>> tools,
                                      ModelTier tier) {
        String model = router != null ? router.selectModel(tier.name()) : defaultModel;

        List<Map<String, Object>> allMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatMessage cm : messages) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", cm.getRole());
            msg.put("content", cm.getContent());
            if (cm.getToolCallId() != null) {
                msg.put("tool_call_id", cm.getToolCallId());
            }
            if (cm.getToolCalls() != null && !cm.getToolCalls().isEmpty()) {
                msg.put("tool_calls", cm.getToolCalls());
                if (cm.getContent() == null || cm.getContent().isEmpty()) {
                    msg.put("content", null);
                }
            }
            allMessages.add(msg);
        }

        return chat(allMessages, tools, model);
    }

    /**
     * 带 tools 的流式对话 — SSE 逐 chunk 解析
     * 每收到文本 chunk 调用 onChunk 回调
     * 每收到思考 chunk 调用 onReasoning 回调
     */
    public LlmResponse chatWithToolsStream(String systemPrompt,
                                             List<ChatMessage> messages,
                                             List<Map<String, Object>> tools,
                                             ModelTier tier,
                                             Consumer<String> onChunk,
                                             Consumer<String> onReasoning) {
        String model = router != null ? router.selectModel(tier.name()) : defaultModel;

        List<Map<String, Object>> allMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        for (ChatMessage cm : messages) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", cm.getRole());
            msg.put("content", cm.getContent());
            if (cm.getToolCallId() != null) {
                msg.put("tool_call_id", cm.getToolCallId());
            }
            if (cm.getToolCalls() != null && !cm.getToolCalls().isEmpty()) {
                msg.put("tool_calls", cm.getToolCalls());
                if (cm.getContent() == null || cm.getContent().isEmpty()) {
                    msg.put("content", null);
                }
            }
            allMessages.add(msg);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model != null ? model : defaultModel);
            body.put("messages", allMessages);
            body.put("stream", true);

            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofMinutes(5))
                    .build();

            HttpResponse<InputStream> httpResponse = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() != 200) {
                String errorBody = new String(httpResponse.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("[LlmClient] HTTP {}: {}", httpResponse.statusCode(), errorBody);
                String detail = parseErrorMessage(httpResponse.statusCode(), errorBody);
                return LlmResponse.error(detail);
            }

            // 流式解析：文本/思考 chunk 实时回调
            LlmResponse resp = parseSSEStream(httpResponse.body(), onChunk, onReasoning);
            return resp;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LlmClient] 流式调用失败: {}", e.getMessage());
            return LlmResponse.error("AI 服务暂时不可用: " + java.util.Objects.toString(e.getMessage(), "连接超时或网络异常"));
        }
    }

    /**
     * 解析 SSE 流，逐 chunk 提取 content/reasoning_content delta，累积 tool_calls
     */
    @SuppressWarnings("unchecked")
    private LlmResponse parseSSEStream(InputStream inputStream, Consumer<String> onChunk, Consumer<String> onReasoning) throws Exception {
        StringBuilder fullContent = new StringBuilder();
        StringBuilder fullReasoning = new StringBuilder();
        // tool_calls 按 index 累积: index -> {id, name, arguments}
        Map<Integer, Map<String, String>> toolCallsMap = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if (data.equals("[DONE]")) break;

                Map<String, Object> chunk;
                try {
                    chunk = objectMapper.readValue(data, Map.class);
                } catch (Exception e) {
                    continue; // 跳过无法解析的行
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                if (choices == null || choices.isEmpty()) continue;

                Map<String, Object> choice = choices.get(0);
                Map<String, Object> delta = (Map<String, Object>) choice.get("delta");
                if (delta == null) continue;

                // 思考内容（根据厂商配置的 thinkingField 解析）
                String reasoning = null;
                String thinkField = providerFields.getThinkingField();
                if (thinkField != null && !thinkField.isEmpty()) {
                    reasoning = (String) delta.get(thinkField);
                }
                if (reasoning != null && !reasoning.isEmpty()) {
                    fullReasoning.append(reasoning);
                    if (onReasoning != null) {
                        onReasoning.accept(reasoning);
                    }
                }

                // 文本内容（根据厂商配置的 deltaContentField 解析）
                String content = (String) delta.get(providerFields.getDeltaContentField());
                if (content != null && !content.isEmpty()) {
                    fullContent.append(content);
                    if (onChunk != null) {
                        onChunk.accept(content);
                    }
                }

                // 工具调用（流式累积，根据厂商配置的 deltaToolCallsField 解析）
                List<Map<String, Object>> toolCallsDelta = (List<Map<String, Object>>) delta.get(providerFields.getDeltaToolCallsField());
                if (toolCallsDelta != null) {
                    // 调试：当同时有 content 和 tool_calls 时打印
                    if (content != null && !content.isEmpty()) {
                        log.info("[LlmClient] 模型在工具调用时输出了文本: {}", content);
                    }
                    for (Map<String, Object> tcDelta : toolCallsDelta) {
                        int index = ((Number) tcDelta.get("index")).intValue();
                        Map<String, String> accumulated = toolCallsMap.computeIfAbsent(index, k -> new LinkedHashMap<>());

                        String id = (String) tcDelta.get("id");
                        if (id != null) accumulated.put("id", id);

                        Map<String, String> func = (Map<String, String>) tcDelta.get("function");
                        if (func != null) {
                            String name = func.get("name");
                            if (name != null) accumulated.put("name", name);

                            String arguments = func.get("arguments");
                            if (arguments != null) {
                                accumulated.merge("arguments", arguments, (a, b) -> a + b);
                            }
                        }
                    }
                }
            }
        }

        // 构造结果
        String reasoning = fullReasoning.toString();
        if (!toolCallsMap.isEmpty()) {
            List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();
            for (Map<String, String> tc : toolCallsMap.values()) {
                toolCalls.add(new LlmResponse.ToolCall(
                        tc.getOrDefault("id", ""),
                        tc.getOrDefault("name", ""),
                        tc.getOrDefault("arguments", "")));
            }
            String content = fullContent.toString();
            log.info("[LlmClient] 工具调用 - content: {}字, reasoning: {}字, 工具: {}", content.length(), reasoning.length(), toolCalls.size());
            return LlmResponse.toolCalls(content, reasoning, toolCalls);
        }

        String content = fullContent.toString();
        if (content.isEmpty() && !reasoning.isEmpty()) {
            // content 为空但 reasoning 非空：模型可能把工具调用 JSON 放到了 reasoning_content 中
            log.info("[LlmClient] content 为空, reasoning={}字, reasoning片段: {}", reasoning.length(), reasoning.substring(0, Math.min(80, reasoning.length())));
            // 尝试从 reasoning 中提取工具调用
            List<LlmResponse.ToolCall> extracted = extractToolCallsFromReasoning(reasoning);
            if (!extracted.isEmpty()) {
                log.info("[LlmClient] 从 reasoning 中提取到 {} 个工具调用", extracted.size());
                return LlmResponse.toolCalls("", reasoning, extracted);
            }
            // 提取不到工具调用，返回 reasoning 中的纯文本（去除 JSON 片段）
            String cleanReasoning = stripJsonFromText(reasoning);
            if (!cleanReasoning.isBlank()) {
                return LlmResponse.text(cleanReasoning, reasoning);
            }
            return LlmResponse.text("AI 未返回有效响应", reasoning);
        }
        if (content.isEmpty()) {
            log.info("[LlmClient] SSE 流结束 - content 和 reasoning 均为空");
        }
        return LlmResponse.text(
                content.isEmpty() ? "AI 未返回有效响应" : content,
                reasoning);
    }

    /**
     * 从 reasoning 文本中尝试提取工具调用 JSON
     * 处理模型把 {"tool_calls":...} 放到 reasoning_content 的情况
     */
    private List<LlmResponse.ToolCall> extractToolCallsFromReasoning(String reasoning) {
        List<LlmResponse.ToolCall> result = new ArrayList<>();
        try {
            int toolCallsIdx = reasoning.indexOf("\"tool_calls\"");
            if (toolCallsIdx == -1) toolCallsIdx = reasoning.indexOf("tool_calls");
            if (toolCallsIdx == -1) return result;

            // 找到 tool_calls 字段对应的 JSON 数组（跳过 "tool_calls": 部分）
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
            List<Map<String, Object>> toolCallsRaw = objectMapper.readValue(arrayJson, List.class);
            for (Map<String, Object> tc : toolCallsRaw) {
                String id = (String) tc.getOrDefault("id", "call_" + System.currentTimeMillis());
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
            log.warn("[LlmClient] 从 reasoning 提取工具调用失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 从文本中去除 JSON 工具调用片段，保留自然语言部分
     */
    private String stripJsonFromText(String text) {
        if (text == null || text.isEmpty()) return "";
        // 查找工具调用 JSON 起始位置
        int idx = text.indexOf("{\"tool_calls\"");
        if (idx == -1) idx = text.indexOf("{\"function\"");
        if (idx == -1) idx = text.indexOf("\"tool_calls\"");
        if (idx > 0) return text.substring(0, idx).trim();
        if (idx == 0) return "";
        // 没找到明确的工具调用关键字，检查整个文本是否主要是 JSON
        String trimmed = text.trim();
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
                if (i == start) i++;
                if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                    result.append('\n');
                }
            } else {
                result.append(c);
                i++;
            }
        }
        String cleaned = result.toString().trim();
        return cleaned.replaceAll("\\n{3,}", "\n\n");
    }

    /**
     * 带 system prompt 和模型级别的对话（供 DecisionAgent 等使用）
     */
    public String chatWithSystemPrompt(String systemPrompt,
                                        List<Map<String, String>> messages,
                                        String modelTier) {
        String model = router != null ? router.selectModel(modelTier) : defaultModel;

        List<Map<String, String>> allMessages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            allMessages.add(Map.of("role", "system", "content", systemPrompt));
        }
        allMessages.addAll(messages);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", allMessages);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[LlmClient] HTTP {}", response.statusCode());
                String detail = parseErrorMessage(response.statusCode(), response.body());
                throw new LlmException(detail);
            }

            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
            if (choices != null && !choices.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg == null) return null;
                return (String) msg.get(providerFields.getContentField());
            }
            return null;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("[LlmClient] 调用失败: {}", e.getMessage());
            throw new LlmException("AI 服务暂时不可用: " + java.util.Objects.toString(e.getMessage(), "连接超时或网络异常"));
        }
    }

    /**
     * 带工具的对话（供 AgentCore 等使用）
     */
    public LlmResponse chat(List<Map<String, Object>> messages,
                            List<Map<String, Object>> tools,
                            String model) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model != null ? model : defaultModel);
            body.put("messages", messages);

            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() != 200) {
                log.error("[LlmClient] HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
                String detail = parseErrorMessage(httpResponse.statusCode(), httpResponse.body());
                return LlmResponse.error(detail);
            }

            return parseResponse(httpResponse.body());
        } catch (Exception e) {
            log.error("[LlmClient] 请求失败: {}", e.getMessage());
            return LlmResponse.error("AI 服务暂时不可用: " + java.util.Objects.toString(e.getMessage(), "连接超时或网络异常"));
        }
    }

    /**
     * 解析 LLM API 错误信息，返回用户友好的提示
     */
    private String parseErrorMessage(int statusCode, String body) {
        // 尝试从响应体提取具体错误
        String apiMsg = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(body, Map.class);
            // OpenAI 格式: {"error": {"message": "...", "type": "..."}}
            @SuppressWarnings("unchecked")
            Map<String, Object> error = (Map<String, Object>) result.get("error");
            if (error != null) {
                apiMsg = (String) error.get("message");
            }
            // 智谱格式: {"msg": "..."}
            if (apiMsg == null) {
                apiMsg = (String) result.get("msg");
            }
        } catch (Exception ignored) {}

        return switch (statusCode) {
            case 401 -> "API Key 无效或已过期，请在后端设置面板中更新 API Key";
            case 403 -> "API 访问被拒绝，请检查 API Key 权限";
            case 429 -> "API 请求频率超限，请稍后重试";
            case 500, 502, 503 -> "AI 服务端异常 (" + statusCode + ")" +
                    (apiMsg != null ? ": " + apiMsg : "，请稍后重试");
            default -> "AI 服务返回错误 (" + statusCode + ")" +
                    (apiMsg != null ? ": " + apiMsg : "");
        };
    }

    @SuppressWarnings("unchecked")
    private LlmResponse parseResponse(String body) throws Exception {
        Map<String, Object> responseBody = objectMapper.readValue(body, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) {
            return LlmResponse.text("AI 未返回有效响应");
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        String content = (String) message.get(providerFields.getContentField());
        List<Map<String, Object>> toolCallsRaw = (List<Map<String, Object>>) message.get(providerFields.getToolCallsField());

        if (toolCallsRaw != null && !toolCallsRaw.isEmpty()) {
            List<LlmResponse.ToolCall> toolCalls = new ArrayList<>();
            for (Map<String, Object> tc : toolCallsRaw) {
                String id = (String) tc.get("id");
                Map<String, String> func = (Map<String, String>) tc.get("function");
                toolCalls.add(new LlmResponse.ToolCall(id, func.get("name"), func.get("arguments")));
            }
            return LlmResponse.toolCalls(toolCalls);
        }

        return LlmResponse.text(content != null ? content : "");
    }
}
