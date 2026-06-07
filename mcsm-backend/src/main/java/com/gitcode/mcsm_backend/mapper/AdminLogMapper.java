package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.Entity.AdminLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员日志数据访问 - 管理员操作日志的 CRUD 操作
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {
}