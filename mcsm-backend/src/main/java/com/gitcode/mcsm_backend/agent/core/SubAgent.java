package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.communication.AgentEventStream;
import com.gitcode.mcsm_backend.agent.communication.AgentP2P;
import com.gitcode.mcsm_backend.agent.communication.SharedTaskState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 子 Agent 基类
 */
public abstract class SubAgent {

    protected final String agentId;
    protected final AgentType type;
    protected final ModelTier modelTier;
    protected String systemPrompt;
    protected SharedTaskState sharedState;
    protected AgentEventStream eventStream;
    protected AgentP2P p2p;
    protected String userId;
    protected String parentAgentId;
    protected final List<AgentResult.ToolCallRecord> toolCallRecords = new ArrayList<>();

    protected SubAgent(AgentType type, ModelTier modelTier, String systemPrompt) {
        this.agentId = type.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.modelTier = modelTier;
        this.systemPrompt = systemPrompt;
    }

    public void setParentAgentId(String parentAgentId) {
        this.parentAgentId = parentAgentId;
    }

    public void setSharedState(SharedTaskState sharedState) {
        this.sharedState = sharedState;
    }

    public void setEventStream(AgentEventStream eventStream) {
        this.eventStream = eventStream;
    }

    public void setP2p(AgentP2P p2p) {
        this.p2p = p2p;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AgentType getType() {
        return type;
    }

    public String getAgentId() {
        return agentId;
    }

    public void resetContext(String newPrompt) {
        this.systemPrompt = newPrompt;
        this.toolCallRecords.clear();
    }

    public int getContextSize() {
        return systemPrompt != null ? systemPrompt.length() : 0;
    }

    /**
     * 执行 Agent 任务
     */
    public abstract AgentResult execute();

    protected void emitAgentStart(String task) {
        if (eventStream != null && userId != null) {
            eventStream.emitAgentStart(userId, agentId, type.name(), task, parentAgentId);
        }
    }

    protected void emitToolCall(String toolName, String args) {
        if (eventStream != null && userId != null) {
            eventStream.emitToolCall(userId, agentId, toolName, args);
        }
    }

    protected void emitToolResult(String toolName, boolean success, String summary) {
        if (eventStream != null && userId != null) {
            eventStream.emitToolResult(userId, agentId, toolName, success, summary);
        }
    }

    protected void emitAgentDone(String result) {
        if (eventStream != null && userId != null) {
            eventStream.emitAgentDone(userId, agentId, result);
        }
    }

    protected void emitAgentError(String error) {
        if (eventStream != null && userId != null) {
            eventStream.emitAgentError(userId, agentId, error);
        }
    }
}
