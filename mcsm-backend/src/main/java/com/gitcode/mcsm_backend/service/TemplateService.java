package com.gitcode.mcsm_backend.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 服务器模板服务 — 扫描 data/templates/ 下的模板 zip，支持解压创建新实例
 */
@Service
public class TemplateService {

    private static final String TEMPLATES_DIR = com.gitcode.mcsm_backend.common.McsmPaths.TEMPLATES_DIR;

    /**
     * 扫描 data/templates/paper/ 下的 zip 文件，返回服务器模板列表
     */
    public List<Map<String, Object>> listTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();
        File templatesRoot = new java.io.File(TEMPLATES_DIR);
        File paperDir = new File(templatesRoot, "paper");
        if (!paperDir.exists() || !paperDir.isDirectory()) return templates;

        File[] zips = paperDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (zips == null) return templates;

        for (File zip : zips) {
            Map<String, Object> template = new LinkedHashMap<>();
            String fileName = zip.getName();
            String id = "paper/" + fileName;
            String name = fileName.replace(".zip", "").replace("-", " ");

            template.put("id", id);
            template.put("name", name);
            template.put("fileName", fileName);
            template.put("size", formatSize(zip.length()));
            template.put("sizeBytes", zip.length());
            template.put("category", "paper");
            templates.add(template);
        }

        return templates;
    }

    /**
     * 使用模板创建服务器：解压 zip 到 data/servers/{serverId}/
     *
     * @param templateId 模板 ID（如 "paper/paper-1.20.1.zip"）
     * @param serverId   新服务器 ID
     * @return 解压后的服务器目录路径
     */
    public String useTemplate(String templateId, String serverId) throws IOException {
        // 校验 templateId 格式（只允许 paper/ 子目录）
        if (templateId.contains("..") || templateId.startsWith("/") || !templateId.startsWith("paper/")) {
            throw new IllegalArgumentException("非法模板 ID");
        }

        File templatesRoot = new java.io.File(TEMPLATES_DIR);
        File zipFile = new File(templatesRoot, templateId);
        if (!zipFile.exists()) {
            throw new FileNotFoundException("模板文件不存在: " + templateId);
        }

        // 目标目录（使用绝对路径，兼容开发和生产环境）
        File baseDir = new File(System.getProperty("user.dir"));
        File serversDir = new File(baseDir, com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR);
        File serverDir = new File(serversDir, serverId);
        if (serverDir.exists()) {
            throw new IOException("服务器目录已存在: " + serverDir.getAbsolutePath());
        }
        serverDir.mkdirs();

        // 解压
        extractZip(zipFile, serverDir);

        return serverDir.getAbsolutePath();
    }

    private void extractZip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());

                // 安全检查：防止 zip slip
                if (!outFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Zip entry 超出目标目录: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        zis.transferTo(fos);
                    }
                }
            }
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
