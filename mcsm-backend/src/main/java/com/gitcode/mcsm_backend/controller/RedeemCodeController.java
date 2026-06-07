package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.RedeemCode;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.RedeemCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 兑换码接口 - 生成、查询、使用、删除兑换码
 */
@RestController
@RequestMapping("/api/redeem")
public class RedeemCodeController {

    @Autowired
    private RedeemCodeService redeemCodeService;

    @PostMapping("/use")
    public Result<String> useCode(@RequestBody Map<String, String> params) {
        String code = params.get("code");
        if (code == null || code.trim().isEmpty()) {
            return Result.error("请输入兑换码");
        }

        MyUser currentUser = (MyUser) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        return redeemCodeService.redeem(code.trim(), currentUser.getId());
    }

    // ==================== 管理员端点 ====================

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<IPage<RedeemCode>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RedeemCode> p = new Page<>(page, size);
        p.setOrders(java.util.List.of(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time")));
        IPage<RedeemCode> result = redeemCodeService.page(p);
        return Result.success("获取成功", result);
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(module = "兑换码管理", action = "创建兑换码", description = "创建兑换码，金额: #{#body['amount']}")
    public Result<RedeemCode> create(@RequestBody Map<String, Object> body) {
        int amount = Integer.parseInt(body.get("amount").toString());
        String code = body.containsKey("code") ? (String) body.get("code")
                : UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        RedeemCode redeemCode = new RedeemCode();
        redeemCode.setCode(code);
        redeemCode.setAmount(amount);
        redeemCode.setStatus(0);

        if (body.containsKey("expireTime")) {
            redeemCode.setExpireTime(LocalDateTime.parse((String) body.get("expireTime")));
        }

        redeemCodeService.save(redeemCode);
        return Result.success("创建成功", redeemCode);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(module = "兑换码管理", action = "删除兑换码", description = "删除兑换码: #{#id}")
    public Result<String> delete(@PathVariable Long id) {
        return redeemCodeService.removeById(id)
                ? Result.successMsg("删除成功") : Result.error("删除失败");
    }
}