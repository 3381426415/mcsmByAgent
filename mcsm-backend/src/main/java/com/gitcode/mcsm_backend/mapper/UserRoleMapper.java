package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户-角色关联数据访问 - 用户与角色的多对多关系维护
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {}