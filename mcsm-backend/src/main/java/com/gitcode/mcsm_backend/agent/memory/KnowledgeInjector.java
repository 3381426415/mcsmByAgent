package com.gitcode.mcsm_backend.agent.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识注入器 - 自动查询知识库，注入到 Agent prompt 中
 *
 * v4 架构：知识以文件系统 MD 文件为主，SQL 查询为辅
 * - 插件级: plugins/{插件名}/knowledge.md（主）
 * - 全局级: knowledge/*.md（主）
 * - SQL 错误模式、运维知识（辅）
 */
public class KnowledgeInjector {

    private final AgentMemoryService memoryService;
    private final LocalKnowledgeStore localKnowledgeStore;

    public KnowledgeInjector(AgentMemoryService memoryService, LocalKnowledgeStore localKnowledgeStore) {
        this.memoryService = memoryService;
        this.localKnowledgeStore = localKnowledgeStore;
    }

    /**
     * 注入知识到 Agent prompt
     *
     * @param agentPrompt 原始系统提示词
     * @param userMessage 用户消息（用于提取插件名）
     * @param taskType    任务类型
     * @param serverId    服务器ID
     * @return 注入知识后的提示词
     */
    public String inject(String agentPrompt, String userMessage,
                          String taskType, String serverId) {
        StringBuilder injection = new StringBuilder();

        // 1. 从文件系统读取插件 knowledge.md（主）
        String pluginName = extractPluginName(userMessage);
        if (pluginName != null) {
            String knowledgeMd = readPluginKnowledge(pluginName, serverId);
            if (knowledgeMd != null) {
                injection.append("\n\n--- 插件知识 (").append(pluginName).append(") ---\n");
                // 只注入头部摘要（前 20 行）
                String[] lines = knowledgeMd.split("\n");
                int headLines = Math.min(20, lines.length);
                for (int i = 0; i < headLines; i++) {
                    injection.append(lines[i]).append("\n");
                }
                if (lines.length > headLines) {
                    injection.append("... (共 ").append(lines.length).append(" 行，使用 read_file 读取完整内容)\n");
                }
            }
        }

        // 2. 从文件系统读取全局知识摘要（主）
        String globalSummary = readGlobalKnowledgeSummary(serverId);
        if (globalSummary != null) {
            injection.append("\n--- 全局知识 ---\n");
            injection.append(globalSummary);
        }

        // 3. 从 SQL 查询错误模式（辅）
        List<AgentErrorPatternEntity> patterns = memoryService != null
                ? memoryService.getErrorPatterns(pluginName, serverId) : List.of();
        if (!patterns.isEmpty()) {
            injection.append("\n--- 已知错误模式 (SQL) ---\n");
            for (AgentErrorPatternEntity p : patterns) {
                if (!"INEFFECTIVE".equals(p.getEffectiveness())) {
                    injection.append("- ").append(p.getSummary()).append("\n");
                }
            }
        }

        // 4. 从 SQL 查询运维知识（辅）
        List<AgentOpsKnowledgeEntity> knowledge = memoryService != null
                ? memoryService.getKnowledge(pluginName, serverId) : List.of();
        if (!knowledge.isEmpty()) {
            injection.append("\n--- 运维知识 (SQL) ---\n");
            for (AgentOpsKnowledgeEntity k : knowledge) {
                injection.append("- ").append(k.getSubject())
                        .append(" ").append(k.getRelation())
                        .append(" ").append(k.getObject()).append("\n");
            }
        }

        if (injection.length() > 0) {
            return agentPrompt + injection;
        }
        return agentPrompt;
    }

    /**
     * 检查插件的 knowledge.md 是否存在（知识就绪门控）
     */
    public boolean isKnowledgeReady(String pluginName, String serverId) {
        if (pluginName == null) return true;
        String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
        String[] possiblePaths = {
                baseDir + "/plugins/" + pluginName + "/knowledge.md",
                baseDir + "/plugins/" + pluginName.toLowerCase() + "/knowledge.md"
        };
        for (String path : possiblePaths) {
            if (Files.exists(Path.of(path))) return true;
        }
        return false;
    }

