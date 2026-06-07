package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("agent_operation_log")
@Data
public class AgentOperationLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String agentId;
    private String agentType;
    private Long userId;
    private String operation;
    private String details;
    private boolean success;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
