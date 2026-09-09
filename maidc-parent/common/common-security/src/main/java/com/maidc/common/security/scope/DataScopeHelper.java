package com.maidc.common.security.scope;

import com.maidc.common.security.context.PermissionContext;

import java.util.List;

/** 数据范围判断助手：Service 查询前调用，据此拼 WHERE（JPA Specification 或 QueryDSL/原生条件） */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    public static boolean needDeptFilter(PermissionContext ctx) {
        return ctx != null && ctx.getDataScope() == DataScope.DEPT;
    }

    public static Long deptId(PermissionContext ctx) {
        return ctx == null ? null : ctx.getDeptId();
    }

    public static Long selfUserId(PermissionContext ctx) {
        return ctx == null ? null : ctx.getUserId();
    }

    public static List<Long> projectIds(PermissionContext ctx) {
        return ctx == null || ctx.getProjectIds() == null ? List.of() : List.copyOf(ctx.getProjectIds());
    }
}
