package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 订单记录实体 - 存储市场商品的购买订单（买家、商品、数量、状态）
 */
@Data
@TableName("mcsm_order_record")
public class OrderRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;                  // 订单流水号

    private String goodsId;             // 关联的原商品ID
    private String sellerId;            // 卖家UUID
    private String buyerId;             // 买家UUID
    private String itemKey;             // 物品材质
    private String displayName;         // 交易时的物品名称
    private String nbtData;             // 交易时的完整NBT
    private Integer finalPrice;         // 成交价格
    private Integer fee;                // 交易手续费
    private Integer deliveryStatus;     // 发货状态: 0-待发货, 1-已发货, 2-发货失败

    private LocalDateTime completeTime; // 成交时间 (对应 DB: complete_time)
}