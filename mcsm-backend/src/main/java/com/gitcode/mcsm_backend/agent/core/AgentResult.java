package com.gitcode.mcsm_backend.agent.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 任务执行结果 - 记录单个 Agent 的执行状态、输出内容和工具调用记录
 */
public class AgentResult {
    private String agentId;
    private AgentType agentType;
    private boolean success;
    private String output;
    private String error;
    private List<ToolCallRecord> toolCalls;

    public AgentResult() {}

    public static AgentResult ok(String agentId, AgentType type, String output) {
        AgentResult r = new AgentResult();
        r.agentId = agentId;
        r.agentType = type;
        r.success = true;
        r.output = output;
        r.toolCalls = new ArrayList<>();
        return r;
    }

    public static AgentResult fail(String agentId, AgentType type, String error) {
        AgentResult r = new AgentResult();
        r.agentId = agentId;
        r.agentType = type;
        r.success = false;
        r.error = error;
        r.toolCalls = new ArrayList<>();
        return r;
    }

    public static AgentResult error(Exception e) {
        AgentResult r = new AgentResult();
        r.success = false;
        r.error = e.getMessage();
        r.toolCalls = new ArrayList<>();
        return r;
    }

    public String getAgentId() { return agentId; }
    public AgentType getAgentType() { return agentType; }
    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public String getError() { return error; }
    public List<ToolCallRecord> getToolCalls() { return toolCalls; }

    public static class ToolCallRecord {
        private String toolName;
        private String args;
        private boolean success;
        private String summary;

        public ToolCallRecord() {}
        public ToolCallRecord(String toolName, String args, boolean success, String summary) {
            this.toolName = toolName;
            this.args = args;
            this.success = success;
            this.summary = summary;
        }

        public String getToolName() { return toolName; }
        public String getArgs() { return args; }
        public boolean isSuccess() { return success; }
        public String getSummary() { return summary; }
    }
}
