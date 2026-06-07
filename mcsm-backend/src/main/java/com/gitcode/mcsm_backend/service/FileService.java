package com.gitcode.mcsm_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件系统操作服务
 *
 * 提供给 AI 智能体操作服务器文件的工具能力。
 * 安全限制：默认只允许操作 servers/ 目录下的文件。
 */
@Service
public class FileService {

    private final Path baseDir;

    public FileService(@Value("${agent.servers-base-dir:data/servers}") String serversBaseDir) {
        this.baseDir = Path.of(serversBaseDir).toAbsolutePath().normalize();
    }

    /**
     * 安全校验：确保目标路径在允许的基准目录内
     */
    private Path resolveSafe(String relativePath, String serverId) {
        Path target;
        if (serverId != null && !serverId.isEmpty()) {
            target = baseDir.resolve(serverId).resolve(relativePath);
        } else {
            target = baseDir.resolve(relativePath);
        }
        target = target.toAbsolutePath().normalize();

        if (!target.startsWith(baseDir.toAbsolutePath().normalize())) {
            throw new SecurityException("路径越权: " + relativePath);
        }
        return target;
    }

    private Path resolveSafe(String relativePath) {
        return resolveSafe(relativePath, null);
    }

    // ==================== 文件内容操作 ====================

    /**
     * 读取文件内容
     * @param path 相对于 servers/ 目录的路径（或 servers/{serverId}/ 相对路径）
     * @param serverId 可选，指定服务器实例
     * @param maxLines 最大读取行数，默认 500
     */
    public Map<String, Object> readFile(String path, String serverId, int maxLines) {
        try {
            Path target = resolveSafe(path, serverId);
            if (!Files.exists(target)) {
                return error("文件不存在: " + target.getFileName());
            }
            if (Files.isDirectory(target)) {
                return error("是目录而非文件: " + target.getFileName());
            }
            if (Files.size(target) > 5 * 1024 * 1024) {
                return error("文件过大（超过5MB），拒绝读取");
            }

            List<String> lines = Files.readAllLines(target);
            int totalLines = lines.size();
            if (maxLines <= 0) maxLines = 500;

            List<String> result;
            String suffix = "";
            if (totalLines > maxLines) {
                result = lines.subList(0, maxLines);
                suffix = "\n... (共 " + totalLines + " 行，仅显示前 " + maxLines + " 行)";
            } else {
                result = lines;
            }

            return success(String.join("\n", result) + suffix, Map.of(
                    "path", target.toString(),
                    "totalLines", totalLines,
                    "size", Files.size(target)
            ));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * 写入文件内容（覆盖）
     */
    public Map<String, Object> writeFile(String path, String content, String serverId) {
        try {
            Path target = resolveSafe(path, serverId);
            Path parent = target.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(target, content);
            return success("写入成功: " + target.getFileName());
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("写入文件失败: " + e.getMessage());
        }
    }

    /**
     * 按行号精确编辑文件
     */
    public Map<String, Object> editFileLines(String path, String operation, int startLine,
                                              int endLine, String content, String serverId) {
        try {
            Path target = resolveSafe(path, serverId);
            if (!Files.exists(target)) {
                return error("文件不存在: " + path);
            }

            List<String> lines = new ArrayList<>(Files.readAllLines(target));
            int totalLines = lines.size();

            if (startLine < 1 || startLine > totalLines) {
                return error("行号越界: startLine=" + startLine + "，文件共 " + totalLines + " 行");
            }

            // 规范化 endLine
            if (endLine < startLine) endLine = startLine;
            if (endLine > totalLines) endLine = totalLines;

            // 转为 0-based 索引
            int startIdx = startLine - 1;
            int endIdx = endLine - 1;

            switch (operation) {
                case "replace" -> {
                    if (content == null) return error("replace 操作需要 content 参数");
                    List<String> newLines = List.of(content.split("\n", -1));
                    for (int i = endIdx; i >= startIdx; i--) {
                        lines.remove(i);
                    }
                    lines.addAll(startIdx, newLines);
                }
                case "insert" -> {
                    if (content == null) return error("insert 操作需要 content 参数");
                    List<String> newLines = List.of(content.split("\n", -1));
                    lines.addAll(startIdx + 1, newLines);
                }
                case "delete" -> {
                    for (int i = endIdx; i >= startIdx; i--) {
                        lines.remove(i);
                    }
                }
                default -> {
                    return error("不支持的操作: " + operation + "（仅支持 replace/insert/delete）");
                }
            }

            Files.writeString(target, String.join("\n", lines));
            int newTotal = lines.size();
            return success(operation + " 成功", Map.of(
                    "path", path,
                    "operation", operation,
                    "affectedLines", endLine - startLine + 1,
                    "totalLines", newTotal
            ));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("编辑文件失败: " + e.getMessage());
        }
    }

    /**
     * 追加内容到文件末尾
     */
    public Map<String, Object> appendFile(String path, String content, String serverId) {
        try {
            Path target = resolveSafe(path, serverId);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return success("追加成功: " + target.getFileName());
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("追加文件失败: " + e.getMessage());
        }
    }

    // ==================== 目录操作 ====================

    /**
     * 列出目录内容
     */
    public Map<String, Object> listDirectory(String path, String serverId) {
        try {
            Path target = resolveSafe(path != null && !path.isEmpty() ? path : ".", serverId);
            if (!Files.exists(target)) {
                return error("目录不存在: " + target.getFileName());
            }
            if (!Files.isDirectory(target)) {
                return error("不是目录: " + target.getFileName());
            }

            List<Map<String, Object>> items = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
                for (Path p : stream) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", p.getFileName().toString());
                    item.put("type", Files.isDirectory(p) ? "dir" : "file");
                    item.put("size", Files.size(p));
                    item.put("lastModified", Files.getLastModifiedTime(p).toString());
                    items.add(item);
                }
            }

            return success("获取成功", Map.of(
                    "path", target.toString(),
                    "items", items
            ));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("列出目录失败: " + e.getMessage());
        }
    }

