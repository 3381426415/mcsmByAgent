package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Agent 工具执行器
 *
 * 当 Agent 调用 /api/agent-tools/call 时，根据工具名分发到对应的后端 Service 执行。
 * 直接调用本地服务，不再回调 Agent 端。
 */
@Service
public class AgentToolExecutor {

    @Autowired
    private AgentService agentService;

    @Autowired
    private PlayerService playerService;

    /**
     * 根据工具名执行工具
     *
     * @param toolName  工具名（来自 @AgentTool 注解的方法名）
     * @param arguments 工具参数
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public Result<?> execute(String toolName, Map<String, Object> arguments) {
        if (arguments == null) arguments = Map.of();

        return switch (toolName) {
            // ==================== 服务器管理 ====================
            case "startServer" -> agentService.startServer();
            case "stopServer" -> agentService.stopServer();
            case "restartServer" -> agentService.restartServer();
            case "getStatus" -> agentService.getStatus();

            // ==================== 指定服务器管理 ====================
            case "startServerById" -> {
                String serverId = (String) arguments.get("serverId");
                yield agentService.startServer(serverId);
            }
            case "stopServerById" -> {
                String serverId = (String) arguments.get("serverId");
                yield agentService.stopServer(serverId);
            }
            case "restartServerById" -> {
                String serverId = (String) arguments.get("serverId");
                yield agentService.restartServer(serverId);
            }
            case "getServerStatus" -> {
                String serverId = (String) arguments.get("serverId");
                yield agentService.getServerStatus(serverId);
            }

            // ==================== 玩家管理 ====================
            case "listAllPlayers" -> Result.success("获取成功", playerService.getAllPlayers());

            // ==================== 未知工具 ====================
            default -> Result.error("未知工具: " + toolName);
        };
    }
}
