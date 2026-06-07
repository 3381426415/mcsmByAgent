package com.gitcode.mcsm_backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
/**
 * 登录响应视图对象 - 包含登录成功后返回的 token 和用户信息
 */
@Data
@AllArgsConstructor
public class LoginVo {
    private String token;
    private List<String> roles;       // 角色名列表
    private List<String> permissions; // 权限码列表

}