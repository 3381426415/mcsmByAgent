package com.gitcode.mcsm_backend.agent.ai;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心
 *
 * 管理所有可用工具：本地工具 + 远程（后端）工具。
 */
@Slf4j
public class ToolRegistry {

    private final Map<String, ToolDefinition> toolDefs = new ConcurrentHashMap<>();
    private final Map<String, LocalToolProvider> localProviders = new ConcurrentHashMap<>();
    private final Set<String> backendToolNames = ConcurrentHashMap.newKeySet();

    private BackendToolClient backendClient;

    public void setBackendClient(BackendToolClient client) {
        this.backendClient = client;
    }

    /**
     * 注册本地工具提供者
     */
    public void registerLocal(LocalToolProvider provider) {
        for (ToolDefinition def : provider.getToolDefinitions()) {
            toolDefs.put(def.getName(), def);
            localProviders.put(def.getName(), provider);
            log.info("[ToolRegistry] 注册本地工具: {}", def.getName());
        }
    }

    /**
     * 刷新后端工具列表
     */
    public void refreshBackendTools() {
        if (backendClient == null) return;
        List<Map<String, Object>> backendTools = backendClient.fetchBackendTools();
        backendToolNames.clear();

        for (Map<String, Object> tool : backendTools) {
            @SuppressWarnings("unchecked")
            Map<String, Object> func = (Map<String, Object>) tool.get("function");
            if (func != null) {
                String name = (String) func.get("name");
                String desc = (String) func.get("description");
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) func.get("parameters");

                backendToolNames.add(name);
                toolDefs.putIfAbsent(name, new ToolDefinition(name, desc, params));
                log.info("[ToolRegistry] 注册后端工具: {}", name);
            }
        }
    }

    /**
     * 获取所有工具定义对象
     */
    public Collection<ToolDefinition> getAllDefinitions() {
        return toolDefs.values();
    }

    /**
     * 获取所有工具定义（function-calling 格式）
     */
    public List<Map<String, Object>> getAllTools() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDefinition def : toolDefs.values()) {
            result.add(def.toFunctionCallingFormat());
        }
        return result;
    }

    /**
     * 是否为后端工具
     */
    public boolean isBackendTool(String toolName) {
        return backendToolNames.contains(toolName);
    }

    /**
     * 获取本地工具的执行 Provider
     */
    public LocalToolProvider getLocalProvider(String toolName) {
        return localProviders.get(toolName);
    }

    public BackendToolClient getBackendClient() {
        return backendClient;
    }
}
