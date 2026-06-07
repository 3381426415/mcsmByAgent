package com.gitcode.mcsm_backend.agent.memory;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件备份管理器 - 写入前自动备份，支持回退
 * 备份同时存入内存和磁盘（data/backups/{taskId}/），支持重启后恢复
 */
@Slf4j
public class FileBackupManager {

    private final Map<String, Map<String, byte[]>> taskBackups = new ConcurrentHashMap<>();
    private final Path backupDir;

    public FileBackupManager(String basePath) {
        this.backupDir = Paths.get(basePath, "data", "backups");
        initDirectory();
        recoverFromDisk();
    }

    private void initDirectory() {
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.error("[FileBackup] 创建备份目录失败: {}", e.getMessage());
        }
    }

    private void recoverFromDisk() {
        if (!Files.exists(backupDir)) return;
        try (Stream<Path> taskDirs = Files.list(backupDir)) {
            taskDirs.filter(Files::isDirectory).forEach(taskDir -> {
                String taskId = taskDir.getFileName().toString();
                Map<String, byte[]> backups = new ConcurrentHashMap<>();

                Path mappingFile = taskDir.resolve(".path_mapping");
                Map<String, String> pathMapping = loadPathMapping(mappingFile);

                try (Stream<Path> files = Files.list(taskDir)) {
                    files.filter(p -> Files.isRegularFile(p) && !p.getFileName().toString().equals(".path_mapping"))
                            .forEach(backupFile -> {
                                try {
                                    String encodedName = backupFile.getFileName().toString();
                                    String originalPath = pathMapping.getOrDefault(encodedName, encodedName);
                                    byte[] content = Files.readAllBytes(backupFile);
                                    backups.put(originalPath, content);
                                } catch (IOException e) {
                                    log.error("[FileBackup] 读取备份文件失败: {}", backupFile);
                                }
                            });
                } catch (IOException e) {
                    log.error("[FileBackup] 扫描备份目录失败: {}", taskDir);
                }
                if (!backups.isEmpty()) {
                    taskBackups.put(taskId, backups);
                    log.info("[FileBackup] 恢复磁盘备份: taskId={}, 文件数={}", taskId, backups.size());
                }
            });
        } catch (IOException e) {
            log.error("[FileBackup] 扫描备份根目录失败: {}", e.getMessage());
        }
    }

    private Map<String, String> loadPathMapping(Path mappingFile) {
        Map<String, String> mapping = new HashMap<>();
        if (!Files.exists(mappingFile)) return mapping;
        try {
            List<String> lines = Files.readAllLines(mappingFile);
            for (String line : lines) {
                int sep = line.indexOf("|||");
                if (sep > 0) {
                    mapping.put(line.substring(0, sep), line.substring(sep + 3));
                }
            }
        } catch (IOException e) {
            log.error("[FileBackup] 读取路径映射失败: {}", e.getMessage());
        }
        return mapping;
    }

    private void savePathMapping(String taskId, Map<String, String> mapping) {
        try {
            Path taskDir = backupDir.resolve(taskId);
            Files.createDirectories(taskDir);
            Path mappingFile = taskDir.resolve(".path_mapping");
            List<String> lines = mapping.entrySet().stream()
                    .map(e -> e.getKey() + "|||" + e.getValue())
                    .collect(Collectors.toList());
            Files.write(mappingFile, lines);
        } catch (IOException e) {
            log.error("[FileBackup] 保存路径映射失败: {}", e.getMessage());
        }
    }

    public void backupBeforeWrite(String filePath, String taskId) {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                log.info("[FileBackup] 文件不存在，跳过备份: {}", filePath);
                return;
            }

            byte[] content = Files.readAllBytes(path);
            taskBackups.computeIfAbsent(taskId, k -> new ConcurrentHashMap<>())
                    .put(filePath, content);

            writeToDisk(taskId, filePath, content);

            log.info("[FileBackup] 备份完成: {} ({} bytes)", filePath, content.length);
        } catch (IOException e) {
            log.error("[FileBackup] 备份失败: {}", filePath);
        }
    }

    private void writeToDisk(String taskId, String filePath, byte[] content) {
        try {
            Path taskDir = backupDir.resolve(taskId);
            Files.createDirectories(taskDir);
            String fileName = encodeFileName(filePath);
            Files.write(taskDir.resolve(fileName), content);

            Map<String, String> mapping = loadPathMapping(taskDir.resolve(".path_mapping"));
            mapping.put(fileName, filePath);
            savePathMapping(taskId, mapping);
        } catch (IOException e) {
            log.error("[FileBackup] 写入磁盘备份失败: {} - {}", filePath, e.getMessage());
        }
    }

    public boolean rollback(String taskId) {
        Map<String, byte[]> backups = taskBackups.get(taskId);
        if (backups == null || backups.isEmpty()) {
            log.info("[FileBackup] 没有找到任务 {} 的备份", taskId);
            return false;
        }

        boolean allSuccess = true;
        for (Map.Entry<String, byte[]> entry : backups.entrySet()) {
            try {
                Files.write(Path.of(entry.getKey()), entry.getValue(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("[FileBackup] 回退成功: {}", entry.getKey());
            } catch (IOException e) {
                log.error("[FileBackup] 回退失败: {}", entry.getKey());
                allSuccess = false;
            }
        }

        taskBackups.remove(taskId);
        deleteFromDisk(taskId);
        return allSuccess;
    }

    public void cleanup(String taskId) {
        taskBackups.remove(taskId);
        deleteFromDisk(taskId);
    }

    private void deleteFromDisk(String taskId) {
        try {
            Path taskDir = backupDir.resolve(taskId);
            if (Files.exists(taskDir)) {
                try (Stream<Path> files = Files.list(taskDir)) {
                    files.forEach(f -> {
                        try { Files.delete(f); } catch (IOException ignored) {}
                    });
                }
                Files.delete(taskDir);
            }
        } catch (IOException e) {
            log.error("[FileBackup] 删除磁盘备份失败: {}", taskId);
        }
    }

    public boolean hasBackup(String taskId) {
        return taskBackups.containsKey(taskId) && !taskBackups.get(taskId).isEmpty();
    }

    public Set<String> getPendingTaskIds() {
        return Collections.unmodifiableSet(taskBackups.keySet());
    }

    private String encodeFileName(String filePath) {
        return filePath.replace('\\', '/').replace('/', '_').replace(':', '_');
    }
}
