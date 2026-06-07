package com.gitcode.mcsm_backend.agent.communication;

import com.gitcode.mcsm_backend.agent.core.KeyFinding;
import com.gitcode.mcsm_backend.agent.core.SubTaskState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 共享任务状态 - 所有 Agent 可读写的全局状态
 */
public class SharedTaskState {

    private String taskId;
    private final List<String> userDirectives = new CopyOnWriteArrayList<>();
    private final Map<String, SubTaskState> subTasks = new ConcurrentHashMap<>();
    private final List<KeyFinding> keyFindings = new CopyOnWriteArrayList<>();
    private final Map<String, Object> sharedData = new ConcurrentHashMap<>();

    public SharedTaskState(String taskId) {
        this.taskId = taskId;
    }

    public void addUserDirective(String directive) {
        userDirectives.add(directive);
    }

    public List<String> getUserDirectives() {
        return userDirectives;
    }

    public void updateSubTask(String id, SubTaskState state) {
        subTasks.put(id, state);
    }

    public void addKeyFinding(KeyFinding finding) {
        keyFindings.add(finding);
    }

    public void putSharedData(String key, Object value) {
        sharedData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSharedData(String key) {
        return (T) sharedData.get(key);
    }
}
