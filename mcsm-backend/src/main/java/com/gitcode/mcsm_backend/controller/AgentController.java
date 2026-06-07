package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.annotation.AgentTool;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.AgentService;
import com.gitcode.mcsm_backend.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 服务器管理接口 - 启动/停止/重启 MC 服务器、查看状态、发送控制台命令
 */
@Slf4j
@RestController
@RequestMapping("/api/server")
@PreAuthorize("hasAuthority('admin:server')")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private TemplateService templateService;

    // ==================== Agent 健康检查 ====================

    /**
     * 检查 Agent 是否在线
     */
    @GetMapping("/agent/health")
    @PreAuthorize("isAuthenticated()")
    public Result<Map> agentHealth() {
        return agentService.checkAgentHealth();
    }

    // ==================== 默认服务器操作（向后兼容） ====================

    /**
     * 获取服务器状态
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Result<Map> getStatus() {
        Result<Map> result = agentService.getStatus();
        log.info("返回给前端: {}", result);
        return result;
    }

    /**
     * 启动服务器
     */
    @AgentTool(description = "[后端·自动化流程] 启动指定的 Minecraft 游戏服务器。通过后端业务层调用，带权限校验和日志记录。适用于正式运维操作。", requiresConfirm = true)
    @PostMapping("/start")
    @LogRecord(module = "服务器管理", action = "启动服务器", description = "启动 Minecraft 游戏服务器")
    public Result<String> startServer() {
        return agentService.startServer();
    }

    /**
     * 停止服务器
     */
    @AgentTool(description = "[后端·自动化流程] 停止指定的 Minecraft 游戏服务器。通过后端业务层调用，带权限校验和日志记录。需要用户确认。", requiresConfirm = true)
    @PostMapping("/stop")
    @LogRecord(module = "服务器管理", action = "停止服务器", description = "停止 Minecraft 游戏服务器")
    public Result<String> stopServer() {
        return agentService.stopServer();
    }

    /**
     * 重启服务器
     */
    @PostMapping("/restart")
    @LogRecord(module = "服务器管理", action = "重启服务器", description = "重启 Minecraft 游戏服务器")
    public Result<String> restartServer() {
        return agentService.restartServer();
    }

    // ==================== 指定服务器操作 ====================

    /**
     * 获取指定服务器状态
     */
    @GetMapping("/{serverId}/status")
    public Result<Map> getServerStatus(@PathVariable String serverId) {
        return agentService.getServerStatus(serverId);
    }

    /**
     * 启动指定服务器
     */
    @PostMapping("/{serverId}/start")
    @LogRecord(module = "服务器管理", action = "启动服务器", description = "启动服务器：#{#serverId}")
    public Result<String> startServer(@PathVariable String serverId) {
        return agentService.startServer(serverId);
    }

    /**
     * 停止指定服务器
     */
    @PostMapping("/{serverId}/stop")
    @LogRecord(module = "服务器管理", action = "停止服务器", description = "停止服务器：#{#serverId}")
    public Result<String> stopServer(@PathVariable String serverId) {
        return agentService.stopServer(serverId);
    }

    /**
     * 重启指定服务器
     */
    @PostMapping("/{serverId}/restart")
    @LogRecord(module = "服务器管理", action = "重启服务器", description = "重启服务器：#{#serverId}")
    public Result<String> restartServer(@PathVariable String serverId) {
        return agentService.restartServer(serverId);
    }

    @PostMapping("/{serverId}/force-stop")
    public Result<String> forceStopServer(@PathVariable String serverId) {
        return agentService.forceStopServer(serverId);
    }

    /**
     * 获取指定服务器控制台日志
     */
    @GetMapping("/{serverId}/console")
    public Result<Map> getConsole(@PathVariable String serverId,
                                  @RequestParam(defaultValue = "-1") long since) {
        return agentService.getConsole(serverId, since);
    }

    /**
     * 向指定服务器发送命令
     */
    @PostMapping("/{serverId}/command")
    @LogRecord(module = "服务器管理", action = "执行命令", description = "执行命令：#{#cmd} @ #{#serverId}")
    public Result<String> sendCommand(@PathVariable String serverId, @RequestParam String cmd) {
        return agentService.sendCommand(serverId, cmd);
    }

    /**
     * 更新指定服务器配置
     */
    @PutMapping("/{serverId}/config")
    @LogRecord(module = "服务器管理", action = "更新配置", description = "更新服务器配置：#{#serverId}")
    public Result<Map> updateServerConfig(@PathVariable String serverId,
                                          @RequestBody Map<String, Object> config) {
        return agentService.updateServerConfig(serverId, config);
    }

    // ==================== 服务器模板 ====================

    /**
     * 获取可用模板列表
     */
    @GetMapping("/templates")
    public Result<List<Map<String, Object>>> listTemplates() {
        return Result.success("获取成功", templateService.listTemplates());
    }

    /**
     * 使用模板创建服务器
     */
    @PostMapping("/use-template")
    @LogRecord(module = "服务器管理", action = "模板创建服务器", description = "使用模板创建服务器")
    public Result<?> useTemplate(@RequestBody Map<String, String> body) {
        String templateId = body.get("templateId");
        String name = body.get("name");
        String port = body.getOrDefault("port", "25565");
        String javaArgs = body.getOrDefault("javaArgs", "-Xms1G -Xmx2G");

        if (templateId == null || templateId.isBlank()) {
            return Result.error("模板 ID 不能为空");
        }
        if (name == null || name.isBlank()) {
            return Result.error("服务器名称不能为空");
        }

        try {
            String serverId = "s" + System.currentTimeMillis();
            String serverPath = templateService.useTemplate(templateId, serverId);

            // 解压后目录结构为 servers/{serverId}/paper-1.20.1/
            // 需要定位到实际的服务器子目录
            File extractedDir = new File(serverPath);
            File[] children = extractedDir.listFiles(File::isDirectory);
            String actualDir = (children != null && children.length == 1)
                    ? children[0].getAbsolutePath()
                    : extractedDir.getAbsolutePath();

            // 自动查找 jar 文件
            String jarFile = findJarFile(new File(actualDir));
            if (jarFile == null) {
                return Result.error("模板中未找到 jar 文件");
            }

            // 注册到 ServerManager
            Map<String, Object> registerBody = new java.util.LinkedHashMap<>();
            registerBody.put("serverId", serverId);
            registerBody.put("name", name);
            registerBody.put("directory", actualDir);
            registerBody.put("jarFile", jarFile);
            registerBody.put("port", port);
            registerBody.put("javaArgs", javaArgs);
            return agentService.registerServer(registerBody);
        } catch (Exception e) {
            return Result.error("模板创建失败: " + e.getMessage());
        }
    }

    // ==================== 服务器管理 ====================

    /**
     * 获取所有服务器列表
     */
    @GetMapping("/list")
    public Result<?> listServers() {
        return agentService.listServers();
    }

    /**
     * 注册新服务器
     */
    @PostMapping("/register")
    @LogRecord(module = "服务器管理", action = "注册服务器", description = "注册新服务器")
    public Result<?> registerServer(@RequestBody Map<String, Object> body) {
        return agentService.registerServer(body);
    }

    /**
     * 删除指定服务器
     */
    @DeleteMapping("/{serverId}")
    @LogRecord(module = "服务器管理", action = "删除服务器", description = "删除服务器：#{#serverId}")
    public Result<String> deleteServer(@PathVariable String serverId) {
        return agentService.deleteServer(serverId);
    }

    /**
     * 重命名指定服务器
     */
    @PostMapping("/{serverId}/rename")
    @LogRecord(module = "服务器管理", action = "重命名服务器", description = "重命名服务器：#{#serverId}")
    public Result<Map> renameServer(@PathVariable String serverId, @RequestBody Map<String, String> body) {
        return agentService.renameServer(serverId, body.get("name"));
    }

    private String findJarFile(File dir) {
        File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) return null;
        // 优先返回 server.jar，否则返回第一个
        for (File jar : jars) {
            if ("server.jar".equals(jar.getName())) return "server.jar";
        }
        return jars[0].getName();
    }
}