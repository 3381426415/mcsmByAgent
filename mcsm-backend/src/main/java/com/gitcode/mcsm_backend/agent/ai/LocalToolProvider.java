package com.gitcode.mcsm_backend.agent.ai;

import java.util.List;

/**
 * 本地工具提供者接口
 *
 * 实现此接口即可自动注册工具到 ToolRegistry
 */
public interface LocalToolProvider {

    /** 返回此 Provider 提供的工具定义列表 */
    List<ToolDefinition> getToolDefinitions();

    /**
     * 执行工具调用
     * @param toolName 工具名
     * @param arguments 参数
     * @return 执行结果字符串
     */
    String execute(String toolName, java.util.Map<String, Object> arguments);
}
