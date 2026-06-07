package com.gitcode.mcsm_backend.service;

import java.io.*;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 单个 Minecraft 服务器实例
 * 封装 Process 生命周期 + 控制台环形缓冲区
 */
@Slf4j
public class ServerInstance {

    public enum Status { STOPPED, STARTING, RUNNING, STOPPING }

    private final String serverId;
    private final ServerConfig config;
    private final RingBuffer ringBuffer;
    private Process process;
    private volatile Status status = Status.STOPPED;
    private Thread outputReaderThread;

    public ServerInstance(String serverId, ServerConfig config, int bufferSize) {
        this.serverId = serverId;
        this.config = config;
        this.ringBuffer = new RingBuffer(bufferSize);
    }

    // ==================== 生命周期 ====================

    /**
     * 启动 Minecraft 服务器
     */
    public synchronized boolean start() {
        if (status == Status.RUNNING || status == Status.STARTING) {
            log.info("[{}] 服务器已在运行中", serverId);
            return false;
        }

        // 启动前检查目录
        File dir = new File(config.getDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            status = Status.STOPPED;
            ringBuffer.addLine("[Agent] 启动失败: 服务器目录不存在 " + config.getDirectory());
            log.error("[{}] 启动失败: 服务器目录不存在 {}", serverId, config.getDirectory());
            return false;
        }

        // 清理上次崩溃残留的锁文件
        deleteSessionLock(dir);

        status = Status.STARTING;

        try {
            String javaCmd = resolveJavaCommand();

            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(config.getDirectory()));

            // 命令行传 --port 覆盖 server.properties
            int port = config.getPort() > 0 ? config.getPort() : 25565;
            String[] cmd = (javaCmd + " " + config.getJavaArgs() + " -jar "
                    + config.getJarFile() + " --port " + port + " nogui").split(" ");
            pb.command(cmd);
            pb.redirectErrorStream(true);

            process = pb.start();
            startOutputReader();

            ringBuffer.addLine("[Agent] Minecraft 服务器启动中，PID: " + process.pid());
            log.info("[{}] 服务器已启动，PID: {}", serverId, process.pid());
            return true;

        } catch (IOException e) {
            status = Status.STOPPED;
            ringBuffer.addLine("[Agent] 启动失败: " + e.getMessage());
            log.error("[{}] 启动失败", serverId, e);
            return false;
        }
    }

    /**
     * 停止 Minecraft 服务器（通过 stdin 发送 stop）
     */
    public synchronized boolean stop() {
        if (status == Status.STOPPED) {
            log.info("[{}] 服务器未运行", serverId);
            return true;
        }

        Status prevStatus = status;
        status = Status.STOPPING;

        try {
            ringBuffer.addLine("[Agent] 正在发送 stop 命令...");
            OutputStream os = process.getOutputStream();
            os.write("stop\n".getBytes());
            os.flush();

            boolean exited = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);

            if (!exited) {
                ringBuffer.addLine("[Agent] 服务器未响应，强制终止");
                log.warn("[{}] 服务器未响应，强制终止", serverId);
                process.destroyForcibly();
            }

            process = null;
            status = Status.STOPPED;
            ringBuffer.addLine("[Agent] Minecraft 服务器已停止");
            log.info("[{}] 服务器已停止", serverId);
            return true;

        } catch (Exception e) {
            status = prevStatus; // 恢复
            log.error("[{}] 停止失败", serverId, e);
            if (process != null) {
                process.destroyForcibly();
                process = null;
                status = Status.STOPPED;
            }
            return false;
        }
    }

    /**
     * 强制终止进程（清理启动失败残留）
     */
    public synchronized boolean forceStop() {
        if (process == null || !process.isAlive()) {
            status = Status.STOPPED;
            ringBuffer.addLine("[Agent] 没有正在运行的进程");
            return true;
        }
        ringBuffer.addLine("[Agent] 正在强制终止进程 PID: " + process.pid());
        process.destroyForcibly();
        try { process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        if (process.isAlive()) {
            ringBuffer.addLine("[Agent] 强制终止失败，进程仍存活");
            return false;
        }
        process = null;
        status = Status.STOPPED;
        ringBuffer.addLine("[Agent] 进程已强制终止");
        return true;
    }

    /**
     * 重启服务器
     */
    public synchronized boolean restart() {
        log.info("[{}] 正在重启...", serverId);
        stop();
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return start();
    }

    // ==================== 命令 ====================

    /**
     * 向服务器 stdin 发送命令
     */
    public synchronized boolean sendCommand(String command) {
        if (status != Status.RUNNING || process == null) {
            return false;
        }
        try {
            OutputStream os = process.getOutputStream();
            os.write((command + "\n").getBytes());
            os.flush();
            return true;
        } catch (IOException e) {
            log.error("[{}] 发送命令失败", serverId, e);
            return false;
        }
    }

    // ==================== 控制台日志 ====================

    /**
     * 获取控制台日志增量
     * @param since 起始行号（-1 = 首次拉取最近 100 行）
     */
    public RingBuffer.ConsoleData getConsole(long since) {
        return ringBuffer.getLinesSince(since);
    }

    // ==================== 状态 ====================

    public Status getStatus() { return status; }
    public long getPid() { return (process != null && process.isAlive()) ? process.pid() : -1; }
    public String getServerId() { return serverId; }
    public ServerConfig getConfig() { return config; }

    public void rename(String newName) {
        this.config.setName(newName);
    }

    public Map<String, Object> getStatusInfo() {
        File dir = new File(config.getDirectory());
        boolean dirValid = dir.exists() && dir.isDirectory();
        return Map.of(
                "serverId", serverId,
                "name", config.getName(),
                "status", status.name(),
                "pid", getPid(),
                "port", config.getPort(),
                "directory", config.getDirectory(),
                "directoryValid", dirValid,
                "javaArgs", config.getJavaArgs()
        );
    }

    // ==================== 内部 ====================

    /**
     * 解析 Java 命令路径
     * 优先级：配置文件 javaHome > JAVA_HOME 环境变量 > PATH 中的 java
     */
    private String resolveJavaCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String javaExe = os.contains("win") ? "java.exe" : "java";

        // 优先用自带的 data/jre
        File bundledJava = new File("data/jre/bin/" + javaExe);
        if (bundledJava.exists()) return bundledJava.getAbsolutePath();

        // 其次用配置文件指定的 javaHome
        String configuredHome = config.getJavaHome();
        if (configuredHome != null && !configuredHome.isEmpty()) {
            configuredHome = configuredHome.replace('\\', '/');
            if (configuredHome.endsWith("/bin")) return configuredHome + "/" + javaExe;
            if (configuredHome.endsWith("/" + javaExe)) return configuredHome;
            return configuredHome + "/bin/" + javaExe;
        }

        // 再次用系统 JAVA_HOME
        String envHome = System.getenv("JAVA_HOME");
        if (envHome != null && !envHome.isEmpty()) {
            envHome = envHome.replace('\\', '/');
            if (envHome.endsWith("/bin")) return envHome + "/" + javaExe;
            return envHome + "/bin/" + javaExe;
        }

        // 最后尝试 PATH 中的 java
        return "java";
    }

    private void deleteSessionLock(File dir) {
        try {
            java.nio.file.Files.walk(dir.toPath(), 3)
                    .filter(p -> p.getFileName().toString().equals("session.lock"))
                    .forEach(p -> { try { java.nio.file.Files.deleteIfExists(p); log.info("[{}] 已清理锁文件: {}", serverId, p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    private void startOutputReader() {
        outputReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ringBuffer.addLine(line);
                    log.info("[{}] {}", serverId, line);

                    if (line.contains("Done") && status == Status.STARTING) {
                        status = Status.RUNNING;
                        log.info("[{}] 服务器已完全启动", serverId);
                        ringBuffer.addLine("[Agent] 服务器已完全启动");
                    }
                }
            } catch (IOException e) {
                if (status != Status.STOPPING) {
                    log.error("[{}] 读取输出异常", serverId, e);
                }
            }
            // 进程退出，标记停止
            if (status != Status.STOPPING) {
                status = Status.STOPPED;
                ringBuffer.addLine("[Agent] 服务器进程已退出");
            }
        }, "MC-Output-" + serverId);
        outputReaderThread.setDaemon(true);
        outputReaderThread.start();
    }
}
