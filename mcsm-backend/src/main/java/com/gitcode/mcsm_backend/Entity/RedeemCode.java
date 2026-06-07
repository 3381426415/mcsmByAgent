package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 兑换码实体 - 存储礼品兑换码及其使用状态、关联商品
 */
@Data
@TableName("mcsm_redeem_code")
public class RedeemCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Integer amount;
    private Integer status;          // 0-未使用，1-已使用
    private Long usedBy;
    private LocalDateTime usedTime;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}