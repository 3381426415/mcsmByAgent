package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.agent.memory.AgentOperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentOperationLogMapper extends BaseMapper<AgentOperationLogEntity> {
}
