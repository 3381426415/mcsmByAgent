// src/main/java/com/gitcode/mcsm_backend/service/AgentToolService.java

package com.gitcode.mcsm_backend.service;

import com.gitcode.mcsm_backend.Entity.ParameterDef;
import com.gitcode.mcsm_backend.Entity.ToolDefinition;
import com.gitcode.mcsm_backend.annotation.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 工具注册服务 - 扫描 @AgentTool 注解，自动注册工具定义供 Agent 调用
 */
@Slf4j
@Service
public class AgentToolService {

    private final ApplicationContext applicationContext;

    /** 存储所有扫描到的工具 */

    private final Map<String, ToolDefinition> toolRegistry = new ConcurrentHashMap<>();

    public AgentToolService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 启动时扫描所有带 @AgentTool 注解的方法
     */
    @PostConstruct
    public void scanTools() {
        // 获取所有带 @RestController 注解的 Bean
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Object controller : controllers.values()) {
            Class<?> controllerClass = controller.getClass();
            // Spring 代理会生成 CGLIB 类，需要获取原始类
            if (controllerClass.getName().contains("CGLIB")) {
                controllerClass = controllerClass.getSuperclass();
            }

            for (Method method : controllerClass.getDeclaredMethods()) {
                AgentTool annotation = method.getAnnotation(AgentTool.class);
                if (annotation == null) continue;

                ToolDefinition tool = buildToolDefinition(method, annotation, controllerClass);
                toolRegistry.put(tool.getName(), tool);
                log.info("[AgentTool] 注册工具: {} -> {}", tool.getName(), tool.getPath());
            }
        }

        log.info("[AgentTool] 扫描完成，共注册 {} 个工具", toolRegistry.size());
    }

    /**
     * 构建工具定义
     */
    private ToolDefinition buildToolDefinition(Method method, AgentTool annotation, Class<?> controllerClass) {
        // 工具名：优先用注解指定的，否则用「接口路径最后一段」
        String name = annotation.name();
        if (name.isEmpty()) {
            name = method.getName();
        }

        // 获取 HTTP 方法和路径
        String httpMethod = extractHttpMethod(method);
        String path = extractPath(controllerClass, method);

        // 提取参数列表
        List<ParameterDef> parameters = extractParameters(method);

        return ToolDefinition.builder()
                .name(name)
                .description(annotation.description())
                .httpMethod(httpMethod)
                .path(path)
                .requiresConfirm(annotation.requiresConfirm())
                .parameters(parameters)
                .build();
    }

    /**
     * 提取 HTTP 方法
     */
    private String extractHttpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            return rm.method().length > 0 ? rm.method()[0].name() : "GET";
        }
        return "POST";
    }

    /**
     * 提取完整路径
     */
    private String extractPath(Class<?> controllerClass, Method method) {
        String classPath = "";
        String methodPath = "";

        RequestMapping classRm = controllerClass.getAnnotation(RequestMapping.class);
        if (classRm != null && classRm.value().length > 0) {
            classPath = classRm.value()[0];
        }

        if (method.isAnnotationPresent(GetMapping.class)) {
            methodPath = method.getAnnotation(GetMapping.class).value().length > 0
                    ? method.getAnnotation(GetMapping.class).value()[0] : "";
        } else if (method.isAnnotationPresent(PostMapping.class)) {
            methodPath = method.getAnnotation(PostMapping.class).value().length > 0
                    ? method.getAnnotation(PostMapping.class).value()[0] : "";
        } else if (method.isAnnotationPresent(PutMapping.class)) {
            methodPath = method.getAnnotation(PutMapping.class).value().length > 0
                    ? method.getAnnotation(PutMapping.class).value()[0] : "";
        } else if (method.isAnnotationPresent(DeleteMapping.class)) {
            methodPath = method.getAnnotation(DeleteMapping.class).value().length > 0
                    ? method.getAnnotation(DeleteMapping.class).value()[0] : "";
        } else if (method.isAnnotationPresent(RequestMapping.class)) {
            methodPath = method.getAnnotation(RequestMapping.class).value().length > 0
                    ? method.getAnnotation(RequestMapping.class).value()[0] : "";
        }

        return classPath + methodPath;
    }

    /**
     * 提取方法参数
     */
    private List<ParameterDef> extractParameters(Method method) {
        List<ParameterDef> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            // 跳过 Spring 框架注入的参数
            if (param.getType().getName().contains("HttpServletRequest")
                    || param.getType().getName().contains("HttpServletResponse")) {
                continue;
            }

            String paramName = param.getName();
            String paramType = mapJavaType(param.getType());
            boolean required = true;
            String location = "query";

            // 从 @RequestParam 或 @RequestBody 获取详细信息
            if (param.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = param.getAnnotation(RequestParam.class);
                paramName = rp.value().isEmpty() ? paramName : rp.value();
                required = rp.required();
                location = "query";
            } else if (param.isAnnotationPresent(PathVariable.class)) {
                PathVariable pv = param.getAnnotation(PathVariable.class);
                paramName = pv.value().isEmpty() ? paramName : pv.value();
                required = true;
                location = "path";
            } else if (param.isAnnotationPresent(RequestBody.class)) {
                location = "body";
            }

            params.add(ParameterDef.builder()
                    .name(paramName)
                    .type(paramType)
                    .description(paramName)
                    .required(required)
                    .location(location)
                    .build());
        }
        return params;
    }

    /**
     * Java 类型映射到 JSON Schema 类型
     */
    private String mapJavaType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) return "integer";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        if (type == Double.class || type == double.class || type == Float.class || type == float.class) return "number";
        return "string";
    }

    /**
     * 获取所有工具
     */
    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(toolRegistry.values());
    }

    /**
     * 根据名称获取工具
     */
    public ToolDefinition getTool(String name) {
        return toolRegistry.get(name);
    }
}