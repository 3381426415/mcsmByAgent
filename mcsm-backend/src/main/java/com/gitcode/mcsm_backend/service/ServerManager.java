package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.config.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器实例总管
 * 管理所有 Minecraft 服务器实例的注册、启停、日志
 */
@Slf4j
@Service
public class ServerManager {

    private final Map<String, ServerInstance> servers = new ConcurrentHashMap<>();
    private int consoleBufferSize = 1000;

    @Lazy
    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private PluginDeployService pluginDeployService;

    // ==================== 注册管理 ====================

    /**
     * 注册一个服务器实例（来自配置文件或动态注册）
     */
    public void register(String serverId, ServerConfig config) {
        ServerInstance si = new ServerInstance(serverId, config, consoleBufferSize);
        servers.put(serverId, si);
        log.info("[ServerManager] 已注册服务器: {} ({})", serverId, config.getName());
    }

    /**
     * 移除服务器实例
     */
    public void unregister(String serverId) {
        ServerInstance si = servers.remove(serverId);
        if (si != null && si.getStatus() != ServerInstance.Status.STOPPED) {
            si.stop();
        }
    }

    /**
     * 获取服务器实例
     */
    public ServerInstance get(String serverId) {
        return servers.get(serverId);
    }

    public ServerConfig getConfig(String serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null ? si.getConfig() : null;
    }

    /**
     * 列出所有实例
     */
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServerInstance si : servers.values()) {
            result.add(si.getStatusInfo());
        }
        return result;
    }

    /**
     * 设置控制台缓冲区大小
     */
    public void setConsoleBufferSize(int size) {
        this.consoleBufferSize = size;
    }

    /**
     * 关闭所有服务器（用于退出钩子）
     */
    public void shutdown() {
        log.info("[ServerManager] 正在关闭所有服务器...");
        for (ServerInstance si : servers.values()) {
            if (si.getStatus() != ServerInstance.Status.STOPPED) {
                si.stop();
            }
        }
        log.info("[ServerManager] 所有服务器已关闭");
    }

    /**
     * 检查是否至少一个服务器在运行
     */
    public boolean hasRunningServer() {
        return servers.values().stream()
                .anyMatch(si -> si.getStatus() == ServerInstance.Status.RUNNING
                        || si.getStatus() == ServerInstance.Status.STARTING);
    }

    /**
     * 获取"默认"服务器（用于向后兼容）
     */
    public ServerInstance getDefault() {
        if (servers.isEmpty()) return null;
        if (servers.containsKey("default")) return servers.get("default");
        return servers.values().iterator().next();
    }

    /**
     * 动态注册服务器（来自前端请求）
     * 校验 serverId 不重复、目录存在、jar 存在
     */
    public String registerDynamic(String serverId, ServerConfig config) {
        // 1. 校验 serverId
        if (serverId == null || serverId.isBlank()) {
            return "serverId 不能为空";
        }
        if (servers.containsKey(serverId)) {
            return "服务器 ID 已存在: " + serverId;
        }

        // 2. 校验目录
        File dir = new File(config.getDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            return "服务器目录不存在: " + dir.getAbsolutePath();
        }

        // 3. 校验 jar
        File jar = new File(dir, config.getJarFile());
        if (!jar.exists() || !jar.isFile()) {
            return "jar 文件不存在: " + jar.getAbsolutePath();
        }

        // 4. 部署插件（复制 jar + 生成配置）
        String deployResult = pluginDeployService.deployPlugin(config.getDirectory(), serverId);
        if (deployResult != null) {
            log.error("[ServerManager] 插件部署失败: {}", deployResult);
            // 不中断注册，插件可以稍后手动部署
        }

        // 5. 注册
        register(serverId, config);

        // 6. 持久化到配置文件
        if (agentConfig != null) {
            agentConfig.addServer(serverId, config);
        }

        return null; // null = 成功
    }
}
