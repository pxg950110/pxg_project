package com.maidc.common.security.annotation;

import java.lang.annotation.*;

/** 显式声明无需权限注解的端点（健康检查/内部端点等），ArchUnit 白名单依据 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {
    String reason() default "";
}
