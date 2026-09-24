# MAIDC 权限系统设计（RBAC + 数据范围 + 脱敏联动）

- 日期：2026-09-08
- 状态：已确认（设计经分节评审通过）
- 范围：功能权限（RBAC）扩展 + 数据范围（行级）控制 + 角色×脱敏联动 + 后端强制执行全覆盖

---

## 1. 背景与现状

### 1.1 已有基础（复用，不推翻）

| 层面 | 现状 |
|------|------|
| 数据库 | 经典 RBAC 5 表：`system.s_user` / `s_role` / `s_permission` / `s_user_role` / `s_role_permission`（含 `org_id`，`s_user_role.expires_at` 支持授权过期） |
| 种子角色 | 6 个：`admin` / `data_admin` / `researcher` / `ai_engineer` / `doctor` / `auditor` |
| 种子权限 | 仅 15 条粗粒度（菜单为主），覆盖不了 50+ Controller 功能面 |
| 网关 | `AuthFilter` 纯认证：JWT 验签 + Redis 黑名单，透传 `X-User-Id` / `X-Org-Id` / `X-Username` |
| JWT | claims 含 `userId` / `orgId` / `roles`（不含权限集） |
| 后端 | 仅 audit / auth / cdr 质控部分 Controller 有 `@PreAuthorize`，大部分接口登录即可调 |
| 前端 | `permission` store 按 `meta.permission` 过滤路由 + `PermissionWrapper` 按钮级控制，机制完整 |
| 脱敏 | `c_desensitize_rule` 规则引擎已有（按字段/场景配置，与角色无关） |

### 1.2 问题

1. 没有护士角色；科研只有一个粗粒度 `researcher`
2. 权限码太少，功能矩阵覆盖不全
3. 后端无统一强制鉴权，越权风险高
4. 无行级数据范围：任何登录用户可看全院患者
5. 脱敏与角色无关：科研人员可见明文患者数据，不满足医疗合规

## 2. 设计决策（已确认）

| 决策点 | 结论 |
|--------|------|
| 总体路线 | 在现有 RBAC 上扩展，引入数据范围控制 |
| 角色清单 | 保留现有 6 个 + 新增 `nurse`、`researcher_pi`，共 8 个 |
| 数据范围维度 | 通用枚举 `ALL / DEPT / SELF / PROJECT`（角色级配置）+ RDR 项目成员制 |
| 脱敏 | 规则增加豁免角色维度：豁免角色看明文，其余按规则脱敏 |
| 后端鉴权架构 | 方案 B：服务内 `@RequirePermission` 注解 + AOP + Redis 权限缓存；网关保持纯认证；ArchUnit 兜底防漏加 |
| 医生边界 | 患者导出**免审批**；参与模型审批；可当标注审核员；可管理病种队列 |
| 护士边界 | 可用临床检索；可当标注审核员；导出**需审批**；队列只读 |
| 科研边界 | 维持默认：可脱敏临床检索、PI 管项目、成员项目内只读、导出需 PI 授权 |

**不纳入本期（YAGNI）**：权限的机构级（org）隔离细化、字段级权限（用脱敏豁免替代）、动态权限热更新 UI、API 级限流联动。

## 3. 角色清单（8 个）

| 角色码 | 名称 | data_scope | 定位 |
|--------|------|-----------|------|
| `admin` | 系统管理员 | ALL | 全部权限，系统/角色/配置管理 |
| `doctor` | 临床医生 | DEPT | 本科室患者全视图、临床检索、推理、模型审批、队列管理、标注审核 |
| `nurse` | 护士 | DEPT | 本科室患者视图、临床检索（脱敏更多）、标注、导出需审批 |
| `researcher_pi` | 科研负责人 | PROJECT | 科研项目全生命周期：建项目、定义队列、抽取数据集、导出、分配标注 |
| `researcher` | 科研成员 | PROJECT | 成员项目内只读、参与标注、导出需 PI 授权 |
| `data_admin` | 数据管理员 | ALL | 数据接入/ETL/质控/术语映射/主数据/脱敏规则 |
| `ai_engineer` | AI工程师 | SELF | 模型注册/评估/部署/路由/监控/告警，自己创建的模型与任务 |
| `auditor` | 审计员 | ALL（只读） | 操作/数据访问/系统事件审计、合规报告 |

`data_scope` 语义：

