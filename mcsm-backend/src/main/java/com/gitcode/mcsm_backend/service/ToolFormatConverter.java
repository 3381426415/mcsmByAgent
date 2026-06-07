package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.Entity.ParameterDef;
import com.gitcode.mcsm_backend.Entity.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具格式转换器 - 将后端 ToolDefinition 转换为 Agent 可用的 OpenAI function calling 格式
 */
@Component
public class ToolFormatConverter {

    public List<Map<String, Object>> toFunctionCallingFormat(List<ToolDefinition> tools) {
        return tools.stream().map(tool -> {
            Map<String, Object> toolMap = new LinkedHashMap<>();
            toolMap.put("type", "function");

            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());

            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put("type", "object");

            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();

            for (ParameterDef param : tool.getParameters()) {
                Map<String, Object> prop = new LinkedHashMap<>();
                prop.put("type", param.getType());
                prop.put("description", param.getDescription());
                properties.put(param.getName(), prop);

                if (param.isRequired()) {
                    required.add(param.getName());
                }
            }

            parameters.put("properties", properties);
            if (!required.isEmpty()) {
                parameters.put("required", required);
            }

            function.put("parameters", parameters);
            toolMap.put("function", function);

            return toolMap;
        }).collect(Collectors.toList());
    }
}
