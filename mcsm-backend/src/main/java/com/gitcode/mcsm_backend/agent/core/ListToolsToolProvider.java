package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.LocalToolProvider;
import com.gitcode.mcsm_backend.agent.ai.ToolDefinition;
import com.gitcode.mcsm_backend.agent.ai.ToolRegistry;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * list_tools 工具 — 让 LLM 自主发现系统具备哪些能力
 */
public class ListToolsToolProvider implements LocalToolProvider {

    private final ToolRegistry toolRegistry;

    public ListToolsToolProvider(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        return List.of(new ToolDefinition("list_tools",
                "查看所有可用工具的列表，了解系统具备哪些能力。当你不确定系统能做什么时调用。",
                ToolDefinition.noParams()));
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        String list = toolRegistry.getAllDefinitions().stream()
                .filter(d -> !"list_tools".equals(d.getName()))
                .map(d -> "- " + d.getName() + ": " + d.getDescription())
                .collect(Collectors.joining("\n"));
        return "系统可用工具：\n" + list;
    }
}