- `ALL`：不过滤
- `DEPT`：患者就诊科室 ∈ 用户 `dept_id`（患者侧匹配 CDR 就诊记录科室）
- `SELF`：记录 `created_by` = 当前用户（模型、推理任务等）
- `PROJECT`：`rdr.r_study_member` 含当前用户（status=ACTIVE）**∪** 本人创建的记录（队列/任务）

## 4. 总体架构：鉴权链路

```
登录: maidc-auth 校验 → 签发 JWT(userId/orgId/roles)
      → 权限集写入 Redis: maidc:auth:perm:{userId}
        { permissions: [...], dataScope: "DEPT", deptId, roles }
        TTL 与 token 生命周期对齐

请求: 前端带 Bearer JWT
  → 网关 AuthFilter: 验签 + 黑名单 → 透传 X-User-Id / X-Org-Id / X-Username（保持现状）
  → 业务服务 UserIdHeaderFilter: 填充 SecurityContext（已有）
  → PermissionAspect 拦截 @RequirePermission:
      ① 功能权限: 权限集包含权限码? 否 → 403 PERMISSION_DENIED
      ② 数据范围: Service 调 DataScopeHelper 拼 WHERE（DEPT/SELF/PROJECT）
      ③ 范围外资源（直接拼 ID）→ 404 + 写审计 data_access 事件
  → 返回前: DesensitizeRoleInterceptor 按角色匹配豁免 → 脱敏/明文

失效: 角色授权 / 角色权限 / 脱敏规则变更 → auth 服务删受影响用户的 Redis key → 懒加载重建
```

JWT 不塞权限集：避免 token 膨胀与角色变更后旧 token 权限残留，权限以 Redis 为唯一真源。

## 5. 数据模型变更

```sql
-- ① 角色增加数据范围
ALTER TABLE system.s_role ADD COLUMN data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF';

-- ② 用户增加科室归属（DEPT 过滤依据，关联主数据机构表）
ALTER TABLE system.s_user ADD COLUMN dept_id BIGINT;

-- ③ 脱敏规则增加豁免角色（逗号分隔，如 'doctor,data_admin'；命中豁免 → 原文）
--    表已存在：cdr.r_desensitize_rule
ALTER TABLE cdr.r_desensitize_rule ADD COLUMN exempt_role_codes TEXT;

-- ④ RDR 项目成员表已存在：rdr.r_study_member（role: PI/CO_PI/RESEARCHER/DATA_MANAGER）
--    新增 PI 授权导出标志（researcher 成员导出数据集需 PI 授予）
ALTER TABLE rdr.r_study_member ADD COLUMN can_export BOOLEAN NOT NULL DEFAULT FALSE;

-- ⑤ 护士 CDR 患者数据导出审批流（轻量，新建）
--    注意：RDR 数据集导出已有 rdr.rdr_export_task 链路（含匿名化级别），复用不重建
CREATE TABLE cdr.c_export_request (
    id            BIGSERIAL PRIMARY KEY,
    requester_id  BIGINT NOT NULL,
    patient_ids   JSONB,          -- 具体患者清单或筛选条件快照
    purpose       VARCHAR(512) NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING/APPROVED/REJECTED
    approver_id   BIGINT,
    approved_at   TIMESTAMP,
    reject_reason VARCHAR(512),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
```

不动的部分：RBAC 5 表结构、JWT 结构、网关 AuthFilter、前端路由过滤机制。

种子数据变更（`docker/init-db/02-system.sql`）：

1. `s_role`：新增 `nurse`（data_scope=DEPT）、`researcher_pi`（PROJECT）；`researcher` 改 data_scope=PROJECT；doctor 改 DEPT；admin/data_admin/auditor 改 ALL；ai_engineer 改 SELF
2. `s_permission`：替换现有 15 条为第 6 节完整清单（约 42 条），菜单型（resource_type=MENU）+ API/按钮型
3. `s_role_permission`：按第 7 节矩阵灌入

## 6. 权限码体系（`模块:资源:动作`，共 9 模块约 42 码）

