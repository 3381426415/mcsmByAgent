// src/main/java/com/gitcode/mcsm_backend/Entity/ParameterDef.java

package com.gitcode.mcsm_backend.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具参数定义 - 描述 Agent 工具的单个参数名称、类型和描述
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDef {

    /** 参数名 */
    private String name;

    /** 参数类型：string / integer / boolean */
    private String type;

    /** 参数描述 */
    private String description;

    /** 是否必填 */
    private boolean required;

    /** 参数位置：query / path / body */
    private String location;
}