package com.gitcode.mcsmServerBridge.Entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PendingItem {
    private Long id;
    private String playerUuid;
    private String itemKey;
    private int amount;
    private String displayName;
    private String nbtData;
    private String source;
    private String sourceId;
    private LocalDateTime createTime;
    private boolean claimed;
    private LocalDateTime claimTime;
}