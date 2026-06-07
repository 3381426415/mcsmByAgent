package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("agent_error_patterns")
@Data
public class AgentErrorPatternEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scope;
    private String serverId;
    private String plugin;
    private String errorType;
    private String errorMessage;
    private String solution;
    private String summary;
    private String effectiveness;
    private int recurrenceCount;
    private boolean stale;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
