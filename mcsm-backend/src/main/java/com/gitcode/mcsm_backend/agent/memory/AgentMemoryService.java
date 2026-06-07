package com.gitcode.mcsm_backend.agent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gitcode.mcsm_backend.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentMemoryService {

    @Autowired
    private AgentSessionMapper sessionMapper;

    @Autowired
    private AgentChangeHistoryMapper changeHistoryMapper;

    @Autowired
    private AgentOperationLogMapper operationLogMapper;

    @Autowired
    private AgentErrorPatternMapper errorPatternMapper;

    @Autowired
    private AgentOpsKnowledgeMapper opsKnowledgeMapper;

    // ========== 会话管理 ==========

    public void saveSession(AgentSessionEntity session) {
        if (session.getId() == null) {
            sessionMapper.insert(session);
        } else {
            sessionMapper.updateById(session);
        }
    }

    public AgentSessionEntity getSession(String sessionId) {
        return sessionMapper.selectBySessionId(sessionId);
    }

    // ========== 变更历史 ==========

    public void recordChange(String taskId, Long userId, String serverId,
                              String filePath, String key, String oldValue,
                              String newValue, String reason, String status) {
        AgentChangeHistoryEntity entity = new AgentChangeHistoryEntity();
        entity.setTaskId(taskId);
        entity.setUserId(userId);
        entity.setServerId(serverId);
        entity.setFilePath(filePath);
        entity.setChangeKey(key);
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        entity.setReason(reason);
        entity.setStatus(status);
        changeHistoryMapper.insert(entity);
    }

    // ========== 操作日志 ==========

    public void recordOperation(String taskId, String agentId, String agentType,
                                 Long userId, String operation, String details, boolean success) {
        AgentOperationLogEntity entity = new AgentOperationLogEntity();
        entity.setTaskId(taskId);
        entity.setAgentId(agentId);
        entity.setAgentType(agentType);
        entity.setUserId(userId);
        entity.setOperation(operation);
        entity.setDetails(details);
        entity.setSuccess(success);
        operationLogMapper.insert(entity);
    }

    // ========== 错误模式 ==========

    public void recordErrorPattern(String scope, String serverId, String plugin,
                                    String errorType, String errorMessage, String solution) {
        AgentErrorPatternEntity entity = new AgentErrorPatternEntity();
        entity.setScope(scope);
        entity.setServerId(serverId);
        entity.setPlugin(plugin);
        entity.setErrorType(errorType);
        entity.setErrorMessage(errorMessage);
        entity.setSolution(solution);
        entity.setSummary(errorType + ": " + errorMessage);
        entity.setEffectiveness("PENDING");
        entity.setRecurrenceCount(0);
        entity.setStale(false);
        errorPatternMapper.insert(entity);
    }

    public List<AgentErrorPatternEntity> getErrorPatterns(String plugin, String serverId) {
        LambdaQueryWrapper<AgentErrorPatternEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentErrorPatternEntity::isStale, false);
        wrapper.in(AgentErrorPatternEntity::getScope, "GLOBAL", "SERVER");
        if (plugin != null) {
            wrapper.eq(AgentErrorPatternEntity::getPlugin, plugin);
        }
        if (serverId != null) {
            wrapper.and(w -> w.eq(AgentErrorPatternEntity::getServerId, serverId)
                    .or().eq(AgentErrorPatternEntity::getScope, "GLOBAL"));
        }
        return errorPatternMapper.selectList(wrapper);
    }

    // ========== 运维知识 ==========

    public void recordKnowledge(String scope, String serverId, String category,
                                 String subject, String relation, String object,
                                 String detail, String confidence) {
        AgentOpsKnowledgeEntity entity = new AgentOpsKnowledgeEntity();
        entity.setScope(scope);
        entity.setServerId(serverId);
        entity.setCategory(category);
        entity.setSubject(subject);
        entity.setRelation(relation);
        entity.setObject(object);
        entity.setDetail(detail);
        entity.setConfidence(confidence);
        entity.setVerified(false);
        entity.setStale(false);
        opsKnowledgeMapper.insert(entity);
    }

    public List<AgentOpsKnowledgeEntity> getKnowledge(String subject, String serverId) {
        LambdaQueryWrapper<AgentOpsKnowledgeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentOpsKnowledgeEntity::isStale, false);
        wrapper.in(AgentOpsKnowledgeEntity::getScope, "GLOBAL", "SERVER");
        if (subject != null) {
            wrapper.eq(AgentOpsKnowledgeEntity::getSubject, subject);
        }
        if (serverId != null) {
            wrapper.and(w -> w.eq(AgentOpsKnowledgeEntity::getServerId, serverId)
                    .or().eq(AgentOpsKnowledgeEntity::getScope, "GLOBAL"));
        }
        return opsKnowledgeMapper.selectList(wrapper);
    }

    public void markKnowledgeStale(String subject, String plugin) {
        LambdaQueryWrapper<AgentOpsKnowledgeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentOpsKnowledgeEntity::getSubject, subject);
        AgentOpsKnowledgeEntity update = new AgentOpsKnowledgeEntity();
        update.setStale(true);
        opsKnowledgeMapper.update(update, wrapper);
    }
}
