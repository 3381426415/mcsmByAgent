package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PluginConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统设置管理接口 - 管理员查看和修改 AI、插件等配置
 */
@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasAuthority('admin:server')")
public class SettingsController {

    @Autowired
    private PluginConnectionManager pluginConnectionManager;

    @Value("${server.port:8000}")
    private int serverPort;

    /** 获取当前配置 */
    @GetMapping
    public Result<Map<String, Object>> getConfig() {
        try {
            File propsFile = findApplicationProperties();
            if (propsFile == null) return Result.error("找不到配置文件");

            String content = Files.readString(propsFile.toPath());
            Map<String, Object> config = new LinkedHashMap<>();

            // 数据库（只读展示）
            Map<String, String> db = new LinkedHashMap<>();
            db.put("url", extractValue(content, "spring.datasource.url", ""));
            db.put("username", extractValue(content, "spring.datasource.username", ""));
            config.put("database", db);

            // 服务器信息
            Map<String, String> server = new LinkedHashMap<>();
            server.put("host", InetAddress.getLocalHost().getHostAddress());
            server.put("port", String.valueOf(serverPort));
            config.put("server", server);

            // 插件配置
            Map<String, String> plugin = new LinkedHashMap<>();
            plugin.put("secret", extractValue(content, "plugin.secret", ""));
            config.put("plugin", plugin);

            return Result.success("获取成功", config);
        } catch (Exception e) {
            return Result.error("读取配置失败: " + e.getMessage());
        }
    }

    /** 保存配置 */
    @PutMapping
    public Result<String> updateConfig(@RequestBody Map<String, Object> body) {
        try {
            File propsFile = findApplicationProperties();
            if (propsFile == null) return Result.error("找不到配置文件");

            String content = Files.readString(propsFile.toPath());

            // 服务器端口
            @SuppressWarnings("unchecked")
            Map<String, String> server = (Map<String, String>) body.get("server");
            if (server != null && server.containsKey("port")) {
                content = updateProperty(content, "server.port", server.get("port"));
            }

            try (FileWriter fw = new FileWriter(propsFile)) {
                fw.write(content);
            }

            return Result.successMsg("配置已保存，部分配置需要重启后端才能生效");
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /** 获取已连接的插件列表 */
    @GetMapping("/plugins")
    public Result<List<Map<String, Object>>> getConnectedPlugins() {
        return Result.success("获取成功", pluginConnectionManager.listConnected());
    }

    /** 测试数据库连接 */
    @PostMapping("/test-db")
    public Result<String> testDb(@RequestBody Map<String, String> body) {
        String url = body.getOrDefault("url", "");
        String user = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            return Result.successMsg("数据库连接成功");
        } catch (Exception e) {
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    private String extractValue(String content, String key, String defaultValue) {
        String pattern = key + "=";
        int start = content.indexOf(pattern);
        if (start == -1) return defaultValue;
        int valueStart = start + pattern.length();
        int end = content.indexOf('\n', valueStart);
        if (end == -1) end = content.length();
        return content.substring(valueStart, end).trim();
    }

    private String updateProperty(String content, String key, String value) {
        String pattern = key + "=";
        int start = content.indexOf(pattern);
        if (start == -1) {
            return content + "\n" + key + "=" + value;
        }
        int end = content.indexOf('\n', start);
        if (end == -1) {
            end = content.length();
        }
        return content.substring(0, start) + key + "=" + value + content.substring(end);
    }

    private File findApplicationProperties() {
        File external = new File(com.gitcode.mcsm_backend.common.McsmPaths.APPLICATION_PROPERTIES);
        if (external.exists()) return external;

        File dev = new File("src/main/resources/application.properties");
        if (dev.exists()) return dev;

        return null;
    }
}
