package com.gitcode.mcsm_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 本地插件管理服务（文件操作）
 * 直接操作服务器目录下的 plugins 文件夹，不通过 HTTP 代理
 * 注意：与 PluginService（代理到游戏插件端口 9001）不同，本服务处理本地文件
 */
@Slf4j
@Service
public class LocalPluginService {

    @Autowired
    private ServerManager serverManager;

    // ==================== 指定 serverId ====================

    public List<Map<String, Object>> getPluginList(String serverId) {
        return getPluginList(serverManager.get(serverId));
    }

    public boolean savePlugin(String serverId, String fileName, InputStream inputStream) {
        return savePlugin(serverManager.get(serverId), fileName, inputStream);
    }

    public boolean enablePlugin(String serverId, String fileName) {
        return enablePlugin(serverManager.get(serverId), fileName);
    }

    public boolean disablePlugin(String serverId, String fileName) {
        return disablePlugin(serverManager.get(serverId), fileName);
    }

    public boolean deletePlugin(String serverId, String fileName) {
        return deletePlugin(serverManager.get(serverId), fileName);
    }

    // ==================== 默认服务器（向后兼容） ====================

    public List<Map<String, Object>> getPluginList() {
        ServerInstance si = serverManager.getDefault();
        return si != null ? getPluginList(si) : Collections.emptyList();
    }

    public boolean savePlugin(String fileName, InputStream inputStream) {
        ServerInstance si = serverManager.getDefault();
        return si != null && savePlugin(si, fileName, inputStream);
    }

    public boolean enablePlugin(String fileName) {
        ServerInstance si = serverManager.getDefault();
        return si != null && enablePlugin(si, fileName);
    }

    public boolean disablePlugin(String fileName) {
        ServerInstance si = serverManager.getDefault();
        return si != null && disablePlugin(si, fileName);
    }

    public boolean deletePlugin(String fileName) {
        ServerInstance si = serverManager.getDefault();
        return si != null && deletePlugin(si, fileName);
    }

    // ==================== 核心实现 ====================

    private File getPluginsDir(ServerInstance si) {
        File dir = new File(si.getConfig().getDirectory(), "plugins");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private List<Map<String, Object>> getPluginList(ServerInstance si) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (si == null) return result;
        File pluginsDir = getPluginsDir(si);
        File[] files = pluginsDir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            String fileName = file.getName();
            if (!fileName.endsWith(".jar") && !fileName.endsWith(".jar.disabled")) continue;

            boolean enabled = fileName.endsWith(".jar");
            String baseName = fileName.replace(".jar.disabled", "").replace(".jar", "");
            String displayName = baseName.replaceAll("-\\d+(\\.\\d+)*.*$", "");

            Map<String, Object> info = new HashMap<>();
            info.put("fileName", fileName);
            info.put("name", displayName);
            info.put("enabled", enabled);
            info.put("size", file.length());
            info.put("lastModified", file.lastModified());
            result.add(info);
        }
        return result;
    }

    private boolean savePlugin(ServerInstance si, String fileName, InputStream inputStream) {
        if (si == null) return false;
        fileName = decode(fileName);
        if (!fileName.endsWith(".jar")) {
            log.error("[LocalPluginService:{}] 拒绝保存非 jar 文件: {}", si.getServerId(), fileName);
            return false;
        }
        File targetFile = new File(getPluginsDir(si), fileName);
        try {
            Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[LocalPluginService:{}] 插件已保存: {}", si.getServerId(), fileName);
            return true;
        } catch (IOException e) {
            log.error("[LocalPluginService:{}] 保存失败: {}", si.getServerId(), e.getMessage());
            return false;
        }
    }

    private boolean enablePlugin(ServerInstance si, String fileName) {
        if (si == null) return false;
        fileName = decode(fileName);
        File pluginsDir = getPluginsDir(si);
        File disabledFile = new File(pluginsDir,
                fileName.endsWith(".disabled") ? fileName : fileName + ".disabled");
        if (!disabledFile.exists()) {
            log.error("[LocalPluginService:{}] 启用失败 - 文件不存在: {}", si.getServerId(), disabledFile.getName());
            return false;
        }
        String jarFileName = disabledFile.getName().replace(".disabled", "");
        File jarFile = new File(pluginsDir, jarFileName);
        try {
            Files.move(disabledFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[LocalPluginService:{}] 插件已启用: {}", si.getServerId(), jarFileName);
            return true;
        } catch (IOException e) {
            log.error("[LocalPluginService:{}] 启用失败: {}", si.getServerId(), e.getMessage());
            return false;
        }
    }

    private boolean disablePlugin(ServerInstance si, String fileName) {
        if (si == null) return false;
        fileName = decode(fileName);
        File pluginsDir = getPluginsDir(si);
        String jarFileName = fileName.replace(".disabled", "");
        File jarFile = new File(pluginsDir, jarFileName);
        if (!jarFile.exists()) {
            log.error("[LocalPluginService:{}] 禁用失败 - 文件不存在: {}", si.getServerId(), jarFile.getName());
            return false;
        }
        File disabledFile = new File(pluginsDir, jarFileName + ".disabled");
        try {
            Files.move(jarFile.toPath(), disabledFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("[LocalPluginService:{}] 插件已禁用: {}", si.getServerId(), jarFileName);
            return true;
        } catch (IOException e) {
            log.error("[LocalPluginService:{}] 禁用失败: {}", si.getServerId(), e.getMessage());
            return false;
        }
    }

    private boolean deletePlugin(ServerInstance si, String fileName) {
        if (si == null) return false;
        fileName = decode(fileName);
        File pluginsDir = getPluginsDir(si);
        File jarFile = new File(pluginsDir, fileName);
        File disabledFile = new File(pluginsDir, fileName + ".disabled");
        boolean deleted = false;
        if (jarFile.exists()) deleted = jarFile.delete();
        if (disabledFile.exists()) deleted = disabledFile.delete() || deleted;
        if (deleted) {
            log.info("[LocalPluginService:{}] 插件已删除: {}", si.getServerId(), fileName);
        } else {
            log.error("[LocalPluginService:{}] 删除失败 - 文件不存在: {}", si.getServerId(), fileName);
        }
        return deleted;
    }

    private String decode(String fileName) {
        return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
    }
}
