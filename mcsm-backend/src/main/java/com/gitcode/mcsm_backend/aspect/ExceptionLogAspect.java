package com.gitcode.mcsm_backend.aspect;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;


import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
/**
 * 异常日志切面 - 拦截 Controller 层未捕获异常，记录详细错误信息
 */
@Component
@Slf4j
public class ExceptionLogAspect {

    // 1. 定义切点：拦截 controller 包及其子包下的所有方法
    @Pointcut("execution(* com.gitcode.mcsm_backend.controller..*.*(..))")
    public void controllerPointcut() {}

    // 2. 异常通知：只有抛出异常时才会触发
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        // 获取当前请求的详情（路径、方法等）
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        log.error("=================== [发生错误] ===================");
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            log.error("URL    : {}", request.getRequestURL().toString());
            log.error("HTTP Method : {}", request.getMethod());
        }

        // 打印发生异常的类名和方法名
        log.error("方法: {}.{}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());

        // 打印核心：具体的堆栈信息（IDE 控制台看这里）
        log.error("报错详情: ", e);

        log.error("=================================================================");
    }
}