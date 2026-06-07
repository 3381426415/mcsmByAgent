package com.gitcode.mcsm_backend.Entity.VO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
/**
 * 市场商品视图对象 - 包含商品信息和卖家昵称，用于前端展示
 */
public class MarketGoodsVO {
    private String id;
    private String sellerId;
    private String sellerNickname;
    private String itemKey;
    private String displayName;
    private String nbtData;
    private Integer price;
    private Integer amount;
    private Integer status;
    private LocalDateTime creatTime;
    private LocalDateTime updateTime;
}