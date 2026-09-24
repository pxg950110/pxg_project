package com.maidc.common.security.scope;

import java.util.Objects;

public enum DataScope {
    /** 不过滤 */
    ALL,
    /** 本科室（患者就诊科室 ∈ 用户 dept_id） */
    DEPT,
    /** 本人创建（created_by = userId） */
    SELF,
    /** 项目成员（r_study_member 含用户）∪ 本人创建 */
    PROJECT;

    /** 多角色取更宽的范围（ALL > DEPT > PROJECT > SELF）；null 入参视为编程错误，快速失败（调用方 Task 4 用 reduce(SELF, widest)，null 不会进入本方法） */
    public static DataScope widest(DataScope a, DataScope b) {
        Objects.requireNonNull(a, "dataScope");
        Objects.requireNonNull(b, "dataScope");
        if (a == ALL || b == ALL) return ALL;
        if (a == DEPT || b == DEPT) return DEPT;
        if (a == PROJECT || b == PROJECT) return PROJECT;
        return SELF;
    }
}
