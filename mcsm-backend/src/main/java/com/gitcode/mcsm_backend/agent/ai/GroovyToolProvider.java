package com.gitcode.mcsm_backend.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;

/**
 * Groovy 脚本执行工具提供者（兜底工具）
 *
 * 当其他专用工具无法满足需求时，AI 智能体可通过此工具执行任意 Groovy 脚本。
 * Groovy 基于 JVM，可直接使用项目所有依赖（MySQL 驱动、Jackson、SnakeYAML 等）。
 */
public class GroovyToolProvider implements LocalToolProvider {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "GroovyWorker");
        t.setDaemon(true);
        return t;
    });

    public GroovyToolProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(new ToolDefinition("execute_groovy",
                "[兜底工具] 执行 Groovy 脚本。当其他工具无法满足需求时使用，如：数据库查询、复杂数据处理、自定义逻辑。" +
                "脚本最后一行自动作为返回值。预置变量：ctx（Spring 上下文，可 getBean 获取任意服务）、db（DataSource）。" +
                "可直接使用项目依赖（com.mysql.cj.jdbc.Driver、com.fasterxml.jackson.databind.ObjectMapper 等）。" +
                "示例：\"import javax.sql.DataSource\\nds = ctx.getBean(DataSource.class)\\ncon = ds.getConnection()\\nrs = con.createStatement().executeQuery('SELECT 1')\\nrs.next()\\nrs.getInt(1)\"",
                Map.of("type", "object",
                        "properties", Map.of(
                                "code", Map.of("type", "string", "description", "要执行的 Groovy 脚本代码"),
                                "timeout", Map.of("type", "integer", "description", "超时秒数，默认 10，最大 30")
                        ),
                        "required", List.of("code"))));

        return tools;
    }

    @Override
    public String execute(String toolName, Map<String, Object> arguments) {
        if (!"execute_groovy".equals(toolName)) {
            return toJson(Map.of("error", "未知工具: " + toolName));
        }

        String code = (String) arguments.get("code");
        if (code == null || code.isBlank()) {
            return toJson(Map.of("success", false, "error", "code 不能为空"));
        }

        int timeout = 10;
        if (arguments.containsKey("timeout")) {
            try {
                timeout = Integer.parseInt(String.valueOf(arguments.get("timeout")));
            } catch (NumberFormatException ignored) {
            }
        }
        timeout = Math.max(1, Math.min(30, timeout));

        long startTime = System.currentTimeMillis();

        try {
            // 创建 Binding，注入预定义变量
            Binding binding = new Binding();
            binding.setVariable("ctx", applicationContext);
            try {
                binding.setVariable("db", applicationContext.getBean(DataSource.class));
            } catch (Exception ignored) {
                // DataSource 不可用时不注入
            }

            // 异步执行，带超时控制
            CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
                GroovyShell shell = new GroovyShell(binding);
                return shell.evaluate(code);
            }, executor);

            Object result = future.get(timeout, TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;

            String resultStr = formatResult(result);
            if (resultStr.length() > 5000) {
                resultStr = resultStr.substring(0, 5000) + "\n...(结果过长，已截断)";
            }

            return toJson(Map.of(
                    "success", true,
                    "result", resultStr,
                    "executionTime", elapsed + "ms"
            ));

        } catch (TimeoutException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return toJson(Map.of(
                    "success", false,
                    "error", "脚本执行超时（" + timeout + " 秒）。请检查是否有死循环或耗时操作。",
                    "executionTime", elapsed + "ms"
            ));
        } catch (ExecutionException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return toJson(Map.of(
                    "success", false,
                    "error", cause.getClass().getSimpleName() + ": " + cause.getMessage(),
                    "executionTime", elapsed + "ms"
            ));
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return toJson(Map.of(
                    "success", false,
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage(),
                    "executionTime", elapsed + "ms"
            ));
        }
    }

    private String formatResult(Object result) {
        if (result == null) return "null";
        if (result instanceof String s) return s;
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\":\"JSON 序列化失败: " + e.getMessage() + "\"}";
        }
    }
}
