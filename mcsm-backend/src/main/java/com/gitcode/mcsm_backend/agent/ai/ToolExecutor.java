package com.gitcode.mcsm_backend.agent.ai;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 工具执行器
 *
 * 根据工具注册信息，将调用分发到本地或后端（远程）。
 */
@Slf4j
public class ToolExecutor {

    private final ToolRegistry registry;

    public ToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    /**
     * 执行工具调用
     *
     * @param toolName  工具名
     * @param arguments 参数
     * @return 执行结果字符串
     */
    public String execute(String toolName, Map<String, Object> arguments) {
        if (arguments == null) arguments = Map.of();

        // 本地工具优先
        LocalToolProvider provider = registry.getLocalProvider(toolName);
        if (provider != null) {
            try {
                return provider.execute(toolName, arguments);
            } catch (Exception e) {
                log.error("[ToolExecutor] 本地工具执行失败: {} - {}", toolName, e.getMessage());
                return "工具执行失败: " + java.util.Objects.toString(e.getMessage(), "未知错误");
            }
        }

        // 后端工具
        if (registry.isBackendTool(toolName) && registry.getBackendClient() != null) {
            return executeBackend(toolName, arguments);
        }

        return "未知工具: " + toolName;
    }

    private String executeBackend(String toolName, Map<String, Object> arguments) {
        BackendToolClient client = registry.getBackendClient();
        if (client == null) {
            return "后端未连接，无法执行远程工具: " + toolName;
        }

        Map<String, Object> result = client.callBackendTool(toolName, arguments);
        int code = ((Number) result.getOrDefault("code", 3000)).intValue();
        if (code == 2000) {
            Object data = result.get("data");
            String msg = (String) result.getOrDefault("msg", "执行成功");
            if (data != null) return msg + ": " + data.toString();
            return msg;
        }
        return "后端工具执行失败: " + result.getOrDefault("msg", "未知错误");
    }
}
