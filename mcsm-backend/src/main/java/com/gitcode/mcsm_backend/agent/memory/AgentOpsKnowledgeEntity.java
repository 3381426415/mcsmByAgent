package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("agent_ops_knowledge")
@Data
public class AgentOpsKnowledgeEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scope;
    private String serverId;
    private String category;
    private String subject;
    private String relation;
    private String object;
    private String detail;
    private String confidence;
    private boolean verified;
    private boolean stale;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
