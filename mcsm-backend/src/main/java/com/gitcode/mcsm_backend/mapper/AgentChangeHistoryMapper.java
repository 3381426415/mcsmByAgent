package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.agent.memory.AgentChangeHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentChangeHistoryMapper extends BaseMapper<AgentChangeHistoryEntity> {
}
