// src/main/java/com/gitcode/mcsm_backend/annotation/AgentTool.java

package com.gitcode.mcsm_backend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * Agent 工具注解 - 标记在 Controller 方法上，由 AgentToolService 自动注册为 Agent 可调用的工具
 */
public @interface AgentTool {

    /** 工具名称，不填则自动生成 */
    String name() default "";

    /** 工具描述，告诉 AI 这个工具是干什么的 */
    String description();

    /** 是否允许 AI 自主调用（false 时需要人确认） */
    boolean requiresConfirm() default false;
}