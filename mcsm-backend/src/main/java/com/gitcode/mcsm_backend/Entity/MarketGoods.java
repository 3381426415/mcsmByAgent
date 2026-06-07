package com.gitcode.mcsm_backend.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 市场商品实体 - 存储市场上架的商品信息（名称、价格、库存、卖家等）
 */
@Data
@TableName("mcsm_market_goods")
public class MarketGoods {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;               // 交易唯一ID(UUID)

    private String sellerId;         // 卖家玩家UUID
    private String itemKey;          // 物品材质(如 minecraft:diamond_sword)
    private String displayName;      // 物品名称(搜索核心)
    private String nbtData;          // 完整的NBT数据(JSON)
    private Integer price;           // 售价
    private Integer amount;          // 道具数量
    private Integer status;          // 状态: 0-待售, 1-交易中, 2-已成交, 3-已撤回

    private LocalDateTime creatTime; // 创建时间 (对应 DB: creat_time)
    private LocalDateTime updateTime;// 更新时间 (对应 DB: update_time)

    @TableLogic
    private Integer isDeleted;       // 逻辑删除标记
}