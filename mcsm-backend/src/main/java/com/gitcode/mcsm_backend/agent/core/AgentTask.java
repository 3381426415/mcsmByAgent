package com.gitcode.mcsm_backend.agent.core;


import java.util.ArrayList;
import java.util.List;

/**
 * Agent 任务描述
 */
public class AgentTask {

    private String id;
    private AgentType type;
    private String description;
    private List<String> targetFiles = new ArrayList<>();
    private List<String> dependsOn = new ArrayList<>();
    private String complexity;
    private SubAgent agent;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public AgentType getType() { return type; }
    public void setType(AgentType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTargetFiles() { return targetFiles; }
    public void setTargetFiles(List<String> targetFiles) { this.targetFiles = targetFiles; }
    public List<String> getDependsOn() { return dependsOn; }
    public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public SubAgent getAgent() { return agent; }
    public void setAgent(SubAgent agent) { this.agent = agent; }
}
