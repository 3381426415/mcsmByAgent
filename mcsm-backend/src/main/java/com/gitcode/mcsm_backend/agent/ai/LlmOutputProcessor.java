package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 输出处理器 - 从模型输出提取结构化数据
 */
@Slf4j
public class LlmOutputProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[\\s\\S]*\\]");

    /**
     * 从 LLM 输出中提取 JSON 对象
     */
    public Optional<JsonNode> extractJson(String output) {
        if (output == null || output.isBlank()) return Optional.empty();

        // 尝试直接解析
        try {
            return Optional.of(objectMapper.readTree(output));
        } catch (Exception ignored) {}

        // 尝试从文本中提取 JSON
        Matcher matcher = JSON_PATTERN.matcher(output);
        if (matcher.find()) {
            try {
                return Optional.of(objectMapper.readTree(matcher.group()));
            } catch (Exception ignored) {}
        }

        return Optional.empty();
    }

    /**
     * 从 LLM 输出中提取 JSON 数组
     */
    public Optional<JsonNode> extractJsonArray(String output) {
        if (output == null || output.isBlank()) return Optional.empty();

        try {
            JsonNode node = objectMapper.readTree(output);
            if (node.isArray()) return Optional.of(node);
        } catch (Exception ignored) {}

        Matcher matcher = JSON_ARRAY_PATTERN.matcher(output);
        if (matcher.find()) {
            try {
                return Optional.of(objectMapper.readTree(matcher.group()));
            } catch (Exception ignored) {}
        }

        return Optional.empty();
    }

    /**
     * 提取 JSON 并转换为指定类型
     */
    public <T> Optional<T> extractAs(String output, Class<T> type) {
        return extractJson(output).map(node -> {
            try {
                return objectMapper.treeToValue(node, type);
            } catch (Exception e) {
                log.info("[LlmOutputProcessor] 类型转换失败: {}", e.getMessage());
                return null;
            }
        });
    }

    /**
     * 检查输出是否包含错误
     */
    public boolean containsError(String output) {
        if (output == null) return false;
        String lower = output.toLowerCase();
        return lower.contains("\"error\"") || lower.contains("\"passed\": false");
    }
}
