package com.maidc.common.security.annotation;

import com.maidc.common.security.scope.DataScope;

import java.lang.annotation.*;

/** 方法级功能权限；dataScope 声明该接口额外受数据范围约束（切面只校验功能码，范围过滤由 Service 层 Helper 完成） */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 权限码，模块:资源:动作 */
    String value();
    /** 该接口的数据范围语义（文档性声明，供 ArchUnit 与 code review 检查） */
    DataScope scope() default DataScope.ALL;
}