| 模块 | 权限码 |
|------|--------|
| workspace | `workspace:read` |
| dashboard | `dashboard:overview:read` `dashboard:data:read` `dashboard:model:read` |
| cdr | `cdr:patient:read` `cdr:patient:export` `cdr:search:read` `cdr:cohort:read` `cdr:cohort:manage` `cdr:quality:manage` `cdr:term:manage` `cdr:lineage:read` |
| etl | `etl:pipeline:read` `etl:pipeline:design` `etl:pipeline:execute` `datasource:manage` `sync:manage` `desensitize:manage` |
| rdr | `rdr:project:read` `rdr:project:manage` `rdr:cohort:manage` `rdr:dataset:read` `rdr:dataset:export` `rdr:extraction:manage` `rdr:feature:manage` |
| model | `model:read` `model:register` `model:evaluate` `model:approve` `model:deploy` `model:route:manage` `model:infer` `model:monitor:read` `alert:read` `alert:manage` |
| label | `label:task:read` `label:task:assign` `label:task:work` `label:task:review` |
| audit | `audit:operation:read` `audit:dataaccess:read` `audit:event:read` `audit:compliance:read` |
| masterdata | `masterdata:element:manage` `masterdata:codesystem:manage` `masterdata:mapping:manage` `masterdata:dict:manage` `masterdata:knowledge:read` `masterdata:institution:manage` |
| system | `system:user:manage` `system:role:manage` `system:permission:manage` `system:config:manage` `schedule:task:manage` `message:template:manage` |

## 7. 角色 × 功能权限矩阵（最终版）

✓=有 ◐=有但受限（括号说明） —=无

| 权限组 | admin | doctor | nurse | res_pi | researcher | data_admin | ai_eng | auditor |
|--------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| workspace:read | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| dashboard:overview | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| dashboard:data / model | ✓ | — | — | ◐data | ◐data | ✓data | ✓model | — |
| cdr:patient:read（患者360） | ✓ | ✓ | ✓ | — | — | ✓ | — | — |
| cdr:patient:export | ✓ | ✓免审批 | ✓需审批 | — | — | ✓ | — | — |
| cdr:search:read | ✓ | ✓ | ✓ | ◐脱敏 | ◐脱敏 | ✓ | ◐脱敏 | — |
| cdr:cohort:read | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — | — |
| cdr:cohort:manage | ✓ | ✓ | — | ✓ | — | ✓ | — | — |
| cdr:quality/term:manage | ✓ | — | — | — | — | ✓ | — | — |
| cdr:lineage:read | ✓ | — | — | — | — | ✓ | — | ✓ |
| etl:pipeline:read/design/execute | ✓ | — | — | — | — | ✓ | — | — |
| datasource/sync:manage | ✓ | — | — | — | — | ✓ | — | — |
| desensitize:manage | ✓ | — | — | — | — | ✓ | — | — |
| rdr:project:read | ✓ | — | — | ✓ | ◐成员项目 | ✓ | ✓ | — |
| rdr:project:manage | ✓ | — | — | ✓ | — | — | — | — |
| rdr:cohort:manage | ✓ | — | — | ✓ | ◐项目内 | ✓ | — | — |
| rdr:dataset:read | ✓ | — | — | ◐项目内 | ◐项目内 | ✓ | ◐申请 | — |
| rdr:dataset:export | ✓ | — | — | ✓ | ◐can_export | ✓ | ◐ | — |
| rdr:extraction/feature:manage | ✓ | — | — | ✓ | ◐PI授权 | ✓ | ◐ | — |
| model:read/register/evaluate | ✓ | — | — | — | — | — | ✓ | — |
| model:approve | ✓ | ✓ | — | — | — | — | — | — |
| model:deploy / model:route:manage | ✓ | — | — | — | — | — | ✓ | — |
| model:infer | ✓ | ✓ | — | — | — | — | ✓ | — |
| model:monitor:read / alert:read | ✓ | — | — | — | — | — | ✓ | ✓ |
| alert:manage | ✓ | — | — | — | — | — | ✓ | — |
| label:task:read | ✓ | — | — | ✓ | ✓ | — | ✓ | — |
| label:task:assign | ✓ | — | — | ✓ | — | — | ✓ | — |
| label:task:work | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| label:task:review | ✓ | ✓ | ✓ | ✓ | — | — | ✓ | — |
| audit:operation/dataaccess/event/compliance | ✓ | — | — | — | — | — | — | ✓ |
| masterdata:*:manage（6项） | ✓ | — | — | — | — | ✓ | — | — |
| masterdata:knowledge:read | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| system:user/role/permission/config | ✓ | — | — | — | — | — | — | — |
| schedule:task / message:template:manage | ✓ | — | — | — | — | — | — | — |

### 导出链路（两条，互不混淆）

