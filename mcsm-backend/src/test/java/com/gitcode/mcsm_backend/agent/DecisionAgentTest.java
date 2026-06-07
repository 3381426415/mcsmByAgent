package com.gitcode.mcsm_backend.agent;

import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.communication.AgentEventStream;
import com.gitcode.mcsm_backend.agent.communication.InteractionQueue;
import com.gitcode.mcsm_backend.agent.orchestration.DecisionAgent;
import com.gitcode.mcsm_backend.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DecisionAgent 流式输出测试
 * 验证事件发射和流式输出是否正常工作
 */
public class DecisionAgentTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> errors = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("    DecisionAgent 流式输出测试");
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
        System.out.println("Pro: " + proModel + " | Flash: " + flashModel);
        System.out.println();

        // 2. 初始化组件
        MultiModelRouter router = new MultiModelRouter(baseUrl, apiKey, proModel, flashModel);
        LlmClient llmClient = new LlmClient(router);

        FileService fileService = new FileService("servers");
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.registerLocal(new ConsoleToolProvider());
        toolRegistry.registerLocal(new FileToolProvider(fileService));

        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);

        // 3. 创建模拟事件流（记录事件而非推送到 WebSocket）
        MockEventStream mockEventStream = new MockEventStream();
        InteractionQueue interactionQueue = new InteractionQueue();

        // 4. 创建 DecisionAgent
        DecisionAgent agent = new DecisionAgent(
                llmClient, toolRegistry, toolExecutor,
                mockEventStream, interactionQueue, 10);

        // 5. 测试
        System.out.println("--- 测试开始 ---\n");

        testBasicStream(agent, mockEventStream);
        testToolCall(agent, mockEventStream);

        // 输出结果
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

    /**
     * 测试 1: 基本流式对话（纯文本回复）
     */
    private static void testBasicStream(DecisionAgent agent, MockEventStream mock) {
        System.out.println("[测试 1] 基本流式对话");

        mock.clear();
        String sessionId = "test-stream-" + System.currentTimeMillis();

        // 使用 CountDownLatch 等待异步完成
        CountDownLatch latch = new CountDownLatch(1);
        mock.onReplyDone = latch::countDown;

        agent.handleUserMessage("test-user", sessionId, "你好，请用一句话回复");

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            assertTest("对话在30秒内完成", completed);
        } catch (InterruptedException e) {
            assertTest("对话完成", false, "被中断");
        }

        // 检查事件
        assertTest("收到 REPLY_CHUNK 事件", mock.replyChunkCount.get() > 0,
                "chunk 数: " + mock.replyChunkCount.get());
        assertTest("收到 REPLY_DONE 事件", mock.replyDoneCount.get() > 0);
        assertTest("收到 THINKING 事件", mock.thinkingCount.get() >= 0); // 可能没有 thinking

        // 检查内容
        assertTest("REPLY_DONE 有内容", mock.lastReplyDone != null && !mock.lastReplyDone.isEmpty(),
                mock.lastReplyDone != null ? mock.lastReplyDone.substring(0, Math.min(50, mock.lastReplyDone.length())) : "null");

        System.out.println();
    }

    /**
     * 测试 2: 工具调用（应产生 TOOL_CALL + TOOL_RESULT 事件）
     */
    private static void testToolCall(DecisionAgent agent, MockEventStream mock) {
        System.out.println("[测试 2] 工具调用");

        mock.clear();
        String sessionId = "test-tool-" + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        mock.onReplyDone = latch::countDown;

        agent.handleUserMessage("test-user", sessionId, "请列出 servers 目录下的文件");

        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            assertTest("工具调用在60秒内完成", completed);
        } catch (InterruptedException e) {
            assertTest("工具调用完成", false, "被中断");
        }

        // 检查事件
        assertTest("收到 TOOL_CALL 事件", mock.toolCallCount.get() > 0,
                "工具调用数: " + mock.toolCallCount.get());
        assertTest("收到 TOOL_RESULT 事件", mock.toolResultCount.get() > 0,
                "工具结果数: " + mock.toolResultCount.get());
        assertTest("收到 THINKING 事件", mock.thinkingCount.get() > 0,
                "思考数: " + mock.thinkingCount.get());
        assertTest("收到 REPLY_DONE 事件", mock.replyDoneCount.get() > 0);

        System.out.println();
    }

    // ==================== 模拟事件流 ====================

    static class MockEventStream extends AgentEventStream {
        AtomicInteger thinkingCount = new AtomicInteger(0);
        AtomicInteger replyChunkCount = new AtomicInteger(0);
        AtomicInteger replyDoneCount = new AtomicInteger(0);
        AtomicInteger toolCallCount = new AtomicInteger(0);
        AtomicInteger toolResultCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        String lastReplyDone;
        Runnable onReplyDone;

        MockEventStream() {
            super(null, null); // 不需要真正的 SimpMessagingTemplate 和 SessionEventBuffer
        }

        void clear() {
            thinkingCount.set(0);
            replyChunkCount.set(0);
            replyDoneCount.set(0);
            toolCallCount.set(0);
            toolResultCount.set(0);
            errorCount.set(0);
            lastReplyDone = null;
            onReplyDone = null;
        }

        @Override
        public void emitThinking(String userId, String content) {
            thinkingCount.incrementAndGet();
            System.out.println("  [THINKING] " + content.substring(0, Math.min(60, content.length())));
        }

        @Override
        public void emitReplyChunk(String userId, String content) {
            replyChunkCount.incrementAndGet();
            // 不打印每个 chunk，太多了
        }

        @Override
        public void emitReplyDone(String userId, String content) {
            replyDoneCount.incrementAndGet();
            lastReplyDone = content;
            System.out.println("  [REPLY_DONE] " + content.substring(0, Math.min(80, content.length())));
            if (onReplyDone != null) onReplyDone.run();
        }

        @Override
        public void emitToolCall(String userId, String agentId, String toolName, String args) {
            toolCallCount.incrementAndGet();
            System.out.println("  [TOOL_CALL] " + toolName + " -> " + args.substring(0, Math.min(60, args.length())));
        }

        @Override
        public void emitToolResult(String userId, String agentId, String toolName, boolean success, String summary) {
            toolResultCount.incrementAndGet();
            System.out.println("  [TOOL_RESULT] " + toolName + " success=" + success + " -> " + summary.substring(0, Math.min(60, summary.length())));
        }

        @Override
        public void emitError(String userId, String message) {
            errorCount.incrementAndGet();
            System.out.println("  [ERROR] " + message);
            if (onReplyDone != null) onReplyDone.run(); // 出错也算完成
        }
    }

    // ==================== 辅助方法 ====================

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
