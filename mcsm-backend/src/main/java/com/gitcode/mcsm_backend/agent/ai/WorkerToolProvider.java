package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.agent.core.ResultCollector;
import com.gitcode.mcsm_backend.agent.core.WorkerAgent;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * Worker 管理工具提供者
 *
 * 提供 spawn_workers 工具，让 DecisionAgent 可以派发多个 Worker 并行执行。
 * 内部用虚拟线程并发执行多个 WorkerAgent，收集结果后返回压缩摘要。
 */
@Slf4j
public class WorkerToolProvider implements LocalToolProvider {

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final ResultCollector resultCollector;
    private final int maxConcurrency;

    public WorkerToolProvider(LlmClient llmClient, ToolExecutor toolExecutor,
                               ToolRegistry toolRegistry, int maxConcurrency) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = toolRegistry;
        this.objectMapper = new ObjectMapper();
        this.resultCollector = new ResultCollector();
        this.maxConcurrency = maxConcurrency;
    }

    public WorkerToolProvider(LlmClient llmClient, ToolExecutor toolExecutor, ToolRegistry toolRegistry) {
        this(llmClient, toolExecutor, toolRegistry, 5); // 默认最大 5 个并发
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition("spawn_workers",
                "[本地·Worker管理] 派发多个 Worker 并行执行任务。用于批量插件配置、多文件操作等复杂任务。每个 Worker 收到工单后独立执行，完成后返回结构化结果。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "workers", Map.of(
                                        "type", "array",
                                        "description", "Worker 工单列表",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "targetPlugin", Map.of("type", "string", "description", "目标插件名"),
                                                        "fileScope", Map.of(
                                                                "type", "array",
                                                                "items", Map.of("type", "string"),
                                                                "description", "写入范围文件列表（写入限定，超出会被拒绝）"
                                                        ),
                                                        "taskGoal", Map.of("type", "string", "description", "任务目标，一句话描述"),
                                                        "extraContext", Map.of("type", "string", "description", "额外信息（用户原始需求、上一层产出等）")
                                                ),
                                                "required", List.of("targetPlugin", "taskGoal", "fileScope")
                                        )
                                )
                        ),
                        "required", List.of("workers"))));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        if (!"spawn_workers".equals(toolName)) {
            return toJson(Map.of("error", "未知工具: " + toolName));
        }

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> workOrders = (List<Map<String, Object>>) arguments.get("workers");

            if (workOrders == null || workOrders.isEmpty()) {
                return toJson(Map.of("error", "工单列表为空"));
            }

            // 限制并发数
            if (workOrders.size() > maxConcurrency) {
                return toJson(Map.of("error", "Worker 数量超过最大并发限制 (" + maxConcurrency + ")"));
            }

            log.info("[WorkerToolProvider] 派发 {} 个 Worker", workOrders.size());

            // 虚拟线程并发执行
            List<String> results = executeWorkers(workOrders);

            // ResultCollector 收集结果
            String summary = resultCollector.collect(results);

            log.info("[WorkerToolProvider] 所有 Worker 完成");
            return summary;

        } catch (Exception e) {
            return toJson(Map.of("error", "spawn_workers 执行失败: " + e.getMessage()));
        }
    }

    private List<String> executeWorkers(List<Map<String, Object>> workOrders) throws Exception {
        List<String> results = new ArrayList<>();

        // 使用虚拟线程并发执行
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();

            for (Map<String, Object> workOrder : workOrders) {
                futures.add(executor.submit(() -> {
                    WorkerAgent worker = new WorkerAgent(llmClient, toolExecutor, toolRegistry);
                    String result = worker.execute(workOrder);
                    log.info("[WorkerToolProvider] Worker 完成: {}", workOrder.get("targetPlugin"));
                    return result;
                }));
            }

            // 收集所有结果
            for (Future<String> future : futures) {
                try {
                    results.add(future.get(5, TimeUnit.MINUTES)); // 5 分钟超时
                } catch (TimeoutException e) {
                    results.add(buildTimeoutResult());
                } catch (Exception e) {
                    results.add(buildErrorResult(e.getMessage()));
                }
            }
        }

        return results;
    }

    private String buildTimeoutResult() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", "Worker 执行超时（5分钟）");
            result.put("results", List.of(Map.of("file", "-", "status", "FAILED", "detail", "超时")));
            result.put("self_check", Map.of("passed", false, "concerns", List.of("执行超时"), "verified", List.of()));
            result.put("discoveries", List.of());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"summary\":\"超时\"}";
        }
    }

    private String buildErrorResult(String error) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("summary", "Worker 执行异常: " + error);
            result.put("results", List.of(Map.of("file", "-", "status", "FAILED", "detail", error)));
            result.put("self_check", Map.of("passed", false, "concerns", List.of(error), "verified", List.of()));
            result.put("discoveries", List.of());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"summary\":\"异常\"}";
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"JSON 序列化失败\"}";
        }
    }
}
