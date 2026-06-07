package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gitcode.mcsm_backend.Entity.MarketGoods;
import com.gitcode.mcsm_backend.Entity.OrderRecord;
import com.gitcode.mcsm_backend.common.Result;
import java.util.List;
import com.gitcode.mcsm_backend.Entity.VO.MarketGoodsVO;
/**
 * 市场服务接口 - 定义商品上架、购买、查询、下架等交易业务方法
 */
public interface MarketService extends IService<MarketGoods> {
    // 上架：包含远程扣除逻辑
    Result<String> uploadGoods(String uuid, int slot, int amount,int price, String displayName, String itemKey, String nbtData);

    // 下架/撤回：包含返还道具逻辑
    Result<String> withdrawGoods(String goodsId, String uuid);

    // 购买：核心交易逻辑（扣钱、改状态、生成订单、发货）
    Result<String> purchaseGoods(String goodsId, String buyerId);

    // 搜索：模糊查询
    IPage<MarketGoodsVO> searchGoods(String keyword, int page, int size);

    // 查询当前登录用户正在上架售卖的商品
    IPage<MarketGoodsVO> getMyOnSaleGoods(String uuid, int page, int size);

    // 查询当前登录用户已经买到的商品记录（订单快照）
    List<OrderRecord> getMyBoughtHistory(String uuid);

    // 查询与当前用户相关的所有交易流水（作为卖家或买家的所有订单）
    List<OrderRecord> getMyAllOrderHistory(String uuid);
    /**
     * 管理员强制下架商品（不校验所有权）
     */
    Result<String> adminWithdrawGoods(String goodsId);
}