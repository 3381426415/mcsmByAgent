package com.gitcode.mcsm_backend.agent.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具定义（OpenAI function-calling 兼容格式）
 */
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, Object> parameters; // JSON Schema

    public ToolDefinition() {}

    public ToolDefinition(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    /**
     * 转为 OpenAI function-calling 格式
     */
    public Map<String, Object> toFunctionCallingFormat() {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters != null ? parameters : Map.of(
                "type", "object",
                "properties", Map.of()
        ));

        tool.put("function", function);
        return tool;
    }

    /**
     * 快捷构建工具参数（无参数工具）
     */
    public static Map<String, Object> noParams() {
        return Map.of("type", "object", "properties", Map.of());
    }

    /**
     * 快捷构建带字符串参数的工具
     */
    public static Map<String, Object> stringParam(String name, String description, boolean required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", "string");
        prop.put("description", description);
        props.put(name, prop);
        schema.put("properties", props);
        if (required) {
            schema.put("required", List.of(name));
        }
        return schema;
    }
}
