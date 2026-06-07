package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 插件管理服务 - 安装、卸载、启用、禁用 Minecraft 插件
 * 直接调用 LocalPluginService 操作本地文件，不再通过 HTTP 代理
 */
@Service
@SuppressWarnings("unchecked")
public class PluginService {

    @Autowired
    private LocalPluginService localPluginService;

    // ==================== 旧版单服务器方法（向后兼容） ====================

    public Result<List<Map<String, Object>>> getPluginList() {
        List<Map<String, Object>> list = localPluginService.getPluginList();
        return Result.success("获取成功", list);
    }

    public Result<String> enablePlugin(String fileName) {
        boolean success = localPluginService.enablePlugin(fileName);
        return success ? Result.successMsg("插件已启用") : Result.error("启用失败，请检查插件文件名");
    }

    public Result<String> disablePlugin(String fileName) {
        boolean success = localPluginService.disablePlugin(fileName);
        return success ? Result.successMsg("插件已禁用") : Result.error("禁用失败，请检查插件文件名");
    }

    public Result<String> deletePlugin(String fileName) {
        boolean success = localPluginService.deletePlugin(fileName);
        return success ? Result.successMsg("插件已删除") : Result.error("删除失败，请检查插件文件名");
    }

    public Result<String> uploadPlugin(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.endsWith(".jar")) {
                return Result.error("只支持 .jar 文件");
            }
            boolean success = localPluginService.savePlugin(fileName, file.getInputStream());
            return success ? Result.successMsg("插件上传成功") : Result.error("插件上传失败");
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    // ==================== 新版多服务器插件方法 ====================

    public Result<List<Map<String, Object>>> getPluginList(String serverId) {
        List<Map<String, Object>> list = localPluginService.getPluginList(serverId);
        return Result.success("获取成功", list);
    }

    public Result<String> enablePlugin(String serverId, String fileName) {
        boolean success = localPluginService.enablePlugin(serverId, fileName);
        return success ? Result.successMsg("插件已启用") : Result.error("启用失败，请检查插件文件名");
    }

    public Result<String> disablePlugin(String serverId, String fileName) {
        boolean success = localPluginService.disablePlugin(serverId, fileName);
        return success ? Result.successMsg("插件已禁用") : Result.error("禁用失败，请检查插件文件名");
    }

    public Result<String> deletePlugin(String serverId, String fileName) {
        boolean success = localPluginService.deletePlugin(serverId, fileName);
        return success ? Result.successMsg("插件已删除") : Result.error("删除失败，请检查插件文件名");
    }

    public Result<String> uploadPlugin(String serverId, MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.endsWith(".jar")) {
                return Result.error("只支持 .jar 文件");
            }
            boolean success = localPluginService.savePlugin(serverId, fileName, file.getInputStream());
            return success ? Result.successMsg("插件上传成功") : Result.error("插件上传失败");
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