    /**
     * 批量检查多个插件的 knowledge.md 是否就绪
     *
     * @return 未就绪的插件名列表
     */
    public List<String> checkReadiness(List<String> pluginNames, String serverId) {
        List<String> notReady = new ArrayList<>();
        for (String name : pluginNames) {
            if (!isKnowledgeReady(name, serverId)) {
                notReady.add(name);
            }
        }
        return notReady;
    }

    /**
     * 从用户消息中提取插件名
     */
    private String extractPluginName(String userMessage) {
        if (userMessage == null) return null;

        // 常见插件名（优先匹配）
        String[] knownPlugins = {
                "EssentialsX", "CMI", "Vault", "LuckPerms", "WorldEdit", "WorldGuard",
                "PlaceholderAPI", "AuthMe", "Citizens", "HolographicDisplays",
                "Multiverse", "PlotSquared", "GriefPrevention", "CoreProtect"
        };

        for (String plugin : knownPlugins) {
            if (userMessage.toLowerCase().contains(plugin.toLowerCase())) {
                return plugin;
            }
        }

        // 扫描文件系统中实际存在的插件 knowledge.md，匹配用户消息中的插件名
        try {
            String baseDir = com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default/plugins";
            Path pluginsPath = Path.of(baseDir);
            if (Files.isDirectory(pluginsPath)) {
                try (var dirs = Files.list(pluginsPath)) {
                    List<String> dirNames = dirs.filter(Files::isDirectory)
                            .map(p -> p.getFileName().toString()).toList();
                    for (String dirName : dirNames) {
                        if (userMessage.toLowerCase().contains(dirName.toLowerCase())) {
                            return dirName;
                        }
                    }
                }
            }
        } catch (IOException ignored) {}

        return null;
    }

    /**
     * 从文件系统读取插件的 knowledge.md
     */
    private String readPluginKnowledge(String pluginName, String serverId) {
        String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";

        // 尝试不同的插件目录命名格式
        String[] possiblePaths = {
                baseDir + "/plugins/" + pluginName + "/knowledge.md",
                baseDir + "/plugins/" + pluginName.toLowerCase() + "/knowledge.md",
                baseDir + "/plugins/" + pluginName + "-基础插件/knowledge.md",
                baseDir + "/plugins/" + pluginName + "-综合管理/knowledge.md"
        };

        for (String path : possiblePaths) {
            try {
                Path filePath = Path.of(path);
                if (Files.exists(filePath)) {
                    return Files.readString(filePath);
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }

    /**
     * 读取全局知识文件摘要
     */
    private String readGlobalKnowledgeSummary(String serverId) {
        String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
        Path knowledgeDir = Path.of(baseDir, "knowledge");

        if (!Files.isDirectory(knowledgeDir)) return null;

        StringBuilder summary = new StringBuilder();
        try {
            List<Path> files;
            try (var stream = Files.list(knowledgeDir)) {
                files = stream.filter(p -> p.toString().endsWith(".md")).toList();
            }

            for (Path file : files) {
                List<String> lines = Files.readAllLines(file);
                if (!lines.isEmpty()) {
                    // 读取文件名和前 3 行作为摘要
                    summary.append("- ").append(file.getFileName()).append(": ");
                    int head = Math.min(3, lines.size());
                    for (int i = 0; i < head; i++) {
                        String line = lines.get(i).trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            summary.append(line);
                            break;
                        }
                    }
                    summary.append("\n");
                }
            }
        } catch (IOException ignored) {
        }

        return summary.length() > 0 ? summary.toString() : null;
    }

    /**
     * 搜索全局知识文件
     */
    public String searchGlobalKnowledge(String query, String serverId) {
        String baseDir = serverId != null ? com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId : com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/default";
        Path knowledgeDir = Path.of(baseDir, "knowledge");

        if (!Files.isDirectory(knowledgeDir)) return null;

        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);

        try {
            List<Path> files;
            try (var stream = Files.list(knowledgeDir)) {
                files = stream.filter(p -> p.toString().endsWith(".md")).toList();
            }

            for (Path file : files) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    if (pattern.matcher(lines.get(i)).find()) {
                        result.append("- [").append(file.getFileName()).append(":").append(i + 1)
                                .append("] ").append(lines.get(i).trim()).append("\n");
                    }
                }
            }
        } catch (IOException ignored) {
        }

        return result.length() > 0 ? result.toString() : null;
    }
}
