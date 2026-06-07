package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.stream.Collectors;



import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
/**
 * 权限管理接口 - 权限的增删改查及角色权限分配
 */
@RequestMapping("/api/permissions")
@PreAuthorize("hasAuthority('admin:role')")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    // ================= 权限查询 =================

    /**
     * 获取指定用户拥有的所有角色名
     */
    @GetMapping("/user/roles/{userId}")
    public Result<List<String>> getUserRoles(@PathVariable Long userId) {
        List<String> roles = permissionService.getRoleNamesByUserId(userId);
        return Result.success("获取用户角色成功", roles);
    }

    /**
     * 获取指定用户拥有的所有权限标识码 (Code)
     */
    @GetMapping("/user/codes/{userId}")
    public Result<List<String>> getUserPermissionCodes(@PathVariable Long userId) {
        List<String> codes = permissionService.getPermissionCodesByUserId(userId);
        return Result.success("获取用户权限标识成功", codes);
    }

    // ================= 权限管理 =================

    /**
     * 给用户分配角色
     * body 格式: { "userId": 1, "roleIds": [1, 2, 3] }
     */
    @PostMapping("/user/update-roles")
    @LogRecord(
            module = "权限管理",
            action = "分配角色",
            description = "为用户 #{#params['userId']} 分配角色，角色ID：#{#params['roleId']}"
    )
    public Result<Void> updateUserRoles(@RequestBody Map<String, Object> params)
    {
        // 1. 解析参数
        Long userId = Long.valueOf(params.get(
                "userId"
        ).toString());

        // 2. 获取单个 roleId
        Object roleIdObj = params.get(
                "roleId"
        );
        if (roleIdObj == null
        ) {
            return Result.error("请选择一个角色"
            );
        }
        Long roleId = Long.valueOf(roleIdObj.toString());

        // 3. 包装成 List 调用你现有的 Service (保持兼容性)
        // 这样 Service 层不需要改动，它会先删掉旧角色，再插入这一个新角色
        permissionService.updateUserRoles(userId, Collections.singletonList(roleId));

        return Result.successMsg("用户角色更新成功"
        );
    }

    /**
     * 给角色分配权限
     * body 格式: { "roleId": 1, "permIds": [10, 11, 12] }
     */
    @PostMapping("/role/update-permissions")
    @LogRecord(
            module = "权限管理",
            action = "分配权限",
            description = "为角色 #{#params['roleId']} 分配权限"
    )
    @SuppressWarnings("unchecked")
    public Result<Void> updateRolePermissions(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        List<Integer> permIds = (List<Integer>) params.get("permIds");

        permissionService.updateRolePermissions(roleId, permIds);
        return Result.successMsg("角色权限更新成功");
    }






    /**
     * 获取当前登录用户的权限码列表（供前端动态控制菜单/按钮）
     */
    @GetMapping("/current")
    public Result<List<String>> getCurrentUserPermissions() {
        // 从 SecurityContext 获取当前用户
        MyUser currentUser = (MyUser) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        List<String> permissions = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());

        return Result.success("获取当前用户权限成功", permissions);
    }


















}