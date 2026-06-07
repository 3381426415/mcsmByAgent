package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitcode.mcsm_backend.mapper.MyUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.gitcode.mcsm_backend.Entity.MyUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
/**
 * Spring Security 用户详情服务 - 从数据库加载用户信息用于认证和授权
 */
@Service
public class userDetailsService implements UserDetailsService {

    @Autowired
    private MyUserMapper myUserMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询基础用户信息
        MyUser user = myUserMapper.selectOne(new LambdaQueryWrapper<MyUser>()
                .eq(MyUser::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("账号或密码错误");
        }

        // 2. 从独立的权限服务/表中获取数据
        List<String> roleNames = permissionService.getRoleNamesByUserId(user.getId());
        List<String> permissionCodes = permissionService.getPermissionCodesByUserId(user.getId());

        // 3. 将字符串直接转换为 Spring Security 的权限对象
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 处理角色 (带 ROLE_ 前缀)
        if (roleNames != null) {
            roleNames.forEach(role ->
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        }

        // 处理具体权限标识
        if (permissionCodes != null) {
            permissionCodes.forEach(code ->
                    authorities.add(new SimpleGrantedAuthority(code)));
        }

        // 4. 直接设置到 User 对象中，不再需要中间字符串变量
        user.setAuthorities(authorities);

        return user;
    }
}

