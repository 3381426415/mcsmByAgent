package com.gitcode.mcsm_backend.agent.orchestration;

import com.gitcode.mcsm_backend.agent.core.SubTaskState;
import com.gitcode.mcsm_backend.agent.core.SubTaskStatus;
import com.gitcode.mcsm_backend.agent.core.AgentType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务规划器 - 代码实现，不依赖模型推理
 * DecisionAgent 理解用户意图后，调用 TaskPlanner 按任务模板拆分
 */
@Slf4j
public class TaskPlanner {

    /**
     * 根据任务类型和目标文件，拆分子任务
     */
    public List<SubTaskState> plan(TaskType taskType, List<String> targetFiles,
                                    String description, String userDirectives) {
        return switch (taskType) {
            case SINGLE_COMMAND -> planSingleCommand(description);
            case DIAGNOSIS -> planDiagnosis(description);
            case PLUGIN_CONFIG -> planPluginConfig(targetFiles, description);
            case BULK_PLUGIN_CONFIG -> planBulkPluginConfig(targetFiles, description);
            case GROUP_SERVER_CONFIG -> planGroupServerConfig(targetFiles, description);
            case INSTALL_PLUGIN -> planInstallPlugin(description);
            case FILE_OPERATION -> planFileOperation(targetFiles, description);
            case QUESTION -> planQuestion(description);
        };
    }

    private List<SubTaskState> planSingleCommand(String description) {
        SubTaskState task = SubTaskState.builder()
                .id("subtask_1")
                .description(description)
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.EXECUTOR)
                .targetFiles(List.of())
                .dependsOn(List.of())
                .complexity("LOW")
                .build();
        return List.of(task);
    }

    private List<SubTaskState> planDiagnosis(String description) {
        List<SubTaskState> tasks = new ArrayList<>();

        tasks.add(SubTaskState.builder()
                .id("subtask_1")
                .description("服务器状态检查")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of("server.properties"))
                .dependsOn(List.of())
                .complexity("LOW")
                .build());

        tasks.add(SubTaskState.builder()
                .id("subtask_2")
                .description("日志分析")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of("logs/latest.log"))
                .dependsOn(List.of())
                .complexity("MEDIUM")
                .build());

        tasks.add(SubTaskState.builder()
                .id("subtask_3")
                .description("性能指标检查")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of())
                .dependsOn(List.of())
                .complexity("LOW")
                .build());

        return tasks;
    }

    private List<SubTaskState> planPluginConfig(List<String> targetFiles, String description) {
        SubTaskState task = SubTaskState.builder()
                .id("subtask_1")
                .description(description)
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(targetFiles)
                .dependsOn(List.of())
                .complexity("MEDIUM")
                .build();
        return List.of(task);
    }

    private List<SubTaskState> planBulkPluginConfig(List<String> targetFiles, String description) {
        List<SubTaskState> tasks = new ArrayList<>();
        int batchSize = 15;

        for (int i = 0; i < targetFiles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, targetFiles.size());
            List<String> batch = targetFiles.subList(i, end);
            int groupIndex = i / batchSize + 1;

            tasks.add(SubTaskState.builder()
                    .id("subtask_" + groupIndex)
                    .description("配置第 " + groupIndex + " 组插件")
                    .status(SubTaskStatus.PENDING)
                    .assignedAgentType(AgentType.ROLE)
                    .targetFiles(batch)
                    .dependsOn(List.of())
                    .complexity("MEDIUM")
                    .build());
        }

        if (tasks.size() > 8) {
            log.info("[TaskPlanner] 子任务数量 {} 超过限制 8，将合并", tasks.size());
            return mergeTasks(tasks, 8);
        }

        return tasks;
    }

    private List<SubTaskState> planGroupServerConfig(List<String> targetFiles, String description) {
        List<SubTaskState> tasks = new ArrayList<>();

        java.util.Map<String, List<String>> byServer = new java.util.LinkedHashMap<>();
        for (String file : targetFiles) {
            String serverId = extractServerId(file);
            byServer.computeIfAbsent(serverId, k -> new ArrayList<>()).add(file);
        }

        int index = 1;
        for (java.util.Map.Entry<String, List<String>> entry : byServer.entrySet()) {
            tasks.add(SubTaskState.builder()
                    .id("subtask_" + index++)
                    .description("配置服务器: " + entry.getKey())
                    .status(SubTaskStatus.PENDING)
                    .assignedAgentType(AgentType.ROLE)
                    .targetFiles(entry.getValue())
                    .dependsOn(List.of())
                    .complexity("MEDIUM")
                    .build());
        }

        return tasks;
    }

    private List<SubTaskState> planInstallPlugin(String description) {
        List<SubTaskState> tasks = new ArrayList<>();

        tasks.add(SubTaskState.builder()
                .id("subtask_1")
                .description("搜索插件")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of())
                .dependsOn(List.of())
                .complexity("LOW")
                .build());

        tasks.add(SubTaskState.builder()
                .id("subtask_2")
                .description("安装插件")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.EXECUTOR)
                .targetFiles(List.of("plugins/"))
                .dependsOn(List.of("subtask_1"))
                .complexity("MEDIUM")
                .build());

        tasks.add(SubTaskState.builder()
                .id("subtask_3")
                .description("配置插件")
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of())
                .dependsOn(List.of("subtask_2"))
                .complexity("MEDIUM")
                .build());

        return tasks;
    }

    private List<SubTaskState> planFileOperation(List<String> targetFiles, String description) {
        SubTaskState task = SubTaskState.builder()
                .id("subtask_1")
                .description(description)
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.EXECUTOR)
                .targetFiles(targetFiles)
                .dependsOn(List.of())
                .complexity("LOW")
                .build();
        return List.of(task);
    }

    private List<SubTaskState> planQuestion(String description) {
        SubTaskState task = SubTaskState.builder()
                .id("subtask_1")
                .description(description)
                .status(SubTaskStatus.PENDING)
                .assignedAgentType(AgentType.ROLE)
                .targetFiles(List.of())
                .dependsOn(List.of())
                .complexity("LOW")
                .build();
        return List.of(task);
    }

    private String extractServerId(String filePath) {
        String[] parts = filePath.split("[/\\\\]");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("servers")) {
                return parts[i + 1];
            }
        }
        return "default";
    }

    private List<SubTaskState> mergeTasks(List<SubTaskState> tasks, int targetCount) {
        List<SubTaskState> merged = new ArrayList<>(tasks.subList(0, targetCount - 1));
        List<String> remainingFiles = new ArrayList<>();
        for (int i = targetCount - 1; i < tasks.size(); i++) {
            remainingFiles.addAll(tasks.get(i).getTargetFiles());
        }
        SubTaskState last = tasks.get(targetCount - 1);
        merged.add(SubTaskState.builder()
                .id(last.getId())
                .description(last.getDescription() + " (含合并)")
                .status(last.getStatus())
                .assignedAgentType(last.getAssignedAgentType())
                .targetFiles(remainingFiles)
                .dependsOn(last.getDependsOn())
                .complexity(last.getComplexity())
                .build());
        return merged;
    }
}
