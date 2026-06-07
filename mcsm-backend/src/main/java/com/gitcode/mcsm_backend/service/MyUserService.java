package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.common.Result;

/**
 * 用户服务接口 - 定义用户注册、查询、删除等业务方法
 */
public interface MyUserService extends IService<MyUser> {
    // 如果有复杂的业务逻辑（如登录检查），在这里定义

    /**
     * 注册新用户（含查重、密码加密、默认角色分配）
     */
    Result<String> registerUser(MyUser user);
    /**
     * 检查用户名是否已存在
     */
    boolean checkUsernameExists(String username);

}