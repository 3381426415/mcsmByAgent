package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gitcode.mcsm_backend.Entity.MarketGoods;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.OrderRecord;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.gitcode.mcsm_backend.Entity.VO.MarketGoodsVO;
import java.util.List;
import java.util.Map;

/**
 * 市场交易接口 - 商品上架、购买、订单管理、卖家操作
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    /**
     * 1. 搜索/获取市场商品列表
     * 支持模糊查询，如果不传 keyword 则展示所有在售商品
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('user:market:view', 'admin:market:view')")
    public Result<IPage<MarketGoodsVO>> getMarketList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<MarketGoodsVO> data = marketService.searchGoods(keyword, page, size);
        return Result.success("获取成功", data);
    }


    /**
     * 2. 上架商品
     * 前端模拟背包点击后发送：槽位、价格、名称、材质、NBT
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('user:market:sell')")
    public Result<String> upload(@RequestBody Map<String, Object> params) {
        // 从 Security 上下文获取当前登录的 Web 用户信息
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // ✅ 检查是否被禁止发布
        if (currentUser.getBanPublish() != null && currentUser.getBanPublish()) {
            return Result.error("您已被禁止发布商品，请联系管理员");
        }
        String uuid = currentUser.getBindId(); // 获取绑定的 Minecraft UUID

        int slot = (int) params.get("slot");
        int price = (int) params.get("price");
        int amount = (int) params.get("amount");
        String displayName = (String) params.get("displayName");
        String itemKey = (String) params.get("itemKey");
        String nbtData = (String) params.get("nbtData");

        if (price <= 0) return Result.error("价格必须大于0");

        return marketService.uploadGoods(uuid, slot, amount,price, displayName, itemKey, nbtData);
    }

    /**
     * 2. 修改价格
     */
    @PostMapping("/update-price")
    @PreAuthorize("hasAuthority('user:market:sell')")
    public Result<String> updatePrice(@RequestBody Map<String, Object> params) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String goodsId = (String) params.get("id");

        // 建议使用更安全的类型转换
        Integer newPrice = Integer.valueOf(params.get("price").toString());

        if (newPrice <= 0) return Result.error("无效的价格");

        boolean success = marketService.lambdaUpdate()
                .set(MarketGoods::getPrice, newPrice)
                .eq(MarketGoods::getId, goodsId)
                .eq(MarketGoods::getSellerId, currentUser.getBindId())
                .eq(MarketGoods::getStatus, 0)
                .update();

        return success ? Result.successMsg("价格修改成功") : Result.error("修改失败，请检查商品状态");
    }

    /**
     * 4. 撤回/下架商品
     * 执行逻辑删除并准备返还道具
     */
    @PostMapping("/withdraw/{id}")
    @PreAuthorize("hasAuthority('user:market:sell')")
    public Result<String> withdraw(@PathVariable String id) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return marketService.withdrawGoods(id, currentUser.getBindId());
    }

    /**
     * 5. 购买商品
     * 核心交易入口
     */
    @PostMapping("/buy/{id}")
    @PreAuthorize("hasAuthority('user:market:buy')")
    public Result<String> buy(@PathVariable String id) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 注意：此处 buyerId 传入的是 Web 用户的 bindId (即游戏内UUID)
        return marketService.purchaseGoods(id, currentUser.getBindId());
    }

    /**
     * 6. 获取个人在售商品
     */
    @GetMapping("/my/on-sale")
    @PreAuthorize("isAuthenticated()")
    public Result<IPage<MarketGoodsVO>> getMyOnSale(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        IPage<MarketGoodsVO> data = marketService.getMyOnSaleGoods(currentUser.getBindId(), page, size);
        return Result.success("获取成功", data);
    }

    /**
     * 7. 获取个人购买记录 (已买到的宝贝)
     */
    @GetMapping("/my/bought")
    @PreAuthorize("isAuthenticated()")
    public Result<List<OrderRecord>> getMyBought() {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success("获取成功", marketService.getMyBoughtHistory(currentUser.getBindId()));
    }

    /**
     * 8. 获取个人全量交易账单 (包含卖出和买入)
     */
    @GetMapping("/my/orders")
    @PreAuthorize("isAuthenticated()")
    public Result<List<OrderRecord>> getMyOrders() {
        MyUser currentUser = (MyUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success("获取成功", marketService.getMyAllOrderHistory(currentUser.getBindId()));
    }
    /**
     * 9. 管理员强制下架商品（不校验所有权）
     */
    @PostMapping("/admin/withdraw/{id}")
    @PreAuthorize("hasAuthority('admin:market:withdraw')")
    @LogRecord(
            module = "市场管理",
            action = "强制下架",
            description = "强制下架商品，商品ID：#{#id}"
    )
    public Result<String> adminWithdraw(@PathVariable String id) {
        return marketService.adminWithdrawGoods(id);
    }
}