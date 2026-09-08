package com.maidc.common.log.aspect;

import com.maidc.common.log.annotation.OperLog;
import com.maidc.common.log.model.OperationLogData;
import com.maidc.common.log.model.OperationLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
public class OperationLogAspect {

    private ApplicationEventPublisher eventPublisher;

    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) joinPoint.getSignature()).getMethod().getName();
        String serviceName = operationLog.module();
        String operation = operationLog.operation();

        log.info("[操作日志] 开始 - 服务={}, 操作={}", serviceName, operation);

        Object result = null;
        Exception error = null;
        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[操作日志] 成功 - 服务={}, 操作={}, {}.{} 耗时={}ms",
                    serviceName, operation, className, methodName, elapsed);
            return result;
        } catch (Exception e) {
            error = e;
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[操作日志] 失败 - 服务={}, 操作={}, {}.{} 耗时={}ms, 错误={}",
                    serviceName, operation, className, methodName, elapsed, e.getMessage());
            throw e;
        } finally {
            publishLogEvent(joinPoint, operationLog, startTime, error);
        }
    }

    private void publishLogEvent(ProceedingJoinPoint joinPoint, OperLog operationLog,
                                  long startTime, Exception error) {
        if (eventPublisher == null) {
            return;
        }

        try {
            long elapsed = System.currentTimeMillis() - startTime;
            HttpServletRequest request = getCurrentRequest();

            OperationLogData logData = OperationLogData.builder()
                    .serviceName(operationLog.module())
                    .operation(operationLog.operation())
                    .resourceType(operationLog.resourceType())
                    .resourceId(null)
                    .resourceName(null)
                    .requestMethod(request != null ? request.getMethod() : null)
                    .requestUrl(request != null ? request.getRequestURI() : null)
                    .requestParams(extractParams(joinPoint))
                    .durationMs((int) elapsed)
                    .status(error == null ? "SUCCESS" : "FAILURE")
                    .errorMessage(error != null ? error.getMessage() : null)
                    .ipAddress(request != null ? getClientIp(request) : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .traceId(getTraceId())
                    .username(resolveUsername(joinPoint, request))
                    .userId(resolveUserId(request))
                    .orgId(resolveOrgId(request))
                    .build();

            eventPublisher.publishEvent(new OperationLogEvent(this, logData));
        } catch (Exception e) {
            log.warn("[操作日志] 事件发布失败: {}", e.getMessage());
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String extractParams(ProceedingJoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest || arg instanceof String) {
                    continue;
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(arg != null ? arg.getClass().getSimpleName() : "null");
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFieldValue(ProceedingJoinPoint joinPoint, String fieldName) {
        try {
            for (Object arg : joinPoint.getArgs()) {
                if (arg == null || arg instanceof HttpServletRequest || arg instanceof String) {
                    continue;
                }
                try {
                    var field = arg.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(arg);
                    if (value instanceof String s && !s.isEmpty()) {
                        return s;
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String getTraceId() {
        try {
            Class<?> mdcClass = Class.forName("org.slf4j.MDC");
            var getMethod = mdcClass.getMethod("get", String.class);
            return (String) getMethod.invoke(null, "traceId");
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveUsername(ProceedingJoinPoint joinPoint, HttpServletRequest request) {
        String username = extractFieldValue(joinPoint, "username");
        if (username != null) return username;
        if (request != null) {
            username = request.getHeader("X-Username");
            if (username != null && !username.isEmpty()) return username;
        }
        return resolveFromSecurityContext("getName");
    }

    private Long resolveUserId(HttpServletRequest request) {
        if (request != null) {
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                try {
                    return Long.parseLong(userId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Long resolveOrgId(HttpServletRequest request) {
        if (request != null) {
            String orgId = request.getHeader("X-Org-Id");
            if (orgId != null && !orgId.isEmpty()) {
                try {
                    return Long.parseLong(orgId);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private String resolveFromSecurityContext(String methodName) {
        try {
            Class<?> holderClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            var getContextMethod = holderClass.getMethod("getContext");
            Object context = getContextMethod.invoke(null);
            if (context == null) return null;
            var getAuthMethod = context.getClass().getMethod("getAuthentication");
            Object auth = getAuthMethod.invoke(context);
            if (auth == null) return null;
            var getPrincipalMethod = auth.getClass().getMethod("getPrincipal");
            Object principal = getPrincipalMethod.invoke(auth);
            if (principal == null) return null;
            var nameMethod = principal.getClass().getMethod(methodName);
            Object result = nameMethod.invoke(principal);
            return result instanceof String s && !s.isEmpty() && !"anonymousUser".equals(s) ? s : null;
        } catch (Exception e) {
            return null;
        }
    }
}
