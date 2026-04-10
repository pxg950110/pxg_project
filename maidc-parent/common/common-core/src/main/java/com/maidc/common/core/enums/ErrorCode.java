package com.maidc.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ==================== 通用错误码 4xx ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // ==================== 模型管理 40xx ====================
    MODEL_NOT_FOUND(4001, "模型不存在"),
    MODEL_CODE_DUPLICATE(4002, "模型编码已存在"),
    VERSION_NOT_FOUND(4011, "模型版本不存在"),
    VERSION_NO_DUPLICATE(4012, "版本号已存在"),

    // ==================== 评测 & 审批 402x-403x ====================
    EVALUATION_NOT_FOUND(4021, "评测任务不存在"),
    APPROVAL_NOT_FOUND(4031, "审批记录不存在"),
    APPROVAL_NOT_PENDING(4032, "审批状态非待审批"),

    // ==================== 部署 404x ====================
    DEPLOYMENT_NOT_FOUND(4041, "部署实例不存在"),
    DEPLOYMENT_NOT_RUNNING(4042, "部署实例未运行"),

    // ==================== 数据中心 50xx ====================
    PATIENT_NOT_FOUND(5001, "患者不存在"),
    DATASET_NOT_FOUND(5011, "数据集不存在"),
    ETL_TASK_FAILED(5021, "ETL任务执行失败");

    private final int code;
    private final String message;
}
