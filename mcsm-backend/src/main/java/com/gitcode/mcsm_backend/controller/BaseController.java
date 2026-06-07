package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.Entity.LoginVo;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.MyUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import com.gitcode.mcsm_backend.Entity.MyUser;
import org.springframework.web.bind.annotation.*;
import com.gitcode.mcsm_backend.service.Account;


/**
 * 基础接口 - 登录注册、Token 刷新等通用认证接口
 */
@RestController
@RequestMapping("/api") // 建议统一前缀
public class BaseController {

    @Autowired
    private Account accountService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private MyUserService myUserService;



    @PostMapping("/register")
    public Result<String> register(@RequestBody MyUser user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("账号或密码不能为空");
        }


        return myUserService.registerUser(user);
    }

    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody MyUser user) {
        return  accountService.login(user);
    }

    @GetMapping("/verify")
    public Result<String> veirfy() {
        return Result.successMsg("access");
    }


}


