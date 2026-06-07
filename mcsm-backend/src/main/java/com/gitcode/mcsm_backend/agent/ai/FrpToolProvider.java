package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.service.FrpService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FRP 客户端管理工具提供者（本地）
 *
 * 提供给 AI 智能体的 FRP 管理工具：
 * 查看状态、读写配置、启停 frpc。
 */
public class FrpToolProvider implements LocalToolProvider {

    private final FrpService frpService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FrpToolProvider(FrpService frpService) {
        this.frpService = frpService;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition("frp_get_status",
                "[FRP] 查看 frpc 客户端运行状态（是否已安装、是否运行中）",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("frp_get_config",
                "[FRP] 读取 frpc 配置文件内容（frpc.toml）",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("frp_save_config",
                "[FRP] 保存 frpc 配置文件，重启 frpc 后生效。配置格式为 TOML",
                ToolDefinition.stringParam("content", "frpc.toml 配置文件完整内容", true)));

        tools.add(new ToolDefinition("frp_start",
                "[FRP] 启动 frpc 客户端进程",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("frp_stop",
                "[FRP] 停止 frpc 客户端进程",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("frp_get_logs",
                "[FRP] 获取 frpc 运行日志",
                ToolDefinition.noParams()));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                case "frp_get_status" -> toJson(Map.of(
                        "status", frpService.getStatus(),
                        "installed", frpService.isInstalled(),
                        "running", frpService.isRunning()
                ));

                case "frp_get_config" -> toJson(Map.of(
                        "config", frpService.getConfig()
                ));

                case "frp_save_config" -> {
                    String content = (String) arguments.get("content");
                    if (content == null || content.isBlank()) {
                        yield toJson(Map.of("success", false, "error", "content 不能为空"));
                    }
                    boolean saved = frpService.saveConfig(content);
                    yield toJson(Map.of("success", saved, "msg", saved ? "配置已保存" : "保存失败"));
                }

                case "frp_start" -> {
                    boolean started = frpService.start();
                    yield toJson(Map.of("success", started, "msg", started ? "frpc 已启动" : "启动失败，查看日志了解详情"));
                }

                case "frp_stop" -> {
                    boolean stopped = frpService.stop();
                    yield toJson(Map.of("success", stopped, "msg", stopped ? "frpc 已停止" : "停止失败"));
                }

                case "frp_get_logs" -> toJson(Map.of(
                        "logs", frpService.getLogs(50)
                ));

                default -> toJson(Map.of("error", "未知工具: " + toolName));
            };
        } catch (Exception e) {
            return toJson(Map.of("error", e.getMessage()));
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}
