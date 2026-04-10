package com.maidc.common.log.aspect;

import com.maidc.common.log.annotation.OperLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String module = operationLog.module();
        String operation = operationLog.operation();

        log.info("[操作日志] 开始 - 模块={}, 操作={}", module, operation);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[操作日志] 成功 - 模块={}, 操作={}, {}.{} 耗时={}ms",
                    module, operation, className, methodName, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[操作日志] 失败 - 模块={}, 操作={}, {}.{} 耗时={}ms, 错误={}",
                    module, operation, className, methodName, elapsed, e.getMessage());
            throw e;
        }
    }
}
