package com.gitcode.mcsm_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gitcode.mcsm_backend.agent.memory.AgentSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSessionEntity> {

    @Select("SELECT * FROM agent_sessions WHERE session_id = #{sessionId} LIMIT 1")
    AgentSessionEntity selectBySessionId(String sessionId);
}
