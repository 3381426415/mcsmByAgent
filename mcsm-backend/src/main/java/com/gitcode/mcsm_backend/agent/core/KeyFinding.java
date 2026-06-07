package com.gitcode.mcsm_backend.agent.core;


/**
 * 关键发现 - 任何 Agent 可写入的发现记录
 */
public class KeyFinding {

    private String sourceAgentId;
    private String type;      // CONFLICT, DEPENDENCY, WARNING, INFO
    private String description;
    private long timestamp;
}
