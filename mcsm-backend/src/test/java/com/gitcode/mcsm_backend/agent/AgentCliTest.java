package com.gitcode.mcsm_backend.agent;

import com.gitcode.mcsm_backend.agent.ai.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Scanner;

/**
 * Agent 命令行交互测试 — 不依赖 Spring 容器，直接初始化 AgentCore 与智能体对话。
 *
 * 用法：运行 main 方法，在命令行输入消息与智能体对话，输入 quit 退出。
 */
public class AgentCliTest {

    public static void main(String[] args) throws Exception {
        // 1. 读取 LLM 配置
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
            System.err.println("错误: 未配置 ai.llm.api-key，请在 application.properties 中设置");
            return;
        }

        System.out.println("========== Agent CLI 测试 ==========");
        System.out.println("LLM: " + baseUrl);
        System.out.println("Pro model: " + proModel);
        System.out.println("Flash model: " + flashModel);
        System.out.println("====================================\n");

        // 2. 初始化组件（使用 MultiModelRouter 路由 pro/flash 模型）
        MultiModelRouter router = new MultiModelRouter(baseUrl, apiKey, proModel, flashModel);
        LlmClient llmClient = new LlmClient(router);

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerLocal(new ConsoleToolProvider());

        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        ChatHistory chatHistory = new ChatHistory();
        AgentCore agentCore = new AgentCore(llmClient, toolExecutor, toolRegistry, chatHistory);

        System.out.println("Agent 初始化完成，可以开始对话了。");
        System.out.println("输入 quit 退出，输入 clear 清空会话。\n");

        // 3. 交互循环
        Scanner scanner = new Scanner(System.in);
        String sessionId = "cli-test";

        while (true) {
            System.out.print("你: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;
            if ("quit".equalsIgnoreCase(input) || "exit".equalsIgnoreCase(input)) {
                System.out.println("再见！");
                break;
            }
            if ("clear".equalsIgnoreCase(input)) {
                chatHistory.destroy(sessionId);
                System.out.println("[会话已清空]\n");
                continue;
            }

            try {
                String reply = agentCore.chat(sessionId, input);
                System.out.println("\nAgent: " + reply + "\n");
            } catch (Exception e) {
                System.err.println("\n[错误] " + e.getMessage() + "\n");
            }
        }

        scanner.close();
    }

    private static String resolve(Properties props, String key, String defaultValue) {
        String val = props.getProperty(key, "");
        // 处理 ${...} 引用
        if (val.startsWith("${") && val.endsWith("}")) {
            String ref = val.substring(2, val.length() - 1);
            val = props.getProperty(ref, defaultValue);
        }
        return val.isEmpty() ? defaultValue : val;
    }
}
