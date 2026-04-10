# MAIDC API 契约文档

> **版本**: v1.0
> **日期**: 2026-04-11
> **状态**: 已确认
> **关联**: PRD `docs/superpowers/specs/2026-04-08-maidc-design.md`

---

## 目录

1. [API 通用规范](#1-api-通用规范)
2. [认证接口](#2-认证接口-auth-service-8081)
3. [模型管理接口](#3-模型管理接口-model-service-8083-phase-1)
4. [数据管理接口](#4-数据管理接口-data-service-8082)
5. [任务调度接口](#5-任务调度接口-task-service-8084)
6. [标注服务接口](#6-标注服务接口-label-service-8085)
7. [审计日志接口](#7-审计日志接口-audit-service-8086)
8. [消息通知接口](#8-消息通知接口-msg-service-8087)
9. [AI Worker 接口](#9-ai-worker-接口-aiworker-8090)

---

## 1. API 通用规范

### 1.1 URL 规范

```
基础路径: /api/v1/{resource}
版本控制: URL 路径版本 v1
资源命名: 小写复数名词 /api/v1/models
关联资源: /api/v1/models/{id}/versions
```

### 1.2 HTTP 方法语义

| 方法 | 语义 | 幂等 | 示例 |
|------|------|------|------|
| GET | 查询资源 | 是 | `GET /api/v1/models` |
| POST | 创建资源 | 否 | `POST /api/v1/models` |
| PUT | 全量更新 | 是 | `PUT /api/v1/models/{id}` |
| DELETE | 删除资源 | 是 | `DELETE /api/v1/models/{id}` |

### 1.3 统一请求头

| Header | 必填 | 说明 |
|--------|------|------|
| `Authorization` | 是 | `Bearer {access_token}` |
| `X-Trace-Id` | 否 | 链路追踪ID（网关自动生成） |
| `X-Org-Id` | 否 | 机构ID（网关从JWT提取注入） |
| `Content-Type` | 是 | `application/json` |

### 1.4 统一响应体

```json
{
    "code": 200,
    "message": "success",
    "data": { },
    "trace_id": "a1b2c3d4-e5f6-7890"
}
```

### 1.5 分页响应格式

```json
{
    "code": 200,
    "data": {
        "items": [],
        "total": 1000,
        "page": 1,
        "page_size": 20,
        "total_pages": 50
    }
}
```

### 1.6 错误码体系

| 错误码 | 含义 | 说明 |
|--------|------|------|
| 200 | 成功 | |
| 400 | 请求参数错误 | 字段校验失败 |
| 401 | 未认证 | Token 缺失或过期 |
| 403 | 无权限 | RBAC 校验不通过 |
| 404 | 资源不存在 | |
| 409 | 资源冲突 | 如版本号重复 |
| 422 | 数据验证失败 | 业务规则校验失败 |
| 429 | 请求限流 | Sentinel 限流触发 |
| 500 | 服务内部错误 | |
| 503 | 服务不可用 | 服务熔断 |

### 1.7 日期时间格式

所有日期时间字段使用 ISO 8601 格式：`2026-04-11T10:00:00Z`

### 1.8 全局枚举值

**模型状态 (ModelStatus)**: `DRAFT` | `REGISTERED` | `PUBLISHED` | `DEPRECATED`

**版本状态 (VersionStatus)**: `CREATED` | `TRAINING` | `EVALUATING` | `APPROVED` | `DEPLOYED` | `DEPRECATED`

**部署状态 (DeployStatus)**: `CREATING` | `RUNNING` | `STOPPING` | `STOPPED` | `FAILED`

**评估状态 (EvalStatus)**: `PENDING` | `RUNNING` | `COMPLETED` | `FAILED`

**审批状态 (ApprovalStatus)**: `PENDING` | `APPROVED` | `REJECTED`

**告警状态 (AlertStatus)**: `FIRING` | `ACKNOWLEDGED` | `RESOLVED`

**模型类型 (ModelType)**: `IMAGING` | `NLP` | `GENOMIC` | `STRUCTURED` | `MULTIMODAL`

**框架 (Framework)**: `PYTORCH` | `TENSORFLOW` | `SKLEARN` | `XGBOOST` | `ONNX` | `OTHER`

**就诊类型**: `OUTPATIENT` | `INPATIENT` | `EMERGENCY`

---

## 2. 认证接口 (auth-service :8081)

### 2.1 登录

```
POST /api/v1/auth/login
```

**请求体**:
```json
{
    "username": "admin",
    "password": "P@ssw0rd123"
}
```

**响应** `200 OK`:
```json
{
    "code": 200,
    "data": {
        "access_token": "eyJhbGciOiJIUzI1NiJ9...",
        "refresh_token": "eyJhbGciOiJIUzI1NiJ9...",
        "token_type": "Bearer",
        "expires_in": 7200,
        "user": {
            "id": 1,
            "username": "admin",
            "real_name": "系统管理员",
            "roles": ["admin"],
            "org_id": 1
        }
    }
}
```

### 2.2 刷新令牌

```
POST /api/v1/auth/refresh
```

**请求体**:
```json
{
    "refresh_token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**响应** `200 OK`:
```json
{
    "code": 200,
    "data": {
        "access_token": "eyJhbGciOiJIUzI1NiJ9...",
        "expires_in": 7200
    }
}
```

### 2.3 登出

```
POST /api/v1/auth/logout
```

将当前 Token 加入黑名单。

**响应** `200 OK`:
```json
{ "code": 200, "message": "success" }
```

### 2.4 用户管理

```
GET    /api/v1/users?page=1&page_size=20&keyword=张&status=ACTIVE
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PUT    /api/v1/users/{id}/reset-password
```

**创建用户** `POST /api/v1/users`:
```json
{
    "username": "zhangsan",
    "password": "Init@123",
    "real_name": "张三",
    "email": "zhangsan@hospital.com",
    "phone": "13800138000",
    "role_ids": [2, 3],
    "org_id": 1,
    "must_change_pwd": true
}
```

**响应** `201 Created`:
```json
{
    "code": 201,
    "data": {
        "id": 10,
        "username": "zhangsan",
        "real_name": "张三",
        "status": "ACTIVE",
        "created_at": "2026-04-11T10:00:00Z"
    }
}
```

### 2.5 角色与权限

```
GET    /api/v1/roles
POST   /api/v1/roles
PUT    /api/v1/roles/{id}
PUT    /api/v1/roles/{id}/permissions
GET    /api/v1/permissions/tree
```

**权限树响应** `GET /api/v1/permissions/tree`:
```json
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "code": "model",
            "name": "模型管理",
            "children": [
                { "id": 11, "code": "model:create", "name": "创建模型", "resource_type": "API" },
                { "id": 12, "code": "model:update", "name": "更新模型", "resource_type": "API" },
                { "id": 13, "code": "model:delete", "name": "删除模型", "resource_type": "API" },
                { "id": 14, "code": "model:deploy", "name": "部署模型", "resource_type": "API" }
            ]
        }
    ]
}
```

---

## 3. 模型管理接口 (model-service :8083) ★Phase 1

### 3.1 模型 CRUD

**注册模型** `POST /api/v1/models`:
```json
{
    "model_code": "LUNG_NODULE_DET_001",
    "model_name": "肺结节检测模型",
    "description": "基于3D CNN的肺结节检测模型，支持CT影像",
    "model_type": "IMAGING",
    "task_type": "OBJECT_DETECTION",
    "framework": "PYTORCH",
    "input_schema": {
        "type": "object",
        "properties": {
            "image": { "type": "dicom_series", "description": "CT影像序列" }
        },
        "required": ["image"]
    },
    "output_schema": {
        "type": "array",
        "items": {
            "type": "object",
            "properties": {
                "bbox": { "type": "array", "items": "number" },
                "confidence": { "type": "number" },
                "label": { "type": "string" }
            }
        }
    },
    "tags": "肺部,CT,结节检测,3D-CNN",
    "license": "MIT",
    "project_id": 1
}
```

**响应** `201 Created`:
```json
{
    "code": 201,
    "data": {
        "id": 1,
        "model_code": "LUNG_NODULE_DET_001",
        "model_name": "肺结节检测模型",
        "status": "DRAFT",
        "created_at": "2026-04-11T10:00:00Z"
    }
}
```

**查询模型列表** `GET /api/v1/models?page=1&page_size=20&model_type=IMAGING&status=PUBLISHED&keyword=肺结节`:
```json
{
    "code": 200,
    "data": {
        "items": [
            {
                "id": 1,
                "model_code": "LUNG_NODULE_DET_001",
                "model_name": "肺结节检测模型",
                "model_type": "IMAGING",
                "task_type": "OBJECT_DETECTION",
                "framework": "PYTORCH",
                "status": "PUBLISHED",
                "latest_version": "v1.2.0",
                "tags": ["肺部", "CT"],
                "owner_name": "张三",
                "created_at": "2026-04-01T10:00:00Z",
                "updated_at": "2026-04-11T15:30:00Z"
            }
        ],
        "total": 1,
        "page": 1,
        "page_size": 20
    }
}
```

**获取模型详情** `GET /api/v1/models/{id}`:
```json
{
    "code": 200,
    "data": {
        "id": 1,
        "model_code": "LUNG_NODULE_DET_001",
        "model_name": "肺结节检测模型",
        "description": "...",
        "model_type": "IMAGING",
        "framework": "PYTORCH",
        "input_schema": { },
        "output_schema": { },
        "status": "PUBLISHED",
        "owner": { "id": 1, "name": "张三" },
        "project": { "id": 1, "name": "AI辅助诊断研究" },
        "version_count": 3,
        "latest_version": { "version_no": "v1.2.0", "status": "APPROVED" },
        "active_deployment": { "id": 1, "status": "RUNNING", "endpoint_url": "http://aiworker:8090/v1/infer/lung-nodule" },
        "statistics": { "total_inferences": 15234, "avg_latency_ms": 245, "last_24h_inferences": 567 }
    }
}
```

**更新模型** `PUT /api/v1/models/{id}`:
```json
{ "description": "更新描述", "tags": "肺部,CT,结节检测,v2" }
```

**删除模型** `DELETE /api/v1/models/{id}`: 软删除

### 3.2 版本管理

**创建版本** `POST /api/v1/models/{id}/versions` (multipart/form-data):
```
version_no: "v1.3.0"
description: "优化小结节检测"
changelog: "1. 优化小结节召回率\n2. 减少假阳性"
model_file: <文件上传 .pt/.onnx/.pkl>
config_file: <可选 配置文件>
hyper_params: '{"learning_rate": 0.001, "epochs": 100, "batch_size": 16}'
```

**响应** `201 Created`:
```json
{
    "code": 201,
    "data": {
        "id": 10,
        "model_id": 1,
        "version_no": "v1.3.0",
        "status": "CREATED",
        "model_file_size": 524288000,
        "checksum": "sha256:abc123..."
    }
}
```

**版本列表** `GET /api/v1/models/{id}/versions?page=1&page_size=10`

**版本详情** `GET /api/v1/models/{id}/versions/{vid}`

**版本对比** `GET /api/v1/models/{id}/versions/compare?v1=3&v2=5`:
```json
{
    "code": 200,
    "data": {
        "v1": { "version_no": "v1.2.0", "metrics": {"auc": 0.91}, "hyper_params": {} },
        "v2": { "version_no": "v1.3.0", "metrics": {"auc": 0.92}, "hyper_params": {} },
        "diff": { "auc_delta": 0.01, "param_changes": [] }
    }
}
```

### 3.3 模型评估

**创建评估** `POST /api/v1/evaluations`:
```json
{
    "model_version_id": 10,
    "eval_name": "v1.3.0外部验证集评估",
    "eval_type": "EXTERNAL",
    "dataset_id": 5,
    "metrics_config": {
        "metrics": ["auc", "f1", "precision", "recall"],
        "confidence_threshold": 0.5,
        "iou_threshold": 0.3
    }
}
```

**响应** `201 Created`:
```json
{ "code": 201, "data": { "id": 20, "status": "PENDING", "estimated_duration_min": 30 } }
```

**评估详情** `GET /api/v1/evaluations/{id}`:
```json
{
    "code": 200,
    "data": {
        "id": 20,
        "eval_name": "v1.3.0外部验证集评估",
        "status": "COMPLETED",
        "metrics": { "auc": 0.9234, "f1": 0.8912, "precision": 0.9045, "recall": 0.8786 },
        "confusion_matrix": { "TP": 442, "FP": 46, "FN": 61, "TN": 951 },
        "started_at": "2026-04-11T10:00:00Z",
        "finished_at": "2026-04-11T10:28:45Z",
        "dataset": { "id": 5, "name": "外部验证集-2026Q1", "record_count": 1500 },
        "report_url": "/api/v1/evaluations/20/report"
    }
}
```

**评估报告** `GET /api/v1/evaluations/{id}/report` → PDF 文件下载

### 3.4 审批流程

**提交审批** `POST /api/v1/approvals`:
```json
{
    "model_version_id": 10,
    "approval_type": "DEPLOY",
    "evidence_docs": [
        {"name": "评估报告.pdf", "path": "/docs/eval.pdf", "type": "EVALUATION"},
        {"name": "临床验证.pdf", "path": "/docs/clinical.pdf", "type": "CLINICAL"}
    ],
    "risk_assessment": "低风险，AUC>0.92，误诊率<5%",
    "clinical_validation": "已完成500例回顾性验证"
}
```

**审批操作** `PUT /api/v1/approvals/{id}/review`:
```json
{ "result": "APPROVED", "result_comment": "模型性能达标，同意上线部署" }
```

**审批详情** `GET /api/v1/approvals/{id}`

### 3.5 部署管理

**创建部署** `POST /api/v1/deployments`:
```json
{
    "model_version_id": 10,
    "deployment_name": "肺结节检测-生产环境",
    "deployment_type": "ONLINE",
    "serving_framework": "TRITON",
    "resource_config": { "cpu": "4", "memory": "16Gi", "gpu": 1, "gpu_type": "NVIDIA_T4" },
    "env_vars": { "MODEL_BATCH_SIZE": "8", "MODEL_MAX_LATENCY_MS": "500" },
    "auto_scale": true,
    "min_replicas": 1,
    "max_replicas": 5
}
```

**部署操作**:
```
GET    /api/v1/deployments/{id}/status     # 查询状态
PUT    /api/v1/deployments/{id}/start       # 启动
PUT    /api/v1/deployments/{id}/stop        # 停止
PUT    /api/v1/deployments/{id}/scale       # 扩缩容
POST   /api/v1/deployments/{id}/restart     # 重启
```

**扩缩容** `PUT /api/v1/deployments/{id}/scale`:
```json
{ "target_replicas": 3, "resource_config": { "cpu": "8", "memory": "32Gi", "gpu": 2 } }
```

**部署列表** `GET /api/v1/deployments?status=RUNNING`

### 3.6 流量路由

**创建路由** `POST /api/v1/routes`:
```json
{
    "route_name": "肺结节检测-金丝雀发布",
    "model_id": 1,
    "route_type": "CANARY",
    "default_deployment_id": 1,
    "config": [
        {"deployment_id": 1, "weight": 90},
        {"deployment_id": 3, "weight": 10}
    ],
    "traffic_rules": { "canary_percentage": 10, "success_threshold": 0.95, "auto_promote": false }
}
```

**更新路由** `PUT /api/v1/routes/{id}`

**路由列表** `GET /api/v1/routes?model_id=1`

### 3.7 推理调用

**同步推理** `POST /api/v1/inference/{deployment_id}`:
```json
{
    "request_id": "req-uuid-001",
    "patient_id": 12345,
    "encounter_id": 67890,
    "input": {
        "image_url": "minio://maidc/dicom/2026/04/11/CT001.dcm",
        "parameters": { "confidence_threshold": 0.5, "nms_threshold": 0.3 }
    }
}
```

**响应** `200 OK`:
```json
{
    "code": 200,
    "data": {
        "request_id": "req-uuid-001",
        "results": [
            {
                "bbox": [120.5, 85.3, 12.7, 11.2, 8.5],
                "confidence": 0.94,
                "label": "nodule",
                "classification": "suspicious",
                "volume_mm3": 1247.8
            }
        ],
        "latency_ms": 234,
        "model_version": "v1.3.0"
    }
}
```

### 3.8 监控指标

**推理日志查询** `GET /api/v1/monitoring/deployments/{id}/logs?page=1&page_size=50&start_time=2026-04-11T00:00:00Z&end_time=2026-04-11T23:59:59Z&status=ERROR`

**运行指标查询** `GET /api/v1/monitoring/deployments/{id}/metrics?metric_name=latency_p99&start_time=...&end_time=...&interval=5m`:
```json
{
    "code": 200,
    "data": {
        "metric_name": "latency_p99",
        "interval": "5m",
        "data_points": [
            {"time": "2026-04-11T10:00:00Z", "value": 312.5},
            {"time": "2026-04-11T10:05:00Z", "value": 298.3}
        ]
    }
}
```

### 3.9 告警管理

**创建告警规则** `POST /api/v1/alert-rules`:
```json
{
    "rule_name": "推理延迟告警",
    "deployment_id": 3,
    "metric_name": "latency_p99",
    "condition": "GT",
    "threshold": 500,
    "duration_sec": 300,
    "severity": "WARNING",
    "notify_channels": ["email", "webhook"],
    "notify_users": [1, 2, 3]
}
```

**告警操作**:
```
GET  /api/v1/alerts?status=FIRING           # 活跃告警
PUT  /api/v1/alerts/{id}/acknowledge        # 确认告警
GET  /api/v1/alerts/history?rule_id=1        # 告警历史
GET  /api/v1/alert-rules?deployment_id=3     # 告警规则列表
PUT  /api/v1/alert-rules/{id}                # 更新规则
```

---

## 4. 数据管理接口 (data-service :8082)

### 4.1 CDR 患者管理

```
GET    /api/v1/cdr/patients?page=1&page_size=20&keyword=张&gender=M&source_system=HIS
GET    /api/v1/cdr/patients/{id}
GET    /api/v1/cdr/patients/{id}/encounters
GET    /api/v1/cdr/patients/{id}/360                          ← 患者360视图（聚合全部信息）
POST   /api/v1/cdr/patients                                   ← 手动录入
PUT    /api/v1/cdr/patients/{id}
GET    /api/v1/cdr/encounters/{id}                             ← 就诊详情含关联数据
GET    /api/v1/cdr/encounters/{id}/diagnoses
GET    /api/v1/cdr/encounters/{id}/lab-results
GET    /api/v1/cdr/encounters/{id}/medications
GET    /api/v1/cdr/encounters/{id}/imaging
GET    /api/v1/cdr/encounters/{id}/vital-signs
GET    /api/v1/cdr/encounters/{id}/clinical-notes
```

**患者列表响应**（脱敏）:
```json
{
    "code": 200,
    "data": {
        "items": [
            {
                "id": 1,
                "patient_no": "P20260001",
                "name": "张**",
                "gender": "M",
                "age": 65,
                "phone": "138****1234",
                "last_visit": "2026-04-10T08:30:00Z"
            }
        ],
        "total": 156,
        "page": 1,
        "page_size": 20
    }
}
```

### 4.2 RDR 科研管理

```
POST   /api/v1/rdr/projects                    # 创建研究项目
GET    /api/v1/rdr/projects?page=1&status=ACTIVE
GET    /api/v1/rdr/projects/{id}
PUT    /api/v1/rdr/projects/{id}
POST   /api/v1/rdr/projects/{id}/members        # 邀请成员
GET    /api/v1/rdr/projects/{id}/cohorts         # 队列列表
POST   /api/v1/rdr/projects/{id}/cohorts         # 创建队列
POST   /api/v1/rdr/datasets                     # 创建数据集
GET    /api/v1/rdr/datasets/{id}
POST   /api/v1/rdr/datasets/{id}/versions        # 发布版本
GET    /api/v1/rdr/datasets/{id}/access-log      # 访问日志
```

**创建研究项目**:
```json
{
    "project_code": "AI_LUNG_2026",
    "project_name": "AI辅助肺结节诊断研究",
    "research_type": "AI",
    "principal_investigator": "李主任",
    "start_date": "2026-05-01",
    "end_date": "2027-04-30",
    "ethics_approval": "EC-2026-0123",
    "funding_source": "国家重点研发计划"
}
```

### 4.3 ETL 任务

```
POST   /api/v1/etl/tasks                        # 创建ETL任务
GET    /api/v1/etl/tasks?project_id=1&status=RUNNING
POST   /api/v1/etl/tasks/{id}/execute            # 执行ETL
POST   /api/v1/etl/tasks/{id}/pause              # 暂停
GET    /api/v1/etl/tasks/{id}/logs               # 执行日志
```

### 4.4 数据质量

```
POST   /api/v1/quality/rules                    # 创建质量规则
GET    /api/v1/quality/rules?target_table=cdr.c_patient
GET    /api/v1/quality/results?rule_id=1&is_passed=false
POST   /api/v1/quality/rules/{id}/execute        # 手动执行检测
```

### 4.5 数据字典

```
GET    /api/v1/dict/types                        # 字典类型列表
GET    /api/v1/dict/types/{type}/items           # 字典项
POST   /api/v1/dict/types/{type}/items           # 新增字典项
```

### 4.6 数据脱敏

```
GET    /api/v1/desensitize/rules                 # 脱敏规则列表
POST   /api/v1/desensitize/rules                 # 创建规则
POST   /api/v1/desensitize/preview               # 预览脱敏效果
POST   /api/v1/desensitize/export                # 申请导出（需审批）
```

---

## 5. 任务调度接口 (task-service :8084)

```
GET    /api/v1/tasks                             # 任务列表
POST   /api/v1/tasks                             # 创建定时任务
PUT    /api/v1/tasks/{id}                        # 更新任务
POST   /api/v1/tasks/{id}/trigger                # 手动触发
POST   /api/v1/tasks/{id}/pause                  # 暂停
GET    /api/v1/tasks/{id}/executions             # 执行记录
GET    /api/v1/tasks/{id}/executions/{eid}       # 执行详情
```

**创建定时任务**:
```json
{
    "task_name": "CDR每日增量同步",
    "task_type": "CDR_SYNC",
    "cron_expression": "0 0 2 * * ?",
    "params": { "source_system": "HIS", "sync_mode": "INCREMENTAL" },
    "retry_count": 3,
    "retry_interval_sec": 60,
    "notify_on_fail": true
}
```

---

## 6. 标注服务接口 (label-service :8085)

```
GET    /api/v1/labels/tasks                      # 标注任务列表
POST   /api/v1/labels/tasks                      # 创建标注任务
GET    /api/v1/labels/tasks/{id}                  # 任务详情
PUT    /api/v1/labels/tasks/{id}/assign           # 分配标注员
POST   /api/v1/labels/tasks/{id}/ai-preannotate   # AI预标注
GET    /api/v1/labels/tasks/{id}/stats            # 标注统计
```

**创建标注任务**:
```json
{
    "task_name": "肺结节CT影像标注-批次1",
    "annotation_type": "BBOX",
    "dataset_id": 5,
    "items": ["imaging_dataset_id_1", "imaging_dataset_id_2"],
    "assignees": [3, 4],
    "reviewers": [5],
    "deadline": "2026-05-01"
}
```

---

## 7. 审计日志接口 (audit-service :8086)

```
GET    /api/v1/audit/operations                  # 操作审计列表
GET    /api/v1/audit/operations/{id}              # 操作详情
GET    /api/v1/audit/data-access                  # 数据访问审计
GET    /api/v1/audit/events                       # 系统事件
GET    /api/v1/audit/reports/compliance           # 合规报表
```

**操作审计查询参数**: `user_id`, `operation`, `resource_type`, `start_time`, `end_time`, `status`

**响应**:
```json
{
    "code": 200,
    "data": {
        "items": [
            {
                "id": 1,
                "trace_id": "a1b2c3d4",
                "user_id": 1,
                "username": "admin",
                "service_name": "model-service",
                "operation": "CREATE",
                "resource_type": "Model",
                "resource_id": "1",
                "request_url": "/api/v1/models",
                "response_code": 201,
                "duration_ms": 45,
                "status": "SUCCESS",
                "created_at": "2026-04-11T10:00:00Z"
            }
        ],
        "total": 500,
        "page": 1,
        "page_size": 20
    }
}
```

---

## 8. 消息通知接口 (msg-service :8087)

```
GET    /api/v1/messages?status=UNREAD&page=1     # 消息列表
GET    /api/v1/messages/{id}                      # 消息详情
PUT    /api/v1/messages/{id}/read                  # 标记已读
PUT    /api/v1/messages/read-all                   # 全部已读
GET    /api/v1/notifications/settings              # 通知偏好设置
PUT    /api/v1/notifications/settings              # 更新设置
GET    /api/v1/notifications/templates             # 消息模板
POST   /api/v1/notifications/templates             # 创建模板
```

**消息列表响应**:
```json
{
    "code": 200,
    "data": {
        "items": [
            {
                "id": 1,
                "type": "APPROVAL",
                "title": "模型审批待处理",
                "content": "模型版本[v1.3.0]等待您的技术评审",
                "status": "UNREAD",
                "sender": "系统",
                "created_at": "2026-04-11T14:00:00Z"
            }
        ],
        "total": 3,
        "unread_count": 2
    }
}
```

---

## 9. AI Worker 接口 (aiworker :8090)

### 9.1 同步推理（内部调用）

```
POST /v1/infer/{model_code}
```

```json
{
    "model_version": "v1.3.0",
    "input": { "image_url": "minio://...", "parameters": {} }
}
```

### 9.2 健康检查

```
GET /health
```

### 9.3 模型加载

```
POST /v1/serving/load
```

```json
{
    "model_file_path": "models/1/1/v1.3.0/model.pt",
    "framework": "PYTORCH",
    "resource_config": { "gpu": 1, "memory": "8Gi" }
}
```

### 9.4 Worker 状态

```
GET /v1/workers                   # Worker 节点列表
GET /v1/workers/{id}/status       # 节点状态（GPU利用率/任务队列/内存）
```

---

## 接口统计

| 服务 | 接口数量 | Phase |
|------|----------|-------|
| auth-service | 9 | 1 |
| **model-service** | **36** | **1** |
| data-service | 28 | 2 |
| task-service | 7 | 3 |
| label-service | 6 | 4 |
| audit-service | 5 | 4 |
| msg-service | 8 | 4 |
| aiworker | 4 | 1 |
| **合计** | **~103** | |

---

> **文档结束** - MAIDC API 契约文档 v1.0
