package com.gitcode.mcsm_backend.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.Role;
import com.gitcode.mcsm_backend.mapper.RoleMapper;
import com.gitcode.mcsm_backend.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现 - 角色 CRUD 操作
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
}