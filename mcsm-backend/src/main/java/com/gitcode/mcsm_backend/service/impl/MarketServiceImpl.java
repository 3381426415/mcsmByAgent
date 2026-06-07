package com.gitcode.mcsm_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.MarketGoods;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.OrderRecord;
import com.gitcode.mcsm_backend.Entity.VO.MarketGoodsVO;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.event.NotificationEvent;
import com.gitcode.mcsm_backend.mapper.MarketGoodsMapper;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import com.gitcode.mcsm_backend.mapper.OrderRecordMapper;
import com.gitcode.mcsm_backend.service.MarketService;
import com.gitcode.mcsm_backend.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 市场服务实现 - 商品上架（含远程扣物品）、购买（含金币扣除）、订单管理
 */
@Slf4j
@Service
public class MarketServiceImpl extends ServiceImpl<MarketGoodsMapper, MarketGoods> implements MarketService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private OrderRecordMapper orderRecordMapper;

    @Autowired
    private MyUserMapper myUserMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    /**
     * 上架商品：先扣除游戏内物品，再存入数据库
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> uploadGoods(String uuid, int slot, int amount, int price,
                                      String displayName, String itemKey, String nbtData) {
        Result<String> remoteResult = playerService.updateItemBySlotRemote(uuid, slot, amount);

        if (remoteResult.getCode() != 2000) {
            return Result.error("游戏内物品扣除失败，无法上架: " + remoteResult.getMsg());
        }

        MarketGoods goods = new MarketGoods();
        goods.setSellerId(uuid);
        goods.setItemKey(itemKey);
        goods.setDisplayName(displayName);
        goods.setNbtData(nbtData);
        goods.setPrice(price);
        goods.setAmount(amount);
        goods.setStatus(0);

        this.save(goods);
        return Result.successMsg("商品上架成功");
    }

    /**
     * 分页搜索：模糊查询
     */
    @Override
    public IPage<MarketGoodsVO> searchGoods(String keyword, int page, int size) {
        Page<MarketGoods> pageParam = new Page<>(page, size);

        IPage<MarketGoods> goodsPage = this.lambdaQuery()
                .eq(MarketGoods::getStatus, 0)
                .like(keyword != null && !keyword.trim().isEmpty(),
                        MarketGoods::getDisplayName, keyword)
                .orderByDesc(MarketGoods::getCreatTime)
                .page(pageParam);

        IPage<MarketGoodsVO> voPage = new Page<>(page, size, goodsPage.getTotal());

        List<MarketGoodsVO> voList = goodsPage.getRecords().stream().map(goods -> {
            MarketGoodsVO vo = new MarketGoodsVO();
            vo.setId(goods.getId());
            vo.setSellerId(goods.getSellerId());
            vo.setItemKey(goods.getItemKey());
            vo.setDisplayName(goods.getDisplayName());
            vo.setNbtData(goods.getNbtData());
            vo.setPrice(goods.getPrice());
            vo.setAmount(goods.getAmount());
            vo.setStatus(goods.getStatus());
            vo.setCreatTime(goods.getCreatTime());
            vo.setUpdateTime(goods.getUpdateTime());

            MyUser seller = myUserMapper.selectOne(
                    new LambdaQueryWrapper<MyUser>()
                            .eq(MyUser::getBindId, goods.getSellerId())
                            .select(MyUser::getNickname)
            );
            vo.setSellerNickname(seller != null ? seller.getNickname() : "未知用户");

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 分页查询当前登录用户正在上架售卖的商品
     */
    @Override
    public IPage<MarketGoodsVO> getMyOnSaleGoods(String uuid, int page, int size) {
        Page<MarketGoods> pageParam = new Page<>(page, size);

        IPage<MarketGoods> goodsPage = this.lambdaQuery()
                .eq(MarketGoods::getSellerId, uuid)
                .eq(MarketGoods::getStatus, 0)
                .orderByDesc(MarketGoods::getCreatTime)
                .page(pageParam);

        IPage<MarketGoodsVO> voPage = new Page<>(page, size, goodsPage.getTotal());

        List<MarketGoodsVO> voList = goodsPage.getRecords().stream().map(goods -> {
            MarketGoodsVO vo = new MarketGoodsVO();
            vo.setId(goods.getId());
            vo.setSellerId(goods.getSellerId());
            vo.setItemKey(goods.getItemKey());
            vo.setDisplayName(goods.getDisplayName());
            vo.setNbtData(goods.getNbtData());
            vo.setPrice(goods.getPrice());
            vo.setAmount(goods.getAmount());
            vo.setStatus(goods.getStatus());
            vo.setCreatTime(goods.getCreatTime());
            vo.setUpdateTime(goods.getUpdateTime());

            vo.setSellerNickname("我");

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 下架/撤回：修改状态并调用插件暂存物品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> withdrawGoods(String goodsId, String uuid) {
        MarketGoods goods = this.getById(goodsId);
        if (goods == null || !goods.getSellerId().equals(uuid)) {
            return Result.error("商品不存在或无权操作");
        }
        if (goods.getStatus() != 0) {
            return Result.error("当前状态不可下架");
        }

        goods.setStatus(3);
        this.updateById(goods);

        // ✅ 调用插件端暂存物品接口
        Result<String> pendingResult = playerService.addPendingItem(
                uuid,
                goods.getItemKey(),
                goods.getAmount(),
                goods.getDisplayName(),
                goods.getNbtData(),
                "MARKET_WITHDRAW",
                goods.getId()
        );

        if (pendingResult.getCode() != 2000) {
            return Result.successMsg("下架成功，但物品暂存失败，请联系管理员处理");
        }




// 给卖家发通知
        MyUser seller = myUserMapper.selectOne(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getBindId, uuid));
        if (seller != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    seller.getId(),
                    "GOODS_WITHDRAWN",
                    "商品下架",
                    "您的商品 " + goods.getDisplayName() + " 已下架，物品已返还"
            ));
        }





        return Result.successMsg("下架成功，请在游戏内使用 /claim 领取物品");
    }

    /**
     * 购买商品：核心交易逻辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> purchaseGoods(String goodsId, String buyerId) {
        // 1. 悲观锁查询，防止并发超卖
        MarketGoods goods = ((MarketGoodsMapper) this.baseMapper).selectByIdForUpdate(goodsId);
        if (goods == null || goods.getStatus() != 0) {
            return Result.error("商品已被买走或已下架");
        }

        // 2. 检查是否自己买自己的商品
        if (goods.getSellerId().equals(buyerId)) {
            return Result.error("不能购买自己的商品");
        }

        // 3. ✅ 查询买家信息，检查余额
        MyUser buyer = myUserMapper.selectOne(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getBindId, buyerId));
        if (buyer == null) {
            return Result.error("买家信息不存在");
        }

        Long buyerMoney = buyer.getMoney() == null ? 0L : buyer.getMoney();
        if (buyerMoney < goods.getPrice()) {
            return Result.error("余额不足，当前余额：￥" + buyerMoney);
        }

        // 4. ✅ 查询卖家信息
        MyUser seller = myUserMapper.selectOne(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getBindId, goods.getSellerId()));

        // 5. ✅ 扣除买家余额
        Long newBuyerMoney = buyerMoney - goods.getPrice();
        LambdaUpdateWrapper<MyUser> buyerUpdate = new LambdaUpdateWrapper<>();
        buyerUpdate.eq(MyUser::getId, buyer.getId())
                .set(MyUser::getMoney, newBuyerMoney);
        myUserMapper.update(null, buyerUpdate);

        // 6. ✅ 增加卖家余额（如果卖家存在）
        if (seller != null) {
            Long sellerMoney = seller.getMoney() == null ? 0L : seller.getMoney();
            Long newSellerMoney = sellerMoney + goods.getPrice();
            LambdaUpdateWrapper<MyUser> sellerUpdate = new LambdaUpdateWrapper<>();
            sellerUpdate.eq(MyUser::getId, seller.getId())
                    .set(MyUser::getMoney, newSellerMoney);
            myUserMapper.update(null, sellerUpdate);
        }

        // 7. 更新商品状态为已成交 (状态码2)
        goods.setStatus(2);
        this.updateById(goods);

        // 8. 生成订单快照
        OrderRecord order = new OrderRecord();
        order.setGoodsId(goods.getId());
        order.setSellerId(goods.getSellerId());
        order.setBuyerId(buyerId);
        order.setItemKey(goods.getItemKey());
        order.setDisplayName(goods.getDisplayName());
        order.setNbtData(goods.getNbtData());
        order.setFinalPrice(goods.getPrice());
        order.setDeliveryStatus(0);  // 0-待发货
        order.setFee(0);

        orderRecordMapper.insert(order);

        // 9. ✅ 调用插件端暂存物品接口（将物品发放给买家）
        Result<String> pendingResult = playerService.addPendingItem(
                buyerId,
                goods.getItemKey(),
                goods.getAmount(),
                goods.getDisplayName(),
                goods.getNbtData(),
                "MARKET_PURCHASE",
                goods.getId()
        );

        if (pendingResult.getCode() != 2000) {
            // 交易已完成，但物品暂存失败，记录问题但不回滚
            log.error("购买成功但物品暂存失败: goodsId={}, buyer={}", goodsId, buyerId);
        }

        // 给买家发通知
        eventPublisher.publishEvent(new NotificationEvent(
                buyer.getId(),
                "PURCHASE_SUCCESS",
                "购买成功",
                "您成功购买了 " + goods.getDisplayName() + "，花费 ￥" + goods.getPrice()
        ));

// 给卖家发通知
        if (seller != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    seller.getId(),
                    "GOODS_SOLD",
                    "商品售出",
                    "您的商品 " + goods.getDisplayName() + " 已被购买，收入 ￥" + goods.getPrice()
            ));
        }

        return Result.success("购买成功", order.getId());
    }

    @Override
    public List<OrderRecord> getMyBoughtHistory(String uuid) {
        return orderRecordMapper.selectList(new LambdaQueryWrapper<OrderRecord>()
                .eq(OrderRecord::getBuyerId, uuid)
                .orderByDesc(OrderRecord::getCompleteTime));
    }

    @Override
    public List<OrderRecord> getMyAllOrderHistory(String uuid) {
        return orderRecordMapper.selectList(new LambdaQueryWrapper<OrderRecord>()
                .and(i -> i.eq(OrderRecord::getSellerId, uuid).or().eq(OrderRecord::getBuyerId, uuid))
                .orderByDesc(OrderRecord::getCompleteTime));
    }

    /**
     * 管理员强制下架商品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> adminWithdrawGoods(String goodsId) {
        MarketGoods goods = this.getById(goodsId);
        if (goods == null) {
            return Result.error("商品不存在");
        }
        if (goods.getStatus() != 0) {
            return Result.error("商品已不在待售状态");
        }

        goods.setStatus(3);
        this.updateById(goods);

        // ✅ 调用插件端暂存物品接口（返还给卖家）
        Result<String> pendingResult = playerService.addPendingItem(
                goods.getSellerId(),
                goods.getItemKey(),
                goods.getAmount(),
                goods.getDisplayName(),
                goods.getNbtData(),
                "ADMIN_WITHDRAW",
                goods.getId()
        );

        if (pendingResult.getCode() != 2000) {
            return Result.successMsg("下架成功，但物品暂存失败，请联系管理员处理");
        }




        // 给卖家发通知
        MyUser seller = myUserMapper.selectOne(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getBindId, goods.getSellerId()));
        if (seller != null) {
            eventPublisher.publishEvent(new NotificationEvent(
                    seller.getId(),
                    "GOODS_WITHDRAWN",
                    "商品被强制下架",
                    "您的商品 " + goods.getDisplayName() + " 已被管理员下架，物品已返还"
            ));
        }



        return Result.successMsg("强制下架成功，物品已返还给卖家");
    }
}