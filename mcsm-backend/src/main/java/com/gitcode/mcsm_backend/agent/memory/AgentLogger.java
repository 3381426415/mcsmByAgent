package com.gitcode.mcsm_backend.agent.memory;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Agent 统一日志管理
 * 输出到 data/logs/ 目录，按日期轮转，操作日志独立存储
 */
@Slf4j
public class AgentLogger {

    private final Path logDir;
    private final Path operationsDir;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PrintWriter logWriter;
    private LocalDate currentDate;

    public AgentLogger(String basePath) {
        this.logDir = Paths.get(basePath, "data", "logs");
        this.operationsDir = logDir.resolve("operations");
        initDirectory();
        openLogFile();
    }

    private void initDirectory() {
        try {
            Files.createDirectories(logDir);
            Files.createDirectories(operationsDir);
        } catch (IOException e) {
            log.error("[AgentLogger] 创建日志目录失败: {}", e.getMessage());
        }
    }

    private void openLogFile() {
        try {
            currentDate = LocalDate.now();
            String fileName = "agent-" + currentDate.format(dateFmt) + ".log";
            Path logFile = logDir.resolve(fileName);
            logWriter = new PrintWriter(
                    new BufferedWriter(new java.io.OutputStreamWriter(
                            new java.io.FileOutputStream(logFile.toFile(), true), java.nio.charset.StandardCharsets.UTF_8)), true);
        } catch (IOException e) {
            log.error("[AgentLogger] 打开日志文件失败: {}", e.getMessage());
            logWriter = new PrintWriter(System.out, true);
        }
    }

    private synchronized void checkRotation() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            if (logWriter != null) {
                logWriter.close();
            }
            openLogFile();
        }
    }

    public void info(String msg) {
        write("INFO", msg);
    }

    public void warn(String msg) {
        write("WARN", msg);
    }

    public void error(String msg) {
        write("ERROR", msg);
    }

    public void error(String msg, Throwable t) {
        write("ERROR", msg);
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        write("ERROR", sw.toString());
    }

    public void debug(String msg) {
        write("DEBUG", msg);
    }

    private synchronized void write(String level, String msg) {
        checkRotation();
        String timestamp = LocalDateTime.now().format(timeFmt);
        String line = "[" + timestamp + "] [" + level + "] " + msg;

        if (logWriter != null) {
            logWriter.println(line);
        }

        if ("ERROR".equals(level)) {
            log.error(line);
        } else {
            log.info(line);
        }
    }

    public void operation(String taskId, String operation, String details, boolean success) {
        try {
            String fileName = LocalDate.now().format(dateFmt) + ".log";
            Path opFile = operationsDir.resolve(fileName);

            String timestamp = LocalDateTime.now().format(timeFmt);
            String status = success ? "SUCCESS" : "FAILURE";
            String line = String.format("[%s] [%s] task=%s op=%s %s",
                    timestamp, status, taskId, operation, details);

            Files.writeString(opFile, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("[AgentLogger] 写入操作日志失败: {}", e.getMessage());
        }
    }

    public void operation(String operation, String details, boolean success) {
        operation("-", operation, details, success);
    }

    public Path getLogDir() {
        return logDir;
    }

    public Path getOperationsDir() {
        return operationsDir;
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}
