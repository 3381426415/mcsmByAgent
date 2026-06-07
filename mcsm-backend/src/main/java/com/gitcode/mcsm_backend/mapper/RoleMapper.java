package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色数据访问 - 角色 CRUD 及按用户 ID 查询关联角色
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    // 根据用户 ID 查询关联的角色
    @Select("SELECT r.* FROM role r " +
            "INNER JOIN user_role ur ON r.role_id = ur.role_id " + // 修正为 role_id
            "WHERE ur.user_id = #{userId}")
    List<Role> selectRolesByUserId(Long userId);
}