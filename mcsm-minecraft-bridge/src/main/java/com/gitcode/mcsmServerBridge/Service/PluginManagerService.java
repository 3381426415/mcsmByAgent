package com.gitcode.mcsmServerBridge.Service;

import com.gitcode.mcsmServerBridge.McsmBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class PluginManagerService {

    private final McsmBridge plugin;
    private final File pluginsFolder;

    public PluginManagerService(McsmBridge plugin) {
        this.plugin = plugin;
        this.pluginsFolder = plugin.getDataFolder().getParentFile();
    }

    /**
     * 获取所有插件列表（纯文件判断：.jar = 启用，.jar.disabled = 禁用）
     */
    public List<Map<String, Object>> getPluginList() {
        List<Map<String, Object>> result = new ArrayList<>();

        File[] files = pluginsFolder.listFiles();
        if (files == null) return result;

        for (File file : files) {
            String fileName = file.getName();

            // 只看 .jar 和 .jar.disabled 文件
            if (!fileName.endsWith(".jar") && !fileName.endsWith(".jar.disabled")) {
                continue;
            }

            Map<String, Object> info = new HashMap<>();

            // 判断状态：.jar = 启用，.jar.disabled = 禁用
            boolean enabled = fileName.endsWith(".jar");

            // 提取插件名（去掉后缀和版本号）
            String baseName = fileName.replace(".jar.disabled", "").replace(".jar", "");
            String displayName = baseName.replaceAll("-\\d+(\\.\\d+)*.*$", "");

            info.put("fileName", fileName);
            info.put("name", displayName);
            info.put("enabled", enabled);
            info.put("size", file.length());
            info.put("lastModified", file.lastModified());

            result.add(info);
        }

        return result;
    }

    /**
     * 启用插件：将 .jar.disabled 重命名为 .jar
     */
    public boolean enablePlugin(String fileName) {
        File disabledFile = new File(pluginsFolder, fileName);

        // 如果传入的是 .jar，但实际是 .jar.disabled
        if (!fileName.endsWith(".jar.disabled")) {
            disabledFile = new File(pluginsFolder, fileName + ".disabled");
        }

        if (!disabledFile.exists()) {
            plugin.getLogger().warning("File not found: " + disabledFile.getName());
            return false;
        }

        String jarFileName = disabledFile.getName().replace(".disabled", "");
        File jarFile = new File(pluginsFolder, jarFileName);

        try {
            Files.move(disabledFile.toPath(), jarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Plugin enabled: " + jarFileName);

            // Try loading plugin
            tryLoadPlugin(jarFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to enable plugin: " + e.getMessage());
            return false;
        }
    }

    /**
     * 禁用插件：将 .jar 重命名为 .jar.disabled
     */
    public boolean disablePlugin(String fileName) {
        // 统一处理：去掉可能的 .disabled 后缀
        String jarFileName = fileName.replace(".disabled", "");
        File jarFile = new File(pluginsFolder, jarFileName);

        if (!jarFile.exists()) {
            plugin.getLogger().warning("File not found: " + jarFileName);
            return false;
        }

        // Unload loaded plugin first
        unloadPlugin(jarFileName);

        // Rename to .disabled
        File disabledFile = new File(pluginsFolder, jarFileName + ".disabled");
        try {
            Files.move(jarFile.toPath(), disabledFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Plugin disabled: " + jarFileName);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to disable plugin: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除插件
     */
    public boolean deletePlugin(String fileName) {
        // 先卸载
        unloadPlugin(fileName);

        // 删除文件（可能是 .jar 或 .jar.disabled）
        File jarFile = new File(pluginsFolder, fileName);
        File disabledFile = new File(pluginsFolder, fileName + ".disabled");

        boolean deleted = false;
        if (jarFile.exists()) {
            deleted = jarFile.delete();
        }
        if (disabledFile.exists()) {
            deleted = disabledFile.delete() || deleted;
        }

        if (deleted) {
            plugin.getLogger().info("Plugin deleted: " + fileName);
        }
        return deleted;
    }

    /**
     * 保存上传的插件
     */
    public boolean savePlugin(String fileName, InputStream inputStream) {
        if (!fileName.endsWith(".jar")) {
            plugin.getLogger().warning("Rejected non-jar file: " + fileName);
            return false;
        }

        File targetFile = new File(pluginsFolder, fileName);
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            plugin.getLogger().info("Plugin saved: " + fileName);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save plugin: " + e.getMessage());
            return false;
        }
    }

    /**
     * 卸载插件
     */
    private void unloadPlugin(String fileName) {
        String baseName = fileName.replace(".jar.disabled", "").replace(".jar", "");
        String simpleName = baseName.replaceAll("-\\d+(\\.\\d+)*.*$", "");

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            if (p.getName().equalsIgnoreCase(simpleName) ||
                    p.getName().equalsIgnoreCase(baseName)) {
                Bukkit.getPluginManager().disablePlugin(p);
                plugin.getLogger().info("Unloaded plugin: " + p.getName());
                break;
            }
        }
    }

    /**
     * 尝试加载插件
     */
    private void tryLoadPlugin(File jarFile) {
        try {
            Plugin loaded = Bukkit.getPluginManager().loadPlugin(jarFile);
            if (loaded != null) {
                Bukkit.getPluginManager().enablePlugin(loaded);
                plugin.getLogger().info("Plugin loaded: " + loaded.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load plugin: " + e.getMessage());
        }
    }
}