    /**
     * 搜索文件（按名称模式）
     */
    public Map<String, Object> searchFile(String pattern, String serverId) {
        try {
            final Path searchDir = (serverId != null && !serverId.isEmpty()
                    ? baseDir.resolve(serverId) : baseDir)
                    .toAbsolutePath().normalize();

            if (!searchDir.startsWith(baseDir.toAbsolutePath().normalize())) {
                return error("路径越权");
            }

            String lowerPattern = pattern.toLowerCase();
            List<String> matches = Files.walk(searchDir, 5)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                    .limit(100)
                    .map(p -> searchDir.relativize(p).toString())
                    .collect(Collectors.toList());

            return success("找到 " + matches.size() + " 个文件", Map.of(
                    "searchDir", searchDir.toString(),
                    "pattern", pattern,
                    "matches", matches
            ));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("搜索文件失败: " + e.getMessage());
        }
    }

    /**
     * 在文件中搜索内容（类似 grep）
     */
    public Map<String, Object> grepFile(String path, String regex, String serverId) {
        try {
            Path target = resolveSafe(path, serverId);

            if (Files.isDirectory(target)) {
                List<Map<String, Object>> matches = new ArrayList<>();
                Files.walk(target, 3)
                        .filter(p -> {
                            try { return Files.isRegularFile(p) && Files.size(p) < 5 * 1024 * 1024; }
                            catch (IOException e) { return false; }
                        })
                        .limit(100)
                        .forEach(p -> {
                            try {
                                List<String> lines = Files.readAllLines(p);
                                for (int i = 0; i < lines.size(); i++) {
                                    if (lines.get(i).matches(".*" + regex + ".*")) {
                                        matches.add(Map.of(
                                                "file", target.relativize(p).toString(),
                                                "line", i + 1,
                                                "content", lines.get(i).trim()
                                        ));
                                    }
                                }
                            } catch (IOException ignored) {}
                        });
                return success("找到 " + matches.size() + " 处匹配", Map.of("matches", matches));
            } else {
                List<String> lines = Files.readAllLines(target);
                List<Map<String, Object>> matches = new ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).matches(".*" + regex + ".*")) {
                        matches.add(Map.of("line", i + 1, "content", lines.get(i).trim()));
                    }
                }
                return success("找到 " + matches.size() + " 处匹配", Map.of("matches", matches));
            }
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("搜索内容失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件或目录
     */
    public Map<String, Object> deleteFile(String path, String serverId) {
        try {
            Path target = resolveSafe(path, serverId);
            if (!Files.exists(target)) {
                return error("路径不存在: " + target.getFileName());
            }

            if (Files.isDirectory(target)) {
                try (var stream = Files.walk(target)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.delete(p); }
                                catch (IOException ignored) {}
                            });
                }
            } else {
                Files.delete(target);
            }
            return success("删除成功: " + target.getFileName());
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("删除失败: " + e.getMessage());
        }
    }

    // ==================== 特殊操作 ====================

    /**
     * 读取并解析 server.properties
     */
    public Map<String, Object> getServerProperties(String serverId) {
        try {
            Path propsFile = resolveSafe("server.properties", serverId);
            if (!Files.exists(propsFile)) {
                return error("server.properties 不存在");
            }

            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(propsFile)) {
                props.load(is);
            }

            Map<String, String> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                result.put(key, props.getProperty(key));
            }

            return success("获取成功", Map.of("properties", result));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("读取 server.properties 失败: " + e.getMessage());
        }
    }

    /**
     * 更新 server.properties 中的指定键值
     */
    public Map<String, Object> updateServerProperties(String serverId, Map<String, String> updates) {
        try {
            Path propsFile = resolveSafe("server.properties", serverId);
            if (!Files.exists(propsFile)) {
                return error("server.properties 不存在");
            }

            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(propsFile)) {
                props.load(is);
            }

            updates.forEach(props::setProperty);

            try (OutputStream os = Files.newOutputStream(propsFile)) {
                props.store(os, "Updated by MCSM Agent");
            }

            return success("更新成功", Map.of("updatedKeys", updates.keySet()));
        } catch (SecurityException e) {
            return error(e.getMessage());
        } catch (IOException e) {
            return error("更新 server.properties 失败: " + e.getMessage());
        }
    }

    /**
     * 获取磁盘使用情况
     */
    public Map<String, Object> getDiskUsage() {
        File base = baseDir.toFile();
        long total = base.getTotalSpace();
        long free = base.getFreeSpace();
        long used = total - free;
        return success("获取成功", Map.of(
                "totalGB", String.format("%.1f", (double) total / 1024 / 1024 / 1024),
                "usedGB", String.format("%.1f", (double) used / 1024 / 1024 / 1024),
                "freeGB", String.format("%.1f", (double) free / 1024 / 1024 / 1024),
                "usagePercent", Math.round((double) used / total * 100)
        ));
    }

    /**
     * 获取系统信息（含真实系统内存和 JVM 内存）
     */
    public Map<String, Object> getSystemInfo() {
        Runtime rt = Runtime.getRuntime();
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        long totalMem = osBean.getTotalPhysicalMemorySize() / 1024 / 1024;
        long freeMem = osBean.getFreePhysicalMemorySize() / 1024 / 1024;

        return success("获取成功", Map.of(
                "os", System.getProperty("os.name") + " " + System.getProperty("os.arch"),
                "javaVersion", System.getProperty("java.version"),
                "availableProcessors", rt.availableProcessors(),
                "totalMemoryMB", totalMem,
                "freeMemoryMB", freeMem,
                "usedMemoryMB", totalMem - freeMem,
                "jvmMaxMemoryMB", rt.maxMemory() / 1024 / 1024,
                "jvmUsedMemoryMB", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
        ));
    }

    // ==================== 内部方法 ====================

    private Map<String, Object> success(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 2000);
        result.put("msg", msg);
        return result;
    }

    private Map<String, Object> success(String msg, Object data) {
        Map<String, Object> result = success(msg);
        result.put("data", data);
        return result;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 3000);
        result.put("msg", msg);
        return result;
    }
}
