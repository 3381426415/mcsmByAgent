package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
/**
 * 插件管理接口 - 安装、卸载、启用、禁用、上传 Minecraft 插件
 */
@RequestMapping("/api/plugins")
@PreAuthorize("hasAuthority('admin:server')")
public class PluginController {

    @Autowired
    private PluginService pluginService;

    // ==================== 旧版单服务器端点（保持不变） ====================

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return pluginService.getPluginList();
    }

    @PostMapping("/enable")
    @LogRecord(module = "插件管理", action = "启用插件", description = "启用插件：#{#fileName}")
    public Result<String> enable(@RequestParam String fileName) {
        return pluginService.enablePlugin(fileName);
    }

    @PostMapping("/disable")
    @LogRecord(module = "插件管理", action = "禁用插件", description = "禁用插件：#{#fileName}")
    public Result<String> disable(@RequestParam String fileName) {
        return pluginService.disablePlugin(fileName);
    }

    @DeleteMapping("/delete")
    @LogRecord(module = "插件管理", action = "删除插件", description = "删除插件：#{#fileName}")
    public Result<String> delete(@RequestParam String fileName) {
        return pluginService.deletePlugin(fileName);
    }

    @PostMapping("/upload")
    @LogRecord(module = "插件管理", action = "上传插件", description = "上传插件文件", recordParams = false)
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return pluginService.uploadPlugin(file);
    }

    // ==================== 新版多服务器插件端点 ====================

    @GetMapping("/{serverId}/list")
    public Result<List<Map<String, Object>>> listByServer(@PathVariable String serverId) {
        return pluginService.getPluginList(serverId);
    }

    @PostMapping("/{serverId}/enable")
    @LogRecord(module = "插件管理", action = "启用插件", description = "启用插件：#{#fileName} @ #{#serverId}")
    public Result<String> enableByServer(@PathVariable String serverId, @RequestParam String fileName) {
        return pluginService.enablePlugin(serverId, fileName);
    }

    @PostMapping("/{serverId}/disable")
    @LogRecord(module = "插件管理", action = "禁用插件", description = "禁用插件：#{#fileName} @ #{#serverId}")
    public Result<String> disableByServer(@PathVariable String serverId, @RequestParam String fileName) {
        return pluginService.disablePlugin(serverId, fileName);
    }

    @DeleteMapping("/{serverId}/delete")
    @LogRecord(module = "插件管理", action = "删除插件", description = "删除插件：#{#fileName} @ #{#serverId}")
    public Result<String> deleteByServer(@PathVariable String serverId, @RequestParam String fileName) {
        return pluginService.deletePlugin(serverId, fileName);
    }

    @PostMapping("/{serverId}/upload")
    @LogRecord(module = "插件管理", action = "上传插件", description = "上传插件文件 @ #{#serverId}", recordParams = false)
    public Result<String> uploadByServer(@PathVariable String serverId, @RequestParam("file") MultipartFile file) {
        return pluginService.uploadPlugin(serverId, file);
    }
}
