package com.gitcode.mcsm_backend.aspect;

import com.gitcode.mcsm_backend.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 返回值校验切面 - 检查 Controller 返回的 Result 是否包含异常数据
 */
@Slf4j
@Aspect
@Component
public class ResultCheckAspect {

    // 1. 定义切入点：拦截 com.gitcode.mcsm_backend.service 包下所有返回值为 Result 的方法
    @Pointcut("execution(com.gitcode.mcsm_backend.common.Result com.gitcode.mcsm_backend.service.*.*(..))")
    public void serviceResultPointcut() {}

    // 2. 在方法返回结果后执行检测
    @AfterReturning(pointcut = "serviceResultPointcut()", returning = "result")
    public void afterServiceReturn(JoinPoint joinPoint, Object result) {
        if (result instanceof Result) {
            Result<?> res = (Result<?>) result;

            // 如果业务代码不是 2000，则在控制台打印详情
            if (res.getCode() != 2000) {
                String methodName = joinPoint.getSignature().getName();
                String className = joinPoint.getTarget().getClass().getSimpleName();

                log.error("----------------------------------------");
                log.error("[插件业务报错日志]");
                log.error("来自类: {}", className);
                log.error("调用方法: {}", methodName);
                log.error("状态码: {}", res.getCode());
                log.error("错误原因: {}", res.getMsg());
                log.error("----------------------------------------");
            }
        }
    }
}