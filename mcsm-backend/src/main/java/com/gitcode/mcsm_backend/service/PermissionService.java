package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitcode.mcsm_backend.Entity.*;
import com.gitcode.mcsm_backend.mapper.PermissionMapper;
import com.gitcode.mcsm_backend.mapper.RoleMapper;
import com.gitcode.mcsm_backend.mapper.RolePermissionMapper;
import com.gitcode.mcsm_backend.mapper.UserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务 - 管理角色权限分配、权限校验、角色与权限的关联操作
 */
@Service
public class PermissionService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    // ================= 权限查询 (用于登录校验) =================

    /**
     * 获取用户角色名称列表
     */
    public List<String> getRoleNamesByUserId(Long userId) {
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) return Collections.emptyList();

        return roles.stream()
                .map(Role::getRoleName) //
                .collect(Collectors.toList());
    }

    /**
     * 获取用户权限标识列表
     */
    public List<String> getPermissionCodesByUserId(Long userId) {
        List<Role> roles = roleMapper.selectRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) return Collections.emptyList();

        List<Long> roleIds = roles.stream()
                .map(Role::getRoleId) //
                .collect(Collectors.toList());

        return permissionMapper.selectPermissionsByRoleIds(roleIds)
                .stream()
                .map(Permission::getName) //
                .collect(Collectors.toList());
    }

    // ================= 权限管理 (用于管理面板) =================

    /**
     * 修改用户的角色分配
     * @param userId 用户ID
     * @param roleIds 新的角色ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRoles(Long userId, List<Long> roleIds) {
        // 1. 删除旧的角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId));

        // 2. 插入新的关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    /**
     * 修改角色的权限分配
     * @param roleId 角色ID
     * @param permIds 新的权限ID列表 (int类型)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRolePermissions(Long roleId, List<Integer> permIds) {
        // 1. 删除旧的权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));

        // 2. 插入新的关联 (使用 permId)
        if (permIds != null && !permIds.isEmpty()) {
            for (Integer pId : permIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermId(pId.longValue()); // 注意：实体中是 long permId
                rolePermissionMapper.insert(rp);
            }
        }
    }



    /**
     * 根据角色 ID 获取权限名称列表（用于表格展示）
     */
    public List<String> getPermissionNamesByRoleId(Long roleId) {
        // 1. 先从中间表查询该角色关联的所有权限 ID
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        if (rps == null || rps.isEmpty()) return Collections.emptyList();

        // 2. 提取权限 ID 列表
        List<Long> permIds = rps.stream()
                .map(RolePermission::getPermId)
                .collect(Collectors.toList());

        // 3. 根据权限 ID 列表查询权限对象，并提取名称
        List<Integer> permIdInts = permIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>().in(Permission::getPermId, permIdInts)
        ).stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色 ID 获取权限 ID 列表（用于前端勾选回显）
     */
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId)
        );
        return rps.stream()
                .map(RolePermission::getPermId)
                .collect(Collectors.toList());
    }
    // ================= agent鉴权 =================
    /**
     * 获取系统中全部权限列表
     * 供 Agent 鉴权、权限分配面板等场景使用
     */
    public List<Permission> getAllPermissions() {
        return permissionMapper.selectList(null);
    }







}