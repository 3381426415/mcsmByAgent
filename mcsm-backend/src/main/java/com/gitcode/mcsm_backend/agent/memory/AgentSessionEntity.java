package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("agent_sessions")
@Data
public class AgentSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long userId;
    private String taskState;
    private String chatHistory;
    private String subTaskStates;
    private String pendingChanges;
    private String confirmedChanges;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
