package com.gitcode.mcsm_backend.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
/**
 * 操作日志注解 - 标记在 Controller 方法上，由 LogAspect 自动记录操作日志
 */
public @interface LogRecord {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作动作
     */
    String action();

    /**
     * 操作描述（支持 SpEL 表达式，可选）
     */
    String description() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean recordResult() default false;
}