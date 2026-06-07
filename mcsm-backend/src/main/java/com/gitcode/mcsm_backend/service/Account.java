package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.Entity.LoginVo;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

/**
 * 账号服务 - 用户登录认证、JWT Token 生成与刷新
 */
@Service
public class Account {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public Result<LoginVo> login(MyUser user) {
        try {
            Authentication authenticate = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            MyUser loginUser = (MyUser) authenticate.getPrincipal();
            String token = jwtUtils.createToken(loginUser.getUsername());

            List<String> roles = loginUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> auth.startsWith("ROLE_"))
                    .map(auth -> auth.replace("ROLE_", ""))
                    .collect(Collectors.toList());

            List<String> permissions = loginUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(auth -> !auth.startsWith("ROLE_"))
                    .collect(Collectors.toList());

            LoginVo loginVo = new LoginVo(token, roles, permissions);
            return Result.success("登录成功", loginVo);
        } catch (AuthenticationException e) {
            return Result.error("用户名或密码错误");
        }
    }
}







