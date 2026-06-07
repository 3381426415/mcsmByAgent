package com.gitcode.mcsm_backend.config;

import com.gitcode.mcsm_backend.service.ServerConfig;
import com.gitcode.mcsm_backend.service.ServerManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Agent 全局配置 - 从 agent-config.yml 加载服务器列表和控制台缓冲区大小
 */
@Slf4j
@Service
public class AgentConfig {

    private static final String CONFIG_FILE = com.gitcode.mcsm_backend.common.McsmPaths.AGENT_CONFIG_YML;

    private int consoleBufferSize = 1000;

    private final Map<String, ServerConfig> servers = new LinkedHashMap<>();

    private boolean configValid = true;
    private String errorMessage = "";

    @Autowired
    private ServerManager serverManager;

    @PostConstruct
    public void init() {
        loadOrCreateConfig();
        validateConfig();
        registerServers();
    }

    // ==================== 加载与解析 ====================

    private void loadOrCreateConfig() {
        try {
            Files.createDirectories(Path.of(com.gitcode.mcsm_backend.common.McsmPaths.CONFIG_DIR));
        } catch (IOException e) {
            log.error("[AgentConfig] 创建 config 目录失败: {}", e.getMessage());
        }

        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            log.info("配置文件不存在，正在创建默认配置...");
            createDefaultConfig();
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(fis);

            if (data.containsKey("consoleBufferSize")) {
                consoleBufferSize = toInt(data.get("consoleBufferSize"), 1000);
            }

            // 服务器列表
            Object serversRaw = data.get("servers");
            if (serversRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> serversMap = (Map<String, Object>) serversRaw;
                for (Map.Entry<String, Object> entry : serversMap.entrySet()) {
                    String serverId = entry.getKey();
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> serverData = (Map<String, Object>) entry.getValue();
                        servers.put(serverId, ServerConfig.fromMap(serverData));
                    }
                }
            }

            // 向后兼容：旧格式
            if (servers.isEmpty() && data.containsKey("serverJarPath")) {
                log.info("[AgentConfig] 检测到旧版配置格式，自动转换为 'default' 服务器");
                ServerConfig legacy = new ServerConfig();
                legacy.setName("默认服务器");
                legacy.setDirectory(new File(String.valueOf(data.get("serverJarPath"))).getParent());
                legacy.setJarFile(new File(String.valueOf(data.get("serverJarPath"))).getName());
                if (data.containsKey("javaArgs")) {
                    legacy.setJavaArgs(String.valueOf(data.get("javaArgs")));
                }
                servers.put("default", legacy);
            }

            log.info("配置文件加载成功，共 {} 个服务器实例", servers.size());

        } catch (Exception e) {
            log.error("加载配置文件失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void createDefaultConfig() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("consoleBufferSize", 1000);

        Map<String, Object> defaultServer = new LinkedHashMap<>();
        defaultServer.put("name", "默认服务器");
        defaultServer.put("enabled", true);
        defaultServer.put("directory", "./server");
        defaultServer.put("jarFile", "server.jar");
        defaultServer.put("javaArgs", "-Xmx2G -Xms1G");
        defaultServer.put("port", 25565);
        defaultServer.put("rconPort", 25575);
        defaultServer.put("rconPassword", "");
        defaultServer.put("autoStart", false);
        defaultServer.put("javaHome", "");

        root.put("servers", Map.of("default", defaultServer));

        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            writer.write("# Minecraft Agent 配置文件\n\n");
            Yaml yaml = new Yaml();
            yaml.dump(root, writer);
            log.info("默认配置文件已创建: {}", CONFIG_FILE);
        } catch (IOException e) {
            log.error("创建配置文件失败: {}", e.getMessage());
        }

        servers.put("default", ServerConfig.fromMap(defaultServer));
    }

    private void validateConfig() {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        if (servers.isEmpty()) {
            errors.append("未配置任何服务器实例\n");
        }

        for (Map.Entry<String, ServerConfig> entry : servers.entrySet()) {
            ServerConfig cfg = entry.getValue();
            if (!cfg.isEnabled()) continue;

            File dir = new File(cfg.getDirectory());
            if (!dir.exists()) {
                warnings.append("[").append(entry.getKey()).append("] 服务器目录不存在（将在启动时创建）: ").append(dir.getAbsolutePath()).append("\n");
            }
            File jar = new File(dir, cfg.getJarFile());
            if (!dir.exists() && !jar.exists()) {
                continue;
            }
            if (!jar.exists()) {
                warnings.append("[").append(entry.getKey()).append("] jar 文件不存在: ").append(jar.getAbsolutePath()).append("\n");
            }
        }

        if (errors.length() > 0) {
            configValid = false;
            errorMessage = errors.toString();
        } else {
            configValid = true;
            errorMessage = warnings.length() > 0 ? warnings.toString() : "";
        }
    }

    private void registerServers() {
        serverManager.setConsoleBufferSize(consoleBufferSize);
        for (Map.Entry<String, ServerConfig> entry : servers.entrySet()) {
            if (entry.getValue().isEnabled()) {
                serverManager.register(entry.getKey(), entry.getValue());
            }
        }
    }

    // ==================== 公开方法 ====================

    public boolean isValid() { return configValid; }
    public String getErrorMessage() { return errorMessage; }

    public void reload() {
        servers.clear();
        loadOrCreateConfig();
        validateConfig();
        registerServers();
    }

    // ==================== Getters ====================

    public int getConsoleBufferSize() { return consoleBufferSize; }
    public Map<String, ServerConfig> getServers() { return Collections.unmodifiableMap(servers); }

    // ==================== 工具 ====================

    private static int toInt(Object val, int fallback) {
        try { return Integer.parseInt(String.valueOf(val)); }
        catch (NumberFormatException e) { return fallback; }
    }

    public synchronized void addServer(String serverId, ServerConfig config) {
        servers.put(serverId, config);
        saveConfigToFile();
    }

    public synchronized void removeServer(String serverId) {
        servers.remove(serverId);
        saveConfigToFile();
    }

    @SuppressWarnings("unchecked")
    public void saveConfigToFile() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("consoleBufferSize", consoleBufferSize);

        Map<String, Object> serversMap = new LinkedHashMap<>();
        for (Map.Entry<String, ServerConfig> entry : servers.entrySet()) {
            Map<String, Object> s = new LinkedHashMap<>();
            ServerConfig c = entry.getValue();
            s.put("name", c.getName());
            s.put("enabled", c.isEnabled());
            s.put("directory", c.getDirectory());
            s.put("jarFile", c.getJarFile());
            s.put("javaArgs", c.getJavaArgs());
            s.put("port", c.getPort());
            s.put("rconPort", c.getRconPort());
            s.put("rconPassword", c.getRconPassword());
            s.put("autoStart", c.isAutoStart());
            s.put("javaHome", c.getJavaHome());
            serversMap.put(entry.getKey(), s);
        }
        root.put("servers", serversMap);

        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            writer.write("# Minecraft Agent 配置文件\n\n");
            new Yaml().dump(root, writer);
            log.info("[AgentConfig] 配置文件已更新");
        } catch (IOException e) {
            log.error("[AgentConfig] 保存配置文件失败: {}", e.getMessage());
        }
    }
}
