package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitcode.mcsm_backend.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 文件系统工具提供者（本地）
 *
 * 提供给 AI 智能体操作服务器文件的工具能力：
 * 读/写/搜索/删除文件、YAML/JSON 精确操作、知识库管理、服务器日志诊断。
 */
@Slf4j
public class FileToolProvider implements LocalToolProvider {

    private final FileService fileService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Yaml yaml;

    public FileToolProvider(FileService fileService) {
        this.fileService = fileService;
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        this.yaml = new Yaml(options);
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        // ==================== 基础文件操作 ====================

        tools.add(new ToolDefinition("read_file",
                "[本地·文件操作] 读取服务器目录下的文件（如 server.properties、插件配置等）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件相对于游戏服务器目录的路径"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选"),
                                "maxLines", Map.of("type", "integer", "description", "最大读取行数，默认500")
                        ),
                        "required", List.of("path"))));

        tools.add(new ToolDefinition("write_file",
                "[本地·文件操作] 写入或覆盖服务器目录下的文件",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "content", Map.of("type", "string", "description", "文件内容"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "content"))));

        tools.add(new ToolDefinition("edit_file",
                "[本地·文件操作] 按文本内容匹配编辑服务器文件（全文查找替换，适合修改配置值。如需按行号编辑请用 edit_file_lines）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "oldText", Map.of("type", "string", "description", "要替换的原文"),
                                "newText", Map.of("type", "string", "description", "替换后的文本"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "oldText", "newText"))));

        tools.add(new ToolDefinition("edit_file_lines",
                "[本地·文件操作] 按行号精确编辑服务器文件（支持 replace 替换行、insert 在行后插入、delete 删除行）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "operation", Map.of("type", "string", "description", "操作类型：replace（替换行）、insert（在指定行后插入）、delete（删除行）"),
                                "startLine", Map.of("type", "integer", "description", "起始行号（从 1 开始）"),
                                "endLine", Map.of("type", "integer", "description", "结束行号（可选，仅 replace/delete 使用，不填则只操作 startLine）"),
                                "content", Map.of("type", "string", "description", "新内容（replace/insert 时必填，多行用换行分隔）"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "operation", "startLine"))));

        tools.add(new ToolDefinition("list_directory",
                "[本地·文件操作] 列出服务器目录内容",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "目录路径，不填则列出游戏服务器根目录"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of())));

        tools.add(new ToolDefinition("search_file",
                "[本地·文件操作] 按文件名关键词搜索服务器目录",
                Map.of("type", "object",
                        "properties", Map.of(
                                "pattern", Map.of("type", "string", "description", "文件名搜索关键词"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("pattern"))));

        tools.add(new ToolDefinition("grep_file",
                "[本地·文件操作] 按正则表达式搜索服务器文件内容",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件或目录路径"),
                                "regex", Map.of("type", "string", "description", "搜索的正则表达式"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "regex"))));

        tools.add(new ToolDefinition("delete_file",
                "[本地·文件操作] 删除服务器目录下的文件或目录（危险操作）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "要删除的文件路径"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path"))));

        tools.add(new ToolDefinition("get_server_properties",
                "[本地·文件操作] 读取并解析 server.properties 配置文件",
                ToolDefinition.stringParam("serverId", "游戏服务器ID，可选", false)));

        tools.add(new ToolDefinition("update_server_properties",
                "[本地·文件操作] 修改 server.properties 中的配置项（如 motd、max-players）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选"),
                                "key", Map.of("type", "string", "description", "属性键名，如 motd, max-players"),
                                "value", Map.of("type", "string", "description", "属性值")
                        ),
                        "required", List.of("key", "value"))));

        tools.add(new ToolDefinition("get_disk_usage",
                "[本地·系统监控] 查看主机磁盘使用情况（各分区总容量、已用、可用）",
                ToolDefinition.noParams()));

        tools.add(new ToolDefinition("get_system_info",
                "[本地·系统监控] 查看主机系统信息（OS、CPU 核数、系统内存、JVM 内存）。totalMemoryMB/freeMemoryMB 是系统物理内存，jvmMaxMemoryMB/jvmUsedMemoryMB 是 JVM 堆内存。",
                ToolDefinition.noParams()));

        // ==================== 新增：精确文件操作 ====================

        tools.add(new ToolDefinition("search_file_content",
                "[本地·文件操作] 搜索文件内容，返回匹配的行号和行内容（精确查找，不加载整个文件）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "文件路径"),
                                "pattern", Map.of("type", "string", "description", "搜索的正则表达式"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选"),
                                "maxResults", Map.of("type", "integer", "description", "最大返回条数，默认50")
                        ),
                        "required", List.of("path", "pattern"))));

        tools.add(new ToolDefinition("read_yaml_key",
                "[本地·文件操作] 读取 YAML 配置文件中某个 key 的值（支持嵌套路径如 economy.provider）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "YAML 文件路径"),
                                "keyPath", Map.of("type", "string", "description", "key 路径，用 . 分割嵌套层级，如 economy.provider"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "keyPath"))));

        tools.add(new ToolDefinition("edit_yaml_key",
                "[本地·文件操作] 精确修改 YAML 配置文件中某个 key 的值（只改目标行，不动其他内容）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "YAML 文件路径"),
                                "keyPath", Map.of("type", "string", "description", "key 路径，用 . 分割嵌套层级"),
                                "newValue", Map.of("type", "string", "description", "新的值（自动类型转换：数字/布尔/字符串）"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "keyPath", "newValue"))));

        tools.add(new ToolDefinition("read_json_path",
                "[本地·文件操作] 读取 JSON 文件中某个路径的值（支持嵌套路径如 database.host）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "JSON 文件路径"),
                                "jsonPath", Map.of("type", "string", "description", "路径，用 . 分割嵌套层级"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "jsonPath"))));

        tools.add(new ToolDefinition("edit_json_path",
                "[本地·文件操作] 精确修改 JSON 文件中某个路径的值",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "JSON 文件路径"),
                                "jsonPath", Map.of("type", "string", "description", "路径，用 . 分割嵌套层级"),
                                "newValue", Map.of("type", "string", "description", "新的值（自动类型转换）"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path", "jsonPath", "newValue"))));

        tools.add(new ToolDefinition("list_config_keys",
                "[本地·文件操作] 列出配置文件（YAML/Properties）的所有顶层 key",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "配置文件路径"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path"))));

        // ==================== 新增：知识库管理 ====================

        tools.add(new ToolDefinition("search_knowledge",
                "[本地·知识库] 搜索本地知识库（plugins/*/knowledge.md 和 knowledge/*.md）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "pluginName", Map.of("type", "string", "description", "插件名（可选，不填则搜索全局知识）"),
                                "query", Map.of("type", "string", "description", "搜索关键词"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("query"))));

        tools.add(new ToolDefinition("save_knowledge",
                "[本地·知识库] 将知识写入知识库文件（追加到对应 knowledge.md）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "category", Map.of("type", "string", "description", "知识类别：PLUGIN（插件级）或 GLOBAL（全局级）"),
                                "pluginName", Map.of("type", "string", "description", "插件名（PLUGIN 类别时必填）"),
                                "title", Map.of("type", "string", "description", "知识标题"),
                                "content", Map.of("type", "string", "description", "知识内容"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("category", "title", "content"))));

        // ==================== 新增：服务器日志诊断 ====================

        tools.add(new ToolDefinition("grep_server_logs",
                "[本地·服务器诊断] 搜索服务器控制台日志（按正则匹配最近日志）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID"),
                                "pattern", Map.of("type", "string", "description", "搜索的正则表达式"),
                                "maxLines", Map.of("type", "integer", "description", "搜索的最大行数，默认500")
                        ),
                        "required", List.of("serverId", "pattern"))));

        tools.add(new ToolDefinition("get_server_error",
                "[本地·服务器诊断] 获取服务器最近的错误日志（过滤 ERROR/Exception/WARN 关键词）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID"),
                                "maxLines", Map.of("type", "integer", "description", "搜索的最大行数，默认500")
                        ),
                        "required", List.of("serverId"))));

        // ==================== 知识就绪门控 ====================

        tools.add(new ToolDefinition("check_knowledge_readiness",
                "[本地·知识库] 检查插件的 knowledge.md 是否存在（知识就绪门控）。规划配置方案前调用，确认所有涉及插件都有 knowledge.md。",
                Map.of("type", "object",
                        "properties", Map.of(
                                "pluginNames", Map.of(
                                        "type", "array",
                                        "items", Map.of("type", "string"),
                                        "description", "要检查的插件名列表"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("pluginNames"))));

        // ==================== 系统操作工具 ====================

        tools.add(new ToolDefinition("execute_command",
                "[本地·系统操作] 在服务器主机上执行系统命令（如 ls、cat、ps 等）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "要执行的系统命令"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("command"))));

        tools.add(new ToolDefinition("create_directory",
                "[本地·系统操作] 创建目录（支持递归创建多级目录）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "path", Map.of("type", "string", "description", "目录路径"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("path"))));

        tools.add(new ToolDefinition("list_processes",
                "[本地·系统操作] 列出当前运行的进程（可选按名称过滤）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "filter", Map.of("type", "string", "description", "进程名过滤关键词，可选")
                        ),
                        "required", List.of())));

        tools.add(new ToolDefinition("get_environment",
                "[本地·系统操作] 获取系统环境变量",
                Map.of("type", "object",
                        "properties", Map.of(
                                "key", Map.of("type", "string", "description", "环境变量名，可选（不填返回全部）")
                        ),
                        "required", List.of())));

        // ==================== Context7 在线文档 ====================

        tools.add(new ToolDefinition("search_online_docs",
                "[本地·知识库] 查询 Context7 在线文档（先查在线文档索引确认可用性，查到后自动缓存到本地知识库）",
                Map.of("type", "object",
                        "properties", Map.of(
                                "pluginName", Map.of("type", "string", "description", "插件名"),
                                "query", Map.of("type", "string", "description", "查询内容"),
                                "serverId", Map.of("type", "string", "description", "游戏服务器ID，可选")
                        ),
                        "required", List.of("pluginName", "query"))));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        try {
            String serverId = (String) arguments.getOrDefault("serverId", null);

            return switch (toolName) {
                // ==================== 基础文件操作 ====================
                case "read_file" -> {
                    String path = (String) arguments.get("path");
                    int maxLines = arguments.containsKey("maxLines")
                            ? Integer.parseInt(String.valueOf(arguments.get("maxLines"))) : 500;
                    yield toJson(fileService.readFile(path, serverId, maxLines));
                }

                case "write_file" -> {
                    String path = (String) arguments.get("path");
                    String content = (String) arguments.get("content");
                    yield toJson(fileService.writeFile(path, content, serverId));
                }

                case "edit_file" -> {
                    String path = (String) arguments.get("path");
                    String oldText = (String) arguments.get("oldText");
                    String newText = (String) arguments.get("newText");
                    if (oldText == null || oldText.isEmpty()) {
                        yield toJson(Map.of("error", "oldText 不能为空"));
                    }
                    Map<String, Object> readResult = fileService.readFile(path, serverId, 0);
                    if (readResult.containsKey("error")) yield toJson(readResult);
                    String content = (String) readResult.get("content");
                    if (content == null) yield toJson(Map.of("error", "文件内容为空"));
                    String updated = content.replace(oldText, newText);
                    yield toJson(fileService.writeFile(path, updated, serverId));
                }

                case "edit_file_lines" -> {
                    String path = (String) arguments.get("path");
                    String operation = (String) arguments.get("operation");
                    int startLine = Integer.parseInt(String.valueOf(arguments.get("startLine")));
                    int endLine = arguments.containsKey("endLine")
                            ? Integer.parseInt(String.valueOf(arguments.get("endLine"))) : startLine;
                    String content = (String) arguments.getOrDefault("content", null);
                    yield toJson(fileService.editFileLines(path, operation, startLine, endLine, content, serverId));
                }

                case "list_directory" -> {
                    String path = (String) arguments.getOrDefault("path", ".");
                    yield toJson(fileService.listDirectory(path, serverId));
                }

                case "search_file" -> {
                    String pattern = (String) arguments.get("pattern");
                    yield toJson(fileService.searchFile(pattern, serverId));
                }

                case "grep_file" -> {
                    String path = (String) arguments.get("path");
                    String regex = (String) arguments.get("regex");
                    yield toJson(fileService.grepFile(path, regex, serverId));
                }

                case "delete_file" -> {
                    String path = (String) arguments.get("path");
                    yield toJson(fileService.deleteFile(path, serverId));
                }

                case "get_server_properties" -> {
                    yield toJson(fileService.getServerProperties(serverId));
                }

                case "update_server_properties" -> {
                    String key = (String) arguments.get("key");
                    String value = (String) arguments.get("value");
                    yield toJson(fileService.updateServerProperties(serverId, Map.of(key, value)));
                }

                case "get_disk_usage" -> toJson(fileService.getDiskUsage());

                case "get_system_info" -> toJson(fileService.getSystemInfo());

                // ==================== 精确文件操作 ====================
                case "search_file_content" -> {
                    String path = (String) arguments.get("path");
                    String pattern = (String) arguments.get("pattern");
                    int maxResults = arguments.containsKey("maxResults")
                            ? Integer.parseInt(String.valueOf(arguments.get("maxResults"))) : 50;
                    yield toJson(searchFileContent(path, pattern, serverId, maxResults));
                }

                case "read_yaml_key" -> {
                    String path = (String) arguments.get("path");
                    String keyPath = (String) arguments.get("keyPath");
                    yield toJson(readYamlKey(path, keyPath, serverId));
                }

                case "edit_yaml_key" -> {
                    String path = (String) arguments.get("path");
                    String keyPath = (String) arguments.get("keyPath");
                    String newValue = (String) arguments.get("newValue");
                    yield toJson(editYamlKey(path, keyPath, newValue, serverId));
                }

                case "read_json_path" -> {
                    String path = (String) arguments.get("path");
                    String jsonPath = (String) arguments.get("jsonPath");
                    yield toJson(readJsonPath(path, jsonPath, serverId));
                }

                case "edit_json_path" -> {
                    String path = (String) arguments.get("path");
                    String jsonPath = (String) arguments.get("jsonPath");
                    String newValue = (String) arguments.get("newValue");
                    yield toJson(editJsonPath(path, jsonPath, newValue, serverId));
                }

                case "list_config_keys" -> {
                    String path = (String) arguments.get("path");
                    yield toJson(listConfigKeys(path, serverId));
                }

                // ==================== 知识库管理 ====================
                case "search_knowledge" -> {
                    String pluginName = (String) arguments.get("pluginName");
                    String query = (String) arguments.get("query");
                    yield toJson(searchKnowledge(pluginName, query, serverId));
                }

                case "save_knowledge" -> {
                    String category = (String) arguments.get("category");
                    String pluginName = (String) arguments.get("pluginName");
                    String title = (String) arguments.get("title");
                    String content = (String) arguments.get("content");
                    yield toJson(saveKnowledge(category, pluginName, title, content, serverId));
                }

                case "check_knowledge_readiness" -> {
                    @SuppressWarnings("unchecked")
                    List<String> pluginNames = (List<String>) arguments.get("pluginNames");
                    yield toJson(checkKnowledgeReadiness(pluginNames, serverId));
                }

                case "search_online_docs" -> {
                    String pluginName = (String) arguments.get("pluginName");
                    String query = (String) arguments.get("query");
                    yield toJson(searchOnlineDocs(pluginName, query, serverId));
                }

                // ==================== 服务器日志诊断 ====================
                case "grep_server_logs" -> {
                    String serverIdForLog = (String) arguments.get("serverId");
                    String pattern = (String) arguments.get("pattern");
                    int maxLines = arguments.containsKey("maxLines")
                            ? Integer.parseInt(String.valueOf(arguments.get("maxLines"))) : 500;
                    yield toJson(grepServerLogs(serverIdForLog, pattern, maxLines));
                }

                case "get_server_error" -> {
                    String serverIdForErr = (String) arguments.get("serverId");
                    int maxLines = arguments.containsKey("maxLines")
                            ? Integer.parseInt(String.valueOf(arguments.get("maxLines"))) : 500;
                    yield toJson(getServerError(serverIdForErr, maxLines));
                }

                // ==================== 系统操作 ====================
                case "execute_command" -> {
                    String command = (String) arguments.get("command");
                    yield toJson(executeCommand(command));
                }

                case "create_directory" -> {
                    String path = (String) arguments.get("path");
                    yield toJson(createDirectory(path, serverId));
                }

                case "list_processes" -> {
                    String filter = (String) arguments.getOrDefault("filter", null);
                    yield toJson(listProcesses(filter));
                }

                case "get_environment" -> {
                    String key = (String) arguments.getOrDefault("key", null);
                    yield toJson(getEnvironment(key));
                }

                default -> toJson(Map.of("error", "未知工具: " + toolName));
            };
        } catch (Exception e) {
            return toJson(Map.of("error", "执行失败: " + e.getMessage()));
        }
    }

    // ==================== 精确文件操作实现 ====================

    private Map<String, Object> searchFileContent(String path, String pattern, String serverId, int maxResults) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            List<Map<String, Object>> matches = new ArrayList<>();
            List<String> lines = Files.readAllLines(filePath);

            for (int i = 0; i < lines.size(); i++) {
                Matcher m = regex.matcher(lines.get(i));
                if (m.find()) {
                    matches.add(Map.of("line", i + 1, "content", lines.get(i).trim()));
                    if (matches.size() >= maxResults) break;
                }
            }

            return Map.of("path", path, "pattern", pattern, "matches", matches, "total", matches.size());
        } catch (Exception e) {
            return Map.of("error", "搜索失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYamlKey(String path, String keyPath, String serverId) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            Map<String, Object> data = yaml.load(Files.newInputStream(filePath));
            String[] keys = keyPath.split("\\.");

            Object current = data;
            for (String key : keys) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(key);
                } else {
                    return Map.of("error", "路径不存在: " + keyPath + "（在 " + key + " 处中断）");
                }
            }

            if (current == null) return Map.of("error", "key 不存在: " + keyPath);
            return Map.of("path", path, "keyPath", keyPath, "value", current);
        } catch (Exception e) {
            return Map.of("error", "读取 YAML 失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> editYamlKey(String path, String keyPath, String newValue, String serverId) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            Map<String, Object> data = yaml.load(Files.newInputStream(filePath));
            String[] keys = keyPath.split("\\.");

            // 导航到父节点
            Map<String, Object> current = data;
            for (int i = 0; i < keys.length - 1; i++) {
                Object next = current.get(keys[i]);
                if (next instanceof Map) {
                    current = (Map<String, Object>) next;
                } else {
                    return Map.of("error", "路径不存在: " + keyPath);
                }
            }

            // 设置新值（自动类型转换）
            String lastKey = keys[keys.length - 1];
            current.put(lastKey, parseValue(newValue));

            // 写回文件
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml outYaml = new Yaml(options);
            outYaml.dump(data, new FileWriter(filePath.toFile()));

            return Map.of("success", true, "path", path, "keyPath", keyPath, "newValue", newValue);
        } catch (Exception e) {
            return Map.of("error", "编辑 YAML 失败: " + e.getMessage());
        }
    }

    private Map<String, Object> readJsonPath(String path, String jsonPath, String serverId) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            JsonNode root = objectMapper.readTree(filePath.toFile());
            String[] keys = jsonPath.split("\\.");

            JsonNode current = root;
            for (String key : keys) {
                if (current == null || !current.has(key)) {
                    return Map.of("error", "路径不存在: " + jsonPath);
                }
                current = current.get(key);
            }

            return Map.of("path", path, "jsonPath", jsonPath, "value", objectMapper.convertValue(current, Object.class));
        } catch (Exception e) {
            return Map.of("error", "读取 JSON 失败: " + e.getMessage());
        }
    }

    private Map<String, Object> editJsonPath(String path, String jsonPath, String newValue, String serverId) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            ObjectNode root = (ObjectNode) objectMapper.readTree(filePath.toFile());
            String[] keys = jsonPath.split("\\.");

            // 导航到父节点
            ObjectNode current = root;
            for (int i = 0; i < keys.length - 1; i++) {
                JsonNode next = current.get(keys[i]);
                if (next == null || !next.isObject()) {
                    return Map.of("error", "路径不存在: " + jsonPath);
                }
                current = (ObjectNode) next;
            }

            // 设置新值
            String lastKey = keys[keys.length - 1];
            Object val = parseValue(newValue);
            if (val instanceof Integer) current.put(lastKey, (int) val);
            else if (val instanceof Long) current.put(lastKey, (long) val);
            else if (val instanceof Double) current.put(lastKey, (double) val);
            else if (val instanceof Boolean) current.put(lastKey, (boolean) val);
            else current.put(lastKey, val.toString());

            // 写回文件
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), root);

            return Map.of("success", true, "path", path, "jsonPath", jsonPath, "newValue", newValue);
        } catch (Exception e) {
            return Map.of("error", "编辑 JSON 失败: " + e.getMessage());
        }
    }

    private Map<String, Object> listConfigKeys(String path, String serverId) {
        try {
            Path filePath = resolvePath(path, serverId);
            if (!Files.exists(filePath)) return Map.of("error", "文件不存在: " + path);

            String content = Files.readString(filePath);
            List<String> keys = new ArrayList<>();

            if (path.endsWith(".yml") || path.endsWith(".yaml")) {
                Map<String, Object> data = yaml.load(content);
                if (data != null) keys.addAll(data.keySet());
            } else if (path.endsWith(".properties")) {
                Properties props = new Properties();
                props.load(new StringReader(content));
                keys.addAll(props.stringPropertyNames());
            } else {
                return Map.of("error", "不支持的文件格式，仅支持 .yml/.yaml/.properties");
            }

            return Map.of("path", path, "keys", keys, "count", keys.size());
        } catch (Exception e) {
            return Map.of("error", "列出配置 key 失败: " + e.getMessage());
        }
    }

    // ==================== 知识库管理实现 ====================

    private Map<String, Object> searchKnowledge(String pluginName, String query, String serverId) {
        try {
            String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
            List<Map<String, Object>> results = new ArrayList<>();
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

            // 搜索插件级知识
            if (pluginName != null && !pluginName.isEmpty()) {
                Path pluginKnowledge = Path.of(baseDir, "plugins", pluginName, "knowledge.md");
                if (Files.exists(pluginKnowledge)) {
                    searchInFile(pluginKnowledge, pattern, "PLUGIN:" + pluginName, results);
                }
            } else {
                // 搜索所有插件的 knowledge.md
                Path pluginsDir = Path.of(baseDir, "plugins");
                if (Files.isDirectory(pluginsDir)) {
                    try (Stream<Path> stream = Files.list(pluginsDir)) {
                        stream.filter(Files::isDirectory).forEach(dir -> {
                            Path knowledgeFile = dir.resolve("knowledge.md");
                            if (Files.exists(knowledgeFile)) {
                                searchInFile(knowledgeFile, pattern, "PLUGIN:" + dir.getFileName(), results);
                            }
                        });
                    }
                }
            }

            // 搜索全局知识
            Path knowledgeDir = Path.of(baseDir, "knowledge");
            if (Files.isDirectory(knowledgeDir)) {
                try (Stream<Path> stream = Files.list(knowledgeDir)) {
                    stream.filter(p -> p.toString().endsWith(".md")).forEach(file -> {
                        searchInFile(file, pattern, "GLOBAL:" + file.getFileName(), results);
                    });
                }
            }

            return Map.of("query", query, "results", results, "total", results.size());
        } catch (Exception e) {
            return Map.of("error", "搜索知识库失败: " + e.getMessage());
        }
    }

    private void searchInFile(Path file, Pattern pattern, String source, List<Map<String, Object>> results) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                if (pattern.matcher(lines.get(i)).find()) {
                    results.add(Map.of(
                            "source", source,
                            "file", file.toString(),
                            "line", i + 1,
                            "content", lines.get(i).trim()
                    ));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> saveKnowledge(String category, String pluginName, String title, String content, String serverId) {
        try {
            String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
            Path targetFile;

            if ("PLUGIN".equalsIgnoreCase(category) && pluginName != null) {
                targetFile = Path.of(baseDir, "plugins", pluginName, "knowledge.md");
                Files.createDirectories(targetFile.getParent());

                // 如果文件不存在，创建头部
                if (!Files.exists(targetFile)) {
                    String header = "# " + pluginName + " 配置知识\n" +
                            "#\n" +
                            "# 用途: 记录 " + pluginName + " 的配置规则、已知问题和用户偏好。\n" +
                            "# 创建时间: " + java.time.LocalDate.now() + "\n" +
                            "#\n";
                    Files.writeString(targetFile, header);
                }
            } else {
                targetFile = Path.of(baseDir, "knowledge", "用户偏好记录.md");
                Files.createDirectories(targetFile.getParent());

                if (!Files.exists(targetFile)) {
                    Files.writeString(targetFile, "# 用户偏好记录\n\n");
                }
            }

            // 追加写入
            String entry = "\n## " + title + "\n\n" + content + "\n\n---\n";
            Files.writeString(targetFile, entry, StandardOpenOption.APPEND);

            return Map.of("success", true, "file", targetFile.toString(), "title", title);
        } catch (Exception e) {
            return Map.of("error", "保存知识失败: " + e.getMessage());
        }
    }

    /**
     * 知识就绪门控：检查插件的 knowledge.md 是否存在
     */
    private Map<String, Object> checkKnowledgeReadiness(List<String> pluginNames, String serverId) {
        String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> notReady = new ArrayList<>();
        List<String> ready = new ArrayList<>();

        for (String pluginName : pluginNames) {
            String[] possiblePaths = {
                    baseDir + "/plugins/" + pluginName + "/knowledge.md",
                    baseDir + "/plugins/" + pluginName.toLowerCase() + "/knowledge.md"
            };

            boolean found = false;
            String foundPath = null;
            for (String path : possiblePaths) {
                if (Files.exists(Path.of(path))) {
                    found = true;
                    foundPath = path;
                    break;
                }
            }

            if (found) {
                ready.add(pluginName);
                results.add(Map.of("plugin", pluginName, "ready", true, "path", foundPath));
            } else {
                notReady.add(pluginName);
                results.add(Map.of("plugin", pluginName, "ready", false));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("allReady", notReady.isEmpty());
        result.put("ready", ready);
        result.put("notReady", notReady);
        result.put("details", results);

        if (!notReady.isEmpty()) {
            result.put("action", "需要先为以下插件生成 knowledge.md: " + String.join(", ", notReady));
        }

        return result;
    }

    // ==================== 系统操作实现 ====================

    private Map<String, Object> executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (sb.length() > 5000) {
                        sb.append("... (输出过长，已截断)\n");
                        break;
                    }
                }
                output = sb.toString().trim();
            }

            int exitCode = process.waitFor();
            return Map.of("command", command, "exitCode", exitCode, "output", output);
        } catch (Exception e) {
            return Map.of("error", "执行命令失败: " + e.getMessage());
        }
    }

    private Map<String, Object> createDirectory(String path, String serverId) {
        try {
            Path dirPath = resolvePath(path, serverId);
            Files.createDirectories(dirPath);
            return Map.of("success", true, "path", dirPath.toString());
        } catch (Exception e) {
            return Map.of("error", "创建目录失败: " + e.getMessage());
        }
    }

    private Map<String, Object> listProcesses(String filter) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            ProcessBuilder pb;
            if (isWindows) {
                java.util.List<String> cmd = new java.util.ArrayList<>();
                cmd.add("tasklist");
                if (filter != null && !filter.isEmpty()) {
                    // 只允许字母数字和下划线，防止命令注入
                    String safe = filter.replaceAll("[^a-zA-Z0-9_.\\-]", "");
                    cmd.add("/FI");
                    cmd.add("IMAGENAME eq " + safe + "*");
                }
                pb = new ProcessBuilder(cmd);
            } else {
                java.util.List<String> cmd = new java.util.ArrayList<>();
                cmd.add("ps");
                cmd.add("aux");
                pb = new ProcessBuilder(cmd);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            String filterLower = filter != null ? filter.toLowerCase() : null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            isWindows ? java.nio.charset.Charset.forName("GBK") : java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (filterLower == null || line.toLowerCase().contains(filterLower)) {
                        sb.append(line).append("\n");
                    }
                }
                output = sb.toString();
            }

            process.waitFor();
            return Map.of("processes", output, "filter", filter != null ? filter : "all");
        } catch (Exception e) {
            return Map.of("error", "列出进程失败: " + e.getMessage());
        }
    }

    private Map<String, Object> getEnvironment(String key) {
        try {
            if (key != null && !key.isEmpty()) {
                String value = System.getenv(key);
                if (value == null) return Map.of("error", "环境变量不存在: " + key);
                return Map.of("key", key, "value", value);
            }

            Map<String, String> env = System.getenv();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("count", env.size());
            result.put("variables", env);
            return result;
        } catch (Exception e) {
            return Map.of("error", "获取环境变量失败: " + e.getMessage());
        }
    }

    // ==================== Context7 在线文档实现 ====================

    /**
     * 查询 Context7 在线文档
     *
     * 流程：
     * 1. 先查 knowledge/在线文档索引.md 确认可用性
     * 2. 如果索引标记"不可用"→ 返回无文档
     * 3. 如果有 library ID → 查 Context7 API
     * 4. 查到结果 → 缓存到本地知识库
     */
    private Map<String, Object> searchOnlineDocs(String pluginName, String query, String serverId) {
        try {
            String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";

            // 1. 检查在线文档索引
            String indexStatus = checkOnlineDocIndex(pluginName, baseDir);
            if ("UNAVAILABLE".equals(indexStatus)) {
                return Map.of("plugin", pluginName, "available", false,
                        "reason", "在线文档索引标记该插件 Context7 不可用");
            }

            // 2. 解析 library ID（从索引或 API）
            String libraryId = indexStatus;
            if (libraryId == null || libraryId.isEmpty()) {
                libraryId = resolveContext7Library(pluginName);
                if (libraryId == null) {
                    // 记录为不可用
                    updateOnlineDocIndex(pluginName, "UNAVAILABLE", baseDir);
                    return Map.of("plugin", pluginName, "available", false,
                            "reason", "Context7 未找到该插件的文档");
                }
                // 更新索引
                updateOnlineDocIndex(pluginName, libraryId, baseDir);
            }

            // 3. 查询 Context7 API
            String documentation = fetchContext7Docs(libraryId, query);
            if (documentation == null || documentation.isEmpty()) {
                return Map.of("plugin", pluginName, "available", true,
                        "libraryId", libraryId, "result", "未找到相关内容");
            }

            // 4. 缓存到本地知识库
            cacheDocumentation(pluginName, query, documentation, baseDir);

            return Map.of("plugin", pluginName, "available", true,
                    "libraryId", libraryId, "result", documentation,
                    "cached", true);
        } catch (Exception e) {
            return Map.of("error", "查询在线文档失败: " + e.getMessage());
        }
    }

    private String checkOnlineDocIndex(String pluginName, String baseDir) {
        try {
            Path indexPath = Path.of(baseDir, "knowledge", "在线文档索引.md");
            if (!Files.exists(indexPath)) return null;

            List<String> lines = Files.readAllLines(indexPath);
            for (String line : lines) {
                if (line.toLowerCase().contains(pluginName.toLowerCase())) {
                    if (line.contains("不可用") || line.contains("UNAVAILABLE")) {
                        return "UNAVAILABLE";
                    }
                    // 尝试提取 library ID（格式：| 插件名 | 可用 | library-id | ...）
                    String[] parts = line.split("\\|");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (trimmed.contains("/")) {
                            return trimmed; // 可能是 library ID
                        }
                    }
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateOnlineDocIndex(String pluginName, String status, String baseDir) {
        try {
            Path indexPath = Path.of(baseDir, "knowledge", "在线文档索引.md");
            if (!Files.exists(indexPath)) return;

            List<String> lines = new ArrayList<>(Files.readAllLines(indexPath));
            String newLine;
            if ("UNAVAILABLE".equals(status)) {
                newLine = "| " + pluginName + " | ❌ | - | - | Context7 不可用 |";
            } else {
                newLine = "| " + pluginName + " | ✅ | " + status + " | 中 | 自动发现 |";
            }

            // 查找是否已有该插件的记录
            boolean updated = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).toLowerCase().contains(pluginName.toLowerCase())) {
                    lines.set(i, newLine);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                lines.add(newLine);
            }

            Files.writeString(indexPath, String.join("\n", lines) + "\n");
        } catch (Exception ignored) {
        }
    }

    private String resolveContext7Library(String pluginName) {
        try {
            String url = "https://context7.com/api/v1/resolve?query=" + URLEncoder.encode(pluginName, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            if (results != null && !results.isEmpty()) {
                return (String) results.get(0).get("id");
            }
            return null;
        } catch (Exception e) {
            log.error("[Context7] 解析 library 失败: {}", e.getMessage());
            return null;
        }
    }

    private String fetchContext7Docs(String libraryId, String query) {
        try {
            String url = "https://context7.com/api/v1/" + libraryId + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            return (String) result.get("content");
        } catch (Exception e) {
            log.error("[Context7] 获取文档失败: {}", e.getMessage());
            return null;
        }
    }

    private void cacheDocumentation(String pluginName, String query, String documentation, String baseDir) {
        try {
            Path knowledgePath = Path.of(baseDir, "plugins", pluginName, "knowledge.md");
            Files.createDirectories(knowledgePath.getParent());

            String entry = "\n## Context7: " + query + "\n\n" + documentation + "\n\n---\n";
            Files.writeString(knowledgePath, entry, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (Exception e) {
            log.error("[Context7] 缓存文档失败: {}", e.getMessage());
        }
    }

    // ==================== 服务器日志诊断实现 ====================

    private Map<String, Object> grepServerLogs(String serverId, String pattern, int maxLines) {
        try {
            // 通过 FileService 读取日志文件
            String logPath = "logs/latest.log";
            Map<String, Object> readResult = fileService.readFile(logPath, serverId, maxLines);
            if (readResult.containsKey("error")) return readResult;

            String content = (String) readResult.getOrDefault("content", "");
            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            List<Map<String, Object>> matches = new ArrayList<>();

            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (regex.matcher(lines[i]).find()) {
                    matches.add(Map.of("line", i + 1, "content", lines[i].trim()));
                    if (matches.size() >= 100) break;
                }
            }

            return Map.of("serverId", serverId, "pattern", pattern, "matches", matches, "total", matches.size());
        } catch (Exception e) {
            return Map.of("error", "搜索日志失败: " + e.getMessage());
        }
    }

    private Map<String, Object> getServerError(String serverId, int maxLines) {
        try {
            String logPath = "logs/latest.log";
            Map<String, Object> readResult = fileService.readFile(logPath, serverId, maxLines);
            if (readResult.containsKey("error")) return readResult;

            String content = (String) readResult.getOrDefault("content", "");
            Pattern errorPattern = Pattern.compile("(ERROR|Exception|WARN|SEVERE|Caused by)", Pattern.CASE_INSENSITIVE);
            List<Map<String, Object>> errors = new ArrayList<>();

            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (errorPattern.matcher(lines[i]).find()) {
                    errors.add(Map.of("line", i + 1, "content", lines[i].trim()));
                    if (errors.size() >= 50) break;
                }
            }

            return Map.of("serverId", serverId, "errors", errors, "total", errors.size());
        } catch (Exception e) {
            return Map.of("error", "获取错误日志失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private Path resolvePath(String path, String serverId) {
        if (serverId != null && !serverId.isEmpty()) {
            return Path.of(com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR, serverId, path);
        }
        return Path.of(com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR, "default", path);
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"JSON 序列化失败\"}";
        }
    }
}
