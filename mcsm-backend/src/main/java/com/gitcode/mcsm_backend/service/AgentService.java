package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.config.AgentConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Agent 服务
 *
 * 直接调用本地 ServerManager/ServerService，
 * 负责 Minecraft 服务器的启停控制和状态查询。
 */
@Service
@SuppressWarnings("unchecked")
public class AgentService {

    @Autowired
    private ServerManager serverManager;

    @Autowired(required = false)
    private AgentConfig agentConfig;

    @Autowired
    private ServerService serverService;

    // ==================== Agent 健康检查 ====================

    /**
     * 检查 Agent 是否在线（现在 Agent 在后端内部，始终在线）
     */
    public Result<Map> checkAgentHealth() {
        return Result.success("Agent 在线", Map.of("online", true));
    }

    // ==================== 默认服务器操作（向后兼容） ====================

    /**
     * 获取服务器状态（是否运行、PID）
     */
    public Result<Map> getStatus() {
        try {
            ServerInstance si = serverManager.getDefault();
            if (si == null) {
                return Result.error("未配置默认服务器实例");
            }
            return Result.success("获取成功", si.getStatusInfo());
        } catch (Exception e) {
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 启动 Minecraft 服务器
     */
    public Result<String> startServer() {
        try {
            boolean success = serverService.startServer();
            return success ? Result.successMsg("服务器启动成功") : Result.error("服务器启动失败");
        } catch (Exception e) {
            return Result.error("启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止 Minecraft 服务器
     */
    public Result<String> stopServer() {
        try {
            boolean success = serverService.stopServer();
            return success ? Result.successMsg("服务器已停止") : Result.error("服务器停止失败");
        } catch (Exception e) {
            return Result.error("停止失败: " + e.getMessage());
        }
    }

    /**
     * 重启 Minecraft 服务器
     */
    public Result<String> restartServer() {
        try {
            boolean success = serverService.restartServer();
            return success ? Result.successMsg("服务器重启成功") : Result.error("服务器重启失败");
        } catch (Exception e) {
            return Result.error("重启失败: " + e.getMessage());
        }
    }

    // ==================== 指定服务器操作 ====================

    /**
     * 获取指定服务器状态
     */
    public Result<Map> getServerStatus(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) {
                return Result.error("服务器不存在: " + serverId);
            }
            return Result.success("获取成功", si.getStatusInfo());
        } catch (Exception e) {
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }

    /**
     * 启动指定服务器
     */
    public Result<String> startServer(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            boolean success = si.start();
            return success ? Result.successMsg("服务器启动成功") : Result.error("服务器启动失败");
        } catch (Exception e) {
            return Result.error("启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定服务器
     */
    public Result<String> stopServer(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            boolean success = si.stop();
            return success ? Result.successMsg("服务器已停止") : Result.error("服务器停止失败");
        } catch (Exception e) {
            return Result.error("停止失败: " + e.getMessage());
        }
    }

    /**
     * 重启指定服务器
     */
    public Result<String> forceStopServer(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            return si.forceStop() ? Result.successMsg("已强制终止") : Result.error("强制终止失败");
        } catch (Exception e) {
            return Result.error("强制终止失败: " + e.getMessage());
        }
    }

    public Result<String> restartServer(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            boolean success = si.restart();
            return success ? Result.successMsg("服务器重启成功") : Result.error("服务器重启失败");
        } catch (Exception e) {
            return Result.error("重启失败: " + e.getMessage());
        }
    }


    /**
     * 获取指定服务器控制台日志
     */
    public Result<Map> getConsole(String serverId, long since) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            RingBuffer.ConsoleData data = si.getConsole(since);
            return Result.success("获取成功", Map.of(
                    "lines", data.lines(),
                    "latestLineNumber", data.latestLineNumber()
            ));
        } catch (Exception e) {
            return Result.error("获取控制台失败: " + e.getMessage());
        }
    }

    /**
     * 向指定服务器发送命令
     */
    public Result<String> sendCommand(String serverId, String cmd) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            boolean success = si.sendCommand(cmd);
            return success ? Result.successMsg("命令已发送") : Result.error("命令发送失败（服务器可能未运行）");
        } catch (Exception e) {
            return Result.error("发送命令失败: " + e.getMessage());
        }
    }

    /**
     * 更新指定服务器配置
     */
    public Result<Map> updateServerConfig(String serverId, Map<String, Object> config) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);

            ServerConfig sc = si.getConfig();
            if (config.containsKey("name")) sc.setName(String.valueOf(config.get("name")));
            if (config.containsKey("directory")) sc.setDirectory(String.valueOf(config.get("directory")));
            if (config.containsKey("jarFile")) sc.setJarFile(String.valueOf(config.get("jarFile")));
            if (config.containsKey("javaArgs")) sc.setJavaArgs(String.valueOf(config.get("javaArgs")));
            if (config.containsKey("port")) sc.setPort(Integer.parseInt(String.valueOf(config.get("port"))));
            if (config.containsKey("rconPort")) sc.setRconPort(Integer.parseInt(String.valueOf(config.get("rconPort"))));
            if (config.containsKey("rconPassword")) sc.setRconPassword(String.valueOf(config.get("rconPassword")));
            if (config.containsKey("autoStart")) sc.setAutoStart(Boolean.parseBoolean(String.valueOf(config.get("autoStart"))));
            if (config.containsKey("javaHome")) sc.setJavaHome(String.valueOf(config.get("javaHome")));

            // 持久化到 agent-config.yml
            if (agentConfig != null) agentConfig.addServer(serverId, sc);

            return Result.success("配置已更新", si.getStatusInfo());
        } catch (Exception e) {
            return Result.error("更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有服务器列表
     */
    public Result<?> listServers() {
        try {
            return Result.success("获取成功", serverManager.listAll());
        } catch (Exception e) {
            return Result.error("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 注册新服务器
     */
    public Result<?> registerServer(Map<String, Object> body) {
        try {
            String serverId = (String) body.get("serverId");
            if (serverId == null || serverId.isBlank()) return Result.error("服务器 ID 不能为空");

            ServerConfig config = new ServerConfig();
            String dir = body.containsKey("directory") ? String.valueOf(body.get("directory")) : "";
            if (dir.isBlank() && !body.containsKey("templateId")) {
                return Result.error("服务器目录不能为空，请输入完整的服务器目录路径");
            }
            if (body.containsKey("name")) config.setName(String.valueOf(body.get("name")));
            config.setDirectory(dir);
            if (body.containsKey("jarFile")) config.setJarFile(String.valueOf(body.get("jarFile")));
            if (body.containsKey("javaArgs")) config.setJavaArgs(String.valueOf(body.get("javaArgs")));
            if (body.containsKey("port")) config.setPort(Integer.parseInt(String.valueOf(body.get("port"))));
            if (body.containsKey("rconPort")) config.setRconPort(Integer.parseInt(String.valueOf(body.get("rconPort"))));
            if (body.containsKey("rconPassword")) config.setRconPassword(String.valueOf(body.get("rconPassword")));

            String error = serverManager.registerDynamic(serverId, config);
            if (error != null) {
                return Result.error(error);
            }
            return Result.successMsg("服务器注册成功");
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定服务器（含文件删除和配置移除）
     */
    public Result<String> deleteServer(String serverId) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);

            // 获取服务器目录
            String directory = si.getConfig().getDirectory();

            // 从内存移除
            serverManager.unregister(serverId);

            // 从持久化配置移除
            if (agentConfig != null) {
                agentConfig.removeServer(serverId);
            }

            // 删除服务器目录
            File dir = new File(directory);
            if (dir.exists()) {
                deleteDirectory(dir);
            }

            return Result.successMsg("服务器已删除");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    /**
     * 重命名指定服务器
     */
    public Result<Map> renameServer(String serverId, String newName) {
        try {
            ServerInstance si = serverManager.get(serverId);
            if (si == null) return Result.error("服务器不存在: " + serverId);
            si.rename(newName);
            return Result.success("重命名成功", si.getStatusInfo());
        } catch (Exception e) {
            return Result.error("重命名失败: " + e.getMessage());
        }
    }
}
