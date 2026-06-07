// src/main/java/com/gitcode/mcsm_backend/Entity/ToolDefinition.java

package com.gitcode.mcsm_backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 工具定义实体 - 描述 Agent 可调用的工具（名称、描述、参数列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    /** 工具名称，对应接口方法名 */
    private String name;

    /** 工具描述 */
    private String description;

    /** HTTP 方法：GET / POST / PUT / DELETE */
    private String httpMethod;

    /** 接口路径：/api/server/status */
    private String path;

    /** 是否需要人工确认 */
    private boolean requiresConfirm;

    /** 参数列表 */
    private List<ParameterDef> parameters;
}