package com.gitcode.mcsm_backend.agent.orchestration;

import com.gitcode.mcsm_backend.agent.core.SubTaskState;
import com.gitcode.mcsm_backend.agent.core.SubTaskStatus;
import com.gitcode.mcsm_backend.agent.core.TaskStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务进度跟踪器 - 跟踪多 Agent 协作任务的状态、子任务进度和变更历史
 */
@Slf4j
public class TaskTracker {
    private String taskId;
    private String userId;
    private TaskStatus status;
    private final List<SubTaskState> subTasks = new ArrayList<>();
    private final Map<String, Object> sharedState = new ConcurrentHashMap<>();
    private long createdAt;
    private long updatedAt;

    public TaskTracker(String taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
        this.status = TaskStatus.PLANNING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public String getTaskId() { return taskId; }
    public String getUserId() { return userId; }
    public TaskStatus getStatus() { return status; }
    public List<SubTaskState> getSubTasks() { return subTasks; }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updatedAt = System.currentTimeMillis();
        log.info("[TaskTracker] 任务 {} 状态变更: {}", taskId, status);
    }

    public void addSubTask(SubTaskState subTask) {
        subTasks.add(subTask);
        this.updatedAt = System.currentTimeMillis();
    }

    public void updateSubTask(String id, SubTaskState state) {
        for (int i = 0; i < subTasks.size(); i++) {
            if (subTasks.get(i).getId().equals(id)) {
                subTasks.set(i, state);
                this.updatedAt = System.currentTimeMillis();
                return;
            }
        }
    }

    public SubTaskState getSubTask(String id) {
        return subTasks.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean allSubTasksComplete() {
        return subTasks.stream().allMatch(t ->
                t.getStatus() == SubTaskStatus.SUCCESS
                        || t.getStatus() == SubTaskStatus.SKIPPED);
    }

    public int getCompletedCount() {
        return (int) subTasks.stream().filter(t ->
                t.getStatus() == SubTaskStatus.SUCCESS
                        || t.getStatus() == SubTaskStatus.SKIPPED).count();
    }

    public int getFailedCount() {
        return (int) subTasks.stream().filter(t ->
                t.getStatus() == SubTaskStatus.FAILED).count();
    }
}
