package com.gitcode.mcsm_backend.agent;

import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.core.WorkerAgent;
import com.gitcode.mcsm_backend.agent.core.ResultCollector;
import com.gitcode.mcsm_backend.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Agent 复杂任务测试 — 模拟真实运维场景
 *
 * 测试场景：
 * 1. 多文件操作：创建插件配置目录和 knowledge.md
 * 2. 跨文件查询：搜索多个配置文件
 * 3. Worker 并行：多个 Worker 同时处理不同插件
 * 4. 知识库管理：搜索、写入、就绪检查
 * 5. 模拟完整流程：理解 → 知识检查 → 执行 → 总结
 */
public class AgentComplexTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> errors = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("     Agent 复杂任务测试");
        System.out.println("========================================\n");

        // 1. 初始化
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
        System.out.println("Pro: " + proModel + " | Flash: " + flashModel);
        System.out.println();

        MultiModelRouter router = new MultiModelRouter(baseUrl, apiKey, proModel, flashModel);
        LlmClient llmClient = new LlmClient(router);

        FileService fileService = new FileService("servers");
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerLocal(new ConsoleToolProvider());
        toolRegistry.registerLocal(new FileToolProvider(fileService));

        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ChatHistory chatHistory = new ChatHistory();

        // 创建模拟环境
        setupMockEnvironment(toolExecutor);

        System.out.println("--- 复杂任务测试开始 ---\n");

        // 测试 1: 多文件创建和配置
        testMultiFileOperations(toolExecutor);

        // 测试 2: 跨文件搜索
        testCrossFileSearch(toolExecutor);

        // 测试 3: YAML 精确操作
        testYamlOperations(toolExecutor);

        // 测试 4: JSON 精确操作
        testJsonOperations(toolExecutor);

        // 测试 5: 知识库完整流程
        testKnowledgeWorkflow(toolExecutor);

        // 测试 6: Worker 并行执行
        testParallelWorkers(llmClient, toolExecutor, toolRegistry);

        // 测试 7: 复杂对话（多轮工具调用）
        testComplexDialog(llmClient, toolRegistry, toolExecutor, chatHistory);

        // 测试 8: 系统命令执行
        testSystemCommands(toolExecutor);

        // 输出结果
        System.out.println("\n========================================");
        System.out.println("       复杂任务测试结果");
        System.out.println("========================================");
        System.out.println("通过: " + passed);
        System.out.println("失败: " + failed);
        if (!errors.isEmpty()) {
            System.out.println("\n失败详情:");
            errors.forEach(e -> System.out.println("  - " + e));
        }
        System.out.println("========================================");
    }

    // ==================== 测试场景 ====================

    /**
     * 测试 1: 多文件创建和配置
     * 模拟为 EssentialsX 插件创建完整配置
     */
    private static void testMultiFileOperations(ToolExecutor executor) {
        System.out.println("[测试 1] 多文件创建和配置");

        // 创建插件目录
        assertExec(executor, "create_directory",
                Map.of("path", "plugins/EssentialsX", "serverId", "default"),
                result -> result.contains("success"),
                "创建 EssentialsX 目录");

        // 创建 config.yml（使用 serverId 参数保持路径一致性）
        assertExec(executor, "write_file",
                Map.of("path", "plugins/EssentialsX/config.yml",
                        "serverId", "default",
                        "content", "# EssentialsX 配置\n" +
                                "nickname-prefix: '~'\n" +
                                "max-nick-length: 15\n" +
                                "change-displayname: true\n" +
                                "spawn-if-no-home: true\n" +
                                "teleport-cooldown: 0\n" +
                                "teleport-delay: 0\n" +
                                "heal-cooldown: 60\n"),
                result -> result.contains("写入成功") || result.contains("success"),
                "创建 config.yml");

        // 创建 kits.yml
        assertExec(executor, "write_file",
                Map.of("path", "plugins/EssentialsX/kits.yml",
                        "serverId", "default",
                        "content", "# EssentialsX 套餐配置\n" +
                                "kits:\n" +
                                "  starter:\n" +
                                "    delay: 3600\n" +
                                "    items:\n" +
                                "      - 'diamond_sword 1'\n" +
                                "      - 'bread 16'\n"),
                result -> result.contains("写入成功") || result.contains("success"),
                "创建 kits.yml");

        // 创建 knowledge.md
        assertExec(executor, "save_knowledge",
                Map.of("category", "PLUGIN",
                        "pluginName", "EssentialsX",
                        "title", "基础配置知识",
                        "content", "## 配置规则\n" +
                                "- nickname-prefix: 前缀符号，只能是 ~ 或 &\n" +
                                "- max-nick-length: 昵称长度限制，建议不超过 20\n" +
                                "- teleport-cooldown: 传送冷却（秒），0=无冷却\n" +
                                "- heal-cooldown: 治愈冷却（秒），建议 ≥ 30\n\n" +
                                "## 用户偏好\n" +
                                "- 用户确认：禁用传送冷却\n"),
                result -> result.contains("success"),
                "创建 knowledge.md");

        System.out.println();
    }

    /**
     * 测试 2: 跨文件搜索
     * 模拟搜索多个配置文件中的特定设置
     */
    private static void testCrossFileSearch(ToolExecutor executor) {
        System.out.println("[测试 2] 跨文件搜索");

        // 搜索 config.yml 中的 cooldown
        assertExec(executor, "search_file_content",
                Map.of("path", "plugins/EssentialsX/config.yml", "serverId", "default",
                        "pattern", "cooldown"),
                result -> result.contains("matches"),
                "搜索 cooldown 配置");

        // 搜索 knowledge.md 中的用户偏好
        assertExec(executor, "search_knowledge",
                Map.of("query", "用户确认"),
                result -> result.contains("results"),
                "搜索用户偏好");

        // 列出插件目录
        assertExec(executor, "list_directory",
                Map.of("path", "plugins/EssentialsX", "serverId", "default"),
                result -> result.contains("config.yml") || result.contains("kits.yml"),
                "列出插件目录");

        System.out.println();
    }

    /**
     * 测试 3: YAML 精确操作
     * 模拟精确修改 YAML 配置项
     */
    private static void testYamlOperations(ToolExecutor executor) {
        System.out.println("[测试 3] YAML 精确操作");

        // 读取 YAML key
        assertExec(executor, "read_yaml_key",
                Map.of("path", "plugins/EssentialsX/config.yml", "serverId", "default",
                        "keyPath", "max-nick-length"),
                result -> result.contains("value") || result.contains("15"),
                "读取 max-nick-length");

        // 修改 YAML key
        assertExec(executor, "edit_yaml_key",
                Map.of("path", "plugins/EssentialsX/config.yml", "serverId", "default",
                        "keyPath", "max-nick-length",
                        "newValue", "20"),
                result -> result.contains("success") || result.contains("newValue"),
                "修改 max-nick-length 为 20");

        // 验证修改
        assertExec(executor, "read_yaml_key",
                Map.of("path", "plugins/EssentialsX/config.yml", "serverId", "default",
                        "keyPath", "max-nick-length"),
                result -> result.contains("20"),
                "验证 max-nick-length 已改为 20");

        // 列出所有配置 key
        assertExec(executor, "list_config_keys",
                Map.of("path", "plugins/EssentialsX/config.yml", "serverId", "default"),
                result -> result.contains("keys") && result.contains("nickname-prefix"),
                "列出配置 key");

        System.out.println();
    }

    /**
     * 测试 4: JSON 精确操作
     */
    private static void testJsonOperations(ToolExecutor executor) {
        System.out.println("[测试 4] JSON 精确操作");

        // 创建 JSON 文件
        assertExec(executor, "write_file",
                Map.of("path", "plugins/TestPlugin/config.json", "serverId", "default",
                        "content", "{\n" +
                                "  \"database\": {\n" +
                                "    \"host\": \"localhost\",\n" +
                                "    \"port\": 3306,\n" +
                                "    \"name\": \"mcdb\"\n" +
                                "  },\n" +
                                "  \"cache\": {\n" +
                                "    \"enabled\": true,\n" +
                                "    \"ttl\": 300\n" +
                                "  }\n" +
                                "}"),
                result -> result.contains("写入成功") || result.contains("success"),
                "创建 JSON 配置");

        // 读取 JSON path
        assertExec(executor, "read_json_path",
                Map.of("path", "plugins/TestPlugin/config.json", "serverId", "default",
                        "jsonPath", "database.host"),
                result -> result.contains("localhost"),
                "读取 database.host");

        // 修改 JSON path
        assertExec(executor, "edit_json_path",
                Map.of("path", "plugins/TestPlugin/config.json", "serverId", "default",
                        "jsonPath", "database.port",
                        "newValue", "5432"),
                result -> result.contains("success"),
                "修改 database.port 为 5432");

        // 验证修改
        assertExec(executor, "read_json_path",
                Map.of("path", "plugins/TestPlugin/config.json", "serverId", "default",
                        "jsonPath", "database.port"),
                result -> result.contains("5432"),
                "验证 port 已改为 5432");

        System.out.println();
    }

    /**
     * 测试 5: 知识库完整流程
     * 模拟知识就绪检查 → 写入 → 搜索的完整流程
     */
    private static void testKnowledgeWorkflow(ToolExecutor executor) {
        System.out.println("[测试 5] 知识库完整流程");

        // 检查插件的知识就绪状态
        assertExec(executor, "check_knowledge_readiness",
                Map.of("pluginNames", List.of("EssentialsX", "Vault")),
                result -> result.contains("allReady") && result.contains("notReady"),
                "检查多插件知识就绪");

        // 为未就绪的插件创建 knowledge.md
        assertExec(executor, "save_knowledge",
                Map.of("category", "PLUGIN",
                        "pluginName", "Vault",
                        "title", "Vault 基础知识",
                        "content", "## 插件用途\nVault 是权限和经济 API 的桥接层。\n\n" +
                                "## 依赖关系\n- 需要至少一个权限插件\n- 需要至少一个经济插件（如 EssentialsX）\n"),
                result -> result.contains("success"),
                "创建 Vault knowledge.md");

        // 再次检查就绪状态
        assertExec(executor, "check_knowledge_readiness",
                Map.of("pluginNames", List.of("EssentialsX", "Vault")),
                result -> result.contains("allReady") || result.contains("ready"),
                "再次检查知识就绪");

        // 搜索知识库
        assertExec(executor, "search_knowledge",
                Map.of("query", "配置"),
                result -> result.contains("results"),
                "搜索配置相关知识");

        // 保存全局知识
        assertExec(executor, "save_knowledge",
                Map.of("category", "GLOBAL",
                        "title", "插件依赖关系",
                        "content", "## 已知依赖\n" +
                                "- Vault → LuckPerms (CONFIG_DEPENDS)\n" +
                                "- Vault → EssentialsX (CONFIG_DEPENDS)\n" +
                                "- EssentialsX → Vault (LOAD_DEPENDS)\n"),
                result -> result.contains("success"),
                "保存全局依赖知识");

        System.out.println();
    }

    /**
     * 测试 6: Worker 并行执行
     * 模拟多个 Worker 同时处理不同插件
     */
    private static void testParallelWorkers(LlmClient llmClient, ToolExecutor executor, ToolRegistry registry) {
        System.out.println("[测试 6] Worker 并行执行");

        try {
            // 注册 WorkerToolProvider
            ToolRegistry fullRegistry = new ToolRegistry();
            fullRegistry.registerLocal(new ConsoleToolProvider());
            fullRegistry.registerLocal(new FileToolProvider(new FileService("servers")));
            fullRegistry.registerLocal(new WorkerToolProvider(llmClient, executor, fullRegistry));

            ToolExecutor fullExecutor = new ToolExecutor(fullRegistry);

            // 派发 2 个 Worker 并行处理（EssentialsX + Vault）
            Map<String, Object> args = Map.of("workers", List.of(
                    Map.of(
                            "targetPlugin", "EssentialsX",
                            "taskGoal", "读取 config.yml 并返回 max-nick-length 的当前值",
                            "fileScope", List.of("plugins/EssentialsX/config.yml"),
                            "extraContext", "只需读取，不需要修改"
                    ),
                    Map.of(
                            "targetPlugin", "Vault",
                            "taskGoal", "读取 knowledge.md 并返回其内容摘要",
                            "fileScope", List.of("plugins/Vault/knowledge.md"),
                            "extraContext", "测试 Worker 读取能力"
                    )
            ));

            String result = fullExecutor.execute("spawn_workers", args);
            assertTest("spawn_workers 多任务执行", result != null && !result.contains("error"),
                    "结果: " + (result != null ? result.substring(0, Math.min(100, result.length())) : "null"));

            // 检查是否有成功结果
            assertTest("Worker 返回有效结果", result.contains("totalWorkers"),
                    "检查 JSON 格式");

        } catch (Exception e) {
            assertTest("Worker 并行执行", false, e.getMessage());
        }

        System.out.println();
    }

    /**
     * 测试 7: 复杂对话（多轮工具调用）
     * 模拟用户请求配置修改，Agent 自主决定调用多个工具
     */
    private static void testComplexDialog(LlmClient llmClient, ToolRegistry registry,
                                           ToolExecutor executor, ChatHistory history) {
        System.out.println("[测试 7] 复杂对话（多轮工具调用）");

        try {
            AgentCore agentCore = new AgentCore(llmClient, executor, registry, history);
            String sessionId = "test-complex-" + System.currentTimeMillis();

            // 请求：修改 EssentialsX 的 max-nick-length 为 25
            String reply = agentCore.chat(sessionId,
                    "请将 EssentialsX 插件的 max-nick-length 配置修改为 25，修改前先读取当前值确认");

            assertTest("复杂对话有回复", reply != null && !reply.isEmpty(),
                    "回复: " + (reply != null ? reply.substring(0, Math.min(100, reply.length())) : "null"));

            // 验证修改是否生效
            // Agent 按安全规则先展示方案，包含 max-nick-length 和 25
            assertTest("Agent 提出修改方案", reply.contains("max-nick-length") && reply.contains("25"),
                    "Agent 正确识别配置项并提出修改方案");

            history.destroy(sessionId);
        } catch (Exception e) {
            assertTest("复杂对话", false, e.getMessage());
        }

        System.out.println();
    }

    /**
     * 测试 8: 系统命令执行
     */
    private static void testSystemCommands(ToolExecutor executor) {
        System.out.println("[测试 8] 系统命令执行");

        // 执行简单命令
        assertExec(executor, "execute_command",
                Map.of("command", "echo 'Hello from Agent'"),
                result -> result.contains("Hello from Agent"),
                "执行 echo 命令");

        // 列出进程
        assertExec(executor, "list_processes",
                Map.of("filter", "java"),
                result -> result.contains("processes"),
                "列出 Java 进程");

        // 获取环境变量
        assertExec(executor, "get_environment",
                Map.of("key", "PATH"),
                result -> result.contains("value") || result.contains("PATH"),
                "获取 PATH 环境变量");

        // 获取磁盘使用
        assertExec(executor, "get_disk_usage",
                Map.of(),
                result -> result.contains("total") || result.contains("disk"),
                "获取磁盘使用");

        // 获取系统信息
        assertExec(executor, "get_system_info",
                Map.of(),
                result -> result.contains("os") || result.contains("cpu"),
                "获取系统信息");

        System.out.println();
    }

    // ==================== 辅助方法 ====================

    private static void setupMockEnvironment(ToolExecutor executor) {
        System.out.println("[环境] 创建模拟服务器目录...");

        // 创建目录结构
        executor.execute("create_directory", Map.of("path", "plugins", "serverId", "default"));
        executor.execute("create_directory", Map.of("path", "knowledge", "serverId", "default"));
        executor.execute("create_directory", Map.of("path", "config", "serverId", "default"));

        // 创建 server.properties
        executor.execute("write_file", Map.of(
                "path", "server.properties", "serverId", "default",
                "content", "# Minecraft Server Properties\n" +
                        "motd=A Minecraft Server\n" +
                        "max-players=20\n" +
                        "difficulty=normal\n" +
                        "gamemode=survival\n" +
                        "pvp=true\n" +
                        "spawn-protection=16\n"));

        System.out.println("[环境] 模拟环境创建完成\n");
    }

    private static void assertExec(ToolExecutor executor, String toolName, Map<String, Object> args,
                                    java.util.function.Predicate<String> check, String testName) {
        try {
            String result = executor.execute(toolName, args);
            assertTest(testName, check.test(result),
                    result.substring(0, Math.min(80, result.length())));
        } catch (Exception e) {
            assertTest(testName, false, e.getMessage());
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
