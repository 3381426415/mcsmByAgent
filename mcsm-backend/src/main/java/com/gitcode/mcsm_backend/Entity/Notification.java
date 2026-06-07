package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体 - 存储系统通知消息，支持已读/未读状态
 */
@Data
@TableName("mcsm_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Integer isRead;          // 0-未读，1-已读
    private LocalDateTime createTime;
}