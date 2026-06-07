package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员操作日志实体 - 记录管理员的操作行为（操作人、操作类型、详情）
 */
@Data
@TableName("mcsm_admin_log")
public class AdminLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorIp;
    private String module;
    private String action;
    private String description;
    private String methodName;
    private String requestParams;
    private String responseResult;
    private Integer status;          // 1-成功，0-失败
    private String errorMsg;
    private Long executeTime;        // 耗时(ms)
    private LocalDateTime createTime;
}