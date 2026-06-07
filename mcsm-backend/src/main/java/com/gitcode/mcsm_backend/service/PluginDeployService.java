package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.config.AgentConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.Properties;
import java.util.Map;

/**
 * 插件部署服务 - 统一的插件安装流程
 * 复制插件 jar + 生成配置文件，适用于新建服务器和添加已有服务器
 */
@Slf4j
@Service
public class PluginDeployService {

    @Value("${plugin.secret:}")
    private String pluginSecret;

    @Value("${server.port:8000}")
    private int serverPort;

    @Autowired
    private AgentConfig agentConfig;

    /**
     * 部署插件到指定服务器目录
     * @param serverDir 服务器根目录
     * @param serverId  服务器 ID
     * @return null=成功，否则=错误信息
     */
    public String deployPlugin(String serverDir, String serverId) {
        File dir = new File(serverDir);
        if (!dir.exists() || !dir.isDirectory()) {
            return "服务器目录不存在: " + dir.getAbsolutePath();
        }

        File pluginsDir = new File(dir, "plugins");
        if (!pluginsDir.exists()) pluginsDir.mkdirs();

        // 1. 复制插件 jar
        try {
            File bridgeJar = findBridgeJar();
            if (bridgeJar == null) {
                return "找不到 mcsm-bridge.jar 插件文件";
            }
            File target = new File(pluginsDir, "mcsm-bridge.jar");
            Files.copy(bridgeJar.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[PluginDeploy] 插件已复制: {}", target.getAbsolutePath());
        } catch (IOException e) {
            return "复制插件失败: " + e.getMessage();
        }

        // 2. 生成配置文件
        try {
            File configDir = new File(pluginsDir, "mcsn-serverBridge");
            if (!configDir.exists()) configDir.mkdirs();
            File configFile = new File(configDir, "config.yml");

            // 读取数据库配置
            String dbHost = "127.0.0.1", dbPort = "3306", dbUser = "root", dbPass = "mcsm2024", dbName = "mcsm";
            File dbProps = new File("data/config/db.properties");
            if (dbProps.exists()) {
                Properties p = new Properties();
                try (InputStream in = new FileInputStream(dbProps)) { p.load(in); }
                dbHost = p.getProperty("db.host", dbHost);
                dbPort = p.getProperty("db.port", dbPort);
                dbUser = p.getProperty("db.username", dbUser);
                dbPass = p.getProperty("db.password", dbPass);
                dbName = p.getProperty("db.name", dbName);
            }

            String backendHost = getLocalAddress();
            String configContent = String.format(
                    "# MCSM 插件配置（自动生成，请勿手动修改）\n" +
                    "backend-url: ws://%s:%d/ws/plugin\n" +
                    "server-id: %s\n" +
                    "secret: %s\n" +
                    "database:\n" +
                    "  host: %s\n" +
                    "  port: %s\n" +
                    "  username: %s\n" +
                    "  password: %s\n" +
                    "  name: %s\n",
                    backendHost, serverPort, serverId, pluginSecret,
                    dbHost, dbPort, dbUser, dbPass, dbName
            );

            Files.writeString(configFile.toPath(), configContent);
            log.info("[PluginDeploy] 配置已生成: {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            return "生成配置失败: " + e.getMessage();
        }

        return null; // 成功
    }

    /**
     * 后端启动时自动刷新所有已注册服务器的插件配置
     * 确保每次重启后插件配置中的后端地址都是最新的
     */
    @PostConstruct
    public void refreshAllConfigs() {
        Map<String, ServerConfig> servers = agentConfig.getServers();
        if (servers.isEmpty()) return;

        String backendHost = getLocalAddress();
        log.info("[PluginDeploy] 正在刷新所有服务器的插件配置，后端地址: {}:{}", backendHost, serverPort);

        for (Map.Entry<String, ServerConfig> entry : servers.entrySet()) {
            String serverId = entry.getKey();
            ServerConfig config = entry.getValue();
            String result = refreshPluginConfig(config.getDirectory(), serverId, backendHost);
            if (result != null) {
                log.error("[PluginDeploy] 刷新失败 [{}]: {}", serverId, result);
            }
        }
    }

    /**
     * 只刷新插件配置文件（不复制 jar）
     */
    private String refreshPluginConfig(String serverDir, String serverId, String backendHost) {
        File configDir = new File(new File(serverDir, "plugins"), "mcsn-serverBridge");
        File configFile = new File(configDir, "config.yml");
        try {
            if (!configDir.exists()) configDir.mkdirs();
            String configContent = String.format(
                    "# MCSM 插件配置（自动生成，请勿手动修改）\n" +
                    "backend-url: ws://%s:%d/ws/plugin\n" +
                    "server-id: %s\n" +
                    "secret: %s\n",
                    backendHost, serverPort, serverId, pluginSecret
            );
            Files.writeString(configFile.toPath(), configContent);
            log.info("[PluginDeploy] 配置已刷新: {}", configFile.getAbsolutePath());
            return null;
        } catch (IOException e) {
            return "刷新配置失败: " + e.getMessage();
        }
    }

    /**
     * 查找 mcsm-bridge.jar 文件
     * 搜索顺序：data/plugins/ → 当前目录
     */
    private File findBridgeJar() {
        File jar = new File(com.gitcode.mcsm_backend.common.McsmPaths.BRIDGE_JAR);
        if (jar.exists()) return jar;

        jar = new File("mcsm-bridge.jar");
        if (jar.exists()) return jar;

        return null;
    }

    /**
     * 获取本机局域网 IP 地址
     */
    private String getLocalAddress() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String ip = address.getHostAddress();
            // 如果是回环地址，尝试获取真实 IP
            if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                return "127.0.0.1";
            }
            return ip;
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
