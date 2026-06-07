package com.gitcode.mcsm_backend.agent.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * ResultCollector — 纯代码，收集所有 Worker 的结构化输出
 *
 * 拼接为压缩摘要，作为 spawn_workers 工具返回值给 DecisionAgent。
 *
 * 输入：多个 Worker 的 JSON 输出
 * 输出：压缩摘要（成功/失败统计 + 警告 + 新发现）
 */
public class ResultCollector {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 收集所有 Worker 的结果，生成压缩摘要
     *
     * @param workerResults Worker 输出的 JSON 字符串列表
     * @return 压缩摘要
     */
    public String collect(List<String> workerResults) {
        int totalWorkers = workerResults.size();
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        List<Map<String, Object>> allResults = new ArrayList<>();
        List<Map<String, Object>> allDiscoveries = new ArrayList<>();
        List<String> concerns = new ArrayList<>();
        List<String> summaries = new ArrayList<>();

        for (String workerResult : workerResults) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(workerResult,
                        new TypeReference<Map<String, Object>>() {});

                // 统计结果
                String summary = (String) parsed.getOrDefault("summary", "");
                summaries.add(summary);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) parsed.getOrDefault("results", List.of());
                for (Map<String, Object> r : results) {
                    String status = (String) r.getOrDefault("status", "UNKNOWN");
                    switch (status) {
                        case "SUCCESS" -> successCount++;
                        case "FAILED" -> failedCount++;
                        case "SKIPPED" -> skippedCount++;
                    }
                    allResults.add(r);
                }

                // 收集 self_check 的 concerns
                @SuppressWarnings("unchecked")
                Map<String, Object> selfCheck = (Map<String, Object>) parsed.getOrDefault("self_check", Map.of());
                @SuppressWarnings("unchecked")
                List<String> workerConcerns = (List<String>) selfCheck.getOrDefault("concerns", List.of());
                concerns.addAll(workerConcerns);

                // 收集 discoveries
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> discoveries = (List<Map<String, Object>>) parsed.getOrDefault("discoveries", List.of());
                allDiscoveries.addAll(discoveries);

            } catch (Exception e) {
                failedCount++;
                allResults.add(Map.of("file", "-", "status", "FAILED",
                        "detail", "Worker 输出解析失败: " + e.getMessage()));
            }
        }

        // 构建压缩摘要
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalWorkers", totalWorkers);
        summary.put("success", successCount);
        summary.put("failed", failedCount);
        summary.put("skipped", skippedCount);

        // 构建文本摘要
        StringBuilder textSummary = new StringBuilder();
        textSummary.append("执行完成：共 ").append(totalWorkers).append(" 个任务，");
        textSummary.append(successCount).append(" 成功");
        if (failedCount > 0) textSummary.append("，").append(failedCount).append(" 失败");
        if (skippedCount > 0) textSummary.append("，").append(skippedCount).append(" 跳过");

        if (!concerns.isEmpty()) {
            textSummary.append("\n⚠️ 警告：").append(String.join("；", concerns));
        }

        if (!allDiscoveries.isEmpty()) {
            textSummary.append("\n💡 新发现：").append(allDiscoveries.size()).append(" 条");
            for (Map<String, Object> d : allDiscoveries) {
                textSummary.append("\n  - ").append(d.getOrDefault("content", ""));
            }
        }

        summary.put("textSummary", textSummary.toString());
        summary.put("details", allResults);
        summary.put("discoveries", allDiscoveries);
        summary.put("concerns", concerns);

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{\"error\":\"摘要生成失败\"}";
        }
    }
}
