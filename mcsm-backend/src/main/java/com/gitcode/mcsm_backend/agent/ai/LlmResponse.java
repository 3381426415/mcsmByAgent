package com.gitcode.mcsm_backend.agent.ai;

import java.util.List;

/**
 * LLM 响应（支持 reasoning_content 思考链）
 */
public class LlmResponse {

    private boolean isToolCall;
    private boolean isError;
    private String content;
    private String reasoningContent; // 模型思考过程（MiMo/DeepSeek 等）
    private List<ToolCall> toolCalls;

    private LlmResponse(boolean isToolCall, boolean isError, String content,
                         String reasoningContent, List<ToolCall> toolCalls) {
        this.isToolCall = isToolCall;
        this.isError = isError;
        this.content = content;
        this.reasoningContent = reasoningContent;
        this.toolCalls = toolCalls;
    }

    public static LlmResponse text(String content) {
        return new LlmResponse(false, false, content, null, null);
    }

    public static LlmResponse error(String message) {
        return new LlmResponse(false, true, message, null, null);
    }

    public static LlmResponse toolCalls(List<ToolCall> toolCalls) {
        return new LlmResponse(true, false, null, null, toolCalls);
    }

    public static LlmResponse toolCalls(String content, List<ToolCall> toolCalls) {
        return new LlmResponse(true, false, content, null, toolCalls);
    }

    public static LlmResponse toolCalls(String content, String reasoningContent, List<ToolCall> toolCalls) {
        return new LlmResponse(true, false, content, reasoningContent, toolCalls);
    }

    public static LlmResponse text(String content, String reasoningContent) {
        return new LlmResponse(false, false, content, reasoningContent, null);
    }

    public boolean isToolCall() { return isToolCall; }
    public boolean isError() { return isError; }
    public String getContent() { return content; }
    public String getReasoningContent() { return reasoningContent; }
    public List<ToolCall> getToolCalls() { return toolCalls; }

    public static class ToolCall {
        private final String id;
        private final String functionName;
        private final String functionArguments;

        public ToolCall(String id, String functionName, String functionArguments) {
            this.id = id;
            this.functionName = functionName;
            this.functionArguments = functionArguments;
        }

        public String getId() { return id; }
        public String getFunctionName() { return functionName; }
        public String getFunctionArguments() { return functionArguments; }
    }
}
