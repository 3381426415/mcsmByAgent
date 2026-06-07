package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.RedeemCode;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.event.NotificationEvent;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import com.gitcode.mcsm_backend.mapper.RedeemCodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 兑换码服务 - 生成、验证、使用兑换码，发放奖励并发送通知
 */
@Service
public class RedeemCodeService extends ServiceImpl<RedeemCodeMapper, RedeemCode> {

    @Autowired
    private MyUserMapper myUserMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Transactional(rollbackFor = Exception.class)
    public Result<String> redeem(String code, Long userId) {
        // 1. 查询兑换码
        RedeemCode redeemCode = this.getOne(new LambdaQueryWrapper<RedeemCode>()
                .eq(RedeemCode::getCode, code));

        if (redeemCode == null) {
            return Result.error("兑换码不存在");
        }

        // 2. 检查状态
        if (redeemCode.getStatus() == 1) {
            return Result.error("兑换码已被使用");
        }

        // 3. 检查过期
        if (redeemCode.getExpireTime() != null
                && redeemCode.getExpireTime().isBefore(LocalDateTime.now())) {
            return Result.error("兑换码已过期");
        }

        // 4. 先检查用户是否存在
        MyUser user = myUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 5. 更新兑换码状态
        redeemCode.setStatus(1);
        redeemCode.setUsedBy(userId);
        redeemCode.setUsedTime(LocalDateTime.now());
        this.updateById(redeemCode);

        // 6. 给用户加钱
        Long currentMoney = user.getMoney() == null ? 0L : user.getMoney();
        Long newMoney = currentMoney + redeemCode.getAmount();

        // ✅ 使用 LambdaUpdateWrapper 只更新 money 字段
        LambdaUpdateWrapper<MyUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MyUser::getId, userId)
                .set(MyUser::getMoney, newMoney);
        myUserMapper.update(null, updateWrapper);


        eventPublisher.publishEvent(new NotificationEvent(
                userId,
                "RECHARGE_SUCCESS",
                "充值成功",
                "兑换成功，余额增加 ￥" + redeemCode.getAmount() + "，当前余额 ￥" + newMoney
        ));


        return Result.success("兑换成功", "获得 " + redeemCode.getAmount() + " 元，当前余额 " + newMoney + " 元");
    }
}