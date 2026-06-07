package com.gitcode.mcsm_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.Entity.UserRole;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import com.gitcode.mcsm_backend.mapper.UserRoleMapper;
import com.gitcode.mcsm_backend.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现 - 用户注册（含密码加密、默认角色分配）、查询、删除等
 */
@Service
public class MyUserServiceImpl extends ServiceImpl<MyUserMapper, MyUser> implements MyUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Value("${app.default-role-id:2}")
    private Long defaultRoleId;   // ← 从配置读取，不再硬编码

    @Override
    @Transactional
    public Result<String> registerUser(MyUser user) {
        // 1. 基础非空校验
        if (user == null || !StringUtils.hasText(user.getUsername())
                || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名或密码不能为空");
        }

        // 2. 查重
        if (checkUsernameExists(user.getUsername())) {
            return Result.error("该用户名已被占用");
        }

        // 3. 设置默认信息，清除不可注册时设置的字段
        user.setNickname("新用户" + user.getUsername());
        user.setMoney(null);       // 禁止注册时注入余额
        user.setBaned(null);       // 禁止注册时注入封禁状态
        user.setEmail(null);       // 邮箱需单独验证
        user.setBindId(null);      // 绑定ID需单独设置
        user.setBanPublish(null);  // 禁止注册时注入发布权限

        // 4. 密码加密
        try {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } catch (Exception e) {
            log.error("密码加密失败", e);
            return Result.error("密码加密失败");
        }

        // 5. 入库
        int rows = baseMapper.insert(user);
        if (rows > 0) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRoleId);
            userRoleMapper.insert(userRole);
            return Result.successMsg("注册成功");
        }
        return Result.error("注册失败");
    }

    @Override
    public boolean checkUsernameExists(String username) {
        if (!StringUtils.hasText(username)) return false;
        return baseMapper.exists(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getUsername, username));
    }
}
