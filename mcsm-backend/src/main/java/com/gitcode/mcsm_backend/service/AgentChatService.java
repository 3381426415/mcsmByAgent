package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.agent.ai.*;
import com.gitcode.mcsm_backend.agent.communication.AgentEventStream;
import com.gitcode.mcsm_backend.agent.communication.InteractionQueue;
import com.gitcode.mcsm_backend.agent.communication.SessionEventBuffer;
import com.gitcode.mcsm_backend.agent.core.ThinkMoreToolProvider;
import com.gitcode.mcsm_backend.agent.core.ListToolsToolProvider;
import com.gitcode.mcsm_backend.agent.memory.AgentMemoryService;
import com.gitcode.mcsm_backend.agent.memory.KnowledgeInjector;
import com.gitcode.mcsm_backend.agent.memory.LocalKnowledgeStore;
import com.gitcode.mcsm_backend.agent.orchestration.DecisionAgent;
import com.gitcode.mcsm_backend.config.AgentConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.gitcode.mcsm_backend.config.ProviderConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Agent 聊天服务 — 使用 DecisionAgent 处理用户消息
 * 通过 AgentEventStream 直接推送流式事件到前端
 */
@Slf4j
@Service
public class AgentChatService {

    @Value("${ai.llm.base-url:}")
    private String llmBaseUrl;

    @Value("${ai.llm.api-key:}")
    private String llmApiKey;

    @Value("${ai.llm.pro-model:}")
    private String llmProModel;

    @Value("${ai.llm.flash-model:}")
    private String llmFlashModel;

    @Value("${ai.llm.max-context-tokens:150000}")
    private int maxContextTokens;

    @Value("${ai.llm.provider:openai}")
    private String llmProvider;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private ServerService serverService;

    @Autowired
    private LocalPluginService localPluginService;

    @Autowired
    private FileService fileService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentMemoryService memoryService;

    @Autowired
    private FrpService frpService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ProviderConfig providerConfig;

    private DecisionAgent decisionAgent;
    private AgentEventStream eventStream;
    private InteractionQueue interactionQueue;
    private SessionEventBuffer sessionEventBuffer;

