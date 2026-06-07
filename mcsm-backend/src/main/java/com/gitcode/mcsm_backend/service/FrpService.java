package com.gitcode.mcsm_backend.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FRP 客户端（frpc）管理服务
 * 管理 frpc 进程生命周期、配置文件读写、日志捕获
 */
@Slf4j
@Service
public class FrpService {

    private static final String CONFIG_FILE = "frpc.toml";
    private String frpDir;
    private static final String DEFAULT_CONFIG = """
            # MCSM FRP Client Config
            # Docs: https://github.com/fatedier/frp

            serverAddr = "your-server-ip"
            serverPort = 7000
            # auth.method = "token"
            # auth.token = "your-token"

            [[proxies]]
            name = "minecraft"
            type = "tcp"
            localIP = "127.0.0.1"
            localPort = 25565
            remotePort = 25565
            """;

    private final RingBuffer logBuffer = new RingBuffer(500);
    private Process frpcProcess;
    private Thread logReaderThread;

    @PostConstruct
    public void init() {
        frpDir = resolveBaseDir() + File.separator + com.gitcode.mcsm_backend.common.McsmPaths.FRP_DIR;
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            log.info("[FrpService] 配置文件不存在: {}", configFile.getAbsolutePath());
        }
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    // ==================== 进程管理 ====================

    public synchronized boolean start() {
        if (isRunning()) {
            logBuffer.addLine("[FrpService] frpc 已在运行中");
            return false;
        }

        File binary = getBinaryFile();
        if (!binary.exists()) {
            logBuffer.addLine("[FrpService] frpc 二进制不存在: " + binary.getAbsolutePath());
            return false;
        }

        // Linux 下确保可执行权限
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            if (!binary.canExecute()) {
                binary.setExecutable(true, false);
            }
        }

        File configFile = getConfigFile();
        if (!configFile.exists()) {
            logBuffer.addLine("[FrpService] 配置文件不存在: " + configFile.getAbsolutePath());
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    binary.getAbsolutePath(),
                    "-c", configFile.getAbsolutePath()
            );
            pb.directory(new File(frpDir));
            pb.redirectErrorStream(true);

            frpcProcess = pb.start();
            startLogReader();

            logBuffer.addLine("[FrpService] frpc 已启动, PID: " + frpcProcess.pid());
            log.info("[FrpService] frpc 已启动, PID: {}", frpcProcess.pid());
            return true;

        } catch (IOException e) {
            logBuffer.addLine("[FrpService] 启动失败: " + e.getMessage());
            log.error("[FrpService] 启动失败: {}", e.getMessage());
            return false;
        }
    }

    public synchronized boolean stop() {
        if (!isRunning()) {
            return true;
        }

        logBuffer.addLine("[FrpService] 正在停止 frpc...");
        frpcProcess.destroy();

        try {
            if (!frpcProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                frpcProcess.destroyForcibly();
            }
        } catch (InterruptedException e) {
            frpcProcess.destroyForcibly();
            Thread.currentThread().interrupt();
        }

        frpcProcess = null;
        if (logReaderThread != null) {
            logReaderThread.interrupt();
            logReaderThread = null;
        }

        logBuffer.addLine("[FrpService] frpc 已停止");
        log.info("[FrpService] frpc 已停止");
        return true;
    }

    public boolean isRunning() {
        return frpcProcess != null && frpcProcess.isAlive();
    }

    public String getStatus() {
        if (!isInstalled()) return "not_installed";
        return isRunning() ? "running" : "stopped";
    }

    public boolean isInstalled() {
        return getBinaryFile().exists();
    }

    // ==================== 配置文件 ====================

    public String getConfig() {
        File file = getConfigFile();
        if (!file.exists()) {
            return "配置文件不存在，请在工作目录查找 frpc.toml: " + file.getAbsolutePath();
        }
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# 读取失败: " + e.getMessage();
        }
    }

    public boolean saveConfig(String content) {
        try {
            Files.writeString(getConfigFile().toPath(), content, StandardCharsets.UTF_8);
            logBuffer.addLine("[FrpService] 配置已保存");
            return true;
        } catch (IOException e) {
            logBuffer.addLine("[FrpService] 保存配置失败: " + e.getMessage());
            return false;
        }
    }

    // ==================== 日志 ====================

    public List<String> getLogs(int lines) {
        RingBuffer.ConsoleData data = logBuffer.getLinesSince(-1);
        List<String> allLines = data.lines().stream()
                .map(RingBuffer.LogLine::content)
                .collect(Collectors.toList());
        if (allLines.size() <= lines) return allLines;
        return allLines.subList(allLines.size() - lines, allLines.size());
    }

    // ==================== 内部方法 ====================

    private static String resolveBaseDir() {
        try {
            String path = FrpService.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            File jarFile = new File(path);
            return jarFile.isFile() ? jarFile.getParent() : System.getProperty("user.dir");
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }

    private File getBinaryFile() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(frpDir, "windows" + File.separator + "frpc.exe");
        } else {
            return new File(frpDir, "linux" + File.separator + "frpc");
        }
    }

    private File getConfigFile() {
        return new File(frpDir, CONFIG_FILE);
    }

    private void startLogReader() {
        logReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(frpcProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logBuffer.addLine(line);
                }
            } catch (IOException e) {
                if (frpcProcess != null && frpcProcess.isAlive()) {
                    logBuffer.addLine("[FrpService] 日志读取异常: " + e.getMessage());
                }
            }
        }, "FrpLogReader");
        logReaderThread.setDaemon(true);
        logReaderThread.start();
    }
}
