package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联数据访问 - 角色与权限的多对多关系维护
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {}