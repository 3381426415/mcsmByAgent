package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.communication.AgentEventStream;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 子Agent工具提供者 — 将子Agent封装为 LLM 可调用的工具
 */
@Slf4j
public class AgentToolProvider implements LocalToolProvider {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final int maxTurns;

    /**
     * 执行上下文 — 由 DecisionAgent 在调用工具前设置
     */
    public record ExecutionContext(AgentEventStream eventStream, String userId, String parentAgentId) {}

    private static final ThreadLocal<ExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();

    public static void setCurrentContext(ExecutionContext context) {
        CURRENT_CONTEXT.set(context);
    }

    public static void clearCurrentContext() {
        CURRENT_CONTEXT.remove();
    }

    public AgentToolProvider(LlmClient llmClient, ToolRegistry toolRegistry, ToolExecutor toolExecutor, int maxTurns) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.maxTurns = maxTurns;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition("spawn_role_agent",
                "[本地·子Agent] 创建配置分析 Agent，自动读取相关文件并分析插件配置、诊断服务器问题",
                Map.of("type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "分析任务描述，如：检查 Essentials 配置是否有问题"),
                                "serverId", Map.of("type", "string", "description", "服务器ID，可选，默认使用当前服务器")
                        ),
                        "required", List.of("task"))));

        tools.add(new ToolDefinition("spawn_executor_agent",
                "[本地·子Agent] 创建文件执行 Agent，自动完成批量配置修改、文件写入等操作",
                Map.of("type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "执行任务描述，如：将 Essentials 的 spawn-world 改为 world_the_end"),
                                "serverId", Map.of("type", "string", "description", "服务器ID，可选")
                        ),
                        "required", List.of("task"))));

        tools.add(new ToolDefinition("spawn_checker_agent",
                "[本地·子Agent] 创建配置校验 Agent，校验配置文件格式和一致性（通常在执行 Agent 之后调用）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "task", Map.of("type", "string", "description", "校验任务描述，如：校验 Essentials 配置文件格式和一致性"),
                                "serverId", Map.of("type", "string", "description", "服务器ID，可选")
                        ),
                        "required", List.of("task"))));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        if (arguments == null) arguments = Map.of();
        String task = (String) arguments.get("task");
        String serverId = (String) arguments.get("serverId");

        if (task == null || task.isEmpty()) {
            return "错误：缺少 task 参数";
        }

        String fullTask = task;
        if (serverId != null && !serverId.isEmpty()) {
            fullTask = "服务器ID: " + serverId + "\n任务: " + task;
        }

        SubAgent agent = switch (toolName) {
            case "spawn_role_agent" -> new DynamicRoleAgent(llmClient, fullTask, toolRegistry, toolExecutor, maxTurns);
            case "spawn_executor_agent" -> new ExecutorAgent(llmClient, fullTask, toolRegistry, toolExecutor, maxTurns);
            case "spawn_checker_agent" -> new CheckerAgent(llmClient, fullTask, toolRegistry, toolExecutor, maxTurns);
            default -> null;
        };

        if (agent == null) {
            return "未知子Agent类型: " + toolName;
        }

        ExecutionContext ctx = CURRENT_CONTEXT.get();
        if (ctx != null) {
            agent.setEventStream(ctx.eventStream());
            agent.setUserId(ctx.userId());
            agent.setParentAgentId(ctx.parentAgentId());
        }

        try {
            AgentResult result = agent.execute();
            if (result.isSuccess()) {
                return result.getOutput();
            } else {
                return "子Agent执行失败: " + result.getError();
            }
        } catch (Exception e) {
            log.error("[AgentToolProvider] 子Agent执行异常: {}", e.getMessage());
            return "子Agent执行异常: " + java.util.Objects.toString(e.getMessage(), "未知错误");
        }
    }
}
