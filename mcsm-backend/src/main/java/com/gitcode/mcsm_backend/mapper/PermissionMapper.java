package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 权限数据访问 - 权限 CRUD 及按角色 ID 批量查询权限
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    // 根据角色 ID 列表查询所有权限
    @Select("<script>" +
            "SELECT DISTINCT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.perm_id = rp.perm_id " +
            "WHERE rp.role_id IN " +
            "<foreach item='item' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<Permission> selectPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
}