package com.gitcode.mcsm_backend.controller;

import com.gitcode.mcsm_backend.Entity.Permission;
import com.gitcode.mcsm_backend.Entity.Role;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.common.Result;
import com.gitcode.mcsm_backend.mapper.PermissionMapper;
import com.gitcode.mcsm_backend.service.PermissionService;
import com.gitcode.mcsm_backend.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
/**
 * 角色管理接口 - 角色的增删改查及权限分配
 */
@PreAuthorize("hasAuthority('admin:role')")

public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionMapper permissionMapper;

    /**
     * 1. 获取所有角色列表 (查)
     */
    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.success("获取成功", roleService.list());
    }

    /**
     * 2. 添加新角色 (增)
     */
    @PostMapping("/add")
    @LogRecord(
            module = "角色管理",
            action = "添加角色",
            description = "添加新角色：#{#role.roleName}"
    )
    public Result<Void> add(@RequestBody Role role) {
        return roleService.save(role) ? Result.successMsg("添加成功") : Result.error("添加失败");
    }

    /**
     * 3. 修改角色基础信息 (改)
     */
    @PutMapping("/update")
    @LogRecord(
            module = "角色管理",
            action = "修改角色",
            description = "修改角色：#{#role.roleName}"
    )
    public Result<Void> update(@RequestBody Role role) {
        return roleService.updateById(role) ? Result.successMsg("修改成功") : Result.error("修改失败");
    }

    /**
     * 4. 删除角色 (删)
     */
    @DeleteMapping("/{roleId}")
    @LogRecord(
            module = "角色管理",
            action = "删除角色",
            description = "删除角色，ID：#{#roleId}"
    )
    public Result<Void> delete(@PathVariable Long roleId) {

        if (roleId == 1L || roleId == 2L) {
            return Result.error("系统预设角色，禁止删除");
        }
        return roleService.removeById(roleId) ? Result.successMsg("删除成功") : Result.error("删除失败");
    }

    /**
     * 5. 获取数据库中写死的所有权限 (供前端分配权限时作为可选列表)
     */
    @GetMapping("/all-permissions")
    public Result<List<Permission>> getAllPermissions() {
        // 直接从 permission 表获取所有预设权限
        List<Permission> list = permissionService.getAllPermissions();
        return Result.success("获取权限列表成功", list);
    }

    /**
     * 6. 给角色分配权限
     * @param roleId 角色ID
     * @param permIds 前端勾选的权限ID列表 (int类型，对应 Permission 实体)
     */
    @PostMapping("/assign-permissions")
    @LogRecord(
            module = "角色管理",
            action = "分配权限",
            description = "为角色 #{#roleId} 分配权限"
    )
    public Result<Void> assignPermissions(@RequestParam Long roleId, @RequestBody List<Integer> permIds) {
        // 使用 PermissionService 中已实现的事务方法
        permissionService.updateRolePermissions(roleId, permIds);
        return Result.successMsg("权限分配成功");
    }

    /**
     * 7. 获取某个角色已拥有的所有权限
     */
    @GetMapping("/role-permissions/names/{roleId}")
    public Result<List<String>> getRolePermissionNames(@PathVariable Long roleId) {
        // 逻辑：通过角色ID查询关联的权限对象，然后提取 name 列表
        // 假设你已经在 PermissionService 实现了这个逻辑
        List<String> names = permissionService.getPermissionNamesByRoleId(roleId);
        return Result.success("查询成功", names);
    }
    /**
     * 8. 根据角色ID获取已有的权限ID列表 (用于前端配置弹窗回显勾选)
     */
    @GetMapping("/role-permissions/ids/{roleId}")
    public Result<List<Long>> getRolePermissionIds(@PathVariable Long roleId) {
        List<Long> ids = permissionService.getPermissionIdsByRoleId(roleId);
        return Result.success("查询成功", ids);
    }

}