package com.gitcode.mcsm_backend.agent.core;

import java.util.List;
import java.util.Map;

public class SubTaskState {
    private String id;
    private String description;
    private SubTaskStatus status;
    private AgentType assignedAgentType;
    private String assignedAgentId;
    private List<String> targetFiles;
    private List<String> dependsOn;
    private String complexity;
    private String result;
    private String error;
    private Map<String, Object> metadata;

    public SubTaskState() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SubTaskState state = new SubTaskState();
        public Builder id(String id) { state.id = id; return this; }
        public Builder description(String d) { state.description = d; return this; }
        public Builder status(SubTaskStatus s) { state.status = s; return this; }
        public Builder assignedAgentType(AgentType t) { state.assignedAgentType = t; return this; }
        public Builder assignedAgentId(String id) { state.assignedAgentId = id; return this; }
        public Builder targetFiles(List<String> f) { state.targetFiles = f; return this; }
        public Builder dependsOn(List<String> d) { state.dependsOn = d; return this; }
        public Builder complexity(String c) { state.complexity = c; return this; }
        public Builder result(String r) { state.result = r; return this; }
        public Builder error(String e) { state.error = e; return this; }
        public Builder metadata(Map<String, Object> m) { state.metadata = m; return this; }
        public SubTaskState build() { return state; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SubTaskStatus getStatus() { return status; }
    public void setStatus(SubTaskStatus status) { this.status = status; }
    public AgentType getAssignedAgentType() { return assignedAgentType; }
    public void setAssignedAgentType(AgentType assignedAgentType) { this.assignedAgentType = assignedAgentType; }
    public String getAssignedAgentId() { return assignedAgentId; }
    public void setAssignedAgentId(String assignedAgentId) { this.assignedAgentId = assignedAgentId; }
    public List<String> getTargetFiles() { return targetFiles; }
    public void setTargetFiles(List<String> targetFiles) { this.targetFiles = targetFiles; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
