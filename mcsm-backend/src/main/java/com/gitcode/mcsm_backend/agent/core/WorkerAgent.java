package com.gitcode.mcsm_backend.agent.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.ai.*;

import java.util.*;

/**
 * WorkerAgent — 极简执行单元
 *
 * 设计原则（v4 架构）：
 * - 收到工单 → 干活 → 自检 → 返回 JSON
 * - 使用 Flash 模型
 * - 不对用户，不维护记忆
 * - 不区分 ANALYZE/EXECUTE/DIAGNOSE 模式
 *
 * 工单结构：
 * {
 *   "targetPlugin": "EssentialsX",
 *   "fileScope": ["plugins/EssentialsX/config.yml"],
 *   "taskGoal": "将 max-homes 从 3 改为 5",
 *   "extraContext": "用户要求修改家的数量上限"
 * }
 *
 * 输出格式：
 * {
 *   "summary": "一句话摘要",
 *   "results": [{"file": "...", "status": "SUCCESS|FAILED|SKIPPED", "detail": "..."}],
 *   "self_check": {"passed": true, "concerns": [], "verified": []},
 *   "discoveries": [{"type": "CONFIG_RULE|PLUGIN_RELATION|ERROR_PATTERN|GENERAL", "content": "...", "confidence": "HIGH|MEDIUM|LOW"}]
 * }
 */
public class WorkerAgent {

    private static final String SYSTEM_PROMPT = """
            你是 MCSM 运维系统的执行单元。不跟用户对话。上级 DecisionAgent 分配工单给你，你完成后退回。

            ## 行为准则
            1. 先读后写：修改文件前必须先读取当前内容，不凭猜测操作
            2. 精确操作：定位到具体行和具体 key，不做全局替换
            3. 自检：执行写入后立即读回验证，确认修改生效
            4. 不越界：工单说改什么就改什么，不要做额外修改

            ## 记忆使用
            - 处理插件前先读其 knowledge.md（如果存在）
            - 严格遵守 knowledge.md 中标注"用户确认"的规则
            - 新发现写入 discoveries 字段报告，不要自行写入知识库

            ## 输出格式
            严格 JSON，无其他文字：
            {
              "summary": "一句话摘要",
              "results": [{"file": "文件路径", "status": "SUCCESS|FAILED|SKIPPED", "detail": "具体说明"}],
              "self_check": {"passed": true/false, "concerns": ["拿不准的问题"], "verified": ["验证过的操作"]},
              "discoveries": [{"type": "CONFIG_RULE|PLUGIN_RELATION|ERROR_PATTERN|GENERAL", "content": "发现内容", "confidence": "HIGH|MEDIUM|LOW"}]
            }
            """;

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public WorkerAgent(LlmClient llmClient, ToolExecutor toolExecutor, ToolRegistry toolRegistry) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 执行工单
     *
     * @param workOrder 工单内容
     * @return 结构化 JSON 结果
     */
    public String execute(Map<String, Object> workOrder) {
        String targetPlugin = (String) workOrder.getOrDefault("targetPlugin", "unknown");
        String taskGoal = (String) workOrder.getOrDefault("taskGoal", "");
        String extraContext = (String) workOrder.getOrDefault("extraContext", "");
        @SuppressWarnings("unchecked")
        List<String> fileScope = (List<String>) workOrder.getOrDefault("fileScope", List.of());

        // 构建用户消息
        String userMessage = buildWorkOrderMessage(targetPlugin, taskGoal, extraContext, fileScope);

        // 创建会话
        String sessionId = "worker_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ChatHistory chatHistory = new ChatHistory();
        List<Map<String, Object>> messages = chatHistory.getOrCreate(sessionId, SYSTEM_PROMPT);
        chatHistory.addMessage(sessionId, "user", userMessage);

        // ReAct 循环（最多 10 轮，防止 Worker 无限循环）
        List<Map<String, Object>> tools = toolRegistry.getAllTools();
        int maxTurns = 10;

        for (int turn = 0; turn < maxTurns; turn++) {
            LlmResponse response = llmClient.chat(messages, tools, null); // 使用 Flash 模型

            if (response.isError()) {
                return buildErrorResult("LLM 调用失败: " + response.getContent());
            }

            // 最终回复
            if (!response.isToolCall()) {
                String content = response.getContent();
                chatHistory.addMessage(sessionId, "assistant", content);
                return content; // Worker 的 JSON 输出
            }

            // 处理工具调用
            List<LlmResponse.ToolCall> toolCalls = response.getToolCalls();
            chatHistory.addToolCallMessage(sessionId, toolCalls);

            for (LlmResponse.ToolCall tc : toolCalls) {
                String toolName = tc.getFunctionName();
                Map<String, Object> args = parseArguments(tc.getFunctionArguments());

                // 检查写入权限（fileScope 限制）
                if (isWriteTool(toolName) && !isInFileScope(args, fileScope)) {
                    String error = "拒绝写入：文件 " + args.get("path") + " 不在工单 fileScope 范围内";
                    chatHistory.addToolResult(sessionId, tc.getId(), error);
                    continue;
                }

                String result;
                try {
                    result = toolExecutor.execute(toolName, args);
                } catch (Exception e) {
                    result = "工具执行异常: " + java.util.Objects.toString(e.getMessage(), "未知错误");
                }

                chatHistory.addToolResult(sessionId, tc.getId(), result);
            }
        }

        return buildErrorResult("Worker 超过最大轮次限制");
    }

    private String buildWorkOrderMessage(String targetPlugin, String taskGoal,
                                          String extraContext, List<String> fileScope) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 工单\n");
        sb.append("- 目标插件: ").append(targetPlugin).append("\n");
        sb.append("- 任务目标: ").append(taskGoal).append("\n");
        sb.append("- 写入范围: ").append(String.join(", ", fileScope)).append("\n");
        if (extraContext != null && !extraContext.isEmpty()) {
            sb.append("- 额外信息: ").append(extraContext).append("\n");
        }
        sb.append("\n请开始执行。完成后输出严格 JSON 结果。");
        return sb.toString();
    }

    private boolean isWriteTool(String toolName) {
        return Set.of("write_file", "edit_file", "edit_file_lines", "edit_yaml_key",
                "edit_json_path", "update_server_properties", "delete_file").contains(toolName);
    }

    @SuppressWarnings("unchecked")
    private boolean isInFileScope(Map<String, Object> args, List<String> fileScope) {
        if (fileScope.isEmpty()) return true;

        String path = (String) args.get("path");
        if (path == null) return false;

        String normalized = path.replace('\\', '/');
        for (String scope : fileScope) {
            String ns = scope.replace('\\', '/');
            if (normalized.equals(ns) || normalized.startsWith(ns + "/")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String buildErrorResult(String error) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", "执行失败: " + error);
            result.put("results", List.of(Map.of("file", "-", "status", "FAILED", "detail", error)));
            result.put("self_check", Map.of("passed", false, "concerns", List.of(error), "verified", List.of()));
            result.put("discoveries", List.of());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"summary\":\"执行失败\",\"results\":[],\"self_check\":{\"passed\":false},\"discoveries\":[]}";
        }
    }
}
