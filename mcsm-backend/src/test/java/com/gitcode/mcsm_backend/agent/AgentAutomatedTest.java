package com.gitcode.mcsm_backend.agent;

import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.core.WorkerAgent;
import com.gitcode.mcsm_backend.agent.core.ResultCollector;
import com.gitcode.mcsm_backend.agent.memory.KnowledgeInjector;
import com.gitcode.mcsm_backend.agent.memory.LocalKnowledgeStore;
import com.gitcode.mcsm_backend.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Agent 自动化测试 — 不依赖 Spring 容器，测试核心功能
 *
 * 测试内容：
 * 1. LLM 连接和基本对话
 * 2. 工具注册和定义
 * 3. 文件操作工具
 * 4. 知识就绪门控
 * 5. WorkerAgent 执行
 * 6. ResultCollector 收集
 * 7. spawn_workers 工具
 */
public class AgentAutomatedTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> errors = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("       Agent 自动化测试");
        System.out.println("========================================\n");

        // 1. 读取配置
        Properties props = new Properties();
        Path configPath = Path.of(com.gitcode.mcsm_backend.common.McsmPaths.APPLICATION_PROPERTIES);
        if (Files.exists(configPath)) {
            try (var is = Files.newInputStream(configPath)) {
                props.load(is);
            }
        }

        String baseUrl = resolve(props, "ai.llm.base-url", "https://api.openai.com/v1");
        String apiKey = resolve(props, "ai.llm.api-key", "");
        String proModel = resolve(props, "ai.llm.pro-model", "gpt-4o");
        String flashModel = resolve(props, "ai.llm.flash-model", proModel);

        if (apiKey.isEmpty()) {
            System.err.println("错误: 未配置 ai.llm.api-key");
            return;
        }

        System.out.println("LLM: " + baseUrl);
        System.out.println("Pro: " + proModel);
        System.out.println("Flash: " + flashModel);
        System.out.println();

        // 2. 初始化组件
        MultiModelRouter router = new MultiModelRouter(baseUrl, apiKey, proModel, flashModel);
        LlmClient llmClient = new LlmClient(router);

        FileService fileService = new FileService("servers");
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerLocal(new ConsoleToolProvider());
        toolRegistry.registerLocal(new FileToolProvider(fileService));

        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ChatHistory chatHistory = new ChatHistory();

        // 创建测试服务器目录
        setupTestEnvironment();

        // 3. 运行测试
        System.out.println("--- 测试开始 ---\n");

        testToolRegistry(toolRegistry);
        testLlmConnection(llmClient);
        testBasicChat(llmClient, toolRegistry, toolExecutor, chatHistory);
        testFileTools(toolExecutor);
        testKnowledgeReadiness(toolExecutor);
        testWorkerAgent(llmClient, toolExecutor, toolRegistry);
        testResultCollector();
        testSpawnWorkers(llmClient, toolExecutor, toolRegistry);

        // 4. 输出结果
        System.out.println("\n========================================");
        System.out.println("       测试结果");
        System.out.println("========================================");
        System.out.println("通过: " + passed);
        System.out.println("失败: " + failed);
        if (!errors.isEmpty()) {
            System.out.println("\n失败详情:");
            errors.forEach(e -> System.out.println("  - " + e));
        }
        System.out.println("========================================");
    }

    // ==================== 测试用例 ====================

    private static void testToolRegistry(ToolRegistry registry) {
        System.out.println("[测试] 工具注册");
        List<Map<String, Object>> tools = registry.getAllTools();

        assertTest("工具数量 > 0", tools.size() > 0, "工具数量: " + tools.size());

        // 检查关键工具是否存在
        Set<String> toolNames = new HashSet<>();
        for (Map<String, Object> tool : tools) {
            Map<String, Object> func = (Map<String, Object>) tool.get("function");
            if (func != null) toolNames.add((String) func.get("name"));
        }

        String[] requiredTools = {
                "read_file", "write_file", "edit_file", "search_file_content",
                "read_yaml_key", "edit_yaml_key", "search_knowledge", "save_knowledge",
                "check_knowledge_readiness", "search_online_docs",
                "execute_command", "create_directory", "list_processes", "get_environment",
                "grep_server_logs", "get_server_error"
        };

        for (String toolName : requiredTools) {
            assertTest("工具 " + toolName + " 已注册", toolNames.contains(toolName));
        }
        System.out.println();
    }

    private static void testLlmConnection(LlmClient llmClient) {
        System.out.println("[测试] LLM 连接");
        try {
            String response = llmClient.chatWithSystemPrompt(
                    "你是一个测试助手",
                    List.of(Map.of("role", "user", "content", "回复 OK")),
                    "FLASH"
            );
            assertTest("LLM 调用成功", response != null && !response.isEmpty(),
                    "响应: " + (response != null ? response.substring(0, Math.min(50, response.length())) : "null"));
        } catch (Exception e) {
            assertTest("LLM 调用成功", false, e.getMessage());
        }
        System.out.println();
    }

    private static void testBasicChat(LlmClient llmClient, ToolRegistry registry,
                                       ToolExecutor executor, ChatHistory history) {
        System.out.println("[测试] 基本对话");
        try {
            AgentCore agentCore = new AgentCore(llmClient, executor, registry, history);
            String sessionId = "test-basic-" + System.currentTimeMillis();

            String reply = agentCore.chat(sessionId, "你好，请回复一句话");
            assertTest("对话有回复", reply != null && !reply.isEmpty(),
                    "回复: " + (reply != null ? reply.substring(0, Math.min(80, reply.length())) : "null"));

            history.destroy(sessionId);
        } catch (Exception e) {
            assertTest("基本对话", false, e.getMessage());
        }
        System.out.println();
    }

    private static void testFileTools(ToolExecutor executor) {
        System.out.println("[测试] 文件操作工具");

        // 测试 write_file
        try {
            Map<String, Object> writeArgs = Map.of(
                    "path", "default/knowledge/test.md",
                    "content", "# 测试文件\n\n这是一个测试文件\n"
            );
            String result = executor.execute("write_file", writeArgs);
            assertTest("write_file 执行", !result.contains("error"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("write_file 执行", false, e.getMessage());
        }

        // 测试 read_file
        try {
            Map<String, Object> readArgs = Map.of("path", "default/knowledge/test.md");
            String result = executor.execute("read_file", readArgs);
            assertTest("read_file 执行", result.contains("测试文件"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("read_file 执行", false, e.getMessage());
        }

        // 测试 search_file_content（使用 grep_file 替代，因为路径解析方式不同）
        try {
            Map<String, Object> searchArgs = Map.of("path", "default/knowledge/test.md", "regex", "测试");
            String result = executor.execute("grep_file", searchArgs);
            assertTest("grep_file 执行", result.contains("测试"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("grep_file 执行", false, e.getMessage());
        }

        // 测试 save_knowledge
        try {
            Map<String, Object> saveArgs = Map.of(
                    "category", "PLUGIN",
                    "pluginName", "TestPlugin",
                    "title", "测试知识",
                    "content", "这是一条测试知识"
            );
            String result = executor.execute("save_knowledge", saveArgs);
            assertTest("save_knowledge 执行", !result.contains("error"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("save_knowledge 执行", false, e.getMessage());
        }

        // 测试 search_knowledge
        try {
            Map<String, Object> searchKArgs = Map.of("query", "测试");
            String result = executor.execute("search_knowledge", searchKArgs);
            assertTest("search_knowledge 执行", !result.contains("error"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("search_knowledge 执行", false, e.getMessage());
        }

        System.out.println();
    }

    private static void testKnowledgeReadiness(ToolExecutor executor) {
        System.out.println("[测试] 知识就绪门控");

        // 测试 check_knowledge_readiness
        try {
            Map<String, Object> args = Map.of("pluginNames", List.of("TestPlugin", "NonExistentPlugin"));
            String result = executor.execute("check_knowledge_readiness", args);
            assertTest("check_knowledge_readiness 执行", result.contains("allReady"), result.substring(0, Math.min(150, result.length())));
        } catch (Exception e) {
            assertTest("check_knowledge_readiness 执行", false, e.getMessage());
        }

        // 测试 create_directory
        try {
            Map<String, Object> args = Map.of("path", "test-dir/sub-dir");
            String result = executor.execute("create_directory", args);
            assertTest("create_directory 执行", result.contains("success"), result.substring(0, Math.min(100, result.length())));
        } catch (Exception e) {
            assertTest("create_directory 执行", false, e.getMessage());
        }

        System.out.println();
    }

    private static void testWorkerAgent(LlmClient llmClient, ToolExecutor executor, ToolRegistry registry) {
        System.out.println("[测试] WorkerAgent");

        try {
            WorkerAgent worker = new WorkerAgent(llmClient, executor, registry);

            Map<String, Object> workOrder = Map.of(
                    "targetPlugin", "TestPlugin",
                    "taskGoal", "读取 knowledge/test.md 文件并返回其内容摘要",
                    "fileScope", List.of("knowledge/test.md"),
                    "extraContext", "这是一个测试任务"
            );

            String result = worker.execute(workOrder);
            assertTest("WorkerAgent 执行", result != null && !result.isEmpty(),
                    "结果: " + (result != null ? result.substring(0, Math.min(100, result.length())) : "null"));

            // 检查输出格式是否为 JSON
            assertTest("WorkerAgent 输出 JSON", result.contains("summary"), "检查 JSON 格式");
        } catch (Exception e) {
            assertTest("WorkerAgent 执行", false, e.getMessage());
        }

        System.out.println();
    }

    private static void testResultCollector() {
        System.out.println("[测试] ResultCollector");

        try {
            ResultCollector collector = new ResultCollector();

            List<String> workerResults = List.of(
                    "{\"summary\":\"任务1完成\",\"results\":[{\"file\":\"a.yml\",\"status\":\"SUCCESS\",\"detail\":\"已修改\"}],\"self_check\":{\"passed\":true,\"concerns\":[],\"verified\":[\"验证通过\"]},\"discoveries\":[]}",
                    "{\"summary\":\"任务2失败\",\"results\":[{\"file\":\"b.yml\",\"status\":\"FAILED\",\"detail\":\"文件不存在\"}],\"self_check\":{\"passed\":false,\"concerns\":[\"文件不存在\"],\"verified\":[]},\"discoveries\":[]}"
            );

            String summary = collector.collect(workerResults);
            assertTest("ResultCollector 收集", summary.contains("totalWorkers"), summary.substring(0, Math.min(150, summary.length())));
            assertTest("ResultCollector 统计", summary.contains("\"success\":1") && summary.contains("\"failed\":1"), "检查成功/失败统计");
        } catch (Exception e) {
            assertTest("ResultCollector 执行", false, e.getMessage());
        }

        System.out.println();
    }

    private static void testSpawnWorkers(LlmClient llmClient, ToolExecutor executor, ToolRegistry registry) {
        System.out.println("[测试] spawn_workers 工具");

        try {
            // 先注册 WorkerToolProvider
            ToolRegistry fullRegistry = new ToolRegistry();
            fullRegistry.registerLocal(new ConsoleToolProvider());
            fullRegistry.registerLocal(new FileToolProvider(new FileService("servers")));
            fullRegistry.registerLocal(new WorkerToolProvider(llmClient, executor, fullRegistry));

            ToolExecutor fullExecutor = new ToolExecutor(fullRegistry);

            Map<String, Object> args = Map.of("workers", List.of(
                    Map.of(
                            "targetPlugin", "TestPlugin",
                            "taskGoal", "读取 knowledge/test.md 文件内容",
                            "fileScope", List.of("knowledge/test.md"),
                            "extraContext", "测试 spawn_workers"
                    )
            ));

            String result = fullExecutor.execute("spawn_workers", args);
            assertTest("spawn_workers 执行", result != null && !result.contains("error"),
                    "结果: " + (result != null ? result.substring(0, Math.min(150, result.length())) : "null"));
        } catch (Exception e) {
            assertTest("spawn_workers 执行", false, e.getMessage());
        }

        System.out.println();
    }

    // ==================== 辅助方法 ====================

    private static void setupTestEnvironment() {
        try {
            Files.createDirectories(Path.of(com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR, "default", "knowledge"));
            Files.createDirectories(Path.of(com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR, "default", "plugins", "TestPlugin"));
            System.out.println("[环境] 测试目录已创建\n");
        } catch (Exception e) {
            System.err.println("[环境] 创建测试目录失败: " + e.getMessage());
        }
    }

    private static void assertTest(String name, boolean condition) {
        assertTest(name, condition, null);
    }

    private static void assertTest(String name, boolean condition, String detail) {
        if (condition) {
            passed++;
            System.out.println("  ✅ " + name + (detail != null ? " (" + detail + ")" : ""));
        } else {
            failed++;
            String msg = name + (detail != null ? ": " + detail : "");
            errors.add(msg);
            System.out.println("  ❌ " + msg);
        }
    }

    private static String resolve(Properties props, String key, String defaultValue) {
        String val = props.getProperty(key, "");
        if (val.startsWith("${") && val.endsWith("}")) {
            String ref = val.substring(2, val.length() - 1);
            val = props.getProperty(ref, defaultValue);
        }
        return val.isEmpty() ? defaultValue : val;
    }
}
