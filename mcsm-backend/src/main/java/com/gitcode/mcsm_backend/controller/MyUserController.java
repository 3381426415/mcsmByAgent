package com.gitcode.mcsm_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result; // 引入你的 Result 类
import com.gitcode.mcsm_backend.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
/**
 * 用户管理接口 - 用户 CRUD、角色分配、余额充值（管理员操作）
 */
@RequestMapping("/api/users")
@CrossOrigin
public class MyUserController {

    @Autowired
    private MyUserService userService;

    // 1. 获取所有用户
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admin:user')")
    public Result<List<MyUser>> list(@RequestParam(required = false) String query) {
        LambdaQueryWrapper<MyUser> wrapper = new LambdaQueryWrapper<>();
        if (query != null && !query.isEmpty()) {
            wrapper.like(MyUser::getUsername, query)
                    .or()
                    .like(MyUser::getEmail, query);
        }
        List<MyUser> list = userService.list(wrapper);
        return Result.success("查询成功", list); // 使用 success 静态方法包装
    }

    // 2. 添加新用户
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(
            module = "用户管理",
            action = "添加用户",
            description = "添加新用户：#{#user.username}"
    )
    public Result<Void> save(@RequestBody MyUser user) {
        boolean saved = userService.save(user);
        return saved ? Result.successMsg("添加用户成功") : Result.error("添加用户失败");
    }

    // 3. 修改用户信息（包括封禁状态）
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(
            module = "用户管理",
            action = "修改用户",
            description = "修改用户信息：#{#user.username}，封禁状态：#{#user.baned ? '已封禁' : '正常'}"
    )
    public Result<Void> update(@RequestBody MyUser user) {
        boolean updated = userService.updateById(user);
        return updated ? Result.successMsg("更新成功") : Result.error("更新失败");
    }

    // 4. 删除用户
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(
            module = "用户管理",
            action = "删除用户",
            description = "删除用户，ID：#{#id}"
    )
    public Result<Void> delete(@PathVariable Long id) {
        boolean removed = userService.removeById(id);
        return removed ? Result.successMsg("删除用户成功") : Result.error("用户不存在或删除失败");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Result<MyUser> getCurrentUser() {
        // 1. 从安全上下文获取当前登录的用户名
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        // 2. 根据用户名查询用户信息
        LambdaQueryWrapper<MyUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MyUser::getUsername, currentUsername);
        MyUser user = userService.getOne(wrapper);

        if (user != null) {
            // 出于安全考虑，返回给前端前可以将密码抹除
            user.setPassword(null);
            return Result.success("获取当前用户信息成功", user);
        }
        return Result.error("未找到当前登录用户信息");
    }

    @PutMapping("/updateByUsername")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateByName(@RequestBody MyUser user) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return Result.error("用户名不能为空");
        }

        // --- 安全加固 ---
        // 为了防止前端恶意篡改 money 或 bindId，我们手动构造一个只包含允许修改字段的更新对象
        MyUser updateData = new MyUser();
        updateData.setEmail(user.getEmail());
        // updateData.setNickname(user.getNickname()); // 如果允许修改昵称可开启

        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MyUser> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(MyUser::getUsername, user.getUsername());

        // 只更新 email，不更新 money、bindId 等
        boolean updated = userService.update(updateData, updateWrapper);

        return updated ? Result.successMsg("通过用户名更新成功") : Result.error("更新失败，未找到该用户");
    }
    // 5. 解绑接口：清除 bind_id 字段
    @PostMapping("/unbind")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> unbind(@RequestBody MyUser user) {
        String username = user.getUsername();
        if (username == null || username.isEmpty()) {
            return Result.error("用户名不能为空");
        }

        // 使用 LambdaUpdateWrapper 仅更新 bindId 字段为 null
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MyUser> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        updateWrapper.eq(MyUser::getUsername, username)
                .set(MyUser::getBindId, null); // 将数据库中的 bind_id 设为 NULL

        boolean success = userService.update(updateWrapper);

        if (success) {
            return Result.successMsg("解绑成功");
        } else {
            return Result.error("解绑失败，用户不存在");
        }
    }
    /**
     * 设置用户发布权限
     */
    @PutMapping("/ban-publish")
    @PreAuthorize("hasAuthority('admin:user')")
    @LogRecord(
            module = "用户管理",
            action = "设置发布权限",
            description = "设置用户 #{#params['userId']} 的发布权限为 #{#params['banPublish'] ? '禁止' : '允许'}"
    )
    public Result<Void> setBanPublish(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Boolean banPublish = (Boolean) params.get("banPublish");

        if (userId == null || banPublish == null) {
            return Result.error("参数不完整");
        }

        MyUser user = new MyUser();
        user.setId(userId);
        user.setBanPublish(banPublish);

        boolean updated = userService.updateById(user);
        return updated ? Result.successMsg("设置成功") : Result.error("设置失败");
    }


}