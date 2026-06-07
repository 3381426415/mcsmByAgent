package com.gitcode.mcsm_backend.agent.core;

/**
 * 任务状态
 */
public enum TaskStatus {

    /** 规划中 */
    PLANNING,

    /** 执行中 */
    EXECUTING,

    /** 校验中 */
    VERIFYING,

    /** 等待用户确认 */
    AWAITING_CONFIRM,

    /** 已完成 */
    DONE,

    /** 失败 */
    FAILED,

    /** 已取消 */
    CANCELLED
}
