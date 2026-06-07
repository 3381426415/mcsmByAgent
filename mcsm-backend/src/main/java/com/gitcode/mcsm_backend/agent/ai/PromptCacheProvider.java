package com.gitcode.mcsm_backend.agent.ai;

import com.gitcode.mcsm_backend.agent.core.AgentPromptRegistry;
import com.gitcode.mcsm_backend.agent.core.AgentTask;
import com.gitcode.mcsm_backend.agent.core.AgentType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Prompt 缓存提供者 - 双策略缓存
 * 策略一：Pro Agent 保持前缀稳定
 * 策略二：子 Agent 高频 base prompt 共享
 */
public class PromptCacheProvider {

    // 厂商 cache 能力表
    private static final Map<String, Boolean> PROVIDER_CACHE_SUPPORT = Map.of(
            "deepseek", true,
            "openai", true,
            "zhipuai", false
    );

    /**
     * 检查厂商是否支持 prompt cache
     */
    public boolean isCacheSupported(String provider) {
        return PROVIDER_CACHE_SUPPORT.getOrDefault(provider, false);
    }

    /**
     * 构造 prompt：base 在前，动态追加在末尾
     * 保证同类 Agent 的 base prompt 字节级一致
     */
    public String buildPrompt(AgentType type, String dynamicContent) {
        String base = AgentPromptRegistry.getPrompt(type);
        if (dynamicContent == null || dynamicContent.isEmpty()) {
            return base;
        }
        return base + "\n\n--- 动态注入 ---\n" + dynamicContent;
    }

    /**
     * 按缓存优先级排序任务（高频 Agent 先执行，提高缓存命中率）
     */
    public List<AgentTask> sortByCachePriority(List<AgentTask> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparingInt(this::getCachePriority))
                .toList();
    }

    private int getCachePriority(AgentTask task) {
        return switch (task.getType()) {
            case ROLE -> 0;        // 最高优先级（每批 3~7 个）
            case EXECUTOR -> 1;    // 高（每批 2~4 个）
            case CHECKER -> 2;     // 中（每阶段 1 个）
            case SUMMARIZER -> 3;  // 低（每阶段 1 个）
            case PLANNER -> 4;     // 低（每任务 1 个）
            case KNOWLEDGE -> 5;   // 最低（异步触发）
            case DECISION -> 6;    // 不参与缓存排序
        };
    }
}
