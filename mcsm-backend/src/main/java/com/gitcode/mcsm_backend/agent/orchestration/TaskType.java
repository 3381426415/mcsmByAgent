package com.gitcode.mcsm_backend.agent.orchestration;

/**
 * 任务类型
 */
public enum TaskType {

    /** 直接执行单个命令 - "启动服务器" */
    SINGLE_COMMAND,

    /** 固定诊断流程 - "服务器卡了" */
    DIAGNOSIS,

    /** 单插件配置 - "配置 Essentials" */
    PLUGIN_CONFIG,

    /** 批量插件配置 - "配置所有插件" */
    BULK_PLUGIN_CONFIG,

    /** 跨服务器编排 - "BungeeCord + 3子服" */
    GROUP_SERVER_CONFIG,

    /** 插件安装 - "装一个领地插件" */
    INSTALL_PLUGIN,

    /** 文件操作 - "改 motd" */
    FILE_OPERATION,

    /** 问答 - "这个插件怎么用" */
    QUESTION
}