1. **CDR 患者数据导出**（doctor/nurse，新表 `cdr.c_export_request`）：护士发起（患者清单+用途）→ `data_admin`/`admin` 审批（APPROVED/REJECTED）→ 通过后执行导出，文件落 MinIO，全程写审计。医生导出免审批但同样写审计（谁、导出了哪些患者、用途可选填）。
2. **RDR 数据集导出**（researcher_pi/researcher，复用现有 `rdr.rdr_export_task` 链路，已支持匿名化级别 NONE/BASIC/STRICT/HIPAA_COMPLIANT）：PI 直接导出；researcher 成员需 PI 在项目成员管理中开启 `can_export` 标志。ai_engineer 的 ◐ = 被加入项目成为成员后获得项目内只读。

## 8. 角色 × 脱敏豁免

| 角色 | 豁免策略 |
|------|----------|
| admin | 豁免全部（管理需要，审计兜底） |
| doctor | 豁免大部分（姓名/身份证/手机明文） |
| nurse | 豁免少部分（姓名可见；身份证/手机/住址脱敏） |
| researcher_pi / researcher | **不豁免**（科研只见脱敏数据） |
| data_admin | 豁免（规则维护需对照原文） |
| ai_engineer | 不豁免（训练数据来自 RDR 已脱敏集） |
| auditor | 不豁免（日志不含敏感明文） |

**科研与临床数据通道隔离**：科研人员不持有 `cdr:patient:read`，只能通过 `cdr:search:read`（强制脱敏）与 RDR 项目数据集取数——从权限模型上保证"科研不见明文患者数据"。

## 9. 后端组件设计

| 组件 | 位置 | 职责 |
|------|------|------|
| `@RequirePermission(code)` / 可选 `dataScope` 参数 | `common-security/annotation` | 方法级功能权限声明 |
| `PermissionAspect` | `common-security/aspect` | 取 Redis 权限集校验；miss 则同步调 auth 服务懒加载；无权限抛 403 |
| `PermissionCacheService` | `maidc-auth/service` | 登录构建权限集写 Redis；角色/授权/规则变更删受影响 key |
| `DataScopeHelper` | `common-security` | `applyDeptScope` / `applySelfScope` / `applyProjectScope`，查询前拼 WHERE |
| `DesensitizeRoleInterceptor` | `maidc-data` | 现有脱敏引擎出口加角色豁免判断 |
| `ExportApprovalService` | `maidc-data` | `c_export_request` 审批流 |
| ArchUnit 规则 | 各服务 test | 所有 `@RestController` public 方法必须有 `@RequirePermission` 或显式 `@PermitAll`（健康检查等白名单），CI 强制 |

Controller 改造：8 个服务 50+ Controller 全量加注解，**替换现有零散 `@PreAuthorize`**（统一一套）。网关不动。

## 10. 前端适配（maidc-portal）

1. `asyncRoutes.ts` 全部路由 `meta.permission` 对齐第 6 节权限码
2. `PermissionWrapper` / `hasPermission()` 机制不动，按钮权限码对齐
3. 新增：护士导出申请页 + `data_admin` 审批页
4. 登录用户信息增加 `dataScope`，前端展示范围提示（如"仅显示本科室患者"）
5. 403 统一拦截跳转 403 页

## 11. 错误处理

| 场景 | 行为 |
|------|------|
| 无功能权限 | 403 + `PERMISSION_DENIED` |
| 数据范围外访问（拼 ID 越权） | 404（不暴露资源存在性）+ 审计 `data_access` 事件 |
| 权限缓存 miss | 切面内同步重建（一次性开销） |
| 角色变更后权限残留 | 变更即删 key，下次请求懒加载新权限集 |

## 12. 测试策略

1. **单元测试**：PermissionAspect（有/无权限/缓存 miss）、DataScopeHelper 三种 scope 的 WHERE 拼接、脱敏豁免命中逻辑
2. **集成测试**：8 角色各一个测试用户，按第 7 节矩阵覆盖高危接口（患者读取/导出/部署/审批）——矩阵即用例表
3. **ArchUnit**：Controller 注解全覆盖，进 CI
4. **越权用例**：跨科室患者 ID、他人项目数据集 ID → 断言 404 + 审计落库

## 13. 实施顺序建议（供 writing-plans 展开）

1. DDL + 种子数据（02-system.sql 重写权限部分）
2. common-security 注解 + 切面 + DataScopeHelper + 单测
3. auth 服务 PermissionCacheService + 变更失效
4. 各服务 Controller 全量加注解（可按服务分批：auth → data → model → 其余）
5. maidc-data 脱敏豁免 + 导出审批流
6. 前端权限码对齐 + 导出审批页面
7. ArchUnit 规则 + 集成测试补齐
