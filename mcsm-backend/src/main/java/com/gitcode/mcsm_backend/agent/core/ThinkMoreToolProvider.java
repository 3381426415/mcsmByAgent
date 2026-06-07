package com.gitcode.mcsm_backend.agent.core;

import com.gitcode.mcsm_backend.agent.ai.LocalToolProvider;
import com.gitcode.mcsm_backend.agent.ai.ToolDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * think_more 工具 — 让 LLM 自主决定是否继续 ReAct 循环
 *
 * LLM 调用此工具 → 工具返回 "ok" → ReAct 循环继续下一轮
 * LLM 不调用，直接返回文本 → 视为最终回复，退出循环
 */
@Slf4j
public class ThinkMoreToolProvider implements LocalToolProvider {

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        ToolDefinition def = new ToolDefinition(
                "think_more",
                "当你需要继续执行更多操作时调用此工具。调用后系统会继续下一轮执行。如果任务已完成，不要调用，直接回复用户。",
                ToolDefinition.stringParam("reason", "接下来要做什么", true)
        );
        return List.of(def);
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        log.info("[ThinkMore] LLM 请求继续思考: {}", arguments.get("reason"));
        return "ok";
    }
}
