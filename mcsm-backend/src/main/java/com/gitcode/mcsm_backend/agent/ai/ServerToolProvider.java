package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务器管理工具提供者（本地）
 *
 * 提供给 AI 智能体的 Minecraft 游戏服务器管理工具：
 * 服务器列表、启停、命令、插件管理、控制台日志等。
 */
public class ServerToolProvider implements LocalToolProvider {

    private final ServerManager serverManager;
    private final ServerService serverService;
    private final LocalPluginService localPluginService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServerToolProvider(ServerManager serverManager,
                              ServerService serverService,
                              LocalPluginService localPluginService) {
        this.serverManager = serverManager;
        this.serverService = serverService;
        this.localPluginService = localPluginService;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition("list_servers",
                "[本地·直接操作] 列出 Agent 管理的所有 Minecraft 服务器实例及其运行状态（名称、状态、PID、端口）",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("get_server_status",
                "[本地·直接操作] 获取指定服务器的实时状态（在线人数、内存占用、TPS 等运行指标）",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        tools.add(new ToolDefinition("start_server",
                "[本地·直接操作] 直接启动本地服务器进程（比后端 API 更快，无额外权限校验）",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        tools.add(new ToolDefinition("stop_server",
                "[本地·直接操作] 直接停止本地服务器进程（比后端 API 更快，无额外权限校验），此操作会导致玩家断开连接",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        tools.add(new ToolDefinition("restart_server",
                "[本地·直接操作] 重启服务器（先停止再启动，约 30-60 秒）",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        tools.add(new ToolDefinition("send_command",
                "[本地·直接操作] 向服务器控制台发送命令（如 list、say、give 等），直接执行无需确认",
                Map.of("type", "object",
                        "properties", Map.of(
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID"),
                                "command", Map.of("type", "string", "description", "控制台命令，如 list, say, give 等")
                        ),
                        "required", List.of("serverId", "command"))));

        tools.add(new ToolDefinition("get_console",
                "[本地·直接操作] 获取服务器控制台日志（增量拉取，支持 since 参数获取新增日志）",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        tools.add(new ToolDefinition("list_plugins",
                "[本地·直接操作] 列出服务器已安装的插件及启用状态",
                ToolDefinition.stringParam("serverId", "游戏服务器ID", true)));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                case "list_servers" -> toJson(Map.of(
                        "servers", serverManager.listAll(),
                        "count", serverManager.listAll().size()));

                case "get_server_status" -> {
                    String serverId = (String) arguments.get("serverId");
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    yield toJson(si.getStatusInfo());
                }

                case "start_server" -> {
                    String serverId = (String) arguments.get("serverId");
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    boolean success = si.start();
                    yield toJson(Map.of("success", success, "serverId", serverId,
                            "status", si.getStatus().name()));
                }

                case "stop_server" -> {
                    String serverId = (String) arguments.get("serverId");
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    boolean success = si.stop();
                    yield toJson(Map.of("success", success, "serverId", serverId,
                            "status", si.getStatus().name()));
                }

                case "restart_server" -> {
                    String serverId = (String) arguments.get("serverId");
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    boolean success = si.restart();
                    yield toJson(Map.of("success", success, "serverId", serverId,
                            "status", si.getStatus().name()));
                }

                case "send_command" -> {
                    String serverId = (String) arguments.get("serverId");
                    String command = (String) arguments.get("command");
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    boolean success = si.sendCommand(command);
                    yield toJson(Map.of("success", success, "command", command));
                }

                case "get_console" -> {
                    String serverId = (String) arguments.get("serverId");
                    long since = arguments.containsKey("since")
                            ? Long.parseLong(String.valueOf(arguments.get("since"))) : -1;
                    ServerInstance si = serverManager.get(serverId);
                    if (si == null) yield toJson(Map.of("error", "服务器不存在: " + serverId));
                    RingBuffer.ConsoleData data = si.getConsole(since);
                    yield toJson(Map.of(
                            "lines", data.lines().stream().map(l -> Map.of(
                                    "lineNumber", l.lineNumber(), "content", l.content())).toList(),
                            "latestLineNumber", data.latestLineNumber()));
                }

                case "list_plugins" -> {
                    String serverId = (String) arguments.get("serverId");
                    List<Map<String, Object>> plugins = localPluginService.getPluginList(serverId);
                    yield toJson(Map.of("plugins", plugins, "count", plugins.size()));
                }

                default -> toJson(Map.of("error", "未知工具: " + toolName));
            };
        } catch (Exception e) {
            return toJson(Map.of("error", "执行失败: " + e.getMessage()));
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
