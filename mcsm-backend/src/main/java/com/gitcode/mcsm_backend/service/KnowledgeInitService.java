package com.gitcode.mcsm_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * 知识库文件系统初始化服务
 *
 * 在服务器目录下创建 knowledge/ 目录和初始知识文件。
 * 遵循 v4 架构：知识存储在文件系统中，Agent 通过文件工具自主管理。
 */
@Slf4j
@Service
public class KnowledgeInitService {

    /**
     * 初始化服务器的知识库目录结构
     *
     * @param serverId 服务器ID
     */
    public void initKnowledgeBase(String serverId) {
        String baseDir = com.gitcode.mcsm_backend.common.McsmPaths.SERVERS_DIR + "/" + serverId;

        try {
            // 创建 knowledge 目录
            Path knowledgeDir = Path.of(baseDir, "knowledge");
            Files.createDirectories(knowledgeDir);

            // 创建 plugins 目录（如果不存在）
            Path pluginsDir = Path.of(baseDir, "plugins");
            Files.createDirectories(pluginsDir);

            // 创建全局知识文件
            createIfNotExists(knowledgeDir.resolve("README.md"), getReadmeContent());
            createIfNotExists(knowledgeDir.resolve("插件间依赖关系.md"), getDependencyContent());
            createIfNotExists(knowledgeDir.resolve("常见错误与修复方案.md"), getErrorPatternsContent());
            createIfNotExists(knowledgeDir.resolve("在线文档索引.md"), getOnlineDocsContent());
            createIfNotExists(knowledgeDir.resolve("用户偏好记录.md"), getUserPrefsContent());

            log.info("[KnowledgeInit] 知识库初始化完成: {}/knowledge/", baseDir);

        } catch (IOException e) {
            log.error("[KnowledgeInit] 初始化失败: {}", e.getMessage());
        }
    }

    private void createIfNotExists(Path file, String content) throws IOException {
        if (!Files.exists(file)) {
            Files.writeString(file, content);
            log.info("[KnowledgeInit] 创建: {}", file);
        }
    }

    private String getReadmeContent() {
        return "# 知识库索引\n" +
                "\n" +
                "## 使用说明\n" +
                "本目录存储服务器运维知识。每个插件有独立的 knowledge.md 文件，位于 `plugins/{插件名}/knowledge.md`。\n" +
                "\n" +
                "## 全局知识文件\n" +
                "- 插件间依赖关系.md — 插件之间的依赖和冲突关系\n" +
                "- 常见错误与修复方案.md — 错误模式 → 根因 → 解决方案\n" +
                "- 在线文档索引.md — Context7 可用性 + library ID\n" +
                "- 用户偏好记录.md — 用户的跨插件习惯\n" +
                "\n" +
                "## 插件知识\n" +
                "按需查看 `plugins/{插件名}/knowledge.md`\n" +
                "\n" +
                "## 知识来源与可信度\n" +
                "| 来源 | 可信度 |\n" +
                "|------|--------|\n" +
                "| 用户确认 | 最高 |\n" +
                "| Agent 实际修复验证 | 高 |\n" +
                "| AI 分析推断 | 中 |\n" +
                "| Context7 在线文档 | 中 |\n" +
                "| 待验证 | 低 |\n" +
                "\n" +
                "---\n" +
                "*创建时间: " + LocalDate.now() + "*\n";
    }

    private String getDependencyContent() {
        return "# 插件间依赖关系\n" +
                "\n" +
                "## 格式说明\n" +
                "记录插件之间的关系类型：\n" +
                "- LOAD_DEPENDS: 加载顺序依赖\n" +
                "- CONFIG_DEPENDS: 配置值引用\n" +
                "- CONFIG_OVERLAPS: 功能重叠\n" +
                "- REPLACES: 替代关系\n" +
                "- CONFLICT_WITH: 冲突关系\n" +
                "\n" +
                "## 已知关系\n" +
                "*（Agent 在运维过程中会自动补充）*\n" +
                "\n" +
                "---\n" +
                "*创建时间: " + LocalDate.now() + "*\n";
    }

    private String getErrorPatternsContent() {
        return "# 常见错误与修复方案\n" +
                "\n" +
                "## 格式说明\n" +
                "每个错误条目包含：\n" +
                "- 错误日志模式（关键词或正则）\n" +
                "- 根因分析\n" +
                "- 解决方案\n" +
                "- 验证状态\n" +
                "\n" +
                "## 已知错误\n" +
                "*（Agent 在成功修复错误后会自动追加）*\n" +
                "\n" +
                "---\n" +
                "*创建时间: " + LocalDate.now() + "*\n";
    }

    private String getOnlineDocsContent() {
        return "# 在线文档索引\n" +
                "\n" +
                "## 格式说明\n" +
                "记录哪些插件在 Context7 有文档可用。\n" +
                "包括负面记录——确认没有的标记\"不可用\"，避免重复查询。\n" +
                "\n" +
                "| 插件名 | Context7 可用 | Library ID | 内容质量 | 备注 |\n" +
                "|--------|:--:|------------|----------|------|\n" +
                "*（Agent 查询后会自动更新）*\n" +
                "\n" +
                "---\n" +
                "*创建时间: " + LocalDate.now() + "*\n";
    }

    private String getUserPrefsContent() {
        return "# 用户偏好记录\n" +
                "\n" +
                "## 格式说明\n" +
                "记录用户在运维过程中表达的偏好和习惯。\n" +
                "标注\"用户确认\"的条目具有最高优先级。\n" +
                "\n" +
                "## 已记录偏好\n" +
                "*（Agent 在用户纠正或表达偏好时会自动记录）*\n" +
                "\n" +
                "---\n" +
                "*创建时间: " + LocalDate.now() + "*\n";
    }
}
