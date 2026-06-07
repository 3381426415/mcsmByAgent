package com.gitcode.mcsm_backend.agent.ai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 控制台命令执行工具提供者
 *
 * 提供给 AI 智能体的系统控制台交互能力：
 * 执行 shell 命令、列出目录、创建目录、读写文件、系统操作等。
 */
public class ConsoleToolProvider implements LocalToolProvider {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /** 危险命令黑名单 */
    private static final List<String> DANGEROUS_COMMANDS = List.of(
            "rm -rf /", "rm -rf /*", "format ", "del /s", "del /q",
            "rmdir /s", "rmdir /q", "shutdown", "fdisk", "mkfs",
            "dd if=", ":(){ :|:& };:"
    );

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition(
                "execute_command",
                "[本地·系统命令] 执行控制台命令（Windows: cmd / Linux: sh），适用于简单命令",
                Map.of("type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "要执行的命令"),
                                "workDir", Map.of("type", "string", "description", "工作目录（可选）")
                        ),
                        "required", List.of("command"))));

        tools.add(new ToolDefinition(
                "execute_shell",
                "[本地·系统命令] 执行 Shell 命令（Windows: PowerShell / Linux: bash），支持管道和脚本，超时 60 秒",
                Map.of("type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "要执行的 Shell 命令"),
                                "workDir", Map.of("type", "string", "description", "工作目录（可选）")
                        ),
                        "required", List.of("command"))));

        tools.add(new ToolDefinition(
                "create_directory",
                "[本地·系统命令] 创建目录（递归创建父目录）",
                ToolDefinition.stringParam("path", "要创建的目录路径", true)));

        tools.add(new ToolDefinition(
                "read_file_content",
                "[本地·系统命令] 读取主机任意文件（非服务器文件，最大 1MB）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "maxLines", Map.of("type", "integer", "description", "最大读取行数（可选，默认500）")
                        ),
                        "required", List.of("path"))));

        tools.add(new ToolDefinition(
                "write_file_content",
                "[本地·系统命令] 写入主机任意文件（不存在则自动创建）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "content", Map.of("type", "string", "description", "要写入的内容")
                        ),
                        "required", List.of("path", "content"))));

        tools.add(new ToolDefinition(
                "list_processes",
                "[本地·系统监控] 列出主机运行中的进程（按内存排序，前 50 个，可按名称过滤）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "filter", Map.of("type", "string", "description", "按进程名过滤（可选，如 java, chrome）")),
                        "required", List.<String>of())));

        tools.add(new ToolDefinition(
                "kill_process",
                "[本地·系统命令] 终止指定进程（按 PID 或进程名）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "pid", Map.of("type", "integer", "description", "进程 PID（与 name 二选一）"),
                                "name", Map.of("type", "string", "description", "进程名（与 pid 二选一）")),
                        "required", List.<String>of())));

        tools.add(new ToolDefinition(
                "list_services",
                "[本地·系统监控] 列出主机系统服务（Windows 服务 / Linux systemd）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "filter", Map.of("type", "string", "description", "按服务名过滤（可选）")),
                        "required", List.<String>of())));

        tools.add(new ToolDefinition(
                "manage_service",
                "[本地·系统命令] 启动/停止/重启主机系统服务",
                Map.of("type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "服务名"),
                                "action", Map.of("type", "string", "description", "操作：start, stop, restart")
                        ),
                        "required", List.of("name", "action"))));

        tools.add(new ToolDefinition(
                "get_environment",
                "[本地·系统命令] 获取主机环境变量（不传参数返回全部）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "name", Map.of("type", "string", "description", "环境变量名（可选，如 PATH, JAVA_HOME）")),
                        "required", List.<String>of())));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        if (arguments == null) arguments = Map.of();

        return switch (toolName) {
            case "execute_command" -> executeCommand(
                    (String) arguments.get("command"),
                    (String) arguments.get("workDir"));
            case "execute_shell" -> executeShell(
                    (String) arguments.get("command"),
                    (String) arguments.get("workDir"));
            case "create_directory" -> createDirectory((String) arguments.get("path"));
            case "read_file_content" -> readFile(
                    (String) arguments.get("path"),
                    arguments.containsKey("maxLines") ? ((Number) arguments.get("maxLines")).intValue() : 500);
            case "write_file_content" -> writeFile(
                    (String) arguments.get("path"),
                    (String) arguments.get("content"));
            case "list_processes" -> listProcesses((String) arguments.get("filter"));
            case "kill_process" -> killProcess(arguments);
            case "list_services" -> listServices((String) arguments.get("filter"));
            case "manage_service" -> manageService(
                    (String) arguments.get("name"),
                    (String) arguments.get("action"));
            case "get_environment" -> getEnvironment((String) arguments.get("name"));
            default -> "未知工具: " + toolName;
        };
    }

    // ==================== 命令执行 ====================

    private String executeCommand(String command, String workDir) {
        if (command == null || command.trim().isEmpty()) {
            return "错误：命令不能为空";
        }

        String lowerCmd = command.toLowerCase().trim();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCmd.contains(dangerous.toLowerCase())) {
                return "拒绝执行危险命令: " + dangerous;
            }
        }

        try {
            List<String> cmd;
            if (IS_WINDOWS) {
                cmd = List.of("cmd", "/c", command);
            } else {
                cmd = List.of("sh", "-c", command);
            }

            return runProcess(cmd, workDir, 30);
        } catch (Exception e) {
            return "命令执行异常: " + e.getMessage();
        }
    }

    private String executeShell(String command, String workDir) {
        if (command == null || command.trim().isEmpty()) {
            return "错误：命令不能为空";
        }

        String lowerCmd = command.toLowerCase().trim();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lowerCmd.contains(dangerous.toLowerCase())) {
                return "拒绝执行危险命令: " + dangerous;
            }
        }

        try {
            List<String> cmd;
            if (IS_WINDOWS) {
                cmd = List.of("powershell", "-NoProfile", "-Command", command);
            } else {
                cmd = List.of("bash", "-c", command);
            }

            return runProcess(cmd, workDir, 60);
        } catch (Exception e) {
            return "Shell 执行异常: " + e.getMessage();
        }
    }

    // ==================== 系统操作 ====================

    private String listProcesses(String filter) {
        if (IS_WINDOWS) {
            String ps = "Get-Process | Select-Object Id,ProcessName,WorkingSet64 | Sort-Object WorkingSet64 -Descending | Select-Object -First 50";
            if (filter != null && !filter.isEmpty()) {
                ps = "Get-Process -Name '*" + filter.replace("'", "''") + "*' -ErrorAction SilentlyContinue | Select-Object Id,ProcessName,WorkingSet64 | Sort-Object WorkingSet64 -Descending | Select-Object -First 50";
            }
            return runProcess(List.of("powershell", "-NoProfile", "-Command", ps), null, 15);
        } else {
            String cmd = "ps aux --sort=-%mem | head -50";
            if (filter != null && !filter.isEmpty()) {
                cmd = "ps aux --sort=-%mem | grep -i '" + filter.replace("'", "") + "' | head -50";
            }
            return runProcess(List.of("bash", "-c", cmd), null, 15);
        }
    }

    private String killProcess(Map<String, Object> arguments) {
        Number pidNum = (Number) arguments.get("pid");
        String name = (String) arguments.get("name");

        if (pidNum == null && (name == null || name.isEmpty())) {
            return "错误：必须提供 pid 或 name 参数";
        }

        if (IS_WINDOWS) {
            if (pidNum != null) {
                return runProcess(List.of("powershell", "-NoProfile", "-Command",
                        "Stop-Process -Id " + pidNum.intValue() + " -Force -ErrorAction SilentlyContinue; if($?){'进程已终止: PID=" + pidNum.intValue() + "'}else{'终止失败: PID=" + pidNum.intValue() + "'}"),
                        null, 15);
            } else {
                return runProcess(List.of("powershell", "-NoProfile", "-Command",
                        "Stop-Process -Name '" + name.replace("'", "''") + "' -Force -ErrorAction SilentlyContinue; if($?){'进程已终止: " + name + "'}else{'终止失败: " + name + "'}"),
                        null, 15);
            }
        } else {
            if (pidNum != null) {
                return runProcess(List.of("bash", "-c",
                        "kill -9 " + pidNum.intValue() + " 2>&1 && echo '进程已终止: PID=" + pidNum.intValue() + "' || echo '终止失败: PID=" + pidNum.intValue() + "'"),
                        null, 15);
            } else {
                return runProcess(List.of("bash", "-c",
                        "pkill -f '" + name.replace("'", "") + "' 2>&1 && echo '进程已终止: " + name + "' || echo '终止失败: " + name + "'"),
                        null, 15);
            }
        }
    }

    private String listServices(String filter) {
        if (IS_WINDOWS) {
            String ps = "Get-Service | Select-Object Name,DisplayName,Status | Sort-Object Status | Format-Table -AutoSize";
            if (filter != null && !filter.isEmpty()) {
                ps = "Get-Service -Name '*" + filter.replace("'", "''") + "*' | Select-Object Name,DisplayName,Status | Sort-Object Status | Format-Table -AutoSize";
            }
            return runProcess(List.of("powershell", "-NoProfile", "-Command", ps), null, 15);
        } else {
            String cmd = "systemctl list-units --type=service --all --no-pager";
            if (filter != null && !filter.isEmpty()) {
                cmd = "systemctl list-units --type=service --all --no-pager | grep -i '" + filter.replace("'", "") + "'";
            }
            return runProcess(List.of("bash", "-c", cmd), null, 15);
        }
    }

    private String manageService(String name, String action) {
        if (name == null || name.isEmpty()) return "错误：缺少服务名";
        if (action == null || action.isEmpty()) return "错误：缺少操作（start/stop/restart）";

        String lowerAction = action.toLowerCase();
        if (!List.of("start", "stop", "restart").contains(lowerAction)) {
            return "错误：不支持的操作 '" + action + "'，仅支持 start/stop/restart";
        }

        if (IS_WINDOWS) {
            String psAction = switch (lowerAction) {
                case "start" -> "Start-Service";
                case "stop" -> "Stop-Service";
                case "restart" -> "Restart-Service";
                default -> "";
            };
            return runProcess(List.of("powershell", "-NoProfile", "-Command",
                    psAction + " -Name '" + name.replace("'", "''") + "' -ErrorAction Stop; if($?){'" + lowerAction + " 成功: " + name + "'}else{'" + lowerAction + " 失败: " + name + "'}"),
                    null, 30);
        } else {
            return runProcess(List.of("bash", "-c",
                    "systemctl " + lowerAction + " '" + name.replace("'", "") + "' 2>&1 && echo '" + lowerAction + " 成功: " + name + "' || echo '" + lowerAction + " 失败: " + name + "'"),
                    null, 30);
        }
    }

    private String getEnvironment(String name) {
        if (IS_WINDOWS) {
            if (name != null && !name.isEmpty()) {
                return runProcess(List.of("powershell", "-NoProfile", "-Command",
                        "[Environment]::GetEnvironmentVariable('" + name.replace("'", "''") + "')"),
                        null, 10);
            } else {
                return runProcess(List.of("powershell", "-NoProfile", "-Command",
                        "Get-ChildItem Env: | Sort-Object Name | Format-Table -AutoSize"),
                        null, 10);
            }
        } else {
            if (name != null && !name.isEmpty()) {
                return runProcess(List.of("bash", "-c", "echo \"${" + name + ":-未设置}\""), null, 10);
            } else {
                return runProcess(List.of("bash", "-c", "env | sort"), null, 10);
            }
        }
    }

    // ==================== 文件操作 ====================

    private String createDirectory(String path) {
        if (path == null || path.isEmpty()) return "错误：目录路径不能为空";

        try {
            Path dir = Path.of(path);
            if (Files.exists(dir)) return "目录已存在: " + path;
            Files.createDirectories(dir);
            return "目录创建成功: " + path;
        } catch (Exception e) {
            return "创建目录失败: " + e.getMessage();
        }
    }

    private String readFile(String path, int maxLines) {
        if (path == null || path.isEmpty()) return "错误：文件路径不能为空";

        try {
            Path filePath = Path.of(path);
            if (!Files.exists(filePath)) return "文件不存在: " + path;
            if (!Files.isRegularFile(filePath)) return "路径不是文件: " + path;

            long size = Files.size(filePath);
            if (size > 1024 * 1024) return "文件过大（" + size + " bytes），最大支持 1MB";

            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            int end = Math.min(maxLines, lines.size());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < end; i++) {
                sb.append(String.format("%d\t%s%n", i + 1, lines.get(i)));
            }

            if (lines.size() > maxLines) {
                sb.append("... (共 ").append(lines.size()).append(" 行，已显示前 ").append(maxLines).append(" 行)");
            }

            return sb.toString();
        } catch (Exception e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    private String writeFile(String path, String content) {
        if (path == null || path.isEmpty()) return "错误：文件路径不能为空";
        if (content == null) content = "";

        try {
            Path filePath = Path.of(path);
            if (filePath.getParent() != null && !Files.exists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "文件写入成功: " + path + " (" + content.length() + " 字符)";
        } catch (Exception e) {
            return "写入文件失败: " + e.getMessage();
        }
    }

    // ==================== 通用进程执行 ====================

    private String runProcess(List<String> cmd, String workDir, int timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);

            if (workDir != null && !workDir.isEmpty()) {
                File dir = new File(workDir);
                if (dir.exists() && dir.isDirectory()) {
                    pb.directory(dir);
                } else {
                    return "工作目录不存在: " + workDir;
                }
            }

            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "命令执行超时（" + timeoutSeconds + "秒）";
            }

            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return output.isEmpty() ? "(命令执行成功，无输出)" : output;
            } else {
                return "命令执行失败，退出码: " + exitCode + "\n" + output;
            }
        } catch (Exception e) {
            return "命令执行异常: " + e.getMessage();
        }
    }
}
