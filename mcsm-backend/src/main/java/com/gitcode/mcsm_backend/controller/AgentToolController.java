package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.Entity.ToolDefinition;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AgentToolExecutor;
import com.gitcode.mcsm_backend.service.AgentToolService;
import com.gitcode.mcsm_backend.service.ToolFormatConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 工具集开放端点
 *
 * 供 Agent 端调用，获取后端所有可用工具并执行。
 * 认证方式：JWT（Agent 服务账号登录获取 token）
 */
@RestController
public class AgentToolController {

    private final AgentToolService agentToolService;
    private final AgentToolExecutor toolExecutor;
    private final ToolFormatConverter toolFormatConverter;

    @Value("${ai.llm.api-key:}")
    private String llmApiKey;
    @Value("${ai.llm.base-url:}")
    private String llmBaseUrl;
    @Value("${ai.llm.pro-model:}")
    private String llmProModel;
    @Value("${ai.llm.flash-model:}")
    private String llmFlashModel;
    @Value("${ai.llm.provider:api}")
    private String llmProvider;

    public AgentToolController(@Lazy AgentToolService agentToolService,
                               @Lazy AgentToolExecutor toolExecutor,
                               ToolFormatConverter toolFormatConverter) {
        this.agentToolService = agentToolService;
        this.toolExecutor = toolExecutor;
        this.toolFormatConverter = toolFormatConverter;
    }

    // ==================== 旧接口（向后兼容） ====================

    @GetMapping("/api/agent/tools")
    public Result<List<Map<String, Object>>> getTools() {
        List<ToolDefinition> tools = agentToolService.getAllTools();
        return Result.success("获取成功", toolFormatConverter.toFunctionCallingFormat(tools));
    }

    @PostMapping("/api/agent/tools/refresh")
    public Result<String> refreshTools() {
        agentToolService.scanTools();
        return Result.successMsg("工具缓存已刷新");
    }

    // ==================== 新接口：Agent 端调用 ====================

    /**
     * 返回所有后端工具（OpenAI function-calling 格式）
     */
    @PostMapping("/api/agent-tools/list")
    public Result<List<Map<String, Object>>> listTools() {
        List<ToolDefinition> tools = agentToolService.getAllTools();
        return Result.success("获取成功", toolFormatConverter.toFunctionCallingFormat(tools));
    }

    /**
     * 执行指定工具
     *
     * @param body { "name": "工具名", "arguments": { "key": "value", ... } }
     */
    @PostMapping("/api/agent-tools/call")
    public Result<?> callTool(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isEmpty()) {
            return Result.error("缺少工具名称（name）");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) body.get("arguments");
        if (arguments == null) {
            arguments = Map.of();
        }

        return toolExecutor.execute(name, arguments);
    }

    /**
     * 返回 LLM 配置（供 Agent 端自动获取，无需手动配置 agent-config.yml）
     * 直接从 application.properties 读取，不经过 AiConfigController 避免权限检查
     */
    @PostMapping("/api/agent-tools/llm-config")
    public Result<Map<String, String>> llmConfig() {
        if (llmApiKey == null || llmApiKey.isEmpty()) {
            return Result.error("后端未配置 LLM API Key");
        }
        Map<String, String> config = new java.util.LinkedHashMap<>();
        config.put("apiKey", llmApiKey);
        config.put("baseUrl", llmBaseUrl != null ? llmBaseUrl : "");
        config.put("provider", llmProvider != null ? llmProvider : "api");
        if (llmProModel != null && !llmProModel.isEmpty()) {
            config.put("proModel", llmProModel);
        }
        if (llmFlashModel != null && !llmFlashModel.isEmpty()) {
            config.put("flashModel", llmFlashModel);
        }
        return Result.success("获取成功", config);
    }
}