    @PostConstruct
    public void init() {
        if (llmApiKey == null || llmApiKey.isEmpty()) {
            log.info("[AgentChatService] LLM 未配置，Agent 聊天功能暂不可用");
            return;
        }

        try {
            // 1. 多模型路由器
            String proModel = (llmProModel != null && !llmProModel.isEmpty()) ? llmProModel : "gpt-4o";
            String flashModel = (llmFlashModel != null && !llmFlashModel.isEmpty()) ? llmFlashModel : proModel;
            String baseUrl = (llmBaseUrl != null && !llmBaseUrl.isEmpty()) ? llmBaseUrl : "https://api.openai.com/v1";
            String thinkingField = providerConfig.getThinkingField(llmProvider);
            ProviderFields providerFields = new ProviderFields(
                    thinkingField,
                    providerConfig.getContentField(llmProvider),
                    providerConfig.getToolCallsField(llmProvider),
                    providerConfig.getDeltaContentField(llmProvider),
                    providerConfig.getDeltaToolCallsField(llmProvider)
            );
            MultiModelRouter router = new MultiModelRouter(baseUrl, llmApiKey, proModel, flashModel, llmProvider, providerFields);

            // 2. LLM 客户端
            LlmClient llmClient = new LlmClient(router);

            // 3. 工具注册
            ToolRegistry toolRegistry = new ToolRegistry();
            toolRegistry.registerLocal(new ConsoleToolProvider());
            toolRegistry.registerLocal(new ServerToolProvider(serverManager, serverService, localPluginService));
            toolRegistry.registerLocal(new FileToolProvider(fileService));

            // 4. 工具执行器
            ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);

            // 5. Worker 工具（依赖 toolExecutor）
            toolRegistry.registerLocal(new WorkerToolProvider(llmClient, toolExecutor, toolRegistry));

            // 5.5 think_more 工具（让 LLM 自主决定是否继续 ReAct 循环）
            toolRegistry.registerLocal(new ThinkMoreToolProvider());

            // 5.6 FRP 管理工具
            toolRegistry.registerLocal(new FrpToolProvider(frpService));

            // 5.8 Groovy 兜底工具
            toolRegistry.registerLocal(new GroovyToolProvider(applicationContext));

            // 5.7 工具列表发现（依赖 registry，必须最后注册）
            toolRegistry.registerLocal(new ListToolsToolProvider(toolRegistry));

            // 6. 事件流和交互队列
            sessionEventBuffer = new SessionEventBuffer();
            eventStream = new AgentEventStream(messagingTemplate, sessionEventBuffer);
            interactionQueue = new InteractionQueue();

            // 7. 知识注入器
            LocalKnowledgeStore localKnowledgeStore = new LocalKnowledgeStore(".");
            KnowledgeInjector knowledgeInjector = new KnowledgeInjector(memoryService, localKnowledgeStore);

            // 8. 创建 DecisionAgent（流式输出）
            decisionAgent = new DecisionAgent(
                    llmClient, toolRegistry, toolExecutor,
                    eventStream, interactionQueue, 100);
            decisionAgent.setKnowledgeInjector(knowledgeInjector);

            // 9. 上下文窗口管理
            TokenEstimator tokenEstimator = new TokenEstimator();
            ContextManager contextManager = new ContextManager(maxContextTokens, tokenEstimator, llmClient);
            decisionAgent.setContextManager(contextManager);

            log.info("[AgentChatService] Agent 聊天服务初始化完成（DecisionAgent + 流式输出）");
        } catch (Exception e) {
            log.error("[AgentChatService] 初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 热更新 LLM 配置 — 保存 API Key / 模型等配置后自动重新加载，无需重启
     */
    public synchronized void reload(String newProvider, String newBaseUrl, String newApiKey,
                                     String newProModel, String newFlashModel) {
        this.llmProvider = newProvider;
        this.llmBaseUrl = newBaseUrl;
        this.llmApiKey = newApiKey;
        this.llmProModel = newProModel;
        this.llmFlashModel = newFlashModel;

        this.decisionAgent = null;
        this.eventStream = null;
        this.interactionQueue = null;
        this.sessionEventBuffer = null;

        log.info("[AgentChatService] 热更新 LLM 配置: provider={}, baseUrl={}", newProvider, newBaseUrl);
        init();
    }

    public boolean isAvailable() {
        return decisionAgent != null;
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000)
    public void cleanupSessions() {
        if (decisionAgent != null) {
            decisionAgent.cleanupStaleSessions();
        }
        if (sessionEventBuffer != null) {
            sessionEventBuffer.cleanup();
        }
    }

    public SessionEventBuffer getEventBuffer() {
        return sessionEventBuffer;
    }

    /**
     * 异步处理用户聊天消息
     */
    @Async
    public void sendAsync(Map<String, Object> body, String userId) {
        if (!isAvailable()) {
            pushToUser(userId, "ERROR", "Agent 服务未初始化，请检查 LLM 配置。");
            return;
        }

        String message = (String) body.get("message");
        String sessionId = (String) body.getOrDefault("session_id", "default");

        if (message == null || message.trim().isEmpty()) {
            pushToUser(userId, "ERROR", "消息不能为空");
            return;
        }

        try {
            // 发送 AGENT_START 事件，让前端创建 scope 卡片
            eventStream.emitAgentStart(userId, "decision", "DECISION", message, null);

            // 记录用户操作
            Long uid = parseUserId(userId);
            memoryService.recordOperation(sessionId, "agent-chat", "CHAT",
                    uid, "用户消息", message, true);

            // DecisionAgent 内部通过 eventStream 推送 THINKING/TOOL_CALL/REPLY_CHUNK/REPLY_DONE
            decisionAgent.handleUserMessage(userId, sessionId, message);

        } catch (Exception e) {
            log.error("[AgentChatService] 处理失败: {}", e.getMessage(), e);
            memoryService.recordOperation(sessionId, "agent-chat", "CHAT",
                    parseUserId(userId), "处理失败", e.getMessage(), false);
            pushToUser(userId, "ERROR", "Agent 处理失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理确认操作
     * DecisionAgent 不内置危险操作检测，安全规则由 LLM 通过系统提示词执行
     * 用户确认/取消作为普通消息发送给 Agent
     */
    @Async
    public void confirmAsync(Map<String, Object> body, String userId) {
        if (!isAvailable()) {
            pushToUser(userId, "ERROR", "Agent 服务未初始化");
            return;
        }

        String action = (String) body.get("action");
        boolean confirmed = "confirm".equals(action);
        String sessionId = (String) body.getOrDefault("session_id", "default");
        String confirmMessage = confirmed ? "我确认，请继续执行。" : "我取消了这个操作。";

        try {
            eventStream.emitAgentStart(userId, "decision", "DECISION", confirmMessage, null);
            decisionAgent.handleUserMessage(userId, sessionId, confirmMessage);
        } catch (Exception e) {
            log.error("[AgentChatService] 确认处理失败: {}", e.getMessage(), e);
            pushToUser(userId, "ERROR", "处理失败: " + e.getMessage());
        }
    }

    /**
     * 销毁会话
     */
    public void destroyAsync(Map<String, Object> body, String userId) {
        pushToUser(userId, "STATUS", "会话已销毁");
    }

    private void pushToUser(String userId, String type, Object data) {
        if (eventStream != null) {
            eventStream.emit(userId, type, data);
        } else {
            Map<String, Object> message = Map.of("type", type, "data", data);
            messagingTemplate.convertAndSend("/user/" + userId + "/queue/agent", message);
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
