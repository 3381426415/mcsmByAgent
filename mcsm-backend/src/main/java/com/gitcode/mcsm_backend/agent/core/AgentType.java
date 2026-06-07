package com.gitcode.mcsm_backend.agent.core;

/**
 * Agent 类型枚举
 */
public enum AgentType {

    /** 决策智能体 - 唯一和用户对话的角色，Pro 模型 */
    DECISION,

    /** 任务规划 Agent - 全局规划，输出变更方案 */
    PLANNER,

    /** 角色 Agent - 配置分析 + 一致性检查，动态创建 */
    ROLE,

    /** 执行 Agent - 机械写入文件 */
    EXECUTOR,

    /** 校验 Agent - 格式校验 + 准确性 + 一致性 */
    CHECKER,

    /** 总结 Agent - 检查点验证 + 结果压缩 */
    SUMMARIZER,

    /** 知识 Agent - 知识提取和建库，异步触发 */
    KNOWLEDGE
}
