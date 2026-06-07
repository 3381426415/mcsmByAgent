package com.gitcode.mcsm_backend.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitcode.mcsm_backend.Entity.AdminLog;
import com.gitcode.mcsm_backend.Entity.MyUser;
import com.gitcode.mcsm_backend.annotation.LogRecord;
import com.gitcode.mcsm_backend.service.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面 - 拦截 @LogRecord 注解的方法，自动记录管理员操作日志
 */
@Aspect
@Component
public class LogAspect {

    @Autowired
    private AdminLogService adminLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(logRecord)")
    public Object around(ProceedingJoinPoint joinPoint, LogRecord logRecord) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            try {
                saveLog(joinPoint, logRecord, result, error, startTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, LogRecord logRecord,
                         Object result, Exception error, long startTime) {
        try {
            // 获取当前用户
            MyUser currentUser = null;
            try {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof MyUser) {
                    currentUser = (MyUser) principal;
                }
            } catch (Exception e) {
                return;
            }

            if (currentUser == null) {
                return;
            }

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();

            AdminLog adminLog = new AdminLog();

            // 操作人信息
            adminLog.setOperatorId(currentUser.getId());
            adminLog.setOperatorName(currentUser.getUsername());
            adminLog.setOperatorIp(getClientIp());

            // 操作信息
            adminLog.setModule(logRecord.module());
            adminLog.setAction(logRecord.action());

            // ✅ 修复：正确解析 SpEL 表达式
            String description = parseDescription(logRecord.description(), method, args);
            adminLog.setDescription(description);

            // 方法信息
            adminLog.setMethodName(signature.getDeclaringTypeName() + "." + signature.getName());

            // 请求参数
            if (logRecord.recordParams()) {
                Object[] filteredArgs = filterArgs(args);
                adminLog.setRequestParams(truncate(objectMapper.writeValueAsString(filteredArgs), 2000));
            }

            // 返回结果
            if (logRecord.recordResult() && result != null) {
                adminLog.setResponseResult(truncate(objectMapper.writeValueAsString(result), 500));
            }

            // 执行状态
            adminLog.setExecuteTime(System.currentTimeMillis() - startTime);
            adminLog.setStatus(error == null ? 1 : 0);
            if (error != null) {
                adminLog.setErrorMsg(truncate(error.getMessage(), 500));
            }

            adminLog.setCreateTime(LocalDateTime.now());
            adminLogService.saveLog(adminLog);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析 SpEL 表达式
     */
    private String parseDescription(String description, Method method, Object[] args) {
        if (description == null || description.isEmpty()) {
            return description;
        }

        // 如果不包含 #，直接返回
        if (!description.contains("#")) {
            return description;
        }

        try {
            // 创建 EvaluationContext
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 获取参数名
            String[] paramNames = discoverer.getParameterNames(method);

            // 设置参数变量
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }

            // 额外设置 args 数组
            context.setVariable("args", args);

            // ✅ 关键：解析模板字符串中的 SpEL 表达式
            String result = description;

            // 匹配 #{...} 并逐个替换
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\{([^}]+)\\}");
            java.util.regex.Matcher matcher = pattern.matcher(description);

            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String expression = matcher.group(1);
                try {
                    Expression expr = parser.parseExpression(expression);
                    Object value = expr.getValue(context);
                    matcher.appendReplacement(sb, value != null ? value.toString() : "null");
                } catch (Exception e) {
                    // 解析失败，保留原样
                    matcher.appendReplacement(sb, matcher.group(0));
                }
            }
            matcher.appendTail(sb);

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return description;
        }
    }

    /**
     * 过滤掉不能序列化的参数
     */
    private Object[] filterArgs(Object[] args) {
        if (args == null) return new Object[0];
        java.util.List<Object> filtered = new java.util.ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof jakarta.servlet.http.HttpServletRequest ||
                    arg instanceof jakarta.servlet.http.HttpServletResponse ||
                    arg instanceof org.springframework.web.multipart.MultipartFile) {
                filtered.add("[非序列化参数]");
            } else {
                filtered.add(arg);
            }
        }
        return filtered.toArray();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "未知";
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}