package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("agent_change_history")
@Data
public class AgentChangeHistoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private Long userId;
    private String serverId;
    private String filePath;
    private String changeKey;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
