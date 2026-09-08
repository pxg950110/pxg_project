package com.maidc.common.security.annotation;

import com.maidc.common.security.scope.DataScope;

import java.lang.annotation.*;

/**
 * 方法级功能权限；dataScope 声明该接口额外受数据范围约束（切面只校验功能码，范围过滤由 Service 层 Helper 完成）。
 * <p>自递归防护：maidc-auth 的内部端点 /api/v1/internal/permissions/** 必须标注 {@link PublicEndpoint}，
 * 绝不可标注 @RequirePermission —— 否则 Redis miss 时 PermissionStore 懒加载会经本切面对自身发起 HTTP 调用，形成无限递归。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 权限码，模块:资源:动作 */
    String value();
    /** 该接口的数据范围语义（文档性声明，供 ArchUnit 与 code review 检查） */
    DataScope scope() default DataScope.ALL;
}